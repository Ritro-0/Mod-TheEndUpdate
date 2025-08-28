package com.theendupdate.mixin.client;

import com.theendupdate.entity.EtherealOrbEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererHasLabelMixin {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void theendupdate$hideOrbNameplate(Entity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EtherealOrbEntity orb) {
            // Only show label if truly custom named
            cir.setReturnValue(orb.hasCustomName());
        }
    }
}


