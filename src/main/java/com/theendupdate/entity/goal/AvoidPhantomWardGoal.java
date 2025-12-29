package com.theendupdate.entity.goal;

import com.theendupdate.registry.ModStatusEffects;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

/**
 * Causes phantoms to flee from players who have the Phantom Ward effect.
 */
public class AvoidPhantomWardGoal extends Goal {
    private final PhantomEntity phantom;
    private final EntityNavigation navigation;
    private final double fleeDistance;
    private final double slowSpeed;
    private final double fastSpeed;
    
    private PlayerEntity targetPlayer;
    private Path fleePath;

    public AvoidPhantomWardGoal(PhantomEntity phantom, double fleeDistance, double slowSpeed, double fastSpeed) {
        this.phantom = phantom;
        this.navigation = phantom.getNavigation();
        this.fleeDistance = fleeDistance;
        this.slowSpeed = slowSpeed;
        this.fastSpeed = fastSpeed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Find the nearest player with Phantom Ward effect
        List<PlayerEntity> nearbyPlayers = this.phantom.getEntityWorld().getEntitiesByClass(
            PlayerEntity.class,
            this.phantom.getBoundingBox().expand(this.fleeDistance),
            player -> player.isAlive() 
                && !player.isSpectator() 
                && !player.isCreative()
                && player.hasStatusEffect(ModStatusEffects.PHANTOM_WARD)
        );
        
        if (nearbyPlayers.isEmpty()) {
            return false;
        }
        
        // Find the closest one
        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (PlayerEntity player : nearbyPlayers) {
            double dist = this.phantom.squaredDistanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        
        if (closest == null) {
            return false;
        }
        
        this.targetPlayer = closest;
        
        // Calculate flee direction
        Vec3d fleeVector = new Vec3d(
            this.phantom.getX() - closest.getX(),
            this.phantom.getY() - closest.getY(),
            this.phantom.getZ() - closest.getZ()
        ).normalize();
        
        // Try to find a path away from the player
        Vec3d fleePos = new Vec3d(
            this.phantom.getX() + fleeVector.x * 16,
            this.phantom.getY() + fleeVector.y * 16,
            this.phantom.getZ() + fleeVector.z * 16
        );
        
        this.fleePath = this.navigation.findPathTo(fleePos.x, fleePos.y, fleePos.z, 0);
        
        return this.fleePath != null;
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }
        
        // Stop fleeing if player no longer has effect
        if (!this.targetPlayer.hasStatusEffect(ModStatusEffects.PHANTOM_WARD)) {
            return false;
        }
        
        // Stop if we're far enough away
        if (this.phantom.squaredDistanceTo(this.targetPlayer) > this.fleeDistance * this.fleeDistance) {
            return false;
        }
        
        return !this.navigation.isIdle();
    }

    @Override
    public void start() {
        if (this.fleePath != null) {
            this.navigation.startMovingAlong(this.fleePath, this.slowSpeed);
        }
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.fleePath = null;
    }

    @Override
    public void tick() {
        if (this.targetPlayer != null) {
            // Speed up if the player is very close
            double dist = this.phantom.squaredDistanceTo(this.targetPlayer);
            double speed = dist < 49.0 ? this.fastSpeed : this.slowSpeed; // 49 = 7^2
            
            // For flying mobs like phantoms, directly set velocity away from the player
            Vec3d fleeVector = new Vec3d(
                this.phantom.getX() - this.targetPlayer.getX(),
                this.phantom.getY() - this.targetPlayer.getY(),
                this.phantom.getZ() - this.targetPlayer.getZ()
            ).normalize().multiply(speed);
            
            this.phantom.setVelocity(fleeVector);
            
            // Look away from the player
            this.phantom.getLookControl().lookAt(
                this.phantom.getX() + fleeVector.x * 10,
                this.phantom.getY() + fleeVector.y * 10,
                this.phantom.getZ() + fleeVector.z * 10
            );
        }
    }
}

