package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class MiniShadowCreakingEntity extends ShadowCreakingEntity {

    // Each mini carries two preassigned drop roles for the tiny children
    private int childTinyDropRoleA = com.theendupdate.entity.TinyShadowCreakingEntity.DROP_NONE;
    private int childTinyDropRoleB = com.theendupdate.entity.TinyShadowCreakingEntity.DROP_NONE;

    public MiniShadowCreakingEntity(EntityType<? extends ShadowCreakingEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 3;
    }

    public static DefaultAttributeContainer.Builder createMiniAttributes() {
        return ShadowCreakingEntity.createShadowCreakingAttributes()
            .add(EntityAttributes.MAX_HEALTH, 83.3333333333)
            .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.56)
            .add(EntityAttributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected boolean shouldTriggerHalfHealthLevitation() {
        // Minis do not have a second-phase levitation; they are never weeping angels
        return false;
    }

    @Override
    protected boolean isWeepingAngelActive() {
        // Minis never freeze when looked at
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // If spawned by a parent (main creaking), force a one-time intro regardless of altar
        if (!this.getEntityWorld().isClient() && this.age == 1) {
            try {
                java.util.Set<String> tags = this.getCommandTags();
                if (tags != null && tags.contains("theendupdate:spawned_by_parent")) {
                    if (!this.dataTracker.get(LEVITATION_INTRO_PLAYED)) {
                        this.setPose(net.minecraft.entity.EntityPose.EMERGING);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!(this.getEntityWorld() instanceof ServerWorld sw)) return;
        
        // Handle boss bar cleanup
        if (this.bossBarManager != null) {
            this.bossBarManager.removeEntity(this.getUuid());
        }
        
        // Only spawn tiny children if killed by player (consistent with regular shadow creaking)
        if (!wasKilledByPlayer(damageSource)) return;
        
        // Create the two tiny entities to spawn
        TinyShadowCreakingEntity s1 = new TinyShadowCreakingEntity(com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING, sw);
        TinyShadowCreakingEntity s2 = new TinyShadowCreakingEntity(com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING, sw);
        s1.setDropRole(this.childTinyDropRoleA);
        s2.setDropRole(this.childTinyDropRoleB);
        try { s1.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
        try { s2.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
        
        // Set up boss bar for spawned tiny entities
        if (this.bossBarManager != null) {
            s1.bossBarManager = this.bossBarManager;
            s2.bossBarManager = this.bossBarManager;
            s1.isMainEntity = false;
            s2.isMainEntity = false;
            
            // Add tiny entities to boss bar tracking
            this.bossBarManager.addTinyEntity(s1);
            this.bossBarManager.addTinyEntity(s2);
        }
        
        // Find valid spawn positions and spawn entities
        java.util.List<ShadowCreakingEntity> toSpawn = new java.util.ArrayList<>();
        toSpawn.add(s1);
        toSpawn.add(s2);
        spawnEntitiesWithValidPositions(sw, toSpawn, this.getX(), this.getY(), this.getZ());
    }

    @Override
    protected boolean shouldSpawnOnDeath() {
        // Prevent base class from spawning more minis on death
        return false;
    }

    @Override
    public boolean isAiDisabled() {
        // Never allow AI to be disabled due to gaze
        return false;
    }

    public void setChildTinyDropRoles(int roleA, int roleB) {
        this.childTinyDropRoleA = roleA;
        this.childTinyDropRoleB = roleB;
    }

}


