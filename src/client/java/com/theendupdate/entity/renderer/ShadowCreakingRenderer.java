package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.animation.ShadowCreakingAnimData;
import com.theendupdate.entity.model.ShadowCreakingMesh;
import com.theendupdate.entity.model.ShadowCreakingModel;
import com.theendupdate.entity.state.ShadowCreakingRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * Drives Blockbench Roar/Stumble keyframes on the free mesh, plus procedural
 * stand-up, charge lean, and post-stumble T-pose / head-spin finisher
 */
public class ShadowCreakingRenderer extends MobRenderer<ShadowCreakingEntity, ShadowCreakingRenderState, ShadowCreakingModel> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
		TheEndUpdate.MOD_ID, "textures/entity/shadow_creaking/shadow_creaking.png");
	private static final Identifier SHOULDERS_TEXTURE = Identifier.fromNamespaceAndPath(
		TheEndUpdate.MOD_ID, "textures/entity/shadow_creaking/shadow_creaking_shoulders.png");
	private static final RenderType BODY_LAYER = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType SHOULDERS_LAYER = RenderTypes.entityCutout(SHOULDERS_TEXTURE);

	private static final float WALK_CYCLE_RATE = 0.42F;
	private static final float FINISHER_SECONDS = 2.4F;
	/** matches ShadowCreakingEntity.PUNCH_TICKS (24 ticks) */
	private static final float PUNCH_SECONDS = 1.2F;

	public ShadowCreakingRenderer(EntityRendererProvider.Context context) {
		super(context, new ShadowCreakingModel(context.bakeLayer(ShadowCreakingModel.LAYER_LOCATION)), 0.8f);
	}

	@Override
	public ShadowCreakingRenderState createRenderState() {
		return new ShadowCreakingRenderState();
	}

	@Override
	public void extractRenderState(ShadowCreakingEntity entity, ShadowCreakingRenderState state, float tickDelta) {
		super.extractRenderState(entity, state, tickDelta);
		state.canMove = entity.canMove();
		state.levitating = entity.isLevitating();
		state.combatPhase = entity.getCombatPhase();
		state.combatPhaseSeconds = entity.getCombatPhaseSeconds() + (tickDelta / 20.0F);
		state.roarAnimationState.copyFrom(entity.roarAnimationState);
		state.stumbleAnimationState.copyFrom(entity.stumbleAnimationState);
		state.levitatingAnimationState.copyFrom(entity.levitatingAnimationState);
		state.spawnAnimationState.copyFrom(entity.spawnAnimationState);
		state.variantScale = entity.getRenderScale();
		state.spawnIntroActive = entity.isInSpawnIntro();
		state.spawnIntroHidden = entity.isInvisible();
		state.spawnIntroProgress = entity.getSpawnRevealProgress();
	}

	@Override
	public Identifier getTextureLocation(ShadowCreakingRenderState state) {
		return TEXTURE;
	}

	@Override
	public void submit(
		ShadowCreakingRenderState state,
		PoseStack matrices,
		SubmitNodeCollector collector,
		CameraRenderState cameraState
	) {
		if (state.spawnIntroHidden) {
			return;
		}
		matrices.pushPose();
		float scale = state.scale * state.variantScale;
		if (state.spawnIntroActive) {
			float emerge = easeSpawn(state.spawnIntroProgress);
			scale *= 0.05F + emerge * 0.95F;
			matrices.translate(0.0F, (1.0F - emerge) * 3.2F, 0.0F);
		}
		matrices.scale(scale, scale, scale);
		this.setupRotations(state, matrices, state.bodyRot, scale);
		matrices.scale(-1.0F, -1.0F, 1.0F);
		this.scale(state, matrices);
		matrices.translate(0.0F, -0.001F, 0.0F);

		int light = state.lightCoords;
		int overlay = OverlayTexture.NO_OVERLAY;
		int color = ARGB.opaque(this.getModelTint(state));

		BonePose pose = computePose(state);

		// body2 carries torso/head/arms - legs are posed outside it so a body2 lean
		// bends at the waist instead of rotating the whole figure
		matrices.pushPose();
		applyPivotRotation(matrices, ShadowCreakingMesh.BODY2_PIVOT, pose.body2X, pose.body2Y, pose.body2Z);

		matrices.pushPose();
		applyPivotRotation(matrices, ShadowCreakingMesh.BODY_PIVOT, pose.bodyX, pose.bodyY, pose.bodyZ);
		submitMesh(collector, matrices, BODY_LAYER, ShadowCreakingMesh.BODY, light, overlay, color);
		submitMesh(collector, matrices, SHOULDERS_LAYER, ShadowCreakingMesh.SHOULDERS, light, overlay, color);

		submitLocalPart(collector, matrices, BODY_LAYER, ShadowCreakingMesh.HEAD, ShadowCreakingMesh.HEAD_PIVOT,
			pose.headX, pose.headY, pose.headZ, light, overlay, color);
		submitLocalPart(collector, matrices, BODY_LAYER, ShadowCreakingMesh.RIGHT_ARM, ShadowCreakingMesh.RIGHT_ARM_PIVOT,
			pose.rightArmX, pose.rightArmY, pose.rightArmZ, light, overlay, color);
		submitLocalPart(collector, matrices, BODY_LAYER, ShadowCreakingMesh.LEFT_ARM, ShadowCreakingMesh.LEFT_ARM_PIVOT,
			pose.leftArmX, pose.leftArmY, pose.leftArmZ, light, overlay, color);
		matrices.popPose();
		matrices.popPose();

		submitLocalPart(collector, matrices, BODY_LAYER, ShadowCreakingMesh.LEFT_LEG, ShadowCreakingMesh.LEFT_LEG_PIVOT,
			pose.leftLegX, pose.leftLegY, pose.leftLegZ, light, overlay, color);
		submitLocalPart(collector, matrices, BODY_LAYER, ShadowCreakingMesh.RIGHT_LEG, ShadowCreakingMesh.RIGHT_LEG_PIVOT,
			pose.rightLegX, pose.rightLegY, pose.rightLegZ, light, overlay, color);

		matrices.popPose();
		super.submit(state, matrices, collector, cameraState);
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
			matrices.mulPose(Axis.ZP.rotationDegrees(rotZDeg));
		}
		if (rotYDeg != 0.0F) {
			matrices.mulPose(Axis.YP.rotationDegrees(rotYDeg));
		}
		if (rotXDeg != 0.0F) {
			matrices.mulPose(Axis.XP.rotationDegrees(rotXDeg));
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
			ShadowCreakingMesh.render(mesh, pose, buffer, light, overlay, color));
	}

	private static BonePose computePose(ShadowCreakingRenderState state) {
		BonePose pose = new BonePose();
		pose.headX = state.xRot;
		pose.headY = state.yRot;

		if (state.levitating) {
			applyLevitationPose(pose, state);
			return pose;
		}
		if (state.spawnIntroActive) {
			applySpawnIntroPose(pose, state.spawnIntroProgress);
			return pose;
		}

		byte phase = state.combatPhase;
		if (phase == ShadowCreakingEntity.PHASE_ROAR) {
			applySampledRoar(pose, state.combatPhaseSeconds);
			return pose;
		}
		if (phase == ShadowCreakingEntity.PHASE_CHARGE) {
			applyHeavyArmStumble(pose, state);
			applyChargeLean(pose, state.combatPhaseSeconds);
			return pose;
		}
		if (phase == ShadowCreakingEntity.PHASE_STUMBLE) {
			applySampledStumble(pose, state.combatPhaseSeconds);
			return pose;
		}
		if (phase == ShadowCreakingEntity.PHASE_FINISHER) {
			applyFinisherPose(pose, state.combatPhaseSeconds);
			return pose;
		}
		if (phase == ShadowCreakingEntity.PHASE_PUNCH) {
			applyPunchPose(pose, state.combatPhaseSeconds);
			return pose;
		}

		applyHeavyArmStumble(pose, state);
		return pose;
	}

	private static void applySampledRoar(BonePose pose, float t) {
		setBone(pose, toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_BODY2, t)), true);
		setBone(pose, toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_BODY, t)), false);
		float[] head = toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_HEAD, t));
		pose.headX += head[0];
		pose.headY += head[1];
		pose.headZ += head[2];
		float[] la = toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_LEFT_ARM, t));
		pose.leftArmX = la[0];
		pose.leftArmY = la[1];
		pose.leftArmZ = la[2];
		float[] ra = toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_RIGHT_ARM, t));
		pose.rightArmX = ra[0];
		pose.rightArmY = ra[1];
		pose.rightArmZ = ra[2];
		float[] ll = toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_LEFT_LEG, t));
		pose.leftLegX = ll[0];
		pose.leftLegY = ll[1];
		pose.leftLegZ = ll[2];
		float[] rl = toMcRot(ShadowCreakingAnimData.sampleRoar(ShadowCreakingAnimData.ROAR_RIGHT_LEG, t));
		pose.rightLegX = rl[0];
		pose.rightLegY = rl[1];
		pose.rightLegZ = rl[2];
	}

	private static void applySampledStumble(BonePose pose, float t) {
		setBone(pose, toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_BODY2, t)), true);
		setBone(pose, toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_BODY, t)), false);
		float[] head = toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_HEAD, t));
		pose.headX += head[0];
		pose.headY += head[1];
		pose.headZ += head[2];
		float[] la = toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_LEFT_ARM, t));
		pose.leftArmX = la[0];
		pose.leftArmY = la[1];
		pose.leftArmZ = la[2];
		float[] ra = toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_RIGHT_ARM, t));
		pose.rightArmX = ra[0];
		pose.rightArmY = ra[1];
		pose.rightArmZ = ra[2];
		float[] ll = toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_LEFT_LEG, t));
		pose.leftLegX = ll[0];
		pose.leftLegY = ll[1];
		pose.leftLegZ = ll[2];
		float[] rl = toMcRot(ShadowCreakingAnimData.sample(ShadowCreakingAnimData.STUMBLE_RIGHT_LEG, t));
		pose.rightLegX = rl[0];
		pose.rightLegY = rl[1];
		pose.rightLegZ = rl[2];
	}

	/**
	 * LivingEntityRenderer applies scale(-1,-1,1), which conjugates Rx/Ry to their
	 * negatives - so Blockbench pitch/yaw keyframes must be negated here
	 */
	private static float[] toMcRot(float[] bbXyz) {
		return new float[] { -bbXyz[0], -bbXyz[1], bbXyz[2] };
	}

	private static void setBone(BonePose pose, float[] xyz, boolean body2) {
		if (body2) {
			pose.body2X = xyz[0];
			pose.body2Y = xyz[1];
			pose.body2Z = xyz[2];
		} else {
			pose.bodyX = xyz[0];
			pose.bodyY = xyz[1];
			pose.bodyZ = xyz[2];
		}
	}

	private static void applyHeavyArmStumble(BonePose pose, ShadowCreakingRenderState state) {
		float walkAmount = state.canMove ? Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F) : 0.0F;
		float cycle = Mth.cos(state.walkAnimationPos * WALK_CYCLE_RATE);
		float swing = cycle * Math.max(walkAmount, 0.25F);
		float idle = Mth.sin(state.ageInTicks * 0.08F);

		pose.rightArmX = -22.0F + swing * 24.0F + idle * 6.0F;
		pose.rightArmZ = 16.0F + swing * 8.0F + idle * 4.0F;
		pose.body2Z = 6.0F + idle * 3.0F;
		pose.body2Y = -swing * 6.0F;
		pose.leftArmX = -swing * 6.0F;
		pose.leftLegX = -swing * 42.0F;
		pose.rightLegX = swing * 16.0F;
	}

	private static void applyChargeLean(BonePose pose, float t) {
		float p = Mth.clamp(t * 3.0F, 0.0F, 1.0F);
		// positive X in MC space leans forward into the charge (after toMcRot)
		pose.body2X += 16.0F * p;
		pose.rightArmX += 35.0F * p;
		pose.headX += -8.0F * p;
	}

	/**
	 * After stumble: ease upright, raise both arms to the hover T-pose, spin the
	 * head a full turn, then settle toward look
	 */
	private static void applyFinisherPose(BonePose pose, float t) {
		float u = Mth.clamp(t / FINISHER_SECONDS, 0.0F, 1.0F);
		float stand = Mth.clamp(u / 0.25F, 0.0F, 1.0F);
		stand = stand * stand * (3.0F - 2.0F * stand);

		float arms = 0.0F;
		if (u > 0.2F) {
			arms = Mth.clamp((u - 0.2F) / 0.25F, 0.0F, 1.0F);
			arms = arms * arms * (3.0F - 2.0F * arms);
		}

		float spin = 0.0F;
		if (u > 0.45F && u < 0.85F) {
			spin = (u - 0.45F) / 0.40F;
		} else if (u >= 0.85F) {
			spin = 1.0F;
		}

		float hold = arms;
		if (u > 0.85F) {
			hold = 1.0F - Mth.clamp((u - 0.85F) / 0.15F, 0.0F, 1.0F);
		}

		pose.body2X = -8.0F * (1.0F - stand);
		pose.rightArmZ = 90.0F * hold;
		pose.leftArmZ = -90.0F * hold;
		pose.rightArmX = -8.0F * hold;
		pose.leftArmX = -8.0F * hold;
		pose.headY += spin * 360.0F;
	}

	/**
	 * Heavy right-arm swing: rear the arm back over the shoulder, whip it down and
	 * across on the impact frame, then settle - torso twists to sell the weight
	 */
	private static void applyPunchPose(BonePose pose, float t) {
		float u = Mth.clamp(t / PUNCH_SECONDS, 0.0F, 1.0F);

		float windupEnd = 0.42F;
		float strikeEnd = 0.58F;
		float armX;
		float twist;
		if (u < windupEnd) {
			float w = ease(u / windupEnd);
			armX = -85.0F * w;
			twist = 22.0F * w;
		} else if (u < strikeEnd) {
			float s = (u - windupEnd) / (strikeEnd - windupEnd);
			s = s * s;
			armX = Mth.lerp(s, -85.0F, 80.0F);
			twist = Mth.lerp(s, 22.0F, -18.0F);
		} else {
			float r = ease((u - strikeEnd) / (1.0F - strikeEnd));
			armX = Mth.lerp(r, 80.0F, -18.0F);
			twist = Mth.lerp(r, -18.0F, 0.0F);
		}

		pose.rightArmX = armX;
		pose.rightArmZ = 18.0F - twist * 0.4F;
		pose.body2Y = twist;
		pose.body2X = Mth.clamp(armX, -10.0F, 14.0F) * 0.35F;
		pose.leftArmX = -twist * 0.5F;
		pose.leftLegX = twist * 0.25F;
		pose.rightLegX = -twist * 0.2F;
	}

	private static float ease(float x) {
		float c = Mth.clamp(x, 0.0F, 1.0F);
		return c * c * (3.0F - 2.0F * c);
	}

	private static float easeSpawn(float progress) {
		float p = Mth.clamp(progress, 0.0F, 1.0F);
		return p * p * (3.0F - 2.0F * p);
	}

	/** Rising from the altar: hunched, arms pulling free, then snapping upright on reveal */
	private static void applySpawnIntroPose(BonePose pose, float progress) {
		float emerge = easeSpawn(progress);
		float burst = progress > 0.2F ? easeSpawn((progress - 0.2F) / 0.8F) : 0.0F;

		pose.body2X = -35.0F * (1.0F - emerge) + 8.0F * burst;
		pose.headX = 25.0F * (1.0F - emerge);
		pose.rightArmX = -60.0F * (1.0F - emerge) + burst * 20.0F;
		pose.rightArmZ = burst * 70.0F;
		pose.leftArmX = -40.0F * (1.0F - emerge);
		pose.leftArmZ = -burst * 55.0F;
		pose.leftLegX = 20.0F * (1.0F - emerge);
		pose.rightLegX = -15.0F * (1.0F - emerge);
	}

	private static void applyLevitationPose(BonePose pose, ShadowCreakingRenderState state) {
		float t = 0.0F;
		if (state.levitatingAnimationState.isStarted()) {
			t = state.levitatingAnimationState.getTimeInMillis(state.ageInTicks) / 1000.0F;
		}
		float arms = Mth.clamp(t / 2.0F, 0.0F, 1.0F);
		arms = arms * arms * (3.0F - 2.0F * arms);
		pose.rightArmZ = arms * 90.0F;
		pose.leftArmZ = arms * -90.0F;
		pose.rightArmX = arms * -10.0F;
		pose.leftArmX = arms * -10.0F;
		pose.body2X = arms * -6.0F;
		if (t > 2.0F) {
			pose.headY = (t - 2.0F) * 1080.0F;
		}
	}

	private static final class BonePose {
		float body2X, body2Y, body2Z;
		float bodyX, bodyY, bodyZ;
		float headX, headY, headZ;
		float rightArmX, rightArmY, rightArmZ;
		float leftArmX, leftArmY, leftArmZ;
		float leftLegX, leftLegY, leftLegZ;
		float rightLegX, rightLegY, rightLegZ;
	}
}
