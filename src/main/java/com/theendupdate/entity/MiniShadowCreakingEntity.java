package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
// no extra imports needed
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
        if (!this.getWorld().isClient && this.age == 1) {
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
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        // Spawn tiny children if killed by player, or if roles were preset from a parent
        boolean allowSpawn = wasKilledByPlayer(damageSource) ||
            (this.childTinyDropRoleA != com.theendupdate.entity.TinyShadowCreakingEntity.DROP_NONE || this.childTinyDropRoleB != com.theendupdate.entity.TinyShadowCreakingEntity.DROP_NONE);
        if (!allowSpawn) return;
        double baseX = this.getX();
        double baseY = this.getY();
        double baseZ = this.getZ();
        double separation = 1.0; // 2.0 blocks between centers for tinies
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        boolean spawned = false;
        for (int attempt = 0; attempt < 8 && !spawned; attempt++) {
            double a = angle + attempt * (Math.PI / 4.0);
            double dirX = Math.cos(a);
            double dirZ = Math.sin(a);
            double x1 = baseX - dirX * separation;
            double z1 = baseZ - dirZ * separation;
            double x2 = baseX + dirX * separation;
            double z2 = baseZ + dirZ * separation;
            TinyShadowCreakingEntity s1 = new TinyShadowCreakingEntity(com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING, sw);
            TinyShadowCreakingEntity s2 = new TinyShadowCreakingEntity(com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING, sw);
            s1.refreshPositionAndAngles(x1, baseY, z1, this.getYaw(), this.getPitch());
            s2.refreshPositionAndAngles(x2, baseY, z2, this.getYaw(), this.getPitch());
            // Apply preassigned drop roles to tinies
            s1.setDropRole(this.childTinyDropRoleA);
            s2.setDropRole(this.childTinyDropRoleB);
            if (sw.isSpaceEmpty(s1) && sw.isSpaceEmpty(s2)) {
                // Minis spawned from parent should signal to tinies that they are parent-spawned too
                try { s1.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
                try { s2.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
                sw.spawnEntity(s1);
                sw.spawnEntity(s2);
                spawned = true;
            }
        }
        if (!spawned) {
            for (int i = 0; i < 2; i++) {
                TinyShadowCreakingEntity spawn = new TinyShadowCreakingEntity(com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING, sw);
                double ox = baseX + (this.random.nextDouble() - 0.5) * 1.0;
                double oz = baseZ + (this.random.nextDouble() - 0.5) * 1.0;
                spawn.refreshPositionAndAngles(ox, baseY, oz, this.getYaw(), this.getPitch());
                if (i == 0) spawn.setDropRole(this.childTinyDropRoleA);
                else spawn.setDropRole(this.childTinyDropRoleB);
                try { spawn.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
                sw.spawnEntity(spawn);
            }
        }
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


