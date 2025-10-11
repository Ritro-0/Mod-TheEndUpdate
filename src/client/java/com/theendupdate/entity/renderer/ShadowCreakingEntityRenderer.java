package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;
import com.theendupdate.entity.state.ShadowCreakingRenderState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.entity.CreakingEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.CreakingEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.CreakingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.AnimationState;
import java.lang.reflect.Field;
import java.util.Map;

public class ShadowCreakingEntityRenderer extends MobEntityRenderer<ShadowCreakingEntity, ShadowCreakingRenderState, CreakingEntityModel> {
	private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/shadow_creaking.png");
	private static final EntityModelLayer CREAKING_LAYER = new EntityModelLayer(Identifier.ofVanilla("creaking"), "main");
	private static final int EMERGE_DURATION_TICKS = 134;
	private float lastEmergeProgress;
	private final CreakingEntityRenderer<ShadowCreakingEntity> vanillaDelegate;
	private float currentScale = 1.0f;

	public ShadowCreakingEntityRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new ShadowCreakingPlantingModel(ctx.getPart(CREAKING_LAYER)), 0.6f);
		this.vanillaDelegate = new CreakingEntityRenderer<>(ctx);
	}

	@Override
	public ShadowCreakingRenderState createRenderState() {
		return new ShadowCreakingRenderState();
	}

	@Override
	public void updateRenderState(ShadowCreakingEntity entity, ShadowCreakingRenderState state, float tickDelta) {
		// Populate all vanilla creaking animation state via delegate
		// We need to cast because vanillaDelegate expects CreakingEntityRenderState but we have ShadowCreakingRenderState
		CreakingEntityRenderState vanillaState = state; // Safe upcast
		this.vanillaDelegate.updateRenderState(entity, vanillaState, tickDelta);
		
		// Copy animation states from entity to render state
		state.emergingAnimationState.copyFrom(entity.emergingAnimationState);
		state.levitatingAnimationState.copyFrom(entity.levitatingAnimationState);
		
		// Decide scale for this entity instance
		if (entity instanceof MiniShadowCreakingEntity) {
			this.currentScale = 0.5f; // render scale matches 0.5x hitbox
		} else if (entity instanceof TinyShadowCreakingEntity) {
			this.currentScale = 0.25f; // render scale matches 0.25x hitbox
		} else {
			this.currentScale = 1.0f;
		}
		
		// Store run overlay state in render state
		state.runOverlay = entity.isForcingRunOverlay();
		
		try {
			// Drive emerge progress from the entity's AnimationState time when POSE == EMERGING
			float raw;
            if (entity.getPose() == net.minecraft.entity.EntityPose.EMERGING
                && entity.age <= EMERGE_DURATION_TICKS) {
                float ms = (float)entity.emergingAnimationState.getTimeInMilliseconds(entity.age);
                raw = MathHelper.clamp(ms / 6700.0f, 0.0f, 1.0f);
            } else {
				raw = 1.0f;
			}
			// Remapped to surface earlier (~2s), hold mid-height until final rise (~0.70 raw),
			// then complete the rise to standing.
			float mapped;
			if (raw < 0.12f) {
				// Initial settle: shallow sink only
				float x = raw / 0.12f;
				float e = 1.0f - (float)Math.pow(1.0f - x, 2.0);
				mapped = e * 0.12f;
			} else if (raw < 0.30f) {
				// Early surfacing to expose legs/feet above ground by ~2s
				float x = (raw - 0.12f) / 0.18f;
				float e = x < 0.5f ? 2.0f * x * x : 1.0f - (float)Math.pow(-2.0f * x + 2.0f, 2.0) / 2.0f;
				mapped = 0.12f + e * 0.44f; // -> 0.56 at raw=0.30
			} else if (raw < 0.70f) {
				// Plateau so the body holds while the leg provides leverage
				mapped = 0.56f;
			} else if (raw < 0.90f) {
				// Final rise
				float x = (raw - 0.70f) / 0.20f;
				mapped = 0.56f + x * 0.43f; // 0.56 -> 0.99
			} else {
				// Final settle
				float x = (raw - 0.90f) / 0.10f;
				mapped = 0.99f + x * 0.01f;
			}
			this.lastEmergeProgress = MathHelper.clamp(mapped, 0.0f, 1.0f);
			state.emergeProgress = this.lastEmergeProgress;
			
			// Store emerging state
			state.emergingActive = entity.getPose() == net.minecraft.entity.EntityPose.EMERGING && entity.age <= EMERGE_DURATION_TICKS;
			
			// Store levitation state
			boolean lev = entity.isLevitating() || entity.levitatingAnimationState.isRunning();
			state.levitatingActive = lev;
			if (lev) {
				state.levitatingMs = (float)entity.levitatingAnimationState.getTimeInMilliseconds(entity.age) + tickDelta * 50.0f;
			} else {
				state.levitatingMs = 0.0f;
			}
		} catch (Throwable ignored) {
			this.lastEmergeProgress = 1.0f;
			state.emergeProgress = 1.0f;
		}
	}

	@Override
	public Identifier getTexture(ShadowCreakingRenderState state) {
		return TEXTURE;
	}

	@Override
	public void render(ShadowCreakingRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState) {
		float progress = state.emergeProgress; // 0..1
		float inv = 1.0f - progress;
		float h = inv * 1.6f;
		if (h > 0.0f) {
			matrices.translate(0.0, -h, 0.0);
		}
		if (this.currentScale != 1.0f) {
			matrices.scale(this.currentScale, this.currentScale, this.currentScale);
		}
		super.render(state, matrices, commandQueue, cameraState);
	}

	private static final class ShadowCreakingPlantingModel extends CreakingEntityModel {
		private final ModelPart modelRoot;
		private ModelPart body;
		private ModelPart head;
		private ModelPart leftArm;
		private ModelPart rightArm;
		private ModelPart leftLeg;
		private ModelPart rightLeg;
		private final net.minecraft.client.render.entity.animation.Animation emergingAnim;
		private final net.minecraft.client.render.entity.animation.Animation levitatingAnim;
		private float lastLevitationAngle;

		public ShadowCreakingPlantingModel(ModelPart root) {
			super(root);
			this.modelRoot = root;
			try {
				// First, try common direct names
				this.body = tryGet(root, "body");
				this.head = tryGet(root, "head");
				this.leftArm = tryGet(root, "left_arm");
				this.rightArm = tryGet(root, "right_arm");
				this.leftLeg = tryGet(root, "left_leg");
				this.rightLeg = tryGet(root, "right_leg");
				// If missing, do a deep recursive discovery based on name substrings
				if (this.body == null) this.body = findDeepByNames(root, new String[]{"body"}, new String[]{"torso", "spine", "chest"});
				if (this.head == null) this.head = findDeepByNames(root, new String[]{"head"}, new String[]{"skull"});
				if (this.leftArm == null) this.leftArm = findDeepByNames(root, new String[]{"left","arm"}, new String[]{"arm_left","leftarm","left","hand"});
				if (this.rightArm == null) this.rightArm = findDeepByNames(root, new String[]{"right","arm"}, new String[]{"arm_right","rightarm","right","hand"});
				if (this.leftLeg == null) this.leftLeg = findDeepByNames(root, new String[]{"left","leg"}, new String[]{"leg_left","leftleg","left","thigh"});
				if (this.rightLeg == null) this.rightLeg = findDeepByNames(root, new String[]{"right","leg"}, new String[]{"leg_right","rightleg","right","thigh"});
			} catch (Throwable ignored) {}
			this.emergingAnim = com.theendupdate.entity.animation.ShadowCreakingAnimations.EMERGING.createAnimation(root);
			this.levitatingAnim = com.theendupdate.entity.animation.ShadowCreakingAnimations.LEVITATING.createAnimation(root);
		}

		private ModelPart tryGet(ModelPart base, String name) {
			if (base == null) return null;
			try {
				return base.getChild(name);
			} catch (Throwable ignored) {
				return null;
			}
		}

		@Override
		public void setAngles(CreakingEntityRenderState state) {
			// Cast to our custom render state (safe because our renderer always creates ShadowCreakingRenderState)
			if (!(state instanceof ShadowCreakingRenderState)) {
				super.setAngles(state);
				return;
			}
			ShadowCreakingRenderState shadowState = (ShadowCreakingRenderState) state;
			
			// Take full control during LEVITATING just like EMERGING
			if (shadowState.levitatingActive) {
				for (ModelPart part : this.modelRoot.traverse()) part.resetTransform();
				this.levitatingAnim.apply(shadowState.levitatingAnimationState, shadowState.age, 1.0f);
				// Ensure parts resolved for spin overlay
				if (this.head == null) {
					this.head = tryGet(this.modelRoot, "head");
					if (this.head == null) this.head = findDeepByNames(this.modelRoot, new String[]{"head"}, new String[]{"skull","cranium","neck"});
				}
				if (this.body == null) {
					this.body = tryGet(this.modelRoot, "body");
					if (this.body == null) this.body = findDeepByNames(this.modelRoot, new String[]{"body"}, new String[]{"torso","chest"});
				}
				// After the 2s intro, overlay rapid head spin
				float levMsNow = (float)shadowState.levitatingAnimationState.getTimeInMilliseconds(shadowState.age);
				if (levMsNow >= 2000.0f) {
					ModelPart spinTarget = this.head != null ? this.head : this.body;
					if (spinTarget != null) {
						// Use age in ticks to accumulate spin delta frame-over-frame for stability
						float seconds = (shadowState.age % 200000) / 20.0f; // avoid float overflow
						float revolutionsPerSecond = 3.0f;
						float angle = (float)(Math.PI * 2.0) * revolutionsPerSecond * seconds;
						float twoPi = (float)(Math.PI * 2.0);
						angle = angle % twoPi;
						spinTarget.yaw -= this.lastLevitationAngle;
						spinTarget.yaw += angle;
						this.lastLevitationAngle = angle;
					}
				}
				return;
			}
			// Apply EMERGING animation 1:1 during emerge; otherwise defer to vanilla setAngles
			if (shadowState.emergingActive) {
				for (ModelPart part : this.modelRoot.traverse()) part.resetTransform();
				this.emergingAnim.apply(shadowState.emergingAnimationState, shadowState.age, 1.0f);

				// Overlay physical mechanics: hands plant early, then a right-leg step drives the rise
				// without snapping arms backward.
				float p = MathHelper.clamp(shadowState.emergeProgress, 0.0f, 1.0f); // vertical progress 0..1
				float ms = (float)shadowState.emergingAnimationState.getTimeInMilliseconds(shadowState.age);
				float raw = MathHelper.clamp(ms / 6700.0f, 0.0f, 1.0f); // timeline 0..1
				float release = p > 0.98f ? MathHelper.clamp((p - 0.98f) / 0.02f, 0.0f, 1.0f) : 0.0f;
				float overlayFade = 1.0f - MathHelper.clamp((raw - 0.90f) / 0.10f, 0.0f, 1.0f); // fade overlays before the very end
				float scale = (1.0f - release) * overlayFade;
				float heightNorm = MathHelper.clamp(1.0f - p, 0.0f, 1.0f); // 1 high above, 0 near ground

				// Early reach-down while settling (raw≈0.05..0.45)
				float pre = MathHelper.clamp((raw - 0.04f) / 0.41f, 0.0f, 1.0f);
				// Arm plant scales with height to avoid clipping when near ground
				float plantByHeight = MathHelper.clamp(heightNorm * 0.35f + 0.12f, 0.0f, 1.0f); // hover just above surface
				// Positive pitch bends arms forward toward the ground on this rig
				float earlyDownDeg = 52.0f * pre * (1.0f - MathHelper.clamp((raw - 0.45f) / 0.14f, 0.0f, 1.0f)) * plantByHeight * scale;
				float earlyDownRad = earlyDownDeg * (float)Math.PI / 180.0f;
				if (this.leftArm != null) this.leftArm.pitch += earlyDownRad;
				if (this.rightArm != null) this.rightArm.pitch += earlyDownRad;
				// Clamp arm pitch to avoid floor clipping (allow strong forward bend; limit positive up-rotation)
				float maxDownDegHigh = 85.0f;
				float maxDownDegLow = 45.0f;
				float maxDownDeg = maxDownDegHigh + (maxDownDegLow - maxDownDegHigh) * MathHelper.clamp(p, 0.0f, 1.0f);
				float maxDownRad = maxDownDeg * (float)Math.PI / 180.0f;
				float maxUpRad = 0.75f; // ~43°
				if (this.leftArm != null) this.leftArm.pitch = MathHelper.clamp(this.leftArm.pitch, -maxUpRad, maxDownRad);
				if (this.rightArm != null) this.rightArm.pitch = MathHelper.clamp(this.rightArm.pitch, -maxUpRad, maxDownRad);

				// Right-leg step previously driven procedurally is now fully authored in keyframes
				float upT = MathHelper.clamp((raw - 0.42f) / 0.14f, 0.0f, 1.0f);
				float up = (float)Math.sin((Math.PI * 0.5f) * upT);
				float downT = MathHelper.clamp((raw - 0.56f) / 0.44f, 0.0f, 1.0f);
				float down = 1.0f - (float)Math.pow(downT, 1.3);
				float stepFactor = raw < 0.56f ? up : down; // used for arm/body blending only

				// As the step begins, ease arms back toward neutral (no backward snap)
				float armLiftBackDeg = 25.0f * stepFactor * scale * (1.0f - heightNorm);
				float armLiftBackRad = armLiftBackDeg * (float)Math.PI / 180.0f;
				if (this.leftArm != null) this.leftArm.pitch -= armLiftBackRad;
				if (this.rightArm != null) this.rightArm.pitch -= armLiftBackRad;

				// Body bend coordinated with the step and rise (deeper waist bend)
				// Blend waist bend down before the end so stance is neutral when EMERGING ends
				float standWindow = MathHelper.clamp((raw - 0.70f) / 0.26f, 0.0f, 1.0f); // extend to align with slow retract
				float bodyLeanDeg = (18.0f * pre * (1.0f - stepFactor) + 32.0f * stepFactor * (1.0f - standWindow)) * overlayFade;
				if (this.body != null) this.body.pitch += (bodyLeanDeg * scale) * (float)Math.PI / 180.0f;

				// Legs: do not procedurally override; keyframes handle early plant and hold
				return;
			}
			// Cast state for vanilla method - safe because ShadowCreakingRenderState extends CreakingEntityRenderState
			super.setAngles(shadowState);
			// Ensure critical parts are located even if not found at construction time
			if (this.head == null) {
				this.head = tryGet(this.modelRoot, "head");
				if (this.head == null) this.head = findDeepByNames(this.modelRoot, new String[]{"head"}, new String[]{"skull","cranium","neck"});
			}
			if (this.body == null) {
				this.body = tryGet(this.modelRoot, "body");
				if (this.body == null) this.body = findDeepByNames(this.modelRoot, new String[]{"body"}, new String[]{"torso","chest"});
			}
			if (this.leftArm == null) {
				this.leftArm = tryGet(this.modelRoot, "left_arm");
				if (this.leftArm == null) this.leftArm = findDeepByNames(this.modelRoot, new String[]{"left","arm"}, new String[]{"arm_left","leftarm","left","hand"});
			}
			if (this.rightArm == null) {
				this.rightArm = tryGet(this.modelRoot, "right_arm");
				if (this.rightArm == null) this.rightArm = findDeepByNames(this.modelRoot, new String[]{"right","arm"}, new String[]{"arm_right","rightarm","right","hand"});
			}

			// Clear any leftover overlay when levitation ends
			if (!shadowState.levitatingActive && this.lastLevitationAngle != 0.0f) {
				ModelPart spinTarget = this.head != null ? this.head : this.body;
				if (spinTarget != null) {
					spinTarget.yaw -= this.lastLevitationAngle;
				}
				this.lastLevitationAngle = 0.0f;
			}

			// Run overlay: add slight leg swing if requested to ensure motion isn't visually static
			if (shadowState.runOverlay && this.leftLeg != null && this.rightLeg != null) {
				float swing = 0.5f;
				float speed = 0.25f;
				float t = (shadowState.age % 200000) * speed;
				this.leftLeg.pitch += MathHelper.sin(t) * swing * 0.6f;
				this.rightLeg.pitch += MathHelper.sin(t + (float)Math.PI) * swing * 0.6f;
			}

			// Remove manual arm overlay; handled by authored LEVITATING animation
		}

		@SuppressWarnings("unchecked")
		private static Map<String, ModelPart> getChildrenMap(ModelPart part) {
			if (part == null) return null;
			try {
				Field f = null;
				for (Field ff : part.getClass().getDeclaredFields()) {
					if (Map.class.isAssignableFrom(ff.getType())) { f = ff; break; }
				}
				if (f == null) return null;
				f.setAccessible(true);
				Object v = f.get(part);
				if (v instanceof Map) return (Map<String, ModelPart>) v;
			} catch (Throwable ignored) {}
			return null;
		}

		private static ModelPart findDeepByNames(ModelPart base, String[] requiredAll, String[] altAny) {
			ModelPart found = findDeepByNames0(base, requiredAll);
			if (found != null) return found;
			if (altAny != null) {
				for (String token : altAny) {
					found = findDeepByNames0(base, new String[]{token});
					if (found != null) return found;
				}
			}
			return null;
		}

		private static ModelPart findDeepByNames0(ModelPart base, String[] reqs) {
			Map<String, ModelPart> map = getChildrenMap(base);
			if (map == null || map.isEmpty()) return null;
			for (Map.Entry<String, ModelPart> e : map.entrySet()) {
				String key = e.getKey();
				if (key != null) {
					String k = key.toLowerCase();
					boolean ok = true;
					for (String r : reqs) { if (!k.contains(r)) { ok = false; break; } }
					if (ok) return e.getValue();
				}
				ModelPart deeper = findDeepByNames0(e.getValue(), reqs);
				if (deeper != null) return deeper;
			}
			return null;
		}
	}
}
