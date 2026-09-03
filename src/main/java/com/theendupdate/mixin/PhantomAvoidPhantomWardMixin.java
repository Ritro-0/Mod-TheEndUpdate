package com.theendupdate.mixin;

import com.theendupdate.registry.ModStatusEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes regular phantoms (NOT King Phantoms) unable to target players with the Phantom Ward effect
 */
@Mixin(Phantom.class)
public class PhantomAvoidPhantomWardMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void theendupdate$clearTargetIfWarded(CallbackInfo ci) {
        Phantom self = (Phantom)(Object)this;

        // king phantoms should not be repelled by the ward
        if (self instanceof com.theendupdate.entity.KingPhantomEntity) {
            return;
        }

        LivingEntity target = self.getTarget();
        if (target instanceof Player player) {
            if (player.hasEffect(ModStatusEffects.PHANTOM_WARD)) {
                self.setTarget(null);
            }
        }
    }
    
    @Inject(
        method = "canAttack(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void theendupdate$preventTargetingWarded(
        ServerLevel level,
        LivingEntity target,
        TargetingConditions conditions,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Phantom self = (Phantom) (Object) this;

        if (self instanceof com.theendupdate.entity.KingPhantomEntity) {
            return;
        }

        if (target instanceof Player player && player.hasEffect(ModStatusEffects.PHANTOM_WARD)) {
            cir.setReturnValue(false);
        }
    }
}

