package com.theendupdate.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class ShadowCreakingRenderState extends LivingEntityRenderState {
	public final AnimationState roarAnimationState = new AnimationState();
	public final AnimationState stumbleAnimationState = new AnimationState();
	public final AnimationState levitatingAnimationState = new AnimationState();
	public final AnimationState spawnAnimationState = new AnimationState();
	public boolean canMove = true;
	public boolean levitating;
	public byte combatPhase;
	/** Seconds elapsed in the current combat phase (server-driven). */
	public float combatPhaseSeconds;
	public boolean spawnIntroActive;
	public boolean spawnIntroHidden;
	public float spawnIntroProgress;
	public float variantScale = 1.0F;
}
