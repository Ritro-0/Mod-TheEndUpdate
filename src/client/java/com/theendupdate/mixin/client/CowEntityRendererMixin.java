package com.theendupdate.mixin.client;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.cow.Cow;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(CowRenderer.class)
public abstract class CowEntityRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private boolean theendupdate$didPushMilkingPose;

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/animal/cow/Cow;Lnet/minecraft/client/renderer/entity/state/CowRenderState;F)V",
        at = @At("TAIL"))
    private void theendupdate$trackAnimationProgress(Cow entity, CowRenderState state, float tickDelta, CallbackInfo ci) {
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

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/CowRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"))
    private void theendupdate$applyMilkingAnimation(CowRenderState state, PoseStack matrices, SubmitNodeCollector renderCommandQueue,
        CameraRenderState cameraState, CallbackInfo ci) {
        theendupdate$didPushMilkingPose = false;
        float animationProgress = 0.0f;
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            animationProgress = stateAccessor.theendupdate$getAnimationProgress();
        }

        if (animationProgress > 0.0f && animationProgress < 1.0f) {
            float progress = animationProgress;

            float rotationAngle;
            if (progress < 0.2f) {
                rotationAngle = 0.0f;
            } else if (progress < 0.8f) {
                float phaseProgress = (progress - 0.2f) / 0.6f;
                float rotations = 3.0f;
                float accelerated = (float) (Math.sin(phaseProgress * Math.PI * rotations * 2) * 0.3f + phaseProgress);
                rotationAngle = accelerated * 360.0f * rotations;
            } else {
                float phaseProgress = (progress - 0.8f) / 0.2f;
                float easeOut = 1.0f - (float) Math.pow(1.0f - phaseProgress, 3);
                rotationAngle = (3.0f * 360.0f) + (360.0f - (360.0f * easeOut));
            }

            matrices.pushPose();
            theendupdate$didPushMilkingPose = true;
            matrices.translate(0.0f, 0.9f, 0.0f);
            matrices.mulPose(new Quaternionf().rotationX(rotationAngle * Mth.DEG_TO_RAD));
            matrices.translate(0.0f, -0.9f, 0.0f);
        }
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/CowRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("RETURN"))
    private void theendupdate$popMilkingAnimation(CowRenderState state, PoseStack matrices, SubmitNodeCollector renderCommandQueue,
        CameraRenderState cameraState, CallbackInfo ci) {
        if (theendupdate$didPushMilkingPose) {
            matrices.popPose();
            theendupdate$didPushMilkingPose = false;
        }
    }
}
