package com.theendupdate.mixin;

import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;
import net.minecraft.entity.mob.CreakingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent vanilla Creaking gaze freeze (AI disable) for our variants when not desired.
 * - Base ShadowCreaking: only allow gaze freeze above half health
 * - Mini/Tiny: never allow gaze freeze
 */
@Mixin(CreakingEntity.class)
public abstract class ShadowCreakingGazeMixin {

    // Vanilla disables AI during tick when looked at. Intercept right after to always re-enable for our variants.
    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void theendupdate$gateGazeFreeze(CallbackInfo ci) {
        CreakingEntity self = (CreakingEntity) (Object) this;
        if (!(self instanceof ShadowCreakingEntity sc)) return;
        // Only unfreeze when weeping should not be active: mini/tiny always, base at <=50% hp
        boolean isMiniOrTiny = (sc instanceof MiniShadowCreakingEntity) || (sc instanceof TinyShadowCreakingEntity);
        boolean weepingActive = !isMiniOrTiny && sc.getHealth() > sc.getMaxHealth() * 0.5f;
        if (!weepingActive) {
            try { sc.setAiDisabled(false); } catch (Throwable ignored) {}
            // Do not force navigation every tick here; let entity.tick handle it to keep animations smooth
        }
    }
}


