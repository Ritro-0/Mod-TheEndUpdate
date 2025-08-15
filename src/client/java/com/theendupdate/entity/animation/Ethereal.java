package com.theendupdate.entity.animation;

import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

/**
 * Made with Blockbench 4.12.6
 * Exported for Minecraft version 1.19 or later with Yarn mappings
 * This class now exposes the raw Transformations for use with the 1.21.8 API.
 */
public class Ethereal {
	public static final Transformation LEGS_ROTATE = new Transformation(
		Transformation.Targets.ROTATE,
		new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
		new Keyframe(0.4167F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
	);

	public static final Transformation LEGS_MOVE_ORIGIN = new Transformation(
		Transformation.Targets.MOVE_ORIGIN,
		new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
		new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
	);

	public static final Transformation LEGS_ROTATE_BACK = new Transformation(
		Transformation.Targets.ROTATE,
		new Keyframe(0.0F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
		new Keyframe(0.375F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
	);

	public static final Transformation LEGS_MOVE_ORIGIN_CONST = new Transformation(
		Transformation.Targets.MOVE_ORIGIN,
		new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
	);
}