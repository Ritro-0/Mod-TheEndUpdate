package com.theendupdate.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.KingPhantomEntity;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.model.KingPhantomMesh;
import com.theendupdate.entity.state.KingPhantomRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Blockbench free-mesh King Phantom: wing flaps, beak bites, and an {@link RenderTypes#eyes}
 * glow pass for red accents (same approach as glow squids / spectral block glow).
 */
public class KingPhantomEntityRenderer extends MobRenderer<KingPhantomEntity, KingPhantomRenderState, KingPhantomEntityModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
		TheEndUpdate.MOD_ID, "textures/entity/king_phantom.png");
	private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(
		TheEndUpdate.MOD_ID, "textures/entity/king_phantom_glow.png");
	private static final RenderType BODY_LAYER = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType GLOW_LAYER = RenderTypes.eyes(GLOW_TEXTURE);
	private static final int FULL_BRIGHT = 0xF000F0;

	private static final float MODEL_SCALE = 1.75F;

	public KingPhantomEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new KingPhantomEntityModel(context.bakeLayer(KingPhantomEntityModel.LAYER_LOCATION)), 8.0F);
	}

	@Override
	public KingPhantomRenderState createRenderState() {
		return new KingPhantomRenderState();
	}

	@Override
	public void extractRenderState(KingPhantomEntity entity, KingPhantomRenderState state, float tickDelta) {
		super.extractRenderState(entity, state, tickDelta);
		state.swooping = entity.isSwooping();
		state.summoning = entity.isSummoning();
		state.phaseTransition = entity.isInPhaseTransition();
	}

	@Override
	public Identifier getTextureLocation(KingPhantomRenderState state) {
		return TEXTURE;
	}

	@Override
	public void submit(
		KingPhantomRenderState state,
		PoseStack matrices,
		SubmitNodeCollector collector,
		CameraRenderState cameraState
	) {
		matrices.pushPose();
		float scale = state.scale * MODEL_SCALE;
		matrices.scale(scale, scale, scale);
		this.setupRotations(state, matrices, state.bodyRot, scale);
		// OBJ is Y-up; mirror X only (ModelPart convention uses -Y).
		matrices.scale(-1.0F, 1.0F, 1.0F);
		this.scale(state, matrices);
		matrices.translate(0.0F, -0.2F, 0.0F);
		matrices.rotateDegrees(Axis.XP, -state.xRot);

		int light = state.lightCoords;
		int overlay = OverlayTexture.NO_OVERLAY;
		int color = ARGB.opaque(this.getModelTint(state));
		BonePose pose = computePose(state);

		submitPosedModel(collector, matrices, pose, BODY_LAYER, light, overlay, color);
		submitPosedModel(collector, matrices, pose, GLOW_LAYER, FULL_BRIGHT, overlay, color);

		matrices.popPose();
		super.submit(state, matrices, collector, cameraState);
	}

	private static void submitPosedModel(
		SubmitNodeCollector collector,
		PoseStack matrices,
		BonePose pose,
		RenderType layer,
		int light,
		int overlay,
		int color
	) {
		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.BODY_PIVOT, pose.bodyX, pose.bodyY, pose.bodyZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.BODY, light, overlay, color);
		submitMesh(collector, matrices, layer, KingPhantomMesh.BODY_SCALES, light, overlay, color);

		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.LEFT_WING_PIVOT, pose.leftWingX, pose.leftWingY, pose.leftWingZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.LEFT_WING, light, overlay, color);
		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.LEFT_WING_TIP_PIVOT, pose.leftWingTipX, pose.leftWingTipY, pose.leftWingTipZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.LEFT_WING_TIP, light, overlay, color);
		matrices.popPose();
		matrices.popPose();

		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.RIGHT_WING_PIVOT, pose.rightWingX, pose.rightWingY, pose.rightWingZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.RIGHT_WING, light, overlay, color);
		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.RIGHT_WING_TIP_PIVOT, pose.rightWingTipX, pose.rightWingTipY, pose.rightWingTipZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.RIGHT_WING_TIP, light, overlay, color);
		matrices.popPose();
		matrices.popPose();

		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.TAIL_BASE_PIVOT, pose.tailBaseX, pose.tailBaseY, pose.tailBaseZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.TAIL_BASE, light, overlay, color);
		submitMesh(collector, matrices, layer, KingPhantomMesh.TAIL_SCALES, light, overlay, color);
		submitLocalPart(collector, matrices, layer, KingPhantomMesh.LEFT_TAIL, KingPhantomMesh.LEFT_TAIL_PIVOT,
			pose.leftTailX, pose.leftTailY, pose.leftTailZ, light, overlay, color);
		submitLocalPart(collector, matrices, layer, KingPhantomMesh.RIGHT_TAIL, KingPhantomMesh.RIGHT_TAIL_PIVOT,
			pose.rightTailX, pose.rightTailY, pose.rightTailZ, light, overlay, color);
		matrices.popPose();

		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.NECK_PIVOT, pose.neckX, pose.neckY, pose.neckZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.NECK, light, overlay, color);
		submitMesh(collector, matrices, layer, KingPhantomMesh.NECK_SCALES, light, overlay, color);

		matrices.pushPose();
		applyPivotRotation(matrices, KingPhantomMesh.HEAD_PIVOT, pose.headX, pose.headY, pose.headZ);
		submitMesh(collector, matrices, layer, KingPhantomMesh.HEAD, light, overlay, color);
		submitMesh(collector, matrices, layer, KingPhantomMesh.HEAD_SCALES, light, overlay, color);
		submitLocalPart(collector, matrices, layer, KingPhantomMesh.UPPER_BEAK, KingPhantomMesh.UPPER_BEAK_PIVOT,
			pose.upperBeakX, pose.upperBeakY, pose.upperBeakZ, light, overlay, color);
		submitLocalPart(collector, matrices, layer, KingPhantomMesh.LOWER_BEAK, KingPhantomMesh.LOWER_BEAK_PIVOT,
			pose.lowerBeakX, pose.lowerBeakY, pose.lowerBeakZ, light, overlay, color);
		matrices.popPose();
		matrices.popPose();

		matrices.popPose();
	}

	private static BonePose computePose(KingPhantomRenderState state) {
		BonePose pose = new BonePose();
		float t = state.ageInTicks;

		boolean aggressive = state.swooping || state.summoning;
		float flapRate = aggressive ? 2.35F : (state.phaseTransition ? 1.15F : 1.65F);
		float cycle = t * flapRate;

		float flap = Mth.sin(cycle * 0.42F) * 26.0F + Mth.sin(cycle * 0.84F) * 4.5F;
		float tipLag = Mth.sin(cycle * 0.42F - 0.65F) * 34.0F + Mth.sin(cycle * 0.84F - 0.65F) * 6.0F;
		float roll = Mth.sin(cycle * 0.21F) * 3.5F;

		pose.leftWingZ = 8.0F + flap;
		pose.rightWingZ = -8.0F - flap;
		pose.leftWingY = roll * 0.35F;
		pose.rightWingY = -roll * 0.35F;
		pose.leftWingTipZ = 6.0F + tipLag;
		pose.rightWingTipZ = -6.0F - tipLag;
		pose.leftWingTipY = Mth.sin(cycle * 0.42F - 0.4F) * 6.0F;
		pose.rightWingTipY = -Mth.sin(cycle * 0.42F - 0.4F) * 6.0F;

		float tailCycle = t * 1.1F;
		pose.tailBaseY = Mth.sin(tailCycle * 0.28F) * 8.0F;
		pose.tailBaseX = Mth.sin(tailCycle * 0.22F) * 4.0F;
		pose.leftTailY = Mth.sin(tailCycle * 0.28F + 0.55F) * 12.0F;
		pose.rightTailY = Mth.sin(tailCycle * 0.28F + 0.85F) * 12.0F;
		pose.leftTailX = Mth.sin(tailCycle * 0.22F + 0.4F) * 6.0F;
		pose.rightTailX = Mth.sin(tailCycle * 0.22F + 0.7F) * 6.0F;

		pose.neckX = Mth.sin(t * 0.55F) * 3.0F;
		pose.bodyX = Mth.sin(t * 0.35F) * 2.0F;

		float biteWave;
		if (aggressive) {
			biteWave = 0.55F + 0.45F * Mth.sin(t * 1.75F);
		} else {
			float idle = Mth.sin(t * 0.38F);
			biteWave = idle > 0.72F ? (idle - 0.72F) / 0.28F : 0.0F;
		}
		float open = biteWave * biteWave * (3.0F - 2.0F * biteWave);
		pose.upperBeakX = -open * 16.0F;
		pose.lowerBeakX = open * 22.0F;
		pose.headX = open * 4.0F;

		return pose;
	}

	private static void submitLocalPart(
		SubmitNodeCollector collector,
		PoseStack matrices,
		RenderType layer,
		float[] mesh,
		float[] pivot,
		float rotXDeg,
		float rotYDeg,
		float rotZDeg,
		int light,
		int overlay,
		int color
	) {
		matrices.pushPose();
		applyPivotRotation(matrices, pivot, rotXDeg, rotYDeg, rotZDeg);
		submitMesh(collector, matrices, layer, mesh, light, overlay, color);
		matrices.popPose();
	}

	private static void applyPivotRotation(PoseStack matrices, float[] pivot, float rotXDeg, float rotYDeg, float rotZDeg) {
		float px = pivot[0] / 16.0F;
		float py = pivot[1] / 16.0F;
		float pz = pivot[2] / 16.0F;
		matrices.translate(px, py, pz);
		if (rotZDeg != 0.0F) {
			matrices.rotateDegrees(Axis.ZP, rotZDeg);
		}
		if (rotYDeg != 0.0F) {
			matrices.rotateDegrees(Axis.YP, rotYDeg);
		}
		if (rotXDeg != 0.0F) {
			matrices.rotateDegrees(Axis.XP, rotXDeg);
		}
		matrices.translate(-px, -py, -pz);
	}

	private static void submitMesh(
		SubmitNodeCollector collector,
		PoseStack matrices,
		RenderType layer,
		float[] mesh,
		int light,
		int overlay,
		int color
	) {
		collector.submitCustomGeometry(matrices, layer, (pose, buffer) ->
			KingPhantomMesh.render(mesh, pose, buffer, light, overlay, color));
	}

	private static final class BonePose {
		float bodyX, bodyY, bodyZ;
		float leftWingX, leftWingY, leftWingZ;
		float leftWingTipX, leftWingTipY, leftWingTipZ;
		float rightWingX, rightWingY, rightWingZ;
		float rightWingTipX, rightWingTipY, rightWingTipZ;
		float tailBaseX, tailBaseY, tailBaseZ;
		float leftTailX, leftTailY, leftTailZ;
		float rightTailX, rightTailY, rightTailZ;
		float neckX, neckY, neckZ;
		float headX, headY, headZ;
		float upperBeakX, upperBeakY, upperBeakZ;
		float lowerBeakX, lowerBeakY, lowerBeakZ;
	}
}
