package com.theendupdate.entity.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.theendupdate.registry.ModBlocks;

/**
 * Causes a ground mob to move away from nearby Ender Chrysanthemum blocks,
 * mirroring piglin repellent behavior around soul fire.
 */
public class AvoidEnderChrysanthemumGoal extends Goal {
    private final PathfinderMob mob;
    private final PathNavigation navigation;
    private final int avoidRadius;
    private final double speed;

    private BlockPos nearestChrysanthemum;
    private Vec3 fleeTarget;

    public AvoidEnderChrysanthemumGoal(PathfinderMob mob, int avoidRadius, double speed) {
        this.mob = mob;
        this.navigation = mob.getNavigation();
        this.avoidRadius = Math.max(1, avoidRadius);
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()) return false;
        this.nearestChrysanthemum = findNearestChrysanthemum(this.mob.blockPosition(), this.avoidRadius);
        if (this.nearestChrysanthemum == null) return false;

        Vec3 from = new Vec3(this.mob.getX(), this.mob.getY(), this.mob.getZ());
        Vec3 threat = Vec3.atCenterOf(this.nearestChrysanthemum);
        Vec3 awayVector = from.subtract(threat);

        Vec3 candidate = DefaultRandomPos.getPosAway(this.mob, 16, 7, awayVector);
        if (candidate == null) return false;
        this.fleeTarget = candidate;
        return true;
    }

    @Override
    public void start() {
        if (this.fleeTarget != null) {
            this.navigation.moveTo(this.fleeTarget.x, this.fleeTarget.y, this.fleeTarget.z, this.speed);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.navigation.isInProgress()) return false;
        if (this.nearestChrysanthemum == null) return false;
        double distSq = this.mob.distanceToSqr(Vec3.atCenterOf(this.nearestChrysanthemum));
        return distSq < (double) (this.avoidRadius * this.avoidRadius);
    }

    @Override
    public void stop() {
        this.nearestChrysanthemum = null;
        this.fleeTarget = null;
    }

    private BlockPos findNearestChrysanthemum(BlockPos origin, int radius) {
        Level world = this.mob.level();
        int r = radius;
        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.withinManhattan(origin, r, r, r)) {
            if (origin.distSqr(p) > (long) r * r) continue;
            BlockState state = world.getBlockState(p);
            if (state.is(ModBlocks.ENDER_CHRYSANTHEMUM) || state.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) {
                double d = origin.distSqr(p);
                if (d < closestDistSq) {
                    closestDistSq = d;
                    closest = p.immutable();
                }
            }
        }
        return closest;
    }
}


