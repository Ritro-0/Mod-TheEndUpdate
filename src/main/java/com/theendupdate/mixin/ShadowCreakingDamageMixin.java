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
            
            // Check if damage is from a projectile (arrow, trident, firework, etc.)
            if (attacker instanceof ProjectileEntity proj) {
                Entity owner = proj.getOwner();
                
                com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking hit by projectile! Owner: {}", owner != null ? owner.getName().getString() : "null");
                
                if (owner instanceof PlayerEntity player) {
                    com.theendupdate.TemplateMod.LOGGER.info("Firing retaliatory ranged attack at player!");
                    
                    // Target the player who hit us
                    sc.setTarget(player);
                    
                    // Fire back immediately (method checks its own conditions and cooldown)
                    sc.startRangedBeamAttack(player);
                }
            }
        } catch (Throwable e) {
            com.theendupdate.TemplateMod.LOGGER.error("Error in projectile hit handler", e);
        }
    }
}

