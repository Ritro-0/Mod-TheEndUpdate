package com.theendupdate.entity.animation;

import net.minecraft.client.render.entity.animation.*;

/**
 * Made with Blockbench 4.12.6
 * Exported for Minecraft version 1.19 or later with Yarn mappings
 * This class now exposes the raw Transformations for use with the 1.21.8 API.
 */
public class Ethereal {

	public static final AnimationDefinition ANIMATION = AnimationDefinition.Builder.create(0.41667f)
	.addBoneAnimation("legs",
		new Transformation(Transformation.Targets.MOVE_ORIGIN, 
			new Keyframe(0f, AnimationHelper.createTranslationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR), 
			new Keyframe(0.41667f, AnimationHelper.createTranslationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR)))
	.addBoneAnimation("legs",
		new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0f, AnimationHelper.createRotationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.41667f, AnimationHelper.createRotationalVector(12f, 0f, 0f),
				Transformation.Interpolations.LINEAR))).build();
	public static final AnimationDefinition ANIMATION2 = AnimationDefinition.Builder.create(0.375f)
	.addBoneAnimation("legs",
		new Transformation(Transformation.Targets.MOVE_ORIGIN, 
			new Keyframe(0f, AnimationHelper.createTranslationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR)))
	.addBoneAnimation("legs",
		new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0f, AnimationHelper.createRotationalVector(12f, 0f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.375f, AnimationHelper.createRotationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR)))
	.addBoneAnimation("body",
		new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0f, AnimationHelper.createRotationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.1875f, AnimationHelper.createRotationalVector(-5f, 0f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.375f, AnimationHelper.createRotationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR))).build();
	public static final AnimationDefinition ANIMATION3 = AnimationDefinition.Builder.create(2.7916765f)
	.addBoneAnimation("body",
		new Transformation(Transformation.Targets.ROTATE,
			new Keyframe(0f, AnimationHelper.createRotationalVector(0f, 0f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.375f, AnimationHelper.createRotationalVector(0f, 40f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.5834334f, AnimationHelper.createRotationalVector(0f, 37.5f, 0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(2f, AnimationHelper.createRotationalVector(0f, -365f, 0f),
				Transformation.Interpolations.LINEAR)))
	.addBoneAnimation("body",
		new Transformation(Transformation.Targets.SCALE,
			new Keyframe(0f, AnimationHelper.createScalingVector(1f, 1f, 1f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.3433333f, AnimationHelper.createScalingVector(1.0f, 1.25f, 1.0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(0.9167666f, AnimationHelper.createScalingVector(1.0f, 0.7f, 1.0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(1.7083433f, AnimationHelper.createScalingVector(1.0f, 0.8f, 1.0f),
				Transformation.Interpolations.LINEAR),
			new Keyframe(2f, AnimationHelper.createScalingVector(1f, 1f, 1f),
				Transformation.Interpolations.LINEAR))).build();
}