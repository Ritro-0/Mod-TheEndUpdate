package com.theendupdate.entity.state;

import com.theendupdate.entity.EtherealOrbEntity;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * Custom render state for the Ethereal Orb entity
 * 
 * This class extends EntityRenderState to provide custom rendering behavior
 * for the ethereal orb, including animation state and visual effects.
 */
public class EtherealOrbRenderState extends LivingEntityRenderState {
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    public boolean hasCustomName;
}