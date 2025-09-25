package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.world.World;

public class TinyShadowCreakingEntity extends ShadowCreakingEntity {

    // Drop roles
    public static final int DROP_NONE = 0;
    public static final int DROP_ENCHANTED_BOOK_COVER = 1;
    public static final int DROP_ENCHANTED_PAGES = 2;
    public static final int DROP_WOOD_CHIP = 3;

    private int dropRole = DROP_NONE;

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

    public void setDropRole(int role) {
        this.dropRole = role;
    }

    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!(this.getWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return;
        if (!wasKilledByPlayer(damageSource)) return;
        // Apply the deterministic drop based on assigned role
        net.minecraft.item.Item dropItem = null;
        if (this.dropRole == DROP_ENCHANTED_BOOK_COVER) {
            dropItem = com.theendupdate.registry.ModItems.ENCHANTED_BOOK_COVER;
        } else if (this.dropRole == DROP_ENCHANTED_PAGES) {
            dropItem = com.theendupdate.registry.ModItems.ENCHANTED_PAGES;
        } else if (this.dropRole == DROP_WOOD_CHIP) {
            dropItem = com.theendupdate.registry.ModItems.WOOD_CHIP;
        }
        if (dropItem != null) {
            this.dropStack(sw, new net.minecraft.item.ItemStack(dropItem));
        }
    }
}


