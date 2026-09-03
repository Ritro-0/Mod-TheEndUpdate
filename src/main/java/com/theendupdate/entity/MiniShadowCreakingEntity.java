package com.theendupdate.entity;

import com.theendupdate.registry.ModEntities;
import com.theendupdate.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MiniShadowCreakingEntity extends ShadowCreakingEntity {
	private int childTinyDropRoleA = TinyShadowCreakingEntity.DROP_NONE;
	private int childTinyDropRoleB = TinyShadowCreakingEntity.DROP_NONE;

	public MiniShadowCreakingEntity(EntityType<? extends MiniShadowCreakingEntity> entityType, Level world) {
		super(entityType, world);
		this.isMainEntity = false;
		this.xpReward = 8;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModItems.MINI_SHADOW_CREAKING_SPAWN_EGG);
	}

	public static AttributeSupplier.Builder createMiniAttributes() {
		return ShadowCreakingEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.MOVEMENT_SPEED, 0.42)
			.add(Attributes.ATTACK_DAMAGE, 12.0)
			.add(Attributes.FOLLOW_RANGE, 36.0);
	}

	@Override
	public float getRenderScale() {
		return 0.5F;
	}

	@Override
	public float getDamageMultiplier() {
		return 0.7F;
	}

	@Override
	protected boolean usesHalfHealthLevitation() {
		return false;
	}

	@Override
	public boolean isWeepingAngelActive() {
		return false;
	}

	@Override
	protected boolean usesWeepingActivation() {
		return false;
	}

	@Override
	protected boolean shouldSpawnOnDeath() {
		return false;
	}

	public void setChildTinyDropRoles(int roleA, int roleB) {
		this.childTinyDropRoleA = roleA;
		this.childTinyDropRoleB = roleB;
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!wasKilledByPlayer(damageSource)) {
			return;
		}

		TinyShadowCreakingEntity tinyA = new TinyShadowCreakingEntity(ModEntities.TINY_SHADOW_CREAKING, serverLevel);
		TinyShadowCreakingEntity tinyB = new TinyShadowCreakingEntity(ModEntities.TINY_SHADOW_CREAKING, serverLevel);
		tinyA.setDropRole(this.childTinyDropRoleA);
		tinyB.setDropRole(this.childTinyDropRoleB);
		RandomSource random = serverLevel.getRandom();
		tinyA.setScheduledCombatWarmup(30 + random.nextInt(40));
		tinyB.setScheduledCombatWarmup(80 + random.nextInt(50));
		tagParentSpawn(tinyA);
		tagParentSpawn(tinyB);
		if (this.bossBarManager != null) {
			tinyA.bossBarManager = this.bossBarManager;
			tinyB.bossBarManager = this.bossBarManager;
			tinyA.isMainEntity = false;
			tinyB.isMainEntity = false;
			this.bossBarManager.addTinyEntity(tinyA);
			this.bossBarManager.addTinyEntity(tinyB);
		}
		List<ShadowCreakingEntity> toSpawn = new ArrayList<>();
		toSpawn.add(tinyA);
		toSpawn.add(tinyB);
		ShadowCreakingSpawnHelper.spawnEntitiesWithValidPositions(
			serverLevel, toSpawn, this.getX(), this.getY(), this.getZ());
	}
}
