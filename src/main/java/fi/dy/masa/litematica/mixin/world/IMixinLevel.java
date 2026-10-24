package fi.dy.masa.litematica.mixin.world;

//#if MC>=260100
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Level.class)
public interface IMixinLevel
{
    @Invoker("getEntities")
    LevelEntityGetter<Entity> litematica_getEntityLookup();
}
//#endif
