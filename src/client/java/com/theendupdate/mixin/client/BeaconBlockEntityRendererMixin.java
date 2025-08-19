package com.theendupdate.mixin.client;

import com.theendupdate.block.QuantumGatewayBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.util.DyeColor;
import net.minecraft.block.Block;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

 

@Mixin(BeaconBlockEntityRenderer.class)
public abstract class BeaconBlockEntityRendererMixin {
    
    private static int theendupdate$packColor(float r, float g, float b, float a) {
        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;
        int ai = (int)(a * 255.0f) & 0xFF;
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static void theendupdate$emitQuad(VertexConsumer consumer,
                                              MatrixStack.Entry entry,
                                              float x1, float y1, float z1,
                                              float x2, float y2, float z2,
                                              float r, float g, float b, float a,
                                              float u1, float v1, float u2, float v2,
                                              int light, int overlay,
                                              float nx, float ny, float nz) {
        Matrix4f pm = entry.getPositionMatrix();
        Matrix3f nm = entry.getNormalMatrix();
        int color = theendupdate$packColor(r, g, b, a);

        Vector4f p1 = new Vector4f(x1, y1, z1, 1.0f).mul(pm);
        Vector4f p2 = new Vector4f(x2, y1, z1, 1.0f).mul(pm);
        Vector4f p3 = new Vector4f(x2, y2, z2, 1.0f).mul(pm);
        Vector4f p4 = new Vector4f(x1, y2, z2, 1.0f).mul(pm);

        Vector3f n = new Vector3f(nx, ny, nz);
        n.mul(nm);

        consumer.vertex(p1.x, p1.y, p1.z, color, u1, v1, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p2.x, p2.y, p2.z, color, u2, v1, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p3.x, p3.y, p3.z, color, u2, v2, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p4.x, p4.y, p4.z, color, u1, v2, overlay, light, n.x, n.y, n.z);
    }

    private static void theendupdate$emitQuadReversed(VertexConsumer consumer,
                                                      MatrixStack.Entry entry,
                                                      float x1, float y1, float z1,
                                                      float x2, float y2, float z2,
                                                      float r, float g, float b, float a,
                                                      float u1, float v1, float u2, float v2,
                                                      int light, int overlay,
                                                      float nx, float ny, float nz) {
        Matrix4f pm = entry.getPositionMatrix();
        Matrix3f nm = entry.getNormalMatrix();
        int color = theendupdate$packColor(r, g, b, a);

        Vector4f p1 = new Vector4f(x1, y1, z1, 1.0f).mul(pm);
        Vector4f p2 = new Vector4f(x2, y1, z1, 1.0f).mul(pm);
        Vector4f p3 = new Vector4f(x2, y2, z2, 1.0f).mul(pm);
        Vector4f p4 = new Vector4f(x1, y2, z2, 1.0f).mul(pm);

        Vector3f n = new Vector3f(-nx, -ny, -nz);
        n.mul(nm);

        // Reverse the winding: p1, p4, p3, p2
        consumer.vertex(p1.x, p1.y, p1.z, color, u1, v1, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p4.x, p4.y, p4.z, color, u1, v2, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p3.x, p3.y, p3.z, color, u2, v2, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p2.x, p2.y, p2.z, color, u2, v1, overlay, light, n.x, n.y, n.z);
    }

    private static void theendupdate$emitZFace(VertexConsumer consumer,
                                               MatrixStack.Entry entry,
                                               float zConst,
                                               float xMin, float xMax,
                                               float yMin, float yMax,
                                               float r, float g, float b, float a,
                                               int light, int overlay,
                                               float nz) {
        // Oriented along X (u) and Y (v) with constant Z
        theendupdate$emitQuad(consumer, entry,
            xMin, yMin, zConst,
            xMax, yMax, zConst,
            r, g, b, a,
            0.0f, 0.0f, 1.0f, 1.0f,
            light, overlay,
            0.0f, 0.0f, nz);
        // Back face (reverse winding)
        theendupdate$emitQuadReversed(consumer, entry,
            xMin, yMin, zConst,
            xMax, yMax, zConst,
            r, g, b, a,
            0.0f, 0.0f, 1.0f, 1.0f,
            light, overlay,
            0.0f, 0.0f, nz);
    }

    private static void theendupdate$emitXFace(VertexConsumer consumer,
                                               MatrixStack.Entry entry,
                                               float xConst,
                                               float zMin, float zMax,
                                               float yMin, float yMax,
                                               float r, float g, float b, float a,
                                               int light, int overlay,
                                               float nx) {
        Matrix4f pm = entry.getPositionMatrix();
        Matrix3f nm = entry.getNormalMatrix();
        int color = theendupdate$packColor(r, g, b, a);

        Vector4f p1 = new Vector4f(xConst, yMin, zMin, 1.0f).mul(pm);
        Vector4f p2 = new Vector4f(xConst, yMin, zMax, 1.0f).mul(pm);
        Vector4f p3 = new Vector4f(xConst, yMax, zMax, 1.0f).mul(pm);
        Vector4f p4 = new Vector4f(xConst, yMax, zMin, 1.0f).mul(pm);

        Vector3f n = new Vector3f(nx, 0.0f, 0.0f);
        n.mul(nm);

        // Front
        consumer.vertex(p1.x, p1.y, p1.z, color, 0.0f, 0.0f, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p2.x, p2.y, p2.z, color, 1.0f, 0.0f, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p3.x, p3.y, p3.z, color, 1.0f, 1.0f, overlay, light, n.x, n.y, n.z);
        consumer.vertex(p4.x, p4.y, p4.z, color, 0.0f, 1.0f, overlay, light, n.x, n.y, n.z);

        // Back (reverse winding, inverted normal)
        Vector3f nb = new Vector3f(-nx, 0.0f, 0.0f);
        nb.mul(nm);
        consumer.vertex(p4.x, p4.y, p4.z, color, 0.0f, 1.0f, overlay, light, nb.x, nb.y, nb.z);
        consumer.vertex(p3.x, p3.y, p3.z, color, 1.0f, 1.0f, overlay, light, nb.x, nb.y, nb.z);
        consumer.vertex(p2.x, p2.y, p2.z, color, 1.0f, 0.0f, overlay, light, nb.x, nb.y, nb.z);
        consumer.vertex(p1.x, p1.y, p1.z, color, 0.0f, 0.0f, overlay, light, nb.x, nb.y, nb.z);
    }

    

	@Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/util/math/Vec3d;)V",
			at = @At("TAIL"))
    private void theendupdate$prepareTint(BlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light, int overlay, Vec3d cameraPos, CallbackInfo ci) {
		if (!(be instanceof BeaconBlockEntity beacon)) return;
		World world = beacon.getWorld();
		if (world == null) return;
		if (vertices == null) return;
		BlockPos above = beacon.getPos().up();
		BlockState stateAbove = world.getBlockState(above);
		if (!stateAbove.isOf(Blocks.AIR) && stateAbove.getBlock() instanceof QuantumGatewayBlock) {
            
		// Throttle (keep light) but don't cull vertically; overlay path kept lightweight
		if ((world.getTime() & 1L) != 0L) return;

			double time = (world.getTime() % 2000L) + tickDelta;
            float amp = 0.18f;
			float sharp = 8.0f;
			float freqSpace = 0.45f;
			float freqTime = 1.0f;

			int max = 6;
			try {
				int stride = 2;
				int start = (int)(world.getTime() % stride);
                VertexConsumer consumer = vertices.getBuffer(RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, false));
                float[] tint = theendupdate$getRedirectedTint(world, beacon.getPos());
                float r = tint[0];
                float g = tint[1];
                float b = tint[2];
        float a = 1.0f;
        float half = 0.3f;
        
        float height = 1.0f;
				matrices.push();
				matrices.translate(0.5f, 0.0f, 0.5f);
				for (int y = start; y < max; y += stride) {
					float phase = (y * freqSpace) + (float)(time * freqTime);
					float offsetX = (float)(Math.sin(phase) * amp + Math.signum(Math.sin(phase * sharp)) * amp * 0.12f);
					float offsetZ = (float)(Math.cos(phase * 1.07f) * amp + Math.signum(Math.cos(phase * sharp * 0.93f)) * amp * 0.12f);
                    
					matrices.push();
					matrices.translate(offsetX, (float)y, offsetZ);
                    MatrixStack.Entry entry = matrices.peek();
                    // Z faces
                    theendupdate$emitZFace(consumer, entry, -half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, -1.0f);
                    theendupdate$emitZFace(consumer, entry, +half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, 1.0f);
                    // X faces
                    theendupdate$emitXFace(consumer, entry, -half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, -1.0f);
                    theendupdate$emitXFace(consumer, entry, +half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, 1.0f);
					matrices.pop();
				}
				matrices.pop();
			} catch (Throwable err) {
                System.out.println("[theendupdate] Exception in bent overlay loop: "+err);
			}
		}
	}

	@Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/util/math/Vec3d;)V",
			at = @At("HEAD"), cancellable = true)
	private void theendupdate$replaceBeamIfGateway(BlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light, int overlay, Vec3d cameraPos, CallbackInfo ci) {
		if (!(be instanceof BeaconBlockEntity beacon)) return;
		World world = beacon.getWorld();
		if (world == null || vertices == null) return;
		BlockPos above = beacon.getPos().up();
		BlockState stateAbove = world.getBlockState(above);
		if (!(stateAbove.getBlock() instanceof QuantumGatewayBlock)) return;

		// Replace vanilla beam entirely
		ci.cancel();

		double time = (world.getTime() % 2000L) + tickDelta;
		float amp = 0.18f;
		float sharp = 8.0f;
		float freqSpace = 0.45f;
		float freqTime = 1.0f;

		VertexConsumer consumer = vertices.getBuffer(RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, false));
		float[] tint = theendupdate$getRedirectedTint(world, beacon.getPos());
		float r = tint[0];
		float g = tint[1];
		float b = tint[2];
        float a = 1.0f;
        float half = 0.3f;
        
        float height = 1.0f;

		// Render full height to sky: from beacon Y to world top Y
		int beaconY = beacon.getPos().getY();
		int topY = world.getDimension().height();
		int totalSegments = Math.max(0, topY - beaconY);
		int yStart = 2; // start above the beacon's inner blue cube
		matrices.push();
		matrices.translate(0.5f, 0.0f, 0.5f);
        for (int y = yStart; y < totalSegments; y++) {
			float phase = (y * freqSpace) + (float)(time * freqTime);
			float offsetX = (float)(Math.sin(phase) * amp + Math.signum(Math.sin(phase * sharp)) * amp * 0.12f);
			float offsetZ = (float)(Math.cos(phase * 1.07f) * amp + Math.signum(Math.cos(phase * sharp * 0.93f)) * amp * 0.12f);
			matrices.push();
			matrices.translate(offsetX, (float)y, offsetZ);
            MatrixStack.Entry entry = matrices.peek();
            // Z faces
            theendupdate$emitZFace(consumer, entry, -half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, -1.0f);
            theendupdate$emitZFace(consumer, entry, +half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, 1.0f);
            // X faces
            theendupdate$emitXFace(consumer, entry, -half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, -1.0f);
            theendupdate$emitXFace(consumer, entry, +half, -half, +half, 0.0f, height, r, g, b, a, light, overlay, 1.0f);
			matrices.pop();

            
		}
		matrices.pop();
	}

	private static float[] theendupdate$getRedirectedTint(World world, BlockPos beaconPos) {
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

		BlockPos.Mutable scan = new BlockPos.Mutable(gatewayPos.getX(), gatewayPos.getY() + 1, gatewayPos.getZ());
		while (scan.getY() < topYExclusive) {
			BlockState state = world.getBlockState(scan);
			Block block = state.getBlock();
			// Stop if an opaque or otherwise blocking block is encountered
			if (block instanceof TintedGlassBlock) {
				break;
			}
			if (block instanceof StainedGlassBlock sgb) {
				float[] c = theendupdate$fromDyeColor(sgb.getColor());
				rSum += c[0];
				gSum += c[1];
				bSum += c[2];
				count++;
			} else if (block instanceof StainedGlassPaneBlock sgbp) {
				float[] c = theendupdate$fromDyeColor(sgbp.getColor());
				rSum += c[0];
				gSum += c[1];
				bSum += c[2];
				count++;
			} else if (state.isOf(Blocks.AIR) || state.isOf(Blocks.GLASS) || state.isOf(Blocks.GLASS_PANE)) {
				// pass-through, no color contribution
			} else {
				// Non-pass-through block found; stop scanning
				break;
			}
			scan.set(scan.getX(), scan.getY() + 1, scan.getZ());
		}

		if (count > 0) {
			return new float[]{rSum / count, gSum / count, bSum / count};
		}
		return QuantumGatewayBlock.BEAM_TINT;
	}

	private static float[] theendupdate$fromDyeColor(DyeColor dye) {
		switch (dye) {
			case WHITE: return new float[]{249/255f, 255/255f, 254/255f};
			case ORANGE: return new float[]{249/255f, 128/255f, 29/255f};
			case MAGENTA: return new float[]{199/255f, 78/255f, 189/255f};
			case LIGHT_BLUE: return new float[]{58/255f, 179/255f, 218/255f};
			case YELLOW: return new float[]{254/255f, 216/255f, 61/255f};
			case LIME: return new float[]{128/255f, 199/255f, 31/255f};
			case PINK: return new float[]{243/255f, 139/255f, 170/255f};
			case GRAY: return new float[]{71/255f, 79/255f, 82/255f};
			case LIGHT_GRAY: return new float[]{157/255f, 157/255f, 151/255f};
			case CYAN: return new float[]{22/255f, 156/255f, 156/255f};
			case PURPLE: return new float[]{137/255f, 50/255f, 184/255f};
			case BLUE: return new float[]{60/255f, 68/255f, 170/255f};
			case BROWN: return new float[]{131/255f, 84/255f, 50/255f};
			case GREEN: return new float[]{94/255f, 124/255f, 22/255f};
			case RED: return new float[]{176/255f, 46/255f, 38/255f};
			case BLACK: return new float[]{29/255f, 29/255f, 33/255f};
			default: return QuantumGatewayBlock.BEAM_TINT;
		}
	}
}


