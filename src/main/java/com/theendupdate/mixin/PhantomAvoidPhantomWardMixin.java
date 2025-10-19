package com.theendupdate.mixin;

import com.theendupdate.registry.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes regular phantoms (NOT King Phantoms) unable to target players with the Phantom Ward effect
 */
@Mixin(PhantomEntity.class)
public class PhantomAvoidPhantomWardMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void theendupdate$clearTargetIfWarded(CallbackInfo ci) {
        PhantomEntity self = (PhantomEntity)(Object)this;
        
        // Skip if this is a King Phantom (King Phantoms should NOT be repelled)
        if (self instanceof com.theendupdate.entity.KingPhantomEntity) {
            return;
        }
        
        // Check if current target has Phantom Ward effect
        LivingEntity target = self.getTarget();
        if (target instanceof PlayerEntity player) {
            if (player.hasStatusEffect(ModStatusEffects.PHANTOM_WARD)) {
                // Clear the target - phantom will stop attacking
                self.setTarget(null);
            }
        }
    }
    
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void theendupdate$preventTargetingWarded(net.minecraft.entity.EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        PhantomEntity self = (PhantomEntity)(Object)this;
        
        // Skip if this is a King Phantom (King Phantoms should NOT be repelled)
        if (self instanceof com.theendupdate.entity.KingPhantomEntity) {
            return;
        }
        
        // If trying to target a player, check if they have Phantom Ward
        // This prevents targeting players with the effect
        LivingEntity currentTarget = self.getTarget();
        if (currentTarget instanceof PlayerEntity player) {
            if (player.hasStatusEffect(ModStatusEffects.PHANTOM_WARD)) {
                cir.setReturnValue(false);
            }
        }
    }
}

