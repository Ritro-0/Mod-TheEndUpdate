package com.theendupdate.mixin.client;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MushroomCowRenderer.class)
public abstract class MooshroomEntityRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/animal/cow/MushroomCow;Lnet/minecraft/client/renderer/entity/state/MushroomCowRenderState;F)V",
        at = @At("TAIL"))
    private void theendupdate$trackAnimationProgress(MushroomCow entity, MushroomCowRenderState state, float tickDelta, CallbackInfo ci) {
        float animationProgress = 0.0f;

        if (entity instanceof CowEntityAnimationAccessor accessor) {
            long startTime = accessor.theendupdate$getAnimationStartTime();

            if (startTime > 0L) {
                long currentTime = entity.level().getGameTime();
                long elapsed = currentTime - startTime;

                if (elapsed >= 100L) {
                    animationProgress = 0.0f;
                    if (!entity.level().isClientSide()) {
                        accessor.theendupdate$setAnimationStartTime(0L);
                    }
                } else {
                    animationProgress = Mth.clamp((elapsed + tickDelta) / 100.0f, 0.0f, 1.0f);
                }
            }
        }

        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            stateAccessor.theendupdate$setAnimationProgress(animationProgress);
        }
    }
}
