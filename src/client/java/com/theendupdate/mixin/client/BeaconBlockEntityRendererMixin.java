package com.theendupdate.mixin.client;

import com.theendupdate.accessor.BeaconRenderStateQuantumAccessor;
import com.theendupdate.block.QuantumGatewayBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.BeaconBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntityRenderer.class)
public abstract class BeaconBlockEntityRendererMixin {
    
    @Unique
    private static final Logger theendupdate$LOGGER = LoggerFactory.getLogger("TheEndUpdate:BeaconRenderer");
    
    @Unique
    private static World theendupdate$lastWorld = null;
    
    @Unique
    private static long theendupdate$lastTick = 0L;
    
    @Unique
    private static BlockPos theendupdate$lastBeaconPos = null;
    
    /**
     * Phase 1: Capture quantum gateway data and store world reference for animation
     */
    @Inject(method = "updateRenderState",
            at = @At("TAIL"))
    private void theendupdate$captureQuantumGatewayData(BlockEntity blockEntity, BeaconBlockEntityRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, CallbackInfo ci) {
        if (!(blockEntity instanceof BeaconBlockEntity beacon)) return;
        if (!(state instanceof BeaconRenderStateQuantumAccessor accessor)) return;
        
        World world = beacon.getWorld();
        if (world == null) {
            accessor.theendupdate$setHasQuantumGateway(false);
            return;
        }
        
        // Store world reference for animation timing
        theendupdate$lastWorld = world;
        theendupdate$lastTick = world.getTime();
        
        // Check if beacon has an active beam
        if (!hasActiveBeam(beacon)) {
            accessor.theendupdate$setHasQuantumGateway(false);
            return;
        }
        
		BlockPos above = beacon.getPos().up();
		BlockState stateAbove = world.getBlockState(above);
        
        // Check if there's a quantum gateway above the beacon
        if (stateAbove.getBlock() instanceof QuantumGatewayBlock) {
            accessor.theendupdate$setHasQuantumGateway(true);
            boolean powered = world.isReceivingRedstonePower(above);
            accessor.theendupdate$setRedstonePowered(powered);
            accessor.theendupdate$setBeamTint(getRedirectedTint(world, beacon.getPos()));
            accessor.theendupdate$setBeaconY(beacon.getPos().getY());
            // Match vanilla beacon beam height - renders up to ~2048 blocks above beacon
            accessor.theendupdate$setTopY(beacon.getPos().getY() + 2048);
            // Store beacon position for particle spawning
            theendupdate$lastBeaconPos = beacon.getPos();
        } else {
            accessor.theendupdate$setHasQuantumGateway(false);
        }
    }
    
    /**
     * Phase 2: Spawn spiral particles around quantum gateway beacon beam
     */
    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/state/BeaconBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void theendupdate$spawnSpiralParticles(BeaconBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState, CallbackInfo ci) {
        try {
            if (!(state instanceof BeaconRenderStateQuantumAccessor accessor)) return;
            if (!accessor.theendupdate$hasQuantumGateway()) return;
            if (accessor.theendupdate$isRedstonePowered()) return; // Don't spawn particles when powered
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) return;
            
            // Get beam parameters
            float[] tint = accessor.theendupdate$getBeamTint();
            int beaconY = accessor.theendupdate$getBeaconY();
            int topY = accessor.theendupdate$getTopY();
            
            // Spawn spiral particles around the beam
            spawnSpiralParticles(client, beaconY, topY, tint);
        } catch (Exception e) {
            theendupdate$LOGGER.error("Error in spawnSpiralParticles injection", e);
        }
    }
    
    /**
     * Spawns spiral particles around the quantum gateway beacon beam
     * Only spawns particles within a vertical range around the player to avoid lag
     */
    @Unique
    private static void spawnSpiralParticles(MinecraftClient client, int beaconY, int topY, float[] tint) {
        if (theendupdate$lastWorld == null || theendupdate$lastBeaconPos == null) return;
        if (client.player == null) return;
        
        // Only spawn particles if player is within 64 blocks horizontally
        double dx = client.player.getX() - theendupdate$lastBeaconPos.getX();
        double dz = client.player.getZ() - theendupdate$lastBeaconPos.getZ();
        if (dx * dx + dz * dz > 64 * 64) return;
        
        // Calculate vertical range around player (only spawn particles within 32 blocks vertically)
        double playerY = client.player.getY();
        double minY = Math.max(beaconY + 1, playerY - 32);
        double maxY = Math.min(topY, playerY + 32);
        
        // If the range is invalid (player too far from beam), don't spawn particles
        if (minY >= maxY) return;
        
        long worldTime = theendupdate$lastWorld.getTime();
        double beaconX = theendupdate$lastBeaconPos.getX() + 0.5;
        double beaconZ = theendupdate$lastBeaconPos.getZ() + 0.5;
        
        // Spawn particles in a spiral pattern around the beam
        int particlesPerTick = 2;
        double spiralRadius = 0.3;
        double spiralSpeed = 0.1;
        
        for (int i = 0; i < particlesPerTick; i++) {
            double angle = (worldTime * spiralSpeed + i * Math.PI * 2 / particlesPerTick) % (Math.PI * 2);
            double offsetX = Math.cos(angle) * spiralRadius;
            double offsetZ = Math.sin(angle) * spiralRadius;
            
            // Random Y position within the visible range around the player
            double yPos = minY + (maxY - minY) * client.world.random.nextDouble();
            
            // Spawn END_ROD particle (respects particle settings automatically)
            client.particleManager.addParticle(
                ParticleTypes.END_ROD,
                beaconX + offsetX,
                yPos,
                beaconZ + offsetZ,
                0.0,
                0.0,
                0.0
            );
        }
    }

    // ==================== Beacon Detection Methods ====================
    
    private static boolean hasActiveBeam(BeaconBlockEntity beacon) {
		try {
			java.util.List<?> segments = beacon.getBeamSegments();
            return segments != null && !segments.isEmpty();
		} catch (Throwable ignored) {
            return false;
		}
	}

    private static float[] getRedirectedTint(World world, BlockPos beaconPos) {
		BlockPos gatewayPos = beaconPos.up();
		BlockState gatewayState = world.getBlockState(gatewayPos);
		if (!(gatewayState.getBlock() instanceof QuantumGatewayBlock)) {
			return QuantumGatewayBlock.BEAM_TINT;
		}

		int bottomY = world.getBottomY();
		int topYExclusive = bottomY + world.getHeight();
		float rSum = 0.0f;
		float gSum = 0.0f;
		float bSum = 0.0f;
		int count = 0;

        // Scan upward from above the gateway to find stained glass
		BlockPos.Mutable scan = new BlockPos.Mutable(gatewayPos.getX(), gatewayPos.getY() + 1, gatewayPos.getZ());
		while (scan.getY() < topYExclusive) {
			BlockState state = world.getBlockState(scan);
			Block block = state.getBlock();
            
			if (block instanceof TintedGlassBlock) {
                break; // Tinted glass stops the beam
			}
			if (block instanceof StainedGlassBlock sgb) {
                float[] c = fromDyeColor(sgb.getColor());
				rSum += c[0];
				gSum += c[1];
				bSum += c[2];
				count++;
			} else if (block instanceof StainedGlassPaneBlock sgbp) {
                float[] c = fromDyeColor(sgbp.getColor());
				rSum += c[0];
				gSum += c[1];
				bSum += c[2];
				count++;
			} else if (state.isOf(Blocks.AIR) || state.isOf(Blocks.GLASS) || state.isOf(Blocks.GLASS_PANE)) {
                // Transparent blocks - pass through
			} else {
                // Opaque block - stop scanning
				break;
			}
			scan.set(scan.getX(), scan.getY() + 1, scan.getZ());
		}

		if (count > 0) {
			return new float[]{rSum / count, gSum / count, bSum / count};
		}
		return QuantumGatewayBlock.BEAM_TINT;
	}

    private static float[] fromDyeColor(DyeColor dye) {
        return switch (dye) {
            case WHITE -> new float[]{249/255f, 255/255f, 254/255f};
            case ORANGE -> new float[]{249/255f, 128/255f, 29/255f};
            case MAGENTA -> new float[]{199/255f, 78/255f, 189/255f};
            case LIGHT_BLUE -> new float[]{58/255f, 179/255f, 218/255f};
            case YELLOW -> new float[]{254/255f, 216/255f, 61/255f};
            case LIME -> new float[]{128/255f, 199/255f, 31/255f};
            case PINK -> new float[]{243/255f, 139/255f, 170/255f};
            case GRAY -> new float[]{71/255f, 79/255f, 82/255f};
            case LIGHT_GRAY -> new float[]{157/255f, 157/255f, 151/255f};
            case CYAN -> new float[]{22/255f, 156/255f, 156/255f};
            case PURPLE -> new float[]{137/255f, 50/255f, 184/255f};
            case BLUE -> new float[]{60/255f, 68/255f, 170/255f};
            case BROWN -> new float[]{131/255f, 84/255f, 50/255f};
            case GREEN -> new float[]{94/255f, 124/255f, 22/255f};
            case RED -> new float[]{176/255f, 46/255f, 38/255f};
            case BLACK -> new float[]{29/255f, 29/255f, 33/255f};
        };
    }
    
}
