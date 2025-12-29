package com.theendupdate.block.entity;

import com.theendupdate.block.NebulaVentBlock;
import com.theendupdate.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class NebulaVentBlockEntity extends BlockEntity {
    private static final int MIN_INITIAL_DELAY = 40;
    private static final int MAX_INITIAL_DELAY = 120;
    private static final int MIN_COOLDOWN_TICKS = 300;
    private static final int MAX_COOLDOWN_TICKS = 600;
    private static final int MIN_BURST_TICKS = 100;
    private static final int MAX_BURST_TICKS = 200;

    private long nextBurstTick = -1;
    private long burstEndTick = -1;
    private boolean initialized = false;

    public NebulaVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEBULA_VENT, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, NebulaVentBlockEntity be) {
        if (world instanceof ServerWorld serverWorld) {
            be.serverTick(serverWorld, pos, state);
        }
    }

    private void serverTick(ServerWorld world, BlockPos pos, BlockState state) {
        if (!this.initialized) {
            this.initialized = true;
            this.nextBurstTick = world.getTime() + randomRange(world.random, MIN_INITIAL_DELAY, MAX_INITIAL_DELAY);
            sync(world, pos, state);
            return;
        }

        long time = world.getTime();

            if (this.burstEndTick != -1) {
            if (time >= this.burstEndTick) {
                endBurst(world, pos, state, time);
            } else if (!state.get(NebulaVentBlock.WATERLOGGED)) {
                    spawnBurstParticles(world, pos, computeParticleScale(world, pos));
                applyPlayerBoost(world, pos);
            }
            return;
        }

        if (shouldStartBurst(time)) {
            startBurst(world, pos, state, time);
        }
    }

    private double computeParticleScale(ServerWorld world, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double minDistanceSq = Double.MAX_VALUE;
        for (PlayerEntity player : world.getPlayers()) {
            double d = player.squaredDistanceTo(centerX, pos.getY() + 0.5, centerZ);
            if (d < minDistanceSq) {
                minDistanceSq = d;
            }
        }
        if (minDistanceSq <= (24.0 * 24.0)) {
            return 1.0;
        }
        return 0.25;
    }

    private void spawnBurstParticles(ServerWorld world, BlockPos pos, double scale) {
        Random random = world.random;
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double mouthY = pos.getY() + 1.95;

        // Broad plume spread that reaches several blocks up
        int plumeCount = Math.max(1, (int)(70 * scale));
        world.spawnParticles(
            ParticleTypes.FALLING_OBSIDIAN_TEAR,
            centerX,
            mouthY + 0.8,
            centerZ,
            plumeCount,
            0.45,
            2.0,
            0.45,
            0.02
        );

        // Accent jets to give the burst more height and motion variety
        int jetCount = Math.max(1, (int)(25 * scale));
        for (int i = 0; i < jetCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.2 + random.nextDouble() * 0.3;
            double jetX = centerX + Math.cos(angle) * radius;
            double jetZ = centerZ + Math.sin(angle) * radius;
            double jetY = mouthY + 0.5 + random.nextDouble() * 1.8;
            world.spawnParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, jetX, jetY, jetZ, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void startBurst(ServerWorld world, BlockPos pos, BlockState state, long currentTime) {
        this.burstEndTick = currentTime + randomRange(world.random, MIN_BURST_TICKS, MAX_BURST_TICKS);
        this.nextBurstTick = -1;
        sync(world, pos, state);
    }

    private void endBurst(ServerWorld world, BlockPos pos, BlockState state, long currentTime) {
        this.burstEndTick = -1;
        this.nextBurstTick = currentTime + randomRange(world.random, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
        sync(world, pos, state);
    }

    private void applyPlayerBoost(ServerWorld world, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double plumeRadius = 1.25;
        Box plumeBox = new Box(
            centerX - plumeRadius,
            pos.getY(),
            centerZ - plumeRadius,
            centerX + plumeRadius,
            pos.getY() + 6.0,
            centerZ + plumeRadius
        );

        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, plumeBox,
            player -> !player.isSpectator());

        if (players.isEmpty()) {
            return;
        }

        Random random = world.random;
        for (PlayerEntity player : players) {
            Vec3d velocity = player.getVelocity();
            double targetVy = 3.5 + random.nextDouble() * 0.8;
            player.setVelocity(velocity.x, targetVy, velocity.z);
            player.velocityDirty = true;
            player.fallDistance = 0.0f;
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.BLOCKS, 0.8f, 0.8f + random.nextFloat() * 0.2f);
        }
    }

    private boolean shouldStartBurst(long currentTime) {
        return this.burstEndTick == -1 && this.nextBurstTick != -1 && currentTime >= this.nextBurstTick;
    }

    private static int randomRange(Random random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private void sync(ServerWorld world, BlockPos pos, BlockState state) {
        markDirty();
        world.getChunkManager().markForUpdate(pos);
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
    }

}

