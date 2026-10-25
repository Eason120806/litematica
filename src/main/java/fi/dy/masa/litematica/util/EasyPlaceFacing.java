package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;

/**
 * Easy Place facing fix.
 *
 * The placement state is derived from the player rotation the server was last told about, which
 * is unrelated to the orientation Easy Place wants to place. This resolves a rotation for which
 * the vanilla placement logic produces the wanted state, and delivers it to the server for the
 * duration of the use action. The camera is not moved.
 */
public class EasyPlaceFacing
{
    private static final float[] YAWS    = { 0.0F, 22.5F, 45.0F, 67.5F, 90.0F, 112.5F, 135.0F, 157.5F,
                                             180.0F, 202.5F, 225.0F, 247.5F, 270.0F, 292.5F, 315.0F, 337.5F };
    private static final float[] PITCHES = { 0.0F, 90.0F, -90.0F, 46.0F, -46.0F };

    // 放置期间生效的视角覆盖，供 MixinServerboundMovePlayerPacket_easyPlaceFacing 读取。
    // NAN = 未启用。
    private static float lookYaw = Float.NaN;
    private static float lookPitch = Float.NaN;

    public static float getLookYaw()
    {
        return lookYaw;
    }

    public static float getLookPitch()
    {
        return lookPitch;
    }

    /**
     * Runs the use action with the facing fix applied, restoring everything afterwards.
     */
    public static InteractionResult useItemOn(Minecraft mc, Player player, InteractionHand hand,
                                              BlockHitResult hitResult, BlockState stateSchematic)
    {
        if (Configs.Generic.EASY_PLACE_FACING_FIX.getBooleanValue() == false)
        {
            return mc.gameMode.useItemOn(mc.player, hand, hitResult);
        }

        float[] look = resolveLook(stateSchematic, hitResult.getBlockPos(),
                hitResult.getDirection(), hitResult.getLocation(), hand);

        // 与朝向无关，或当前视角本就能得到目标状态
        if (look == null || isAlreadyLookingAt(player, look[0], look[1]))
        {
            return mc.gameMode.useItemOn(mc.player, hand, hitResult);
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        setLook(look[0], look[1]);
        EntityUtils.setEntityRotations(player, look[0], look[1]);

        try
        {
            // 使用物品包自带朝向，服务端处理它时会把玩家朝向对齐到该角度。
            // 方块物品的 use() 是空实现，不会多放方块；其它物品（如桶）有副作用，故只补方块物品。
            if (player.getItemInHand(hand).getItem() instanceof BlockItem)
            {
                mc.gameMode.useItem(mc.player, hand);
            }

            return mc.gameMode.useItemOn(mc.player, hand, hitResult);
        }
        finally
        {
            // 先解除覆盖再还原，否则还原后的移动包会被改回伪造朝向
            clearLook();
            EntityUtils.setEntityRotations(player, yaw, pitch);
        }
    }

    private static boolean isAlreadyLookingAt(Player player, float yaw, float pitch)
    {
        return Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) < 0.5F &&
               Math.abs(pitch - player.getXRot()) < 0.5F;
    }

    private static void setLook(float yaw, float pitch)
    {
        lookYaw = yaw;
        lookPitch = pitch;
    }

    private static void clearLook()
    {
        lookYaw = Float.NaN;
        lookPitch = Float.NaN;
    }

    private static boolean hasOrientationProperty(BlockState state)
    {
        return state.hasProperty(BlockStateProperties.FACING) ||
               state.hasProperty(BlockStateProperties.FACING_HOPPER) ||
               state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ||
               state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION) ||
               state.hasProperty(BlockStateProperties.AXIS) ||
               state.hasProperty(BlockStateProperties.ROTATION_16);
    }

    private static <T extends Comparable<T>> int compareOrientationProperty(BlockState wanted, BlockState candidate, Property<T> property)
    {
        if (wanted.hasProperty(property) == false)
        {
            return 0;
        }

        if (candidate.hasProperty(property) == false)
        {
            return -1;
        }

        return wanted.getValue(property).equals(candidate.getValue(property)) ? 1 : -1;
    }

    /**
     * A candidate rotation only wins if it reproduces every orientation property
     * of the wanted state, so a mismatch on any of them disqualifies it.
     */
    private static int scoreOrientation(BlockState wanted, BlockState candidate)
    {
        if (wanted.getBlock() != candidate.getBlock())
        {
            return -1;
        }

        int[] results =
        {
                compareOrientationProperty(wanted, candidate, BlockStateProperties.FACING),
                compareOrientationProperty(wanted, candidate, BlockStateProperties.FACING_HOPPER),
                compareOrientationProperty(wanted, candidate, BlockStateProperties.HORIZONTAL_FACING),
                compareOrientationProperty(wanted, candidate, BlockStateProperties.VERTICAL_DIRECTION),
                compareOrientationProperty(wanted, candidate, BlockStateProperties.AXIS),
                compareOrientationProperty(wanted, candidate, BlockStateProperties.ROTATION_16),
        };

        int score = 0;

        for (int result : results)
        {
            if (result < 0)
            {
                return -1;
            }

            score += result;
        }

        return score;
    }

    /**
     * Finds a player rotation for which the vanilla placement logic produces the wanted state.
     * Blocks derive their orientation in different ways (stairs use the looking direction,
     * furnaces and pistons its opposite, signs a 16 step rotation), so the resulting state is
     * compared instead of the properties.
     *
     * @return {yaw, pitch}, or null when the state does not depend on the player rotation
     */
    @Nullable
    public static float[] resolveLook(BlockState wanted, BlockPos pos, Direction side, Vec3 hitPos, InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || wanted.isAir() || hasOrientationProperty(wanted) == false)
        {
            return null;
        }

        Block block = wanted.getBlock();
        BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
        float origYaw = mc.player.getYRot();
        float origPitch = mc.player.getXRot();
        float bestYaw = origYaw;
        float bestPitch = origPitch;
        int bestScore = -1;
        int currentScore = -1;

        try
        {
            BlockState current = block.getStateForPlacement(new BlockPlaceContext(new UseOnContext(mc.player, hand, hitResult)));
            currentScore = current != null ? scoreOrientation(wanted, current) : -1;

            for (float pitch : PITCHES)
            {
                for (float yaw : YAWS)
                {
                    EntityUtils.setEntityRotations(mc.player, yaw, pitch);
                    BlockState candidate = block.getStateForPlacement(new BlockPlaceContext(new UseOnContext(mc.player, hand, hitResult)));

                    if (candidate == null)
                    {
                        continue;
                    }

                    int score = scoreOrientation(wanted, candidate);

                    if (score > bestScore)
                    {
                        bestScore = score;
                        bestYaw = yaw;
                        bestPitch = pitch;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Easy Place: failed to resolve the facing rotation", e);
            return null;
        }
        finally
        {
            EntityUtils.setEntityRotations(mc.player, origYaw, origPitch);
        }

        if (bestScore <= 0)
        {
            return null;
        }

        // 当前视角已能得到目标状态时不必伪造
        return currentScore == bestScore ? new float[] { origYaw, origPitch } : new float[] { bestYaw, bestPitch };
    }
}
