package com.theendupdate.entity.animation;

import net.minecraft.client.animation.*;

/**
 * Made with Blockbench 4.12.6
 * Exported for Minecraft version 1.19 or later with Yarn mappings
 * This class now exposes the raw Transformations for use with the 1.21.8 API.
 */
public class Ethereal {

	public static final AnimationDefinition ANIMATION = AnimationDefinition.Builder.withLength(0.41667f)
	.addAnimation("legs",
		new AnimationChannel(AnimationChannel.Targets.POSITION, 
			new Keyframe(0f, KeyframeAnimations.posVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR), 
			new Keyframe(0.41667f, KeyframeAnimations.posVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR)))
	.addAnimation("legs",
		new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.41667f, KeyframeAnimations.degreeVec(12f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR))).build();
	public static final AnimationDefinition ANIMATION2 = AnimationDefinition.Builder.withLength(0.375f)
	.addAnimation("legs",
		new AnimationChannel(AnimationChannel.Targets.POSITION, 
			new Keyframe(0f, KeyframeAnimations.posVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR)))
	.addAnimation("legs",
		new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0f, KeyframeAnimations.degreeVec(12f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.375f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR)))
	.addAnimation("body",
		new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.1875f, KeyframeAnimations.degreeVec(-5f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.375f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR))).build();
	public static final AnimationDefinition ANIMATION3 = AnimationDefinition.Builder.withLength(2.7916765f)
	.addAnimation("body",
		new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.375f, KeyframeAnimations.degreeVec(0f, 40f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5834334f, KeyframeAnimations.degreeVec(0f, 37.5f, 0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2f, KeyframeAnimations.degreeVec(0f, -365f, 0f),
				AnimationChannel.Interpolations.LINEAR)))
	.addAnimation("body",
		new AnimationChannel(AnimationChannel.Targets.SCALE,
			new Keyframe(0f, KeyframeAnimations.scaleVec(1f, 1f, 1f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.3433333f, KeyframeAnimations.scaleVec(1.0f, 1.25f, 1.0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.9167666f, KeyframeAnimations.scaleVec(1.0f, 0.7f, 1.0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.7083433f, KeyframeAnimations.scaleVec(1.0f, 0.8f, 1.0f),
				AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2f, KeyframeAnimations.scaleVec(1f, 1f, 1f),
				AnimationChannel.Interpolations.LINEAR))).build();
}