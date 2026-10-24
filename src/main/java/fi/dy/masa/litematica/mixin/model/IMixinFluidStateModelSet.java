package fi.dy.masa.litematica.mixin.model;

//#if MC>=260100
import java.util.Map;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FluidStateModelSet.class)
public interface IMixinFluidStateModelSet
{
	@Accessor("modelByFluid")
	Map<Fluid, FluidModel> litematica_getModelByFluid();

	@Accessor("missingModel")
	FluidModel litematica_getMissingModel();
}
//#endif
