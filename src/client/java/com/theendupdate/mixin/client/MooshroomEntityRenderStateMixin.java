package com.theendupdate.mixin.client;

import net.minecraft.client.render.entity.state.MooshroomEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MooshroomEntityRenderState.class)
public abstract class MooshroomEntityRenderStateMixin implements com.theendupdate.accessor.CowRenderStateAnimationAccessor {
    
    @Unique
    private float theendupdate$animationProgress = 0.0f;
    
    @Override
    public float theendupdate$getAnimationProgress() {
        return this.theendupdate$animationProgress;
    }
    
    @Override
    public void theendupdate$setAnimationProgress(float progress) {
        this.theendupdate$animationProgress = progress;
    }
}

