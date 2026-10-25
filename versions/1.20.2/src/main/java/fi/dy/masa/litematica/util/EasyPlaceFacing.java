package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

    /**
     * Runs the use action with the facing fix applied, restoring everything afterwards.
     */
    public static ActionResult useItemOn(MinecraftClient mc, PlayerEntity player, Hand hand,
                                         BlockHitResult hitResult, BlockState stateSchematic)
    {
        if (Configs.Generic.EASY_PLACE_FACING_FIX.getBooleanValue() == false)
        {
            return mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        }

        float[] look = resolveLook(stateSchematic, hitResult.getBlockPos(),
                hitResult.getSide(), hitResult.getPos(), hand);

        // 与朝向无关，或当前视角本就能得到目标状态
        if (look == null || isAlreadyLookingAt(player, look[0], look[1]))
        {
            return mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        }

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        EntityUtils.setEntityRotations(player, look[0], look[1]);

        try
        {
            // 使用物品包自带朝向，服务端处理它时会把玩家朝向对齐到该角度。
            // 方块物品的 use() 是空实现，不会多放方块；其它物品（如桶）有副作用，故只补方块物品。
            if (player.getStackInHand(hand).getItem() instanceof BlockItem)
            {
                mc.interactionManager.interactItem(mc.player, hand);
            }

            return mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        }
        finally
        {
            EntityUtils.setEntityRotations(player, yaw, pitch);
        }
    }

    private static boolean isAlreadyLookingAt(PlayerEntity player, float yaw, float pitch)
    {
        return Math.abs(MathHelper.wrapDegrees(yaw - player.getYaw())) < 0.5F &&
               Math.abs(pitch - player.getPitch()) < 0.5F;
    }

    private static boolean hasOrientationProperty(BlockState state)
    {
        return state.contains(Properties.FACING) ||
               state.contains(Properties.HOPPER_FACING) ||
               state.contains(Properties.HORIZONTAL_FACING) ||
               state.contains(Properties.VERTICAL_DIRECTION) ||
               state.contains(Properties.AXIS) ||
               state.contains(Properties.ROTATION);
    }

    private static <T extends Comparable<T>> int compareOrientationProperty(BlockState wanted, BlockState candidate, Property<T> property)
    {
        if (wanted.contains(property) == false)
        {
            return 0;
        }

        if (candidate.contains(property) == false)
        {
            return -1;
        }

        return wanted.get(property).equals(candidate.get(property)) ? 1 : -1;
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
                compareOrientationProperty(wanted, candidate, Properties.FACING),
                compareOrientationProperty(wanted, candidate, Properties.HOPPER_FACING),
                compareOrientationProperty(wanted, candidate, Properties.HORIZONTAL_FACING),
                compareOrientationProperty(wanted, candidate, Properties.VERTICAL_DIRECTION),
                compareOrientationProperty(wanted, candidate, Properties.AXIS),
                compareOrientationProperty(wanted, candidate, Properties.ROTATION),
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
    public static float[] resolveLook(BlockState wanted, BlockPos pos, Direction side, Vec3d hitPos, Hand hand)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null || wanted.isAir() || hasOrientationProperty(wanted) == false)
        {
            return null;
        }

        Block block = wanted.getBlock();
        BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
        float origYaw = mc.player.getYaw();
        float origPitch = mc.player.getPitch();
        float bestYaw = origYaw;
        float bestPitch = origPitch;
        int bestScore = -1;
        int currentScore = -1;

        try
        {
            BlockState current = block.getPlacementState(new ItemPlacementContext(new ItemUsageContext(mc.player, hand, hitResult)));
            currentScore = current != null ? scoreOrientation(wanted, current) : -1;

            for (float pitch : PITCHES)
            {
                for (float yaw : YAWS)
                {
                    EntityUtils.setEntityRotations(mc.player, yaw, pitch);
                    BlockState candidate = block.getPlacementState(new ItemPlacementContext(new ItemUsageContext(mc.player, hand, hitResult)));

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
            Litematica.logger.warn("Easy Place: failed to resolve the facing rotation", e);
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
