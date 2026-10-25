package fi.dy.masa.litematica.mixin.easyplace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import fi.dy.masa.litematica.util.EasyPlaceFacing;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Easy Place facing fix, packet side: while a placement is in flight, outgoing movement packets
 * carry the overridden rotation, otherwise the client's own per-tick packet would overwrite it
 * before the server gets to the use packet. Without an override the arguments are untouched.
 */
@Mixin(value = ServerboundMovePlayerPacket.class, priority = 1010)
public class MixinServerboundMovePlayerPacket_easyPlaceFacing
{
    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static float litematica_overrideYaw(float yaw)
    {
        float override = EasyPlaceFacing.getLookYaw();

        return Float.isNaN(override) ? yaw : override;
    }

    @ModifyVariable(method = "<init>(DDDFFZZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static float litematica_overridePitch(float pitch)
    {
        float override = EasyPlaceFacing.getLookPitch();

        return Float.isNaN(override) ? pitch : override;
    }
}
