package com.theendupdate.mixin;

import com.theendupdate.world.OuterEndLayout;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla End island shapes with our Mirelands islands and Shadowlands continents.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class EndIslandDensityFunctionMixin {
    @Inject(method = "<init>(J)V", at = @At("TAIL"))
    private void theendupdate$captureSeed(long seed, CallbackInfo ci) {
        OuterEndLayout.setSeed(seed);
    }

    @Inject(
        method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D",
        at = @At("RETURN"),
        cancellable = true
    )
    private void theendupdate$ownIslands(
        DensityFunction.FunctionContext context,
        CallbackInfoReturnable<Double> cir
    ) {
        cir.setReturnValue(OuterEndLayout.density(context.blockX(), context.blockZ(), cir.getReturnValueD()));
    }
}
