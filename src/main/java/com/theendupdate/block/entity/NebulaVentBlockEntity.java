package com.theendupdate.block.entity;

import com.theendupdate.block.NebulaVentBlock;
import com.theendupdate.registry.ModParticles;
import com.theendupdate.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NebulaVentBlockEntity extends BlockEntity {
    private static final int MIN_INITIAL_DELAY = 40;
    private static final int MAX_INITIAL_DELAY = 120;
    private static final int MIN_COOLDOWN_TICKS = 300;
    private static final int MAX_COOLDOWN_TICKS = 600;
    private static final int MIN_BURST_TICKS = 100;
    private static final int MAX_BURST_TICKS = 200;
    // degrees/tick - burst is much faster so the cube visibly revs up with the gas
    private static final float IDLE_SPIN_SPEED = 3.0f;
    private static final float BURST_SPIN_SPEED = 14.0f;
    private static final float SPIN_SPEED_LERP = 0.12f;

    private long nextBurstTick = -1;
    private long burstEndTick = -1;
    private boolean initialized = false;
    private float prevSpinAngle;
    private float spinAngle;
    private float spinSpeed = IDLE_SPIN_SPEED;

    public NebulaVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEBULA_VENT, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, NebulaVentBlockEntity be) {
        if (world instanceof ServerLevel serverWorld) {
            be.serverTick(serverWorld, pos, state);
        } else {
            be.clientTick();
        }
    }

    public boolean isEmittingGas() {
        if (this.level == null || this.burstEndTick == -1) {
            return false;
        }
        if (this.getBlockState().getValue(NebulaVentBlock.WATERLOGGED)) {
            return false;
        }
        return this.level.getGameTime() < this.burstEndTick;
    }

    public float getSpinAngle(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevSpinAngle, this.spinAngle);
    }

    private void clientTick() {
        this.prevSpinAngle = this.spinAngle;
        float targetSpeed = this.isEmittingGas() ? BURST_SPIN_SPEED : IDLE_SPIN_SPEED;
        this.spinSpeed = Mth.lerp(SPIN_SPEED_LERP, this.spinSpeed, targetSpeed);
        this.spinAngle += this.spinSpeed;
    }

    private void serverTick(ServerLevel world, BlockPos pos, BlockState state) {
        if (!this.initialized) {
            this.initialized = true;
            this.nextBurstTick = world.getGameTime() + randomRange(world.getRandom(), MIN_INITIAL_DELAY, MAX_INITIAL_DELAY);
            sync(world, pos, state);
            return;
        }

        long time = world.getGameTime();

            if (this.burstEndTick != -1) {
            if (time >= this.burstEndTick) {
                endBurst(world, pos, state, time);
            } else if (!state.getValue(NebulaVentBlock.WATERLOGGED)) {
                    spawnBurstParticles(world, pos, computeParticleScale(world, pos));
                applyPlayerBoost(world, pos);
            }
            return;
        }

        if (shouldStartBurst(time)) {
            startBurst(world, pos, state, time);
        }
    }

    private double computeParticleScale(ServerLevel world, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double minDistanceSq = Double.MAX_VALUE;
        for (Player player : world.players()) {
            double d = player.distanceToSqr(centerX, pos.getY() + 0.5, centerZ);
            if (d < minDistanceSq) {
                minDistanceSq = d;
            }
        }
        if (minDistanceSq <= (24.0 * 24.0)) {
            return 1.0;
        }
        return 0.25;
    }

    private void spawnBurstParticles(ServerLevel world, BlockPos pos, double scale) {
        RandomSource random = world.getRandom();
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double mouthY = pos.getY() + 0.35;

        // broad spread, reaches several blocks up
        int plumeCount = Math.max(1, (int)(35 * scale));
        world.sendParticles(
            ModParticles.NEBULA_VENT_SMOKE,
            centerX,
            mouthY + 0.8,
            centerZ,
            plumeCount,
            0.35,
            1.1,
            0.35,
            0.03
        );

        // accent jets for extra height/motion variety
        int jetCount = Math.max(1, (int)(12 * scale));
        for (int i = 0; i < jetCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.2 + random.nextDouble() * 0.3;
            double jetX = centerX + Math.cos(angle) * radius;
            double jetZ = centerZ + Math.sin(angle) * radius;
            double jetY = mouthY + 0.5 + random.nextDouble() * 1.8;
            // one extra-strong upward puff per jet point
            world.sendParticles(ModParticles.NEBULA_VENT_SMOKE, jetX, jetY, jetZ, 1, 0.0, 1.6, 0.0, 0.02);
        }
    }

    private void startBurst(ServerLevel world, BlockPos pos, BlockState state, long currentTime) {
        this.burstEndTick = currentTime + randomRange(world.getRandom(), MIN_BURST_TICKS, MAX_BURST_TICKS);
        this.nextBurstTick = -1;
        sync(world, pos, state);
    }

    private void endBurst(ServerLevel world, BlockPos pos, BlockState state, long currentTime) {
        this.burstEndTick = -1;
        this.nextBurstTick = currentTime + randomRange(world.getRandom(), MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
        sync(world, pos, state);
    }

    private void applyPlayerBoost(ServerLevel world, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double plumeRadius = 1.25;
        AABB plumeBox = new AABB(
            centerX - plumeRadius,
            pos.getY(),
            centerZ - plumeRadius,
            centerX + plumeRadius,
            pos.getY() + 8.0,
            centerZ + plumeRadius
        );

        List<Player> players = world.getEntitiesOfClass(Player.class, plumeBox,
            player -> !player.isSpectator());

        if (players.isEmpty()) {
            return;
        }

        RandomSource random = world.getRandom();
        for (Player player : players) {
            Vec3 velocity = player.getDeltaMovement();
            double targetVy = 3.5 + random.nextDouble() * 0.8;
            player.setDeltaMovement(velocity.x, targetVy, velocity.z);
            player.needsSync = true;
            player.fallDistance = 0.0f;
            if (player instanceof ServerPlayer serverPlayer) {
                // force-sync velocity to client immediately, same pattern as Tetherling
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            player.setOnGround(false);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.BLOCKS, 0.8f, 0.8f + random.nextFloat() * 0.2f);
        }
    }

    private boolean shouldStartBurst(long currentTime) {
        return this.burstEndTick == -1 && this.nextBurstTick != -1 && currentTime >= this.nextBurstTick;
    }

    private static int randomRange(RandomSource random, int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private void sync(ServerLevel world, BlockPos pos, BlockState state) {
        setChanged();
        world.getChunkSource().blockChanged(pos);
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("NextBurstTick", this.nextBurstTick);
        output.putLong("BurstEndTick", this.burstEndTick);
        output.putBoolean("Initialized", this.initialized);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.nextBurstTick = input.getLongOr("NextBurstTick", -1L);
        this.burstEndTick = input.getLongOr("BurstEndTick", -1L);
        this.initialized = input.getBooleanOr("Initialized", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}

