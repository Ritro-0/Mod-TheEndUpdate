package com.theendupdate.mixin.client;

import com.theendupdate.accessor.BeaconRenderStateQuantumAccessor;
import com.theendupdate.block.QuantumGatewayBlock;
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
    private static World theendupdate$lastWorld = null;
    
    @Unique
    private static long theendupdate$lastTick = 0L;
    
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
            
            // Spawn glowing spiral particles around the beam (only if not redstone powered)
            if (!powered) {
                spawnSpiralParticles(world, beacon.getPos(), accessor.theendupdate$getBeamTint(), beacon.getPos().getY() + 2048);
            }
        } else {
            accessor.theendupdate$setHasQuantumGateway(false);
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Spawns glowing particles in a spiral pattern around the beacon beam
     */
    private static void spawnSpiralParticles(World world, BlockPos beaconPos, float[] tint, int topY) {
        // Only spawn particles occasionally to avoid performance issues
        if (world.getTime() % 2 != 0) return; // Spawn every other tick
        
        double time = world.getTime() * 0.05; // Animation time
        
        // Spiral parameters
        double spiralRadius = 0.6; // Radius of spiral around beam center
        double spiralTightness = 0.4; // How tight the spiral is (radians per block)
        int particlesPerSpiral = 3; // Number of spiral arms
        double verticalSpacing = 0.5; // Blocks between particle layers
        
        // Beam center
        double centerX = beaconPos.getX() + 0.5;
        double centerZ = beaconPos.getZ() + 0.5;
        
        // Spawn particles from beacon up to sky
        int startY = beaconPos.getY() + 2; // Start above beacon base
        int endY = topY;
        
        // Spawn multiple spirals at different heights
        for (double y = startY; y < endY; y += verticalSpacing) {
            for (int spiral = 0; spiral < particlesPerSpiral; spiral++) {
                double spiralOffset = (Math.PI * 2.0 / particlesPerSpiral) * spiral;
                double angle = (y * spiralTightness) + time + spiralOffset;
                
                double x = centerX + Math.cos(angle) * spiralRadius;
                double z = centerZ + Math.sin(angle) * spiralRadius;
                
                // Spawn END_ROD particles for the glowing spiral effect using particle manager
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.particleManager != null) {
                    client.particleManager.addParticle(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        0.0, 0.02, 0.0 // Slight upward drift
                    );
                    
                    // Add extra glow with ELECTRIC_SPARK particles occasionally
                    if (world.getRandom().nextFloat() < 0.2f) {
                        client.particleManager.addParticle(
                            ParticleTypes.ELECTRIC_SPARK,
                            x, y, z,
                            0.0, 0.01, 0.0
                        );
                    }
                }
            }
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
