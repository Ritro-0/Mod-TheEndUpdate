package com.theendupdate.mixin.client;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.client.render.entity.MooshroomEntityRenderer;
import net.minecraft.client.render.entity.state.MooshroomEntityRenderState;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MooshroomEntityRenderer.class)
public abstract class MooshroomEntityRendererMixin {
    
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void theendupdate$trackAnimationProgress(MooshroomEntity entity, MooshroomEntityRenderState state, float tickDelta, CallbackInfo ci) {
        float animationProgress = 0.0f;
        
        if (entity instanceof CowEntityAnimationAccessor accessor) {
            long startTime = accessor.theendupdate$getAnimationStartTime();
            
            if (startTime > 0L) {
                long currentTime = entity.getWorld().getTime();
                long elapsed = currentTime - startTime;
                
                // Animation is 100 ticks (5 seconds), progress from 0.0 to 1.0
                // Automatically stop animation after it completes
                if (elapsed >= 100L) {
                    animationProgress = 0.0f;
                    // Clear the start time on the server to prevent persistence
                    if (!entity.getWorld().isClient) {
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
    
    // NOTE: Render animation is handled by CowEntityRendererMixin.
    // MooshroomEntityRenderer extends CowEntityRenderer, so when it calls super.render(),
    // the CowEntityRendererMixin will apply the animation transformations.
}

