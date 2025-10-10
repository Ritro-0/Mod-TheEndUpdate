package com.theendupdate.mixin.client;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.CowEntityRenderer;
import net.minecraft.client.render.entity.state.CowEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CowEntityRenderer.class)
public abstract class CowEntityRendererMixin {
    
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/passive/CowEntity;Lnet/minecraft/client/render/entity/state/CowEntityRenderState;F)V", 
            at = @At("TAIL"))
    private void theendupdate$trackAnimationProgress(CowEntity entity, CowEntityRenderState state, float tickDelta, CallbackInfo ci) {
        float animationProgress = 0.0f;
        
        if (entity instanceof CowEntityAnimationAccessor accessor) {
            long startTime = accessor.theendupdate$getAnimationStartTime();
            
            if (startTime > 0L) {
                long currentTime = entity.getEntityWorld().getTime();
                long elapsed = currentTime - startTime;
                
                // Animation is 100 ticks (5 seconds), progress from 0.0 to 1.0
                // Automatically stop animation after it completes
                if (elapsed >= 100L) {
                    animationProgress = 0.0f;
                    // Clear the start time on the server to prevent persistence
                    if (!entity.getEntityWorld().isClient()) {
                        accessor.theendupdate$setAnimationStartTime(0L);
                    }
                } else {
                    animationProgress = MathHelper.clamp((elapsed + tickDelta) / 100.0f, 0.0f, 1.0f);
                }
            }
        }
        
        // Always set animation progress (even if 0) to prevent render state reuse issues
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            stateAccessor.theendupdate$setAnimationProgress(animationProgress);
        }
    }
    
    @Inject(method = "render", 
            at = @At("HEAD"))
    private void theendupdate$applyMilkingAnimation(CowEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue renderCommandQueue, CameraRenderState cameraState, CallbackInfo ci) {
        float animationProgress = 0.0f;
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            animationProgress = stateAccessor.theendupdate$getAnimationProgress();
        }
        
        if (animationProgress > 0.0f && animationProgress < 1.0f) {
            float progress = animationProgress;
            
            float rotationAngle = 0.0f;
            
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
            
            matrices.push();
            matrices.translate(0.0f, 0.9f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationAngle));
            matrices.translate(0.0f, -0.9f, 0.0f);
        }
    }
    
    @Inject(method = "render", 
            at = @At("RETURN"))
    private void theendupdate$popMilkingAnimation(CowEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue renderCommandQueue, CameraRenderState cameraState, CallbackInfo ci) {
        float animationProgress = 0.0f;
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            animationProgress = stateAccessor.theendupdate$getAnimationProgress();
        }
        
        if (animationProgress > 0.0f && animationProgress < 1.0f) {
            matrices.pop();
        }
    }
}

