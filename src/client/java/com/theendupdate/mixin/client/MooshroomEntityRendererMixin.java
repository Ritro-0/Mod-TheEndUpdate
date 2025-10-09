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
        if (entity instanceof CowEntityAnimationAccessor accessor) {
            long startTime = accessor.theendupdate$getAnimationStartTime();
            
            if (startTime > 0L) {
                long currentTime = entity.getWorld().getTime();
                long elapsed = currentTime - startTime;
                // Animation is 100 ticks (5 seconds), progress from 0.0 to 1.0
                float animationProgress = MathHelper.clamp((elapsed + tickDelta) / 100.0f, 0.0f, 1.0f);
                
                com.theendupdate.TemplateMod.LOGGER.info("Mooshroom updateRenderState: startTime={}, progress={}", startTime, animationProgress);
                
                // Store in the render state via accessor
                if (state instanceof com.theendupdate.accessor.CowRenderStateAnimationAccessor stateAccessor) {
                    stateAccessor.theendupdate$setAnimationProgress(animationProgress);
                    com.theendupdate.TemplateMod.LOGGER.info("Mooshroom animation stored in state!");
                } else {
                    com.theendupdate.TemplateMod.LOGGER.warn("Mooshroom state NOT an accessor!");
                }
            }
        }
    }
    
    // NOTE: Render animation is handled by CowEntityRendererMixin.
    // MooshroomEntityRenderer extends CowEntityRenderer, so when it calls super.render(),
    // the CowEntityRendererMixin will apply the animation transformations.
}

