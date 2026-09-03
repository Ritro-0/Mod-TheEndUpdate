package com.theendupdate.entity.goal;

import com.theendupdate.registry.ModStatusEffects;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Causes phantoms to flee from players who have the Phantom Ward effect.
 */
public class AvoidPhantomWardGoal extends Goal {
    private final Phantom phantom;
    private final PathNavigation navigation;
    private final double fleeDistance;
    private final double slowSpeed;
    private final double fastSpeed;
    
    private Player targetPlayer;
    private Path fleePath;

    public AvoidPhantomWardGoal(Phantom phantom, double fleeDistance, double slowSpeed, double fastSpeed) {
        this.phantom = phantom;
        this.navigation = phantom.getNavigation();
        this.fleeDistance = fleeDistance;
        this.slowSpeed = slowSpeed;
        this.fastSpeed = fastSpeed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        List<Player> nearbyPlayers = this.phantom.level().getEntitiesOfClass(
            Player.class,
            this.phantom.getBoundingBox().inflate(this.fleeDistance),
            player -> player.isAlive() 
                && !player.isSpectator() 
                && !player.isCreative()
                && player.hasEffect(ModStatusEffects.PHANTOM_WARD)
        );
        
        if (nearbyPlayers.isEmpty()) {
            return false;
        }
        
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (Player player : nearbyPlayers) {
            double dist = this.phantom.distanceToSqr(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        
        if (closest == null) {
            return false;
        }
        
        this.targetPlayer = closest;
        
        Vec3 fleeVector = new Vec3(
            this.phantom.getX() - closest.getX(),
            this.phantom.getY() - closest.getY(),
            this.phantom.getZ() - closest.getZ()
        ).normalize();
        
        Vec3 fleePos = new Vec3(
            this.phantom.getX() + fleeVector.x * 16,
            this.phantom.getY() + fleeVector.y * 16,
            this.phantom.getZ() + fleeVector.z * 16
        );
        
        this.fleePath = this.navigation.createPath(fleePos.x, fleePos.y, fleePos.z, 0);
        
        return this.fleePath != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }
        
        if (!this.targetPlayer.hasEffect(ModStatusEffects.PHANTOM_WARD)) {
            return false;
        }
        
        if (this.phantom.distanceToSqr(this.targetPlayer) > this.fleeDistance * this.fleeDistance) {
            return false;
        }
        
        return !this.navigation.isDone();
    }

    @Override
    public void start() {
        if (this.fleePath != null) {
            this.navigation.moveTo(this.fleePath, this.slowSpeed);
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
            double dist = this.phantom.distanceToSqr(this.targetPlayer);
            double speed = dist < 49.0 ? this.fastSpeed : this.slowSpeed; // 7 blocks squared
            
            // set velocity directly rather than pathing, since phantoms fly
            Vec3 fleeVector = new Vec3(
                this.phantom.getX() - this.targetPlayer.getX(),
                this.phantom.getY() - this.targetPlayer.getY(),
                this.phantom.getZ() - this.targetPlayer.getZ()
            ).normalize().scale(speed);
            
            this.phantom.setDeltaMovement(fleeVector);
            
            this.phantom.getLookControl().setLookAt(
                this.phantom.getX() + fleeVector.x * 10,
                this.phantom.getY() + fleeVector.y * 10,
                this.phantom.getZ() + fleeVector.z * 10
            );
        }
    }
}

