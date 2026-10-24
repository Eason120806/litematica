package fi.dy.masa.litematica.util.invoker;

//#if MC>=260200
import net.minecraft.world.entity.Entity;

public interface IEntityHitboxDebugRendererInvoker
{
	void litematica$addEntityHitbox(Entity entity, float partialTicks, boolean serverEntity);
}
//#endif
