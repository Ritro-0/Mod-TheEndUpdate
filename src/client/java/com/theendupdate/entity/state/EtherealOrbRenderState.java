package com.theendupdate.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class EtherealOrbRenderState extends LivingEntityRenderState {
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    public final AnimationState rotateAnimationState = new AnimationState();
    public boolean charged;
    public boolean baby;
    public boolean stunted;
    public boolean bulbPresent;
}