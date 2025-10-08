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
 * UNLESS they were explicitly spawned with NoAI via commands.
 */
@Mixin(MobEntity.class)
public abstract class MobEntitySetAiMixin {

    @Inject(method = "setAiDisabled", at = @At("HEAD"), cancellable = true)
    private void theendupdate$blockAiDisable(boolean disabled, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        if (self instanceof ShadowCreakingEntity sc) {
            // If trying to enable AI (disabled=false), check if entity has underlying NoAI flag
            if (!disabled && self.isAiDisabled()) {
                return; // Don't enable AI if entity has NoAI flag
            }
            // If trying to disable AI (disabled=true)
            if (disabled) {
                boolean isMiniOrTiny = (sc instanceof MiniShadowCreakingEntity) || (sc instanceof TinyShadowCreakingEntity);
                boolean allowFreeze = !isMiniOrTiny && sc.getHealth() > sc.getMaxHealth() * 0.5f;
                if (!allowFreeze) {
                    ci.cancel();
                }
            }
        }
    }
}


