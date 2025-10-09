package com.theendupdate.mixin.client;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.CowEntityRenderer;
import net.minecraft.client.render.entity.state.CowEntityRenderState;
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
        if (entity instanceof CowEntityAnimationAccessor accessor) {
            long startTime = accessor.theendupdate$getAnimationStartTime();
            
            if (startTime > 0L) {
                long currentTime = entity.getWorld().getTime();
                long elapsed = currentTime - startTime;
                // Animation is 100 ticks (5 seconds), progress from 0.0 to 1.0
                float animationProgress = MathHelper.clamp((elapsed + tickDelta) / 100.0f, 0.0f, 1.0f);
                
                // Store in the render state via accessor
                if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
                    stateAccessor.theendupdate$setAnimationProgress(animationProgress);
                }
            }
        }
    }
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/CowEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", 
            at = @At("HEAD"))
    private void theendupdate$applyMilkingAnimation(CowEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        com.theendupdate.TemplateMod.LOGGER.info("CowRenderer.render: stateClass={}", state.getClass().getSimpleName());
        
        float animationProgress = 0.0f;
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            animationProgress = stateAccessor.theendupdate$getAnimationProgress();
            com.theendupdate.TemplateMod.LOGGER.info("CowRenderer animation: progress={}", animationProgress);
        } else {
            com.theendupdate.TemplateMod.LOGGER.warn("CowRenderer state NOT accessor! Class: {}", state.getClass().getName());
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
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/CowEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", 
            at = @At("RETURN"))
    private void theendupdate$popMilkingAnimation(CowEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        float animationProgress = 0.0f;
        if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
            animationProgress = stateAccessor.theendupdate$getAnimationProgress();
        }
        
        if (animationProgress > 0.0f && animationProgress < 1.0f) {
            matrices.pop();
        }
    }
}

