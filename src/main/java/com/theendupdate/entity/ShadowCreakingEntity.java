package com.theendupdate.entity;

import com.theendupdate.registry.ModEntities;
import com.theendupdate.registry.ModItems;
import com.theendupdate.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ShadowCreakingEntity extends Monster {
	public static final byte PHASE_IDLE = 0;
	public static final byte PHASE_ROAR = 1;
	public static final byte PHASE_CHARGE = 2;
	public static final byte PHASE_STUMBLE = 3;
	public static final byte PHASE_FINISHER = 4;
	public static final byte PHASE_PUNCH = 5;

	private static final EntityDataAccessor<Boolean> CAN_MOVE =
		SynchedEntityData.defineId(ShadowCreakingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> IS_ACTIVE =
		SynchedEntityData.defineId(ShadowCreakingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> LEVITATING =
		SynchedEntityData.defineId(ShadowCreakingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> COMBAT_PHASE =
		SynchedEntityData.defineId(ShadowCreakingEntity.class, EntityDataSerializers.BYTE);

	private static final float ACTIVATION_RANGE_SQ = 144.0F;
	private static final float CHARGE_DAMAGE_EASY = 28.0F;
	private static final float CHARGE_DAMAGE_NORMAL = 34.0F;
	private static final float CHARGE_DAMAGE_HARD = 40.0F;

	private static final int ROAR_TICKS = 81; // Blockbench roar (3.208s) + stand-up (0.85s)
	private static final int STUMBLE_TICKS = 53; // Blockbench stumble (2.625s)
	private static final int FINISHER_TICKS = 48; // ~2.4s stand / T-pose / spin / face
	private static final int PATHFIND_COOLDOWN_TICKS = 40; // 2s after finisher
	private static final double CHARGE_SPEED = 0.85;
	private static final double CHARGE_ARRIVE_DIST_SQ = 1.5;
	private static final double CHARGE_HIT_RANGE_SQ = 3.4 * 3.4;
	private static final double CHARGE_START_RANGE_SQ = 18.0 * 18.0;
	/** Below this it closes in and punches instead of backing up for a charge. */
	private static final double CHARGE_MIN_RANGE_SQ = 8.0 * 8.0;
	private static final double CHARGE_OVERSHOOT = 5.0;
	/** Abort a charge that has barely moved after this many ticks. */
	private static final int CHARGE_STUCK_CHECK_TICKS = 8;
	private static final double CHARGE_MIN_PROGRESS = 2.0;
	/** Ticks to blend the charge's speed down into the stumble glide. */
	private static final double STUMBLE_HANDOFF_TICKS = 3.0;
	/**
	 * Speed the glide starts at once the charge momentum is shed. It falls off
	 * linearly across the whole clip, so it is still creeping on the last frame.
	 */
	private static final double STUMBLE_GLIDE_SPEED = 0.26;

	private static final float PUNCH_DAMAGE_EASY = 18.0F;
	private static final float PUNCH_DAMAGE_NORMAL = 22.0F;
	private static final float PUNCH_DAMAGE_HARD = 26.0F;
	private static final int PUNCH_TICKS = 24;
	private static final int PUNCH_IMPACT_TICK = 11;
	private static final int PUNCH_COOLDOWN_TICKS = 30;
	/** Swing when this close; the charge is saved for anything past it. */
	private static final double PUNCH_RANGE_SQ = 5.0 * 5.0;
	private static final double PUNCH_HIT_RANGE_SQ = 6.5 * 6.5;

	/** Hits from a player who is not the current target before they steal aggro. */
	private static final int AGGRO_STRIKES_TO_SWITCH = 3;
	/** A player's strike count is forgotten after this long without a follow-up hit. */
	private static final int AGGRO_MEMORY_TICKS = 200;
	/** Time spent able to fight without starting a punch or roar before the target counts as unreachable. */
	private static final int AGGRO_UNREACHABLE_TICKS = 60;
	/**
	 * Minimum gap between forced retargets. The roar chain runs roughly twelve seconds,
	 * so without this two players trading hits could keep it locked in the animation.
	 */
	private static final int AGGRO_RETARGET_COOLDOWN_TICKS = 160;

	private static final int COMBAT_WARMUP_MIN_TICKS = 50;
	private static final int COMBAT_WARMUP_MAX_TICKS = 140;

	private static final int LEVITATE_DURATION_TICKS = 140;
	private static final double LEVITATE_SPEED_PER_TICK = 0.02;
	private static final int POST_LAND_FREEZE_TICKS = 60;

	/** Invisible soul-particle buildup before the entity appears. */
	public static final int SPAWN_BUILDUP_TICKS = 70;
	/** Visible emergence with boom after buildup (or altar summon). */
	public static final int SPAWN_REVEAL_TICKS = 25;
	/** Full parent-spawn intro: buildup + reveal. */
	public static final int SPAWN_INTRO_TICKS = SPAWN_BUILDUP_TICKS + SPAWN_REVEAL_TICKS;

	public ShadowCreakingBossBarManager bossBarManager;
	public boolean isMainEntity = true;

	final ShadowCreakingMobility mobility = new ShadowCreakingMobility(this);

	private int spawnIntroTicksRemaining;
	private int spawnIntroTotalTicks;
	private boolean spawnIntroActive;
	private boolean spawnRevealBoomPlayed;

	/**
	 * Damage taken from players is always recorded, even while a gaze freeze or an
	 * animation blocks a response. The switch is applied once the mob is free to move.
	 */
	private final Map<UUID, AggroEntry> aggroStrikes = new HashMap<>();
	@Nullable
	private UUID pendingAggroTarget;
	private int aggroRetargetCooldownTicks;
	private int ticksSinceAttackAttempt;

	private double chargeX;
	private double chargeZ;
	private double chargeStartX;
	private double chargeStartZ;
	private float chargeYaw;
	private double chargeDirX;
	private double chargeDirZ;
	private double stumbleSpeed;
	private int chargeMaxTicks;
	private int punchCooldownTicks;
	private boolean punchHitLanded;
	private boolean chargeHitLanded;
	private boolean playedSpawnSound;
	private int pathfindCooldownTicks;
	private int phaseTick;
	/** Client-only: tick when the current combat phase started. */
	private int clientPhaseStartTick;

	public final AnimationState roarAnimationState = new AnimationState();
	public final AnimationState stumbleAnimationState = new AnimationState();
	public final AnimationState levitatingAnimationState = new AnimationState();
	public final AnimationState spawnAnimationState = new AnimationState();

	private int levitateTicksRemaining;
	private boolean halfHealthLevitationTriggered;
	private boolean spawnedLevitationEndermites;
	private boolean waitingForPostLandFreeze;
	private boolean pendingLevitationLandingBlast;
	private int postLandFreezeTicks;

	/** Idle wander before the first punch or roar after spawning. */
	private int combatWarmupTicks;
	private int scheduledCombatWarmup = -1;

	public ShadowCreakingEntity(EntityType<? extends ShadowCreakingEntity> entityType, Level world) {
		super(entityType, world);
		this.setPersistenceRequired();
		GroundPathNavigation navigation = (GroundPathNavigation) this.getNavigation();
		navigation.setCanFloat(true);
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 500.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.ATTACK_DAMAGE, 18.0)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.5);
	}

	/** Visual scale in the Blockbench renderer (main = 1, mini = 0.5, tiny = 0.25). */
	public float getRenderScale() {
		return 1.0F;
	}

	/** Scales punch/charge damage per variant. */
	public float getDamageMultiplier() {
		return 1.0F;
	}

	protected boolean usesHalfHealthLevitation() {
		return true;
	}

	protected boolean shouldSpawnOnDeath() {
		return this.isMainEntity;
	}

	public boolean isInSpawnIntro() {
		return this.spawnIntroActive;
	}

	public boolean isInSpawnBuildup() {
		return this.spawnIntroActive
			&& this.spawnIntroTotalTicks > SPAWN_REVEAL_TICKS
			&& this.spawnIntroTicksRemaining > SPAWN_REVEAL_TICKS;
	}

	public float getSpawnRevealProgress() {
		if (!this.spawnIntroActive) {
			return 1.0F;
		}
		if (this.isInSpawnBuildup()) {
			return 0.0F;
		}
		return 1.0F - Mth.clamp(this.spawnIntroTicksRemaining / (float) SPAWN_REVEAL_TICKS, 0.0F, 1.0F);
	}

	/** @deprecated Use {@link #getSpawnRevealProgress()} for rendering. */
	@Deprecated
	public float getSpawnIntroProgress() {
		return this.getSpawnRevealProgress();
	}

	public void initializeBossBar(boolean emergingFromAltar) {
		if (!this.isMainEntity || this.bossBarManager != null) {
			return;
		}
		this.bossBarManager = ShadowCreakingBossBarRegistry.createBossBar(this, emergingFromAltar);
	}

	protected static boolean wasKilledByPlayer(DamageSource source) {
		return resolveAttackingPlayer(source) != null;
	}

	/** Resolves the player behind a damage source, following projectiles back to their owner. */
	@Nullable
	public static Player resolveAttackingPlayer(DamageSource source) {
		try {
			Entity attacker = source.getEntity();
			if (attacker instanceof Player player) {
				return player;
			}
			if (attacker instanceof Projectile projectile) {
				Entity owner = projectile.getOwner();
				if (owner instanceof Player player) {
					return player;
				}
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	protected boolean usesWeepingActivation() {
		return true;
	}

	private boolean canBeginChargeOn(LivingEntity target) {
		if (!this.canStartCharge()) {
			return false;
		}
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		double distSq = dx * dx + dz * dz;
		if (distSq <= PUNCH_RANGE_SQ || distSq > CHARGE_START_RANGE_SQ) {
			return false;
		}
		if (!this.mobility.canChargeReachTarget(target)) {
			return false;
		}
		return !(target instanceof Player player) || !this.mobility.isBlockedFromTarget(player);
	}

	private float scaledPunchDamage() {
		return this.getPunchDamage() * this.getDamageMultiplier();
	}

	private float scaledChargeDamage() {
		return this.getChargeDamage() * this.getDamageMultiplier();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CAN_MOVE, true);
		builder.define(IS_ACTIVE, false);
		builder.define(LEVITATING, false);
		builder.define(COMBAT_PHASE, PHASE_IDLE);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (!this.usesWeepingActivation()) {
			this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
		}
		this.goalSelector.addGoal(1, new ShadowCreakingCombatGoal(this));
		this.goalSelector.addGoal(2, new ShadowCreakingStrollGoal(this, 0.28));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F) {
			@Override
			public boolean canUse() {
				return ShadowCreakingEntity.this.canUseIdleGoals() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return this.canUse() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return ShadowCreakingEntity.this.canUseIdleGoals() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return this.canUse() && super.canContinueToUse();
			}
		});
	}

	private boolean canUseIdleGoals() {
		return this.canMove()
			&& !this.isBusy()
			&& this.getCombatPhase() == PHASE_IDLE
			&& this.pathfindCooldownTicks <= 0;
	}

	public boolean canMove() {
		return this.entityData.get(CAN_MOVE);
	}

	public boolean isActive() {
		return this.entityData.get(IS_ACTIVE);
	}

	public boolean isLevitating() {
		return this.entityData.get(LEVITATING);
	}

	public byte getCombatPhase() {
		return this.entityData.get(COMBAT_PHASE);
	}

	public int getPhaseTick() {
		return this.phaseTick;
	}

	public float getCombatPhaseSeconds() {
		if (this.level().isClientSide()) {
			return Math.max(0, this.tickCount - this.clientPhaseStartTick) / 20.0F;
		}
		return this.phaseTick / 20.0F;
	}

	private void setCanMove(boolean value) {
		this.entityData.set(CAN_MOVE, value);
	}

	private void setIsActive(boolean value) {
		this.entityData.set(IS_ACTIVE, value);
	}

	private void setLevitating(boolean value) {
		this.entityData.set(LEVITATING, value);
	}

	private void setCombatPhase(byte phase) {
		this.entityData.set(COMBAT_PHASE, phase);
		this.phaseTick = 0;
		if (!this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) (40 + phase));
		} else {
			this.clientPhaseStartTick = this.tickCount;
		}
	}

	public boolean isWeepingAngelActive() {
		return this.getHealth() > this.getMaxHealth() * 0.5f;
	}

	public boolean isBusy() {
		return this.spawnIntroActive
			|| this.isLevitating()
			|| this.postLandFreezeTicks > 0
			|| this.waitingForPostLandFreeze;
	}

	public boolean isRoaring() {
		return this.getCombatPhase() == PHASE_ROAR;
	}

	public boolean isCharging() {
		return this.getCombatPhase() == PHASE_CHARGE;
	}

	public boolean isStumbling() {
		return this.getCombatPhase() == PHASE_STUMBLE;
	}

	public boolean canStartCharge() {
		boolean engaged = this.isActive() || !this.isWeepingAngelActive();
		return this.canMove()
			&& engaged
			&& !this.isBusy()
			&& this.combatWarmupTicks <= 0
			&& this.getCombatPhase() == PHASE_IDLE
			&& this.pathfindCooldownTicks <= 0;
	}

	public boolean isPunching() {
		return this.getCombatPhase() == PHASE_PUNCH;
	}

	public boolean canStartPunch() {
		boolean engaged = this.isActive() || !this.isWeepingAngelActive();
		return this.canMove()
			&& engaged
			&& !this.isBusy()
			&& this.combatWarmupTicks <= 0
			&& this.getCombatPhase() == PHASE_IDLE
			&& this.punchCooldownTicks <= 0;
	}

	public boolean isInCombatWarmup() {
		return this.combatWarmupTicks > 0;
	}

	/** Staggers the first attack after spawn; twins should use different values. */
	public void setScheduledCombatWarmup(int ticks) {
		this.scheduledCombatWarmup = Math.max(0, ticks);
	}

	/** Swing the heavy right arm at a target that is already in reach. */
	public void beginPunch(LivingEntity target) {
		this.ticksSinceAttackAttempt = 0;
		this.punchHitLanded = false;
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
		this.lookAtTargetNow(target);
		this.setCombatPhase(PHASE_PUNCH);
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.5F, 0.85F);
		this.gameEvent(GameEvent.ENTITY_ACTION);
	}

	private void lookAtTargetNow(LivingEntity target) {
		float desired = (float) (Math.toDegrees(
			Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX())) - 90.0);
		this.setYRot(desired);
		this.yBodyRot = desired;
	}

	private float getPunchDamage() {
		if (this.level() == null) {
			return PUNCH_DAMAGE_NORMAL;
		}
		return switch (this.level().getDifficulty()) {
			case PEACEFUL -> 0.0F;
			case EASY -> PUNCH_DAMAGE_EASY;
			case NORMAL -> PUNCH_DAMAGE_NORMAL;
			case HARD -> PUNCH_DAMAGE_HARD;
		};
	}

	private void landPunchHit(ServerLevel level, LivingEntity target) {
		float damage = this.scaledPunchDamage();
		if (damage <= 0.0F) {
			return;
		}
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		if (dx * dx + dz * dz > PUNCH_HIT_RANGE_SQ) {
			return;
		}
		DamageSource source = level.damageSources().mobAttack(this);
		if (target.hurtServer(level, source, damage)) {
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len > 1.0E-4) {
				target.knockback(1.6F, -dx / len, -dz / len, source, damage);
			}
		}
	}

	private static final class AggroEntry {
		int strikes;
		int lastStrikeTick;
	}

	boolean canUseMobilityEnhancements() {
		return !this.spawnIntroActive && this.postLandFreezeTicks <= 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		try {
			if (source.is(DamageTypes.IN_WALL) && this.mobility.tryEscapeSuffocation(level)) {
				return false;
			}
		} catch (Throwable ignored) {
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) {
			Player attacker = resolveAttackingPlayer(source);
			if (attacker != null) {
				this.recordAggroStrike(attacker);
			}
		}
		return hurt;
	}

	private void recordAggroStrike(Player attacker) {
		if (attacker.isSpectator() || attacker.isCreative()) {
			return;
		}
		LivingEntity current = this.getTarget();
		if (current != null && current.getUUID().equals(attacker.getUUID())) {
			// Hits from whoever it is already chasing don't build a grudge.
			this.aggroStrikes.remove(attacker.getUUID());
			return;
		}
		AggroEntry entry = this.aggroStrikes.computeIfAbsent(attacker.getUUID(), id -> new AggroEntry());
		if (this.tickCount - entry.lastStrikeTick > AGGRO_MEMORY_TICKS) {
			entry.strikes = 0;
		}
		entry.strikes++;
		entry.lastStrikeTick = this.tickCount;
	}

	/** True when a retarget can actually be acted on rather than just remembered. */
	private boolean canActOnAggro() {
		return this.canMove()
			&& !this.isBusy()
			&& (this.isActive() || !this.isWeepingAngelActive())
			&& this.getCombatPhase() == PHASE_IDLE
			&& this.pathfindCooldownTicks <= 0;
	}

	private void tickAggro() {
		if (this.aggroRetargetCooldownTicks > 0) {
			this.aggroRetargetCooldownTicks--;
		}
		this.aggroStrikes.entrySet().removeIf(
			e -> this.tickCount - e.getValue().lastStrikeTick > AGGRO_MEMORY_TICKS);

		boolean free = this.canActOnAggro();
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			this.ticksSinceAttackAttempt = 0;
		} else if (free) {
			this.ticksSinceAttackAttempt++;
		}

		if (this.pendingAggroTarget == null && this.aggroRetargetCooldownTicks <= 0) {
			this.pendingAggroTarget = this.findAggroSwitchCandidate();
		}
		if (this.pendingAggroTarget == null) {
			return;
		}

		Player next = this.level().getPlayerByUUID(this.pendingAggroTarget);
		if (next == null || !next.isAlive() || next.isSpectator() || next.isCreative()) {
			this.pendingAggroTarget = null;
			return;
		}
		double follow = this.getAttributeValue(Attributes.FOLLOW_RANGE);
		if (next.distanceToSqr(this) > follow * follow) {
			return;
		}
		// Out of reach or mid-animation: hold the grudge and apply it once it can move.
		if (free) {
			this.switchAggroTarget(next);
		}
	}

	@Nullable
	private UUID findAggroSwitchCandidate() {
		if (this.aggroStrikes.isEmpty()) {
			return null;
		}
		LivingEntity target = this.getTarget();
		UUID targetId = target == null ? null : target.getUUID();
		// With nobody to chase, or nothing landing on the current target, one hit is enough.
		boolean openToAnyone = targetId == null
			|| !target.isAlive()
			|| this.ticksSinceAttackAttempt >= AGGRO_UNREACHABLE_TICKS;
		int needed = openToAnyone ? 1 : AGGRO_STRIKES_TO_SWITCH;

		UUID best = null;
		int bestStrikes = 0;
		for (Map.Entry<UUID, AggroEntry> entry : this.aggroStrikes.entrySet()) {
			if (entry.getKey().equals(targetId)) {
				continue;
			}
			int strikes = entry.getValue().strikes;
			if (strikes >= needed && strikes > bestStrikes) {
				bestStrikes = strikes;
				best = entry.getKey();
			}
		}
		return best;
	}

	private void switchAggroTarget(Player next) {
		this.setTarget(next);
		this.setIsActive(true);
		this.aggroStrikes.remove(next.getUUID());
		this.pendingAggroTarget = null;
		this.aggroRetargetCooldownTicks = AGGRO_RETARGET_COOLDOWN_TICKS;
		this.ticksSinceAttackAttempt = 0;

		if (this.combatWarmupTicks > 0) {
			return;
		}
		this.tryBeginCombatAttack(next);
	}

	/** Shared punch-or-charge decision for aggro switches and the combat goal. */
	void tryBeginCombatAttack(LivingEntity target) {
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		double distSq = dx * dx + dz * dz;
		if (distSq <= PUNCH_RANGE_SQ) {
			if (this.canStartPunch()) {
				this.beginPunch(target);
			} else {
				this.lookAtTargetNow(target);
			}
			return;
		}
		if (this.canBeginChargeOn(target)) {
			this.beginRoar(target);
		}
	}

	void approachTarget(LivingEntity target) {
		this.approachTarget(target, 0.85);
	}

	void approachTarget(LivingEntity target, double speed) {
		if (!this.mobility.enhanceChase(target)) {
			this.chaseTowards(target.getX(), target.getY(), target.getZ(), speed);
		}
	}

	/** Freeze in place and play the roar (plus stand-up tail). */
	public void beginRoar(LivingEntity target) {
		this.ticksSinceAttackAttempt = 0;
		this.chargeHitLanded = false;
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
		this.setCombatPhase(PHASE_ROAR);
		this.playSound(ModSounds.SHADOW_CREAKING_ROAR, 2.0F, 1.0F);
		this.gameEvent(GameEvent.ENTITY_ACTION);
	}

	/**
	 * After standing from the roar: lock facing to the player and commit a charge of
	 * (current distance + 5) blocks. Never re-steers until the charge ends.
	 */
	private void beginLockedCharge(LivingEntity target) {
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 1.0E-4) {
			float yawRad = (float) Math.toRadians(this.getYRot());
			dx = -Math.sin(yawRad);
			dz = Math.cos(yawRad);
			dist = 1.0;
		}
		this.chargeDirX = dx / dist;
		this.chargeDirZ = dz / dist;
		double travel = dist + CHARGE_OVERSHOOT;
		this.chargeStartX = this.getX();
		this.chargeStartZ = this.getZ();
		this.chargeX = this.getX() + this.chargeDirX * travel;
		this.chargeZ = this.getZ() + this.chargeDirZ * travel;
		this.chargeYaw = (float) (Math.toDegrees(Math.atan2(this.chargeDirZ, this.chargeDirX)) - 90.0);
		this.setYRot(this.chargeYaw);
		this.yBodyRot = this.chargeYaw;
		// Timeout with a small buffer so a failed arrive check can't run ~50 blocks.
		this.chargeMaxTicks = Mth.ceil(travel / CHARGE_SPEED) + 8;
		this.setCombatPhase(PHASE_CHARGE);
		this.makeSound(SoundEvents.CREAKING_ATTACK);
	}

	private void beginStumbleFromCharge() {
		// The charge always runs at CHARGE_SPEED, so hand that off directly rather than
		// reading the delta, which a gaze freeze may already have cleared this tick.
		this.stumbleSpeed = CHARGE_SPEED;
		this.setCombatPhase(PHASE_STUMBLE);
	}

	public void chaseTowards(double x, double y, double z, double speed) {
		this.getNavigation().moveTo(x, y, z, speed);
		this.getMoveControl().setWantedPosition(x, y, z, speed);
	}

	public void shoveTowards(double x, double z, double speed) {
		double dx = x - this.getX();
		double dz = z - this.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-4) {
			return;
		}
		this.setDeltaMovement((dx / len) * speed, this.getDeltaMovement().y, (dz / len) * speed);
		this.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
		this.yBodyRot = this.getYRot();
	}

	private float getChargeDamage() {
		if (this.level() == null) {
			return CHARGE_DAMAGE_NORMAL;
		}
		return switch (this.level().getDifficulty()) {
			case PEACEFUL -> 0.0F;
			case EASY -> CHARGE_DAMAGE_EASY;
			case NORMAL -> CHARGE_DAMAGE_NORMAL;
			case HARD -> CHARGE_DAMAGE_HARD;
		};
	}

	private boolean landChargeHit(ServerLevel level, LivingEntity target) {
		float damage = this.scaledChargeDamage();
		if (damage <= 0.0F) {
			return false;
		}
		DamageSource source = level.damageSources().mobAttack(this);
		boolean hit = target.hurtServer(level, source, damage);
		if (hit) {
			target.knockback(1.4F, -this.chargeDirX, -this.chargeDirZ, source, damage);
		}
		return hit;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		super.remove(reason);
		if (this.level().isClientSide() || this.bossBarManager == null) {
			return;
		}
		if (this.isMainEntity && reason != Entity.RemovalReason.KILLED) {
			this.bossBarManager.removeEntity(this.getUUID());
		} else if (!this.isMainEntity) {
			this.bossBarManager.removeEntity(this.getUUID());
		}
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (this.bossBarManager != null) {
			this.bossBarManager.removeEntity(this.getUUID());
		}
		if (!this.shouldSpawnOnDeath() || !wasKilledByPlayer(damageSource)) {
			return;
		}

		int[] roles = new int[] {
			TinyShadowCreakingEntity.DROP_ENCHANTED_BOOK_COVER,
			TinyShadowCreakingEntity.DROP_ENCHANTED_PAGES,
			TinyShadowCreakingEntity.DROP_WOOD_CHIP,
			TinyShadowCreakingEntity.DROP_WOOD_CHIP
		};
		MiniShadowCreakingEntity miniA = new MiniShadowCreakingEntity(ModEntities.MINI_SHADOW_CREAKING, serverLevel);
		MiniShadowCreakingEntity miniB = new MiniShadowCreakingEntity(ModEntities.MINI_SHADOW_CREAKING, serverLevel);
		miniA.setChildTinyDropRoles(roles[0], roles[1]);
		miniB.setChildTinyDropRoles(roles[2], roles[3]);
		RandomSource random = serverLevel.getRandom();
		miniA.setScheduledCombatWarmup(40 + random.nextInt(50));
		miniB.setScheduledCombatWarmup(100 + random.nextInt(70));
		miniA.beginSpawnIntro();
		miniB.beginSpawnIntro();
		if (this.bossBarManager != null) {
			miniA.bossBarManager = this.bossBarManager;
			miniB.bossBarManager = this.bossBarManager;
			miniA.isMainEntity = false;
			miniB.isMainEntity = false;
			this.bossBarManager.addMiniEntity(miniA);
			this.bossBarManager.addMiniEntity(miniB);
		}
		List<ShadowCreakingEntity> toSpawn = new ArrayList<>();
		toSpawn.add(miniA);
		toSpawn.add(miniB);
		ShadowCreakingSpawnHelper.spawnEntitiesWithValidPositions(
			serverLevel, toSpawn, this.getX(), this.getY(), this.getZ());
	}

	protected static void tagParentSpawn(ShadowCreakingEntity entity) {
		try {
			entity.addTag("theendupdate:spawned_by_parent");
		} catch (Throwable ignored) {
		}
		entity.beginSpawnIntro();
	}

	public void beginSpawnIntro() {
		this.spawnIntroActive = true;
		this.spawnIntroTotalTicks = SPAWN_INTRO_TICKS;
		this.spawnIntroTicksRemaining = SPAWN_INTRO_TICKS;
		this.spawnRevealBoomPlayed = false;
		this.setInvulnerable(true);
		this.setInvisible(true);
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, 0.0, 0.0);
	}

	/** Short reveal after altar particle summon — entity appears with a boom. */
	public void beginSpawnReveal() {
		this.spawnIntroActive = true;
		this.spawnIntroTotalTicks = SPAWN_REVEAL_TICKS;
		this.spawnIntroTicksRemaining = SPAWN_REVEAL_TICKS;
		this.spawnRevealBoomPlayed = false;
		this.setInvulnerable(true);
		this.setInvisible(false);
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, 0.0, 0.0);
	}

	private void playSpawnRevealBoom() {
		this.playSound(ModSounds.SHADOW_CREAKING_SPAWN, 2.0F, 1.0F);
		this.playedSpawnSound = true;
		if (this.level() instanceof ServerLevel serverLevel) {
			double cx = this.getX();
			double cy = this.getY() + this.getBbHeight() * 0.5;
			double cz = this.getZ();
			RandomSource random = this.getRandom();
			serverLevel.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
			for (int i = 0; i < 48; i++) {
				double theta = random.nextDouble() * Math.PI * 2.0;
				double radius = 0.5 + random.nextDouble() * 2.5;
				serverLevel.sendParticles(
					ParticleTypes.SOUL_FIRE_FLAME,
					cx + Math.cos(theta) * radius,
					cy + random.nextDouble() * 2.0,
					cz + Math.sin(theta) * radius,
					1, 0.0, 0.12, 0.0, 0.02);
			}
			for (int i = 0; i < 24; i++) {
				serverLevel.sendParticles(
					ParticleTypes.SOUL,
					cx + (random.nextDouble() - 0.5) * 3.0,
					cy + random.nextDouble() * 2.5,
					cz + (random.nextDouble() - 0.5) * 3.0,
					1, 0.0, 0.06, 0.0, 0.04);
			}
		}
	}

	private void onSpawnIntroComplete() {
		if (!this.usesWeepingActivation()) {
			this.setIsActive(true);
		}
		if (this.scheduledCombatWarmup >= 0) {
			this.combatWarmupTicks = this.scheduledCombatWarmup;
		} else {
			this.combatWarmupTicks = COMBAT_WARMUP_MIN_TICKS
				+ this.getRandom().nextInt(COMBAT_WARMUP_MAX_TICKS - COMBAT_WARMUP_MIN_TICKS + 1);
		}
		this.mobility.onMobilityEnabled();
	}

	private void tickSpawnIntro() {
		if (!this.spawnIntroActive) {
			return;
		}
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, 0.0, 0.0);

		boolean inBuildup = this.isInSpawnBuildup();
		if (inBuildup) {
			this.setInvisible(true);
		} else if (!this.spawnRevealBoomPlayed && !this.level().isClientSide()) {
			this.spawnRevealBoomPlayed = true;
			this.setInvisible(false);
			this.playSpawnRevealBoom();
		}

		if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
			double cx = this.getX();
			double cy = this.getY() + this.getBbHeight() * 0.35;
			double cz = this.getZ();
			RandomSource random = this.getRandom();
			if (inBuildup) {
				float buildupProgress = 1.0F - (this.spawnIntroTicksRemaining - SPAWN_REVEAL_TICKS)
					/ (float) (this.spawnIntroTotalTicks - SPAWN_REVEAL_TICKS);
				int soulCount = 6 + Mth.floor(buildupProgress * 18.0F);
				for (int i = 0; i < soulCount; i++) {
					double ox = (random.nextDouble() - 0.5) * (2.0 + buildupProgress * 2.5);
					double oy = random.nextDouble() * (1.5 + buildupProgress * 2.5);
					double oz = (random.nextDouble() - 0.5) * (2.0 + buildupProgress * 2.5);
					serverLevel.sendParticles(ParticleTypes.SOUL, cx + ox, cy + oy, cz + oz, 1, 0.0, 0.05, 0.0, 0.03);
				}
				if (buildupProgress > 0.65F) {
					serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.5, cz, 2, 0.4, 0.3, 0.4, 0.01);
				}
			} else {
				float revealProgress = this.getSpawnRevealProgress();
				if (revealProgress < 0.5F) {
					for (int i = 0; i < 4; i++) {
						serverLevel.sendParticles(
							ParticleTypes.SMOKE,
							cx + (random.nextDouble() - 0.5) * 2.0,
							cy + random.nextDouble(),
							cz + (random.nextDouble() - 0.5) * 2.0,
							1, 0.0, 0.04, 0.0, 0.01);
					}
				}
			}
		}

		if (--this.spawnIntroTicksRemaining <= 0) {
			this.spawnIntroActive = false;
			this.setInvulnerable(false);
			this.setInvisible(false);
			this.onSpawnIntroComplete();
			if (this.bossBarManager != null) {
				this.bossBarManager.isEmerging = false;
				this.bossBarManager.beginHealthTracking();
			}
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id >= 40 && id <= 45) {
			byte phase = (byte) (id - 40);
			this.entityData.set(COMBAT_PHASE, phase);
			this.phaseTick = 0;
			this.clientPhaseStartTick = this.tickCount;
			this.roarAnimationState.stop();
			this.stumbleAnimationState.stop();
			if (phase == PHASE_ROAR) {
				// The roar sound is played server-side in beginRoar.
				this.roarAnimationState.start(this.tickCount);
			} else if (phase == PHASE_STUMBLE) {
				this.stumbleAnimationState.start(this.tickCount);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (LEVITATING.equals(data) && this.level().isClientSide()) {
			if (this.isLevitating()) {
				this.levitatingAnimationState.start(this.tickCount);
			} else {
				this.levitatingAnimationState.stop();
			}
		}
	}

	@Override
	public void aiStep() {
		if (!this.level().isClientSide()) {
			if (!this.playedSpawnSound && !this.spawnIntroActive) {
				this.playedSpawnSound = true;
				this.playSound(ModSounds.SHADOW_CREAKING_SPAWN, 2.0F, 1.0F);
			}
			if (this.pathfindCooldownTicks > 0) {
				this.pathfindCooldownTicks--;
			}
			if (this.punchCooldownTicks > 0) {
				this.punchCooldownTicks--;
			}
			this.tickCombatPhases();
			this.tickHalfHealthLevitation();
			this.tickAggro();
			this.mobility.tick();
			if (this.combatWarmupTicks > 0) {
				this.combatWarmupTicks--;
			}

			boolean wasCanMove = this.canMove();
			boolean nowCanMove = this.checkCanMove();
			if (nowCanMove != wasCanMove) {
				this.gameEvent(GameEvent.ENTITY_ACTION);
				if (nowCanMove) {
					this.makeSound(SoundEvents.CREAKING_UNFREEZE);
				} else {
					this.getNavigation().stop();
					// A gaze freeze must not kill the stumble's momentum; it has to ride out.
					if (this.getCombatPhase() != PHASE_STUMBLE) {
						this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
					}
					this.makeSound(SoundEvents.CREAKING_FREEZE);
					// Only abort a live charge into stumble; roar stays frozen until it finishes.
					if (this.getCombatPhase() == PHASE_CHARGE) {
						this.beginStumbleFromCharge();
					}
				}
			}
			this.setCanMove(nowCanMove);

			if (!nowCanMove || this.isBusy()) {
				this.getNavigation().stop();
				if (!this.isLevitating() && this.getCombatPhase() != PHASE_CHARGE && this.getCombatPhase() != PHASE_STUMBLE) {
					this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
				}
			}
		}

		super.aiStep();
	}

	private void tickCombatPhases() {
		byte phase = this.getCombatPhase();
		if (phase == PHASE_IDLE) {
			return;
		}

		int tick = ++this.phaseTick;

		if (phase == PHASE_ROAR) {
			// Frozen in place for the whole roar + stand-up.
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
			LivingEntity target = this.getTarget();
			if (target != null) {
				this.getLookControl().setLookAt(target, 60.0F, 60.0F);
			}
			if (tick >= ROAR_TICKS) {
				if (target != null && target.isAlive()) {
					if (this.mobility.canChargeReachTarget(target)) {
						this.beginLockedCharge(target);
					} else {
						this.setCombatPhase(PHASE_IDLE);
					}
				} else {
					this.setCombatPhase(PHASE_IDLE);
				}
			}
			return;
		}

		if (phase == PHASE_CHARGE) {
			this.getNavigation().stop();
			this.setYRot(this.chargeYaw);
			this.yBodyRot = this.chargeYaw;
			this.setDeltaMovement(this.chargeDirX * CHARGE_SPEED, this.getDeltaMovement().y, this.chargeDirZ * CHARGE_SPEED);

			if (!this.chargeHitLanded && this.level() instanceof ServerLevel serverLevel) {
				LivingEntity target = this.getTarget();
				if (target != null && target.isAlive()) {
					double dx = this.getX() - target.getX();
					double dz = this.getZ() - target.getZ();
					if (dx * dx + dz * dz <= CHARGE_HIT_RANGE_SQ) {
						this.chargeHitLanded = true;
						this.landChargeHit(serverLevel, target);
					}
				}
			}

			double toEndX = this.chargeX - this.getX();
			double toEndZ = this.chargeZ - this.getZ();
			boolean arrived = toEndX * toEndX + toEndZ * toEndZ <= CHARGE_ARRIVE_DIST_SQ;
			// Stop as soon as we pass the locked endpoint (don't coast past it for a full timeout).
			boolean passedEnd = toEndX * this.chargeDirX + toEndZ * this.chargeDirZ <= 0.0;
			boolean timedOut = tick >= this.chargeMaxTicks;
			double chargeProgress = (this.getX() - this.chargeStartX) * this.chargeDirX
				+ (this.getZ() - this.chargeStartZ) * this.chargeDirZ;
			boolean lowProgress = tick >= CHARGE_STUCK_CHECK_TICKS && chargeProgress < CHARGE_MIN_PROGRESS;
			boolean blocked = (this.horizontalCollision && tick > 4) || lowProgress;
			LivingEntity chargeTarget = this.getTarget();
			if (blocked && chargeTarget instanceof Player player) {
				this.mobility.tickStuckTracking(player);
			}
			if (arrived || passedEnd || timedOut || blocked) {
				this.beginStumbleFromCharge();
				if (blocked && chargeTarget instanceof Player player) {
					this.mobility.tryBlinkTeleportToTarget(player);
				}
			}
			return;
		}

		if (phase == PHASE_STUMBLE) {
			// Carry the charge's momentum along the locked bearing: most of it bleeds off
			// quickly, then a slow drift keeps it creeping until the clip ends.
			this.getNavigation().stop();
			this.setYRot(this.chargeYaw);
			this.yBodyRot = this.chargeYaw;
			float u = Mth.clamp(tick / (float) STUMBLE_TICKS, 0.0F, 1.0F);
			double handoff = this.stumbleSpeed * Math.exp(-tick / STUMBLE_HANDOFF_TICKS);
			double glide = STUMBLE_GLIDE_SPEED * (1.0 - u);
			double speed = Math.max(handoff, glide);
			this.setDeltaMovement(this.chargeDirX * speed, this.getDeltaMovement().y, this.chargeDirZ * speed);
			if (tick >= STUMBLE_TICKS) {
				this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
				this.setCombatPhase(PHASE_FINISHER);
			}
			return;
		}

		if (phase == PHASE_PUNCH) {
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
			LivingEntity punchTarget = this.getTarget();
			if (punchTarget != null && tick < PUNCH_IMPACT_TICK) {
				// Track until the swing commits, then hold the angle through impact.
				this.lookAtTargetNow(punchTarget);
				this.getLookControl().setLookAt(punchTarget, 60.0F, 60.0F);
			}
			if (tick == PUNCH_IMPACT_TICK
				&& !this.punchHitLanded
				&& this.level() instanceof ServerLevel serverLevel
				&& punchTarget != null
				&& punchTarget.isAlive()) {
				this.punchHitLanded = true;
				this.landPunchHit(serverLevel, punchTarget);
			}
			if (tick >= PUNCH_TICKS) {
				this.punchCooldownTicks = PUNCH_COOLDOWN_TICKS;
				this.setCombatPhase(PHASE_IDLE);
			}
			return;
		}

		if (phase == PHASE_FINISHER) {
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
			LivingEntity target = this.getTarget();
			// Last third of the finisher: ease body toward the player.
			if (tick > FINISHER_TICKS * 2 / 3 && target != null && target.isAlive()) {
				float desired = (float) (Math.toDegrees(Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX())) - 90.0);
				float delta = Mth.wrapDegrees(desired - this.getYRot());
				this.setYRot(this.getYRot() + Mth.clamp(delta, -6.0F, 6.0F));
				this.yBodyRot = this.getYRot();
			}
			if (tick >= FINISHER_TICKS) {
				this.pathfindCooldownTicks = PATHFIND_COOLDOWN_TICKS;
				this.setCombatPhase(PHASE_IDLE);
			}
		}
	}

	private void tickHalfHealthLevitation() {
		if (!this.usesHalfHealthLevitation()) {
			return;
		}
		if (!this.halfHealthLevitationTriggered
			&& this.getHealth() <= this.getMaxHealth() * 0.5f
			&& !this.isLevitating()) {
			this.halfHealthLevitationTriggered = true;
			this.levitateTicksRemaining = LEVITATE_DURATION_TICKS;
			this.spawnedLevitationEndermites = false;
			this.setCombatPhase(PHASE_IDLE);
			this.setLevitating(true);
			this.setNoGravity(true);
			this.setInvulnerable(true);
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, 0.0, 0.0);
		}

		if (this.isLevitating()) {
			this.getNavigation().stop();
			this.setSprinting(false);
			this.setJumping(false);
			this.setInvulnerable(true);

			int elapsedTicks = LEVITATE_DURATION_TICKS - this.levitateTicksRemaining;
			double yMove = elapsedTicks >= 40 ? LEVITATE_SPEED_PER_TICK : 0.0;
			if (!this.level().noCollision(this, this.getBoundingBox().move(0.0, yMove, 0.0))) {
				yMove = 0.0;
			}
			this.setDeltaMovement(0.0, yMove, 0.0);
			this.move(MoverType.SELF, new Vec3(0.0, yMove, 0.0));

			if (!this.spawnedLevitationEndermites && elapsedTicks >= 40) {
				this.spawnedLevitationEndermites = true;
				this.spawnLevitationEndermites();
			}

			if (this.level() instanceof ServerLevel sw) {
				this.spawnLevitationSoulParticles(sw);
			}

			if (--this.levitateTicksRemaining <= 0) {
				this.setLevitating(false);
				this.setNoGravity(false);
				this.setDeltaMovement(this.getDeltaMovement().x, Math.min(this.getDeltaMovement().y, -0.35), this.getDeltaMovement().z);
				this.waitingForPostLandFreeze = true;
				this.pendingLevitationLandingBlast = true;
			}
		}

		if (this.waitingForPostLandFreeze && this.onGround()) {
			this.waitingForPostLandFreeze = false;
			this.postLandFreezeTicks = POST_LAND_FREEZE_TICKS;
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, 0.0, 0.0);
			this.setInvulnerable(true);
			if (this.pendingLevitationLandingBlast) {
				this.pendingLevitationLandingBlast = false;
				this.spawnSoulBurstAndDamage();
			}
		}

		if (this.postLandFreezeTicks > 0) {
			this.getNavigation().stop();
			this.setDeltaMovement(0.0, 0.0, 0.0);
			this.postLandFreezeTicks--;
			if (this.postLandFreezeTicks == 0) {
				this.setInvulnerable(false);
				if (this.getTarget() instanceof Player player && player.isAlive()) {
					this.setIsActive(true);
				}
			}
		}
	}

	private void spawnLevitationEndermites() {
		if (!(this.level() instanceof ServerLevel sw)) {
			return;
		}
		double yawRad = Math.toRadians(this.getYRot());
		double rightX = Math.cos(yawRad);
		double rightZ = Math.sin(yawRad);
		double side = Math.max(1.4, this.getBbWidth() * 0.9);
		double handY = this.getY() + this.getBbHeight() * 0.55;

		Endermite left = new Endermite(net.minecraft.world.entity.EntityTypes.ENDERMITE, sw);
		Endermite right = new Endermite(net.minecraft.world.entity.EntityTypes.ENDERMITE, sw);
		left.snapTo(this.getX() - rightX * side, handY, this.getZ() - rightZ * side, this.getYRot(), 0.0f);
		right.snapTo(this.getX() + rightX * side, handY, this.getZ() + rightZ * side, this.getYRot(), 0.0f);
		sw.addFreshEntity(left);
		sw.addFreshEntity(right);
	}

	private void spawnLevitationSoulParticles(ServerLevel sw) {
		RandomSource r = this.getRandom();
		double cx = this.getX();
		double cy = this.getY() + this.getBbHeight() * 0.45;
		double cz = this.getZ();
		for (int i = 0; i < 6; i++) {
			double ox = (r.nextDouble() - 0.5) * this.getBbWidth() * 1.4;
			double oy = (r.nextDouble() - 0.5) * this.getBbHeight() * 0.6;
			double oz = (r.nextDouble() - 0.5) * this.getBbWidth() * 1.4;
			sw.sendParticles(ParticleTypes.SOUL, cx + ox, cy + oy, cz + oz, 1, 0.0, 0.02, 0.0, 0.0);
		}
	}

	void resumeAiAfterBlinkTeleport() {
		if (this.getCombatPhase() != PHASE_IDLE) {
			this.setCombatPhase(PHASE_IDLE);
		}
		this.getNavigation().stop();
		this.chargeHitLanded = false;
	}

	void spawnSoulBurstAndDamage() {
		if (!(this.level() instanceof ServerLevel sw)) {
			return;
		}
		RandomSource r = this.getRandom();
		double cx = this.getX();
		double cy = this.getY() + this.getBbHeight() * 0.35;
		double cz = this.getZ();
		for (int i = 0; i < 260; i++) {
			double theta = r.nextDouble() * Math.PI * 2.0;
			double phi = Math.acos(2.0 * r.nextDouble() - 1.0);
			double radius = 1.4 + r.nextDouble() * 1.1;
			double sx = cx + radius * Math.sin(phi) * Math.cos(theta);
			double sy = cy + radius * Math.cos(phi);
			double sz = cz + radius * Math.sin(phi) * Math.sin(theta);
			double vx = (sx - cx) * 0.55;
			double vy = (sy - cy) * 0.55;
			double vz = (sz - cz) * 0.55;
			sw.sendParticles(ParticleTypes.SOUL, sx, sy, sz, 1, 0.0, 0.0, 0.0, 0.0);
			sw.sendParticles(ParticleTypes.SOUL, sx, sy, sz, 0, vx, vy, vz, 0.0);
		}

		float damage = 14.0f;
		double radius = 6.0;
		AABB box = new AABB(cx - radius, this.getY() - 1.5, cz - radius, cx + radius, this.getY() + this.getBbHeight() + 1.0, cz + radius);
		for (Player player : sw.getEntitiesOfClass(Player.class, box, Player::isAlive)) {
			var source = sw.damageSources().mobAttack(this);
			player.hurtServer(sw, source, damage);
			double dx = player.getX() - cx;
			double dz = player.getZ() - cz;
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len > 1.0E-4) {
				dx /= len;
				dz /= len;
				float kb = 1.0f + (float) (Math.max(0.0, radius - len) / radius) * 0.8f;
				player.knockback(kb, dx, dz, source, damage);
			}
		}
	}

	@Override
	public void tick() {
		if (!this.level().isClientSide() && this.level().getDifficulty() == Difficulty.PEACEFUL) {
			this.discard();
			return;
		}

		super.tick();

		if (!this.level().isClientSide()) {
			this.tickSpawnIntro();
			if (this.isMainEntity && this.bossBarManager == null && this.tickCount <= 5 && !this.spawnIntroActive) {
				this.initializeBossBar(false);
			}
		}

		if (this.level().isClientSide()) {
			byte phase = this.getCombatPhase();
			this.roarAnimationState.animateWhen(phase == PHASE_ROAR, this.tickCount);
			this.stumbleAnimationState.animateWhen(phase == PHASE_STUMBLE, this.tickCount);
			this.levitatingAnimationState.animateWhen(this.isLevitating(), this.tickCount);
			this.spawnAnimationState.animateWhen(this.spawnIntroActive, this.tickCount);
		}
	}

	@Override
	protected void updateWalkAnimation(float distance) {
		float targetSpeed = Math.min(distance * 10.0F, 1.4F);
		this.walkAnimation.update(targetSpeed, 0.25F, 1.0F);
	}

	public boolean checkCanMove() {
		if (this.isBusy()) {
			return false;
		}
		if (!this.isWeepingAngelActive()) {
			return true;
		}

		List<? extends Player> players = this.level().players();
		boolean active = this.isActive();
		if (players.isEmpty()) {
			if (active) {
				this.deactivate();
			}
			return true;
		}

		boolean hasPotentialTarget = false;
		for (Player player : players) {
			if (!this.canAttack(player) || this.isAlliedTo(player) || player.isSpectator() || player.isCreative()) {
				continue;
			}
			double follow = this.getAttributeValue(Attributes.FOLLOW_RANGE);
			if (player.distanceToSqr(this) > follow * follow) {
				continue;
			}

			hasPotentialTarget = true;
			boolean disguiseOk = !active || LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(player);
			boolean looking = disguiseOk && this.isLookingAtMe(
				player,
				0.5,
				false,
				true,
				this.getEyeY(),
				this.getY() + 0.5 * this.getScale(),
				(this.getEyeY() + this.getY()) / 2.0
			);

			if (looking) {
				if (active) {
					return false;
				}
				if (player.distanceToSqr(this) < ACTIVATION_RANGE_SQ) {
					this.activate(player);
					return false;
				}
			}
		}

		if (!hasPotentialTarget && active) {
			this.deactivate();
		}
		return true;
	}

	public void activate(Player player) {
		this.setTarget(player);
		this.gameEvent(GameEvent.ENTITY_ACTION);
		this.makeSound(SoundEvents.CREAKING_ACTIVATE);
		this.setIsActive(true);
	}

	public void deactivate() {
		if (!this.isWeepingAngelActive()) {
			return;
		}
		this.setTarget(null);
		this.gameEvent(GameEvent.ENTITY_ACTION);
		this.makeSound(SoundEvents.CREAKING_DEACTIVATE);
		this.setIsActive(false);
	}

	@Override
	public boolean isPushable() {
		return super.isPushable() && this.canMove() && !this.isBusy() && !this.isCharging();
	}

	@Override
	public void push(double xa, double ya, double za) {
		if (this.canMove() && !this.isBusy() && !this.isCharging()) {
			super.push(xa, ya, za);
		}
	}

	@Override
	@Nullable
	protected SoundEvent getAmbientSound() {
		return this.isActive() ? null : ModSounds.SHADOW_CREAKING_IDLE;
	}

	@Override
	public int getAmbientSoundInterval() {
		// The idle clip runs ~5s; keep a gap so calls don't stack on themselves.
		return 260;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SHADOW_CREAKING_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SHADOW_CREAKING_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.CREAKING_STEP, 0.15F, 1.0F);
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModItems.SHADOW_CREAKING_SPAWN_EGG);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("HalfHealthLevitationTriggered", this.halfHealthLevitationTriggered);
		output.putBoolean("Levitating", this.isLevitating());
		output.putInt("LevitateTicksRemaining", this.levitateTicksRemaining);
		output.putBoolean("SpawnedLevitationEndermites", this.spawnedLevitationEndermites);
		output.putInt("PostLandFreezeTicks", this.postLandFreezeTicks);
		output.putByte("CombatPhase", this.getCombatPhase());
		output.putInt("PhaseTick", this.phaseTick);
		output.putInt("PathfindCooldown", this.pathfindCooldownTicks);
		output.putBoolean("PlayedSpawnSound", this.playedSpawnSound);
		output.putInt("PunchCooldown", this.punchCooldownTicks);
		output.putBoolean("SpawnIntroActive", this.spawnIntroActive);
		output.putInt("SpawnIntroTicks", this.spawnIntroTicksRemaining);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.halfHealthLevitationTriggered = input.getBooleanOr("HalfHealthLevitationTriggered", false);
		this.levitateTicksRemaining = input.getIntOr("LevitateTicksRemaining", 0);
		this.spawnedLevitationEndermites = input.getBooleanOr("SpawnedLevitationEndermites", false);
		this.postLandFreezeTicks = input.getIntOr("PostLandFreezeTicks", 0);
		boolean levitating = input.getBooleanOr("Levitating", false);
		this.setLevitating(levitating);
		this.setNoGravity(levitating);
		if (levitating || this.postLandFreezeTicks > 0) {
			this.setInvulnerable(true);
		}
		this.entityData.set(COMBAT_PHASE, input.getByteOr("CombatPhase", PHASE_IDLE));
		this.phaseTick = input.getIntOr("PhaseTick", 0);
		this.pathfindCooldownTicks = input.getIntOr("PathfindCooldown", 0);
		this.playedSpawnSound = input.getBooleanOr("PlayedSpawnSound", false);
		this.punchCooldownTicks = input.getIntOr("PunchCooldown", 0);
		this.spawnIntroActive = input.getBooleanOr("SpawnIntroActive", false);
		this.spawnIntroTicksRemaining = input.getIntOr("SpawnIntroTicks", 0);
		if (this.spawnIntroActive) {
			this.setInvulnerable(true);
		}
	}

	private static class ShadowCreakingCombatGoal extends Goal {
		private final ShadowCreakingEntity creaking;

		ShadowCreakingCombatGoal(ShadowCreakingEntity creaking) {
			this.creaking = creaking;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!this.creaking.canMove() || this.creaking.isBusy()) {
				return false;
			}
			if (!this.creaking.isActive() && this.creaking.isWeepingAngelActive()) {
				return false;
			}
			LivingEntity target = this.creaking.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			this.creaking.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = this.creaking.getTarget();
			if (target == null) {
				return;
			}
			if (this.creaking.getCombatPhase() != PHASE_IDLE) {
				return;
			}
			if (this.creaking.isInCombatWarmup()) {
				this.tickCombatWarmup(target);
				return;
			}
			if (this.creaking.pathfindCooldownTicks > 0) {
				this.creaking.getNavigation().stop();
				this.creaking.getLookControl().setLookAt(target, 30.0F, 30.0F);
				return;
			}

			this.creaking.getLookControl().setLookAt(target, 35.0F, 35.0F);

			double dx = target.getX() - this.creaking.getX();
			double dz = target.getZ() - this.creaking.getZ();
			double distSq = dx * dx + dz * dz;

			if (distSq <= PUNCH_RANGE_SQ) {
				if (this.creaking.canStartPunch()) {
					this.creaking.beginPunch(target);
				} else {
					this.creaking.getNavigation().stop();
					this.creaking.setDeltaMovement(0.0, this.creaking.getDeltaMovement().y, 0.0);
					this.creaking.getLookControl().setLookAt(target, 35.0F, 35.0F);
				}
				return;
			}

			if (this.creaking.canBeginChargeOn(target)) {
				this.creaking.beginRoar(target);
				return;
			}

			this.creaking.approachTarget(target);
		}

		private void tickCombatWarmup(LivingEntity target) {
			this.creaking.getLookControl().setLookAt(target, 25.0F, 25.0F);
			if (this.creaking.getNavigation().isInProgress()) {
				return;
			}
			double dx = (this.creaking.getRandom().nextDouble() - 0.5) * 10.0;
			double dz = (this.creaking.getRandom().nextDouble() - 0.5) * 10.0;
			this.creaking.getNavigation().moveTo(
				this.creaking.getX() + dx,
				this.creaking.getY(),
				this.creaking.getZ() + dz,
				0.55
			);
		}
	}

	private static class ShadowCreakingStrollGoal extends WaterAvoidingRandomStrollGoal {
		private final ShadowCreakingEntity creaking;

		ShadowCreakingStrollGoal(ShadowCreakingEntity creaking, double speed) {
			super(creaking, speed);
			this.creaking = creaking;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return this.creaking.canUseIdleGoals()
				&& !this.creaking.isActive()
				&& this.creaking.getTarget() == null
				&& super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return this.creaking.canUseIdleGoals()
				&& !this.creaking.isActive()
				&& this.creaking.getTarget() == null
				&& super.canContinueToUse();
		}
	}
}
