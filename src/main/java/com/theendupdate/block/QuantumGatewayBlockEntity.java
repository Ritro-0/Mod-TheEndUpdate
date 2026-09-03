package com.theendupdate.block;

import com.theendupdate.screen.GatewayScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class QuantumGatewayBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MOVING_SPARKLES = 2;
    private static final float BOB_SPEED = 0.08f;
    private static final float BOB_AMOUNT = 0.5f / 16.0f;
    private static final float MAX_SPIN_SPEED = 5.5f;
    private static final float SPIN_LERP = 0.08f;

    public final SimpleContainer inventory = new SimpleContainer(3);

    private final RandomSource sparkleRandom;
    private float bobTime;
    private final float[] prevRotX = new float[MOVING_SPARKLES];
    private final float[] prevRotY = new float[MOVING_SPARKLES];
    private final float[] prevRotZ = new float[MOVING_SPARKLES];
    private final float[] rotX = new float[MOVING_SPARKLES];
    private final float[] rotY = new float[MOVING_SPARKLES];
    private final float[] rotZ = new float[MOVING_SPARKLES];
    private final float[] velX = new float[MOVING_SPARKLES];
    private final float[] velY = new float[MOVING_SPARKLES];
    private final float[] velZ = new float[MOVING_SPARKLES];
    private final float[] targetVelX = new float[MOVING_SPARKLES];
    private final float[] targetVelY = new float[MOVING_SPARKLES];
    private final float[] targetVelZ = new float[MOVING_SPARKLES];
    private final int[] retargetTicks = new int[MOVING_SPARKLES];
    private boolean sparklesInitialized;

    public QuantumGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(com.theendupdate.registry.ModBlockEntities.QUANTUM_GATEWAY, pos, state);
        this.sparkleRandom = RandomSource.create(pos.asLong() ^ 0x51EDL);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.repair");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new GatewayScreenHandler(syncId, playerInventory, this.inventory, this.getBlockPos());
    }

    public static void tick(Level world, BlockPos pos, BlockState state, QuantumGatewayBlockEntity be) {
        if (world.isClientSide()) {
            be.clientTick();
            return;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        if ((serverWorld.getGameTime() % 15L) != 0L) {
            return;
        }
        if (!isBeaconActiveBelow(serverWorld, pos)) {
            return;
        }
        // redstone-powered gateway suppresses the beam particles
        if (serverWorld.hasNeighborSignal(pos)) {
            return;
        }

        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double baseY = pos.getY() + 1.02;
        double topY = serverWorld.getMaxY();

        // deterministic angle, not random - keeps the beam looking clean instead of jittery
        long t = serverWorld.getGameTime() / 4L;
        double angle = (t * 0.33) % (Math.PI * 2.0);
        double radius = 0.31;
        double px = centerX + Math.cos(angle) * radius;
        double pz = centerZ + Math.sin(angle) * radius;
        serverWorld.sendParticles(
            com.theendupdate.registry.ModParticles.QUANTUM_SPARK,
            px,
            baseY,
            pz,
            0,
            centerX,
            topY,
            centerZ,
            1.0
        );
    }

    public float getBob(float tickProgress) {
        return Mth.sin((this.bobTime + tickProgress) * BOB_SPEED) * BOB_AMOUNT;
    }

    public float getSparkleRotX(int index, float tickProgress) {
        return Mth.lerp(tickProgress, this.prevRotX[index], this.rotX[index]);
    }

    public float getSparkleRotY(int index, float tickProgress) {
        return Mth.lerp(tickProgress, this.prevRotY[index], this.rotY[index]);
    }

    public float getSparkleRotZ(int index, float tickProgress) {
        return Mth.lerp(tickProgress, this.prevRotZ[index], this.rotZ[index]);
    }

    private void clientTick() {
        if (!this.sparklesInitialized) {
            this.sparklesInitialized = true;
            for (int i = 0; i < MOVING_SPARKLES; i++) {
                pickNewSpinTarget(i);
                this.velX[i] = this.targetVelX[i];
                this.velY[i] = this.targetVelY[i];
                this.velZ[i] = this.targetVelZ[i];
            }
        }

        this.bobTime += 1.0f;
        for (int i = 0; i < MOVING_SPARKLES; i++) {
            this.prevRotX[i] = this.rotX[i];
            this.prevRotY[i] = this.rotY[i];
            this.prevRotZ[i] = this.rotZ[i];
            this.retargetTicks[i]--;
            if (this.retargetTicks[i] <= 0) {
                pickNewSpinTarget(i);
            }
            this.velX[i] = Mth.lerp(SPIN_LERP, this.velX[i], this.targetVelX[i]);
            this.velY[i] = Mth.lerp(SPIN_LERP, this.velY[i], this.targetVelY[i]);
            this.velZ[i] = Mth.lerp(SPIN_LERP, this.velZ[i], this.targetVelZ[i]);
            this.rotX[i] += this.velX[i];
            this.rotY[i] += this.velY[i];
            this.rotZ[i] += this.velZ[i];
        }
    }

    private void pickNewSpinTarget(int index) {
        this.retargetTicks[index] = 40 + this.sparkleRandom.nextInt(80);
        this.targetVelX[index] = randomSpinSpeed();
        this.targetVelY[index] = randomSpinSpeed();
        this.targetVelZ[index] = randomSpinSpeed();
    }

    private float randomSpinSpeed() {
        return (this.sparkleRandom.nextFloat() - 0.5f) * 2.0f * MAX_SPIN_SPEED;
    }

    private static boolean isBeaconActiveBelow(ServerLevel world, BlockPos gatewayPos) {
        BlockPos beaconPos = gatewayPos.below();
        if (!world.getBlockState(beaconPos).is(Blocks.BEACON)) {
            return false;
        }
        if (!(world.getBlockEntity(beaconPos) instanceof BeaconBlockEntity beacon)) {
            return false;
        }
        try {
            return beacon.getBeamSections() != null && !beacon.getBeamSections().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
