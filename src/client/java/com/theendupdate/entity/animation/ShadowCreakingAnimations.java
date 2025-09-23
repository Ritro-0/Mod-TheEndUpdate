package com.theendupdate.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.animation.AnimationDefinition;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

@Environment(EnvType.CLIENT)
public final class ShadowCreakingAnimations {
	private ShadowCreakingAnimations() {}

	// Duration: 6.7s (≈134 ticks) to mirror Warden EMERGING timing
	public static final AnimationDefinition EMERGING = AnimationDefinition.Builder.create(6.7f)
		// Body: small early settle, then a forward lean during the push-up, recover to neutral
		.addBoneAnimation("body",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.20f, AnimationHelper.createRotationalVector(0f, -6f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.90f, AnimationHelper.createRotationalVector(0f, -6f, 0f), Transformation.Interpolations.LINEAR),
				// Prep and lean forward right before/into the lift
				new Keyframe(3.80f, AnimationHelper.createRotationalVector(-6f, -2f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(4.60f, AnimationHelper.createRotationalVector(-14f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.30f, AnimationHelper.createRotationalVector(-10f, 2f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.20f, AnimationHelper.createRotationalVector(-4f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		// Head: tilt forward early, hold, then relax (positive pitch = forward on this rig)
		.addBoneAnimation("head",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.12f, AnimationHelper.createRotationalVector(35f, -6f, -3f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.60f, AnimationHelper.createRotationalVector(35f, -6f, -3f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.88f, AnimationHelper.createRotationalVector(45f, -6f, 6f), Transformation.Interpolations.LINEAR),
				new Keyframe(1.16f, AnimationHelper.createRotationalVector(55f, -6f, 2f), Transformation.Interpolations.LINEAR),
				new Keyframe(1.28f, AnimationHelper.createRotationalVector(20f, -3f, 2f), Transformation.Interpolations.LINEAR),
				new Keyframe(3.37f, AnimationHelper.createRotationalVector(20f, -3f, 2f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		// Arms: plant early, then begin retracting BEFORE the rise to simulate a push-up
		.addBoneAnimation("left_arm",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				// Reach down early to find the floor (slightly deeper to ensure visibility)
				new Keyframe(0.06f, AnimationHelper.createRotationalVector(-45f, 8f, 8f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.30f, AnimationHelper.createRotationalVector(-112f, 12f, 10f), Transformation.Interpolations.LINEAR),
				// Maintain contact while body prepares
				new Keyframe(3.80f, AnimationHelper.createRotationalVector(-107f, 12f, 10f), Transformation.Interpolations.LINEAR),
				// Begin releasing as rise starts
				new Keyframe(4.40f, AnimationHelper.createRotationalVector(-65f, 8f, 8f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.00f, AnimationHelper.createRotationalVector(-38f, 6f, 6f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.60f, AnimationHelper.createRotationalVector(-18f, 4f, 4f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.20f, AnimationHelper.createRotationalVector(-6f, 2f, 2f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		// Keep shoulders attached: rely on rotation-only for plant, no origin movement
		.addBoneAnimation("right_arm",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				// Reach down early to find the floor (slightly deeper to ensure visibility)
				new Keyframe(0.06f, AnimationHelper.createRotationalVector(-45f, -8f, -8f), Transformation.Interpolations.LINEAR),
				new Keyframe(0.30f, AnimationHelper.createRotationalVector(-112f, -12f, -10f), Transformation.Interpolations.LINEAR),
				// Maintain contact while body prepares
				new Keyframe(3.80f, AnimationHelper.createRotationalVector(-107f, -12f, -10f), Transformation.Interpolations.LINEAR),
				// Begin releasing as rise starts
				new Keyframe(4.40f, AnimationHelper.createRotationalVector(-65f, -8f, -8f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.00f, AnimationHelper.createRotationalVector(-38f, -6f, -6f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.60f, AnimationHelper.createRotationalVector(-18f, -4f, -4f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.20f, AnimationHelper.createRotationalVector(-6f, -2f, -2f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		// Legs: right leg peaks early then holds until the final rise at ~4.70s, left leg assists
		.addBoneAnimation("left_leg",
			new Transformation(Transformation.Targets.ROTATE,
				// Small assist to balance starting slightly before the right leg peaks
				new Keyframe(2.40f, AnimationHelper.createRotationalVector(6f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(3.00f, AnimationHelper.createRotationalVector(10f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(4.50f, AnimationHelper.createRotationalVector(8f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(5.50f, AnimationHelper.createRotationalVector(4f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.30f, AnimationHelper.createRotationalVector(1f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		.addBoneAnimation("right_leg",
			new Transformation(Transformation.Targets.ROTATE,
				// Bring the foot to its standing contact angle early, hold until final rise, then drift back
				new Keyframe(1.40f, AnimationHelper.createRotationalVector(-28f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(1.80f, AnimationHelper.createRotationalVector(-42f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(2.10f, AnimationHelper.createRotationalVector(-52f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(2.50f, AnimationHelper.createRotationalVector(-52f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(3.50f, AnimationHelper.createRotationalVector(-52f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(4.70f, AnimationHelper.createRotationalVector(-52f, 0f, 0f), Transformation.Interpolations.LINEAR),
				// Then drift back to neutral over the remainder (during the final rise)
				new Keyframe(5.40f, AnimationHelper.createRotationalVector(-34f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.10f, AnimationHelper.createRotationalVector(-18f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.40f, AnimationHelper.createRotationalVector(-6f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(6.64f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR)))
		.build();

	// Duration: 7.0s (2.0s arms-out intro, then hold during hover)
	public static final AnimationDefinition LEVITATING = AnimationDefinition.Builder.create(7.0f)
		// Arms roll outward (side raise) to T-pose over first 2 seconds, then hold
		.addBoneAnimation("left_arm",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(2.00f, AnimationHelper.createRotationalVector(0f, 0f, -90f), Transformation.Interpolations.LINEAR),
				new Keyframe(7.00f, AnimationHelper.createRotationalVector(0f, 0f, -90f), Transformation.Interpolations.LINEAR)))
		.addBoneAnimation("right_arm",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(0.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				new Keyframe(2.00f, AnimationHelper.createRotationalVector(0f, 0f, 90f), Transformation.Interpolations.LINEAR),
				new Keyframe(7.00f, AnimationHelper.createRotationalVector(0f, 0f, 90f), Transformation.Interpolations.LINEAR)))
		// Head: spin rapidly during hover (2s..7s), use yaw keyframes
		.addBoneAnimation("head",
			new Transformation(Transformation.Targets.ROTATE,
				new Keyframe(2.00f, AnimationHelper.createRotationalVector(0f, 0f, 0f), Transformation.Interpolations.LINEAR),
				// 3 rotations/sec for 5s = 5400° yaw
				new Keyframe(7.00f, AnimationHelper.createRotationalVector(0f, 5400f, 0f), Transformation.Interpolations.LINEAR)))
		.build();
}


