package fi.dy.masa.litematica.mixin.client;

//#if MC>=260200
import net.minecraft.util.profiling.ActiveProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ActiveProfiler.class)
public interface IMixinActiveProfiler
{
    @Accessor("started")
    boolean litematica_isStarted();
}
//#endif
