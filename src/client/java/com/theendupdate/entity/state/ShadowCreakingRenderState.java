package com.theendupdate.entity.state;

import net.minecraft.client.render.entity.state.CreakingEntityRenderState;
import net.minecraft.entity.AnimationState;

public class ShadowCreakingRenderState extends CreakingEntityRenderState {
    public final AnimationState emergingAnimationState = new AnimationState();
    public final AnimationState levitatingAnimationState = new AnimationState();
    public boolean runOverlay;
    public float emergeProgress;
    public boolean emergingActive;
    public boolean levitatingActive;
    public float levitatingMs;
    public float lastLevitationAngle; // Per-entity rotation tracking to avoid shared state between entities
}

