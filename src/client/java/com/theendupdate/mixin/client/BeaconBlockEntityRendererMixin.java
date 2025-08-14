package com.theendupdate.mixin.client;

import com.theendupdate.block.QuantumGatewayBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
	private static int THEENDUPDATE$DEBUG_TICK = 0;
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
			if ((THEENDUPDATE$DEBUG_TICK++ % 40) == 0) {
				System.out.println("[theendupdate] Quantum gateway above beacon detected; applying bent overlay");
			}
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
                float[] tint = QuantumGatewayBlock.BEAM_TINT;
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
					if ((THEENDUPDATE$DEBUG_TICK % 40) == 0) {
						System.out.println("[theendupdate] segment y="+y+" offsetX="+offsetX+" offsetZ="+offsetZ);
					}
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
		float[] tint = QuantumGatewayBlock.BEAM_TINT;
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
		matrices.push();
		matrices.translate(0.5f, 0.0f, 0.5f);
		for (int y = 0; y < totalSegments; y++) {
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
}


