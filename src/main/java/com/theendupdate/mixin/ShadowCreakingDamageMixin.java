package com.theendupdate.mixin;

import com.theendupdate.entity.ShadowCreakingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercept damage to Shadow Creaking variants to trigger retaliatory ranged attacks
 * when hit by player projectiles.
 */
@Mixin(CreakingEntity.class)
public abstract class ShadowCreakingDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void theendupdate$onProjectileHit(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        CreakingEntity self = (CreakingEntity) (Object) this;
        
        // Only apply to our Shadow Creaking variants
        if (!(self instanceof ShadowCreakingEntity sc)) return;
        
        try {
            Entity attacker = source.getAttacker();
            
            // Check if damage is from a ranged attack (arrow, trident, etc.)
            boolean isRangedAttack = source.isOf(net.minecraft.entity.damage.DamageTypes.ARROW) 
                || source.isOf(net.minecraft.entity.damage.DamageTypes.TRIDENT)
                || attacker instanceof ProjectileEntity;
            
            if (isRangedAttack && attacker instanceof PlayerEntity player) {
                // Target the player who hit us
                sc.setTarget(player);
                
                // Check if we should fire back (only every 3rd projectile hit)
                if (sc.shouldFireBackAtProjectile()) {
                    // Fire back as retaliation (bypasses cooldown)
                    sc.startRangedBeamAttack(player, true);
                }
                
                // Force aggressive pursuit - teleport if player is far away or out of sight
                sc.forceAggressiveTeleportToTarget(player);
            }
        } catch (Throwable e) {
            com.theendupdate.TemplateMod.LOGGER.error("Error in projectile hit handler", e);
        }
    }
}

