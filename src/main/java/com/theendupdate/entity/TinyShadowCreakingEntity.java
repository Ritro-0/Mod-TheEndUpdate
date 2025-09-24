package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.world.World;

public class TinyShadowCreakingEntity extends ShadowCreakingEntity {

    public TinyShadowCreakingEntity(EntityType<? extends ShadowCreakingEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 1;
    }

    public static DefaultAttributeContainer.Builder createTinyAttributes() {
        // Silverfish reference: health 8.0, damage 1.0, movement ~0.25 (vanilla PathAware default ~0.25)
        return ShadowCreakingEntity.createShadowCreakingAttributes()
            .add(EntityAttributes.MAX_HEALTH, 8.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 1.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 1.12)
            .add(EntityAttributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected boolean shouldTriggerHalfHealthLevitation() {
        // Tinies never trigger below-half-health phase behavior
        return false;
    }

    @Override
    protected boolean isWeepingAngelActive() {
        // Tinies never freeze when looked at
        return false;
    }

    @Override
    protected boolean shouldSpawnOnDeath() {
        // Prevent base class from spawning further entities
        return false;
    }

    @Override
    public boolean isAiDisabled() {
        // Never allow AI to be disabled due to gaze
        return false;
    }
}


