package com.theendupdate.mixin;

import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Globally gate setAiDisabled(true) for our Shadow Creaking variants so they do not freeze from gaze
 * when below half health (base) or at all (mini/tiny).
 */
@Mixin(MobEntity.class)
public abstract class MobEntitySetAiMixin {

    @Inject(method = "setAiDisabled", at = @At("HEAD"), cancellable = true)
    private void theendupdate$blockAiDisable(boolean disabled, CallbackInfo ci) {
        if (!disabled) return; // allow enabling AI
        Object self = this;
        if (self instanceof ShadowCreakingEntity sc) {
            boolean isMiniOrTiny = (sc instanceof MiniShadowCreakingEntity) || (sc instanceof TinyShadowCreakingEntity);
            boolean allowFreeze = !isMiniOrTiny && sc.getHealth() > sc.getMaxHealth() * 0.5f;
            if (!allowFreeze) {
                ci.cancel();
            }
        }
    }
}


