package com.theendupdate.mixin.client;

import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.EyesEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererHasLabelMixin {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void theendupdate$hideOrbNameplate(Entity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EtherealOrbEntity etherealOrb) {
            cir.setReturnValue(etherealOrb.hasCustomName());
        }
        if (entity instanceof EyesEntity eyes) {
            cir.setReturnValue(eyes.hasCustomName());
        }
    }
}


