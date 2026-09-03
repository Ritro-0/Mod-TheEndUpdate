package com.theendupdate.entity;

import com.theendupdate.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class TinyShadowCreakingEntity extends ShadowCreakingEntity {
	public static final int DROP_NONE = 0;
	public static final int DROP_ENCHANTED_BOOK_COVER = 1;
	public static final int DROP_ENCHANTED_PAGES = 2;
	public static final int DROP_WOOD_CHIP = 3;

	private int dropRole = DROP_NONE;
	private boolean dropSpawned;

	public TinyShadowCreakingEntity(EntityType<? extends TinyShadowCreakingEntity> entityType, Level world) {
		super(entityType, world);
		this.isMainEntity = false;
		this.xpReward = 3;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModItems.TINY_SHADOW_CREAKING_SPAWN_EGG);
	}

	public static AttributeSupplier.Builder createTinyAttributes() {
		return ShadowCreakingEntity.createAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.58)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.FOLLOW_RANGE, 28.0);
	}

	@Override
	public float getRenderScale() {
		return 0.25F;
	}

	@Override
	public float getDamageMultiplier() {
		return 0.5F;
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

	public void setDropRole(int role) {
		this.dropRole = role;
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
			this.spawnDeterministicDrop(serverLevel);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHitByPlayer) {
		this.spawnDeterministicDrop(level);
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		if (this.dropRole != DROP_NONE) {
			return true;
		}
		return super.shouldDropLoot(level);
	}

	private void spawnDeterministicDrop(ServerLevel level) {
		if (this.dropSpawned) {
			return;
		}
		ItemStack drop = createDropForRole(this.dropRole);
		if (drop.isEmpty()) {
			return;
		}
		this.spawnAtLocation(level, drop);
		this.dropSpawned = true;
	}

	@Nullable
	private static ItemStack createDropForRole(int role) {
		return switch (role) {
			case DROP_ENCHANTED_BOOK_COVER -> new ItemStack(ModItems.ENCHANTED_BOOK_COVER);
			case DROP_ENCHANTED_PAGES -> new ItemStack(ModItems.ENCHANTED_PAGES);
			case DROP_WOOD_CHIP -> new ItemStack(ModItems.WOOD_CHIP);
			default -> ItemStack.EMPTY;
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("DropRole", this.dropRole);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.dropRole = input.getIntOr("DropRole", DROP_NONE);
	}
}
