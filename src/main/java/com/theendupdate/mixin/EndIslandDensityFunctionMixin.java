package com.theendupdate.mixin;

import com.theendupdate.world.OuterEndLayout;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla End island shapes with our Mirelands islands and Shadowlands continents.
 * Seed capture for OuterEndLayout lives in {@link TheEndUpdateMixin}.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.densityfunction.generator.EndIslandFunction$Sampler")
public class EndIslandDensityFunctionMixin {
    @Inject(method = "sampleValue", at = @At("RETURN"), cancellable = true)
    private void theendupdate$ownIslands(
        SamplerContext context,
        int x,
        int y,
        int z,
        CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue((float) OuterEndLayout.density(x, z, cir.getReturnValueF()));
    }
}
