package com.theendupdate.entity.goal;

import java.util.EnumSet;

import com.theendupdate.registry.ModBlocks;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Causes a ground mob to move away from nearby Ender Chrysanthemum blocks,
 * mirroring piglin repellent behavior around soul fire.
 */
public class AvoidEnderChrysanthemumGoal extends Goal {
    private final PathAwareEntity mob;
    private final EntityNavigation navigation;
    private final int avoidRadius;
    private final double speed;

    private BlockPos nearestChrysanthemum;
    private Vec3d fleeTarget;

    public AvoidEnderChrysanthemumGoal(PathAwareEntity mob, int avoidRadius, double speed) {
        this.mob = mob;
        this.navigation = mob.getNavigation();
        this.avoidRadius = Math.max(1, avoidRadius);
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.mob.getWorld().isClient) return false;
        this.nearestChrysanthemum = findNearestChrysanthemum(this.mob.getBlockPos(), this.avoidRadius);
        if (this.nearestChrysanthemum == null) return false;

        // Compute a target position that moves away from the repellent
        Vec3d from = this.mob.getPos();
        Vec3d threat = Vec3d.ofCenter(this.nearestChrysanthemum);
        Vec3d awayVector = from.subtract(threat);

        Vec3d candidate = NoPenaltyTargeting.findFrom(this.mob, 16, 7, awayVector);
        if (candidate == null) return false;
        this.fleeTarget = candidate;
        return true;
    }

    @Override
    public void start() {
        if (this.fleeTarget != null) {
            this.navigation.startMovingTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, this.speed);
        }
    }

    @Override
    public boolean shouldContinue() {
        if (!this.navigation.isFollowingPath()) return false;
        if (this.nearestChrysanthemum == null) return false;
        // Stop once we're outside the avoidance radius
        double distSq = this.mob.squaredDistanceTo(Vec3d.ofCenter(this.nearestChrysanthemum));
        return distSq < (double) (this.avoidRadius * this.avoidRadius);
    }

    @Override
    public void stop() {
        this.nearestChrysanthemum = null;
        this.fleeTarget = null;
    }

    private BlockPos findNearestChrysanthemum(BlockPos origin, int radius) {
        World world = this.mob.getWorld();
        int r = radius;
        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.iterateOutwards(origin, r, r, r)) {
            if (origin.getSquaredDistance(p) > (long) r * r) continue;
            BlockState state = world.getBlockState(p);
            if (state.isOf(ModBlocks.ENDER_CHRYSANTHEMUM)) {
                double d = origin.getSquaredDistance(p);
                if (d < closestDistSq) {
                    closestDistSq = d;
                    closest = p.toImmutable();
                }
            }
        }
        return closest;
    }
}


