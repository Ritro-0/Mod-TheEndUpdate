package com.theendupdate.mixin;

import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityNavigation.class)
public abstract class EntityNavigationStopMixin {

    @Shadow protected MobEntity entity;

    @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
    private void theendupdate$allowPathingWhileLookedAt(CallbackInfo ci) {
        if (this.entity instanceof ShadowCreakingEntity sc) {
            boolean isMiniOrTiny = (sc instanceof MiniShadowCreakingEntity) || (sc instanceof TinyShadowCreakingEntity);
            boolean weepingActive = !isMiniOrTiny && sc.getHealth() > sc.getMaxHealth() * 0.5f;
            if (!weepingActive) {
                // Cancel vanilla stop() so navigation continues and animations play while looked at
                ci.cancel();
            }
        }
    }
}


