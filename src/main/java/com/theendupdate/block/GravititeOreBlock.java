package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Gravitite Ore Block with explicit piston behavior.
 * A blast-resistant ore block that can be pushed/pulled by pistons.
 */
public class GravititeOreBlock extends Block {
    // same cadence the player magnet uses
    private static final int PULL_INTERVAL = 7;
    private static final int PIECES = 4;

    public GravititeOreBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        return PushReaction.PUSH_PULL; // pushable/pullable, like other ores
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (!world.isClientSide()) {
            world.scheduleTick(pos, this, PULL_INTERVAL);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        pullItems(world, pos);
        world.scheduleTick(pos, this, PULL_INTERVAL);
    }

    private static void pullItems(ServerLevel world, BlockPos pos) {
        int range = 8;
        AABB box = new AABB(pos).inflate(range);
        var items = world.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.isNoGravity());
        if (items.isEmpty()) return;

        Vec3 center = Vec3.atCenterOf(pos);
        double lerpFactor = Math.min(1.0, 0.08 * PULL_INTERVAL);
        double targetSpeed = 0.16 + 0.02 * PIECES;
        double maxSpeed = 0.35 + 0.05 * PIECES;
        double maxDistanceSq = range * range * 4.0;

        for (ItemEntity item : items) {
            Vec3 itemPos = item.position();
            Vec3 diff = center.subtract(itemPos);
            double distSq = diff.lengthSqr();
            if (distSq < 1.0e-4 || distSq > maxDistanceSq) continue;

            double dist = Math.sqrt(distSq);
            Vec3 dir = diff.normalize().add(0.0, 0.15 / Math.max(1.0, dist), 0.0).normalize();
            Vec3 newVel = item.getDeltaMovement().lerp(dir.scale(targetSpeed), lerpFactor);

            if (newVel.lengthSqr() > maxSpeed * maxSpeed) {
                newVel = newVel.normalize().scale(maxSpeed);
            }
            newVel = newVel.scale(0.96);

            item.setDeltaMovement(newVel);
            item.needsSync = true;
            item.tickCount = 0; // age reset so they don't poof while being dragged in
        }
    }
}
