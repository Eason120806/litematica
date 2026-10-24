package fi.dy.masa.litematica.mixin.network;

//#if MC>=260100
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface IMixinClientboundSectionBlocksUpdatePacket
{
    @Accessor("sectionPos")
    SectionPos litematica_getSection();
}
//#endif
