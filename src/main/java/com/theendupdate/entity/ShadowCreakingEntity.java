package com.theendupdate.entity;

// removed unused imports after replacing dig particles with soul particles
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ShadowCreakingEntity extends CreakingEntity {
	private static final int EMERGE_DURATION_TICKS = 134; // match Warden emerging duration
	private static final int LEVITATE_DURATION_TICKS = 140; // 7s total: 2s arms-out + 5s hover
	private static final double LEVITATE_SPEED_PER_TICK = 0.02; // ~2 blocks over 5s

	private static final TrackedData<Boolean> LEVITATING = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> FORCE_RUNNING = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	// Pose-driven animation states (client uses these to time animations)
	public final AnimationState emergingAnimationState = new AnimationState();
	public final AnimationState diggingAnimationState = new AnimationState();
	public final AnimationState levitatingAnimationState = new AnimationState();

	private boolean playedSpawnLevitation;
	private int levitateTicksRemaining;
	private boolean waitingForPostLandFreeze;
	private int postLandFreezeTicks;
	private boolean spawnedLevitationEndermites;
	private boolean pendingLevitationLandingBlast;
	// Enrage/phase 2 controls
	private boolean halfHealthLevitationTriggered;
// removed: weeping state now computed from health/variant
	// One-time removal of vanilla gaze-freeze goals if any are present via reflection
	private boolean prunedGazeFreezeGoals;
	// Cooldown for manual fallback attacks when overriding gaze freeze
	private int gazeOverrideAttackCooldownTicks;
    // removed fallback nudge trackers; rely on navigation-only to preserve animations
	// Track motion progress to detect persistent freeze and apply gentle fallback steps and run overlay
	private int gazeNoProgressTicks;
	private double gazeLastX;
	private double gazeLastZ;
	private int forceRunOverlayTicks;

	public ShadowCreakingEntity(EntityType<? extends CreakingEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 7;
	}

	public static DefaultAttributeContainer.Builder createShadowCreakingAttributes() {
		// Base on hostile default, then tweak for Warden-equivalent punch and half Warden health
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.MAX_HEALTH, 250.0)
			.add(EntityAttributes.ATTACK_DAMAGE, 30.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.28)
			.add(EntityAttributes.FOLLOW_RANGE, 32.0)
			.add(EntityAttributes.ARMOR, 2.0)
			.add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LEVITATING, Boolean.FALSE);
		builder.add(FORCE_RUNNING, Boolean.FALSE);
	}

	public boolean isLevitating() {
		return this.dataTracker.get(LEVITATING);
	}

	private void setLevitating(boolean value) {
		this.dataTracker.set(LEVITATING, value);
	}

	public boolean isForcingRunOverlay() {
		return this.dataTracker.get(FORCE_RUNNING);
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		if (!(this.getWorld() instanceof ServerWorld sw)) return;
		if (!this.shouldSpawnOnDeath()) return;
		if (wasKilledByPlayer(damageSource)) {
			// Spawn two minis with guaranteed horizontal separation so they don't overlap
			double baseX = this.getX();
			double baseY = this.getY();
			double baseZ = this.getZ();
			double separation = 1.25; // half-distance; results in 2.5 blocks between centers
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
				com.theendupdate.entity.MiniShadowCreakingEntity s1 = new com.theendupdate.entity.MiniShadowCreakingEntity(com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING, sw);
				com.theendupdate.entity.MiniShadowCreakingEntity s2 = new com.theendupdate.entity.MiniShadowCreakingEntity(com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING, sw);
				s1.refreshPositionAndAngles(x1, baseY, z1, this.getYaw(), this.getPitch());
				s2.refreshPositionAndAngles(x2, baseY, z2, this.getYaw(), this.getPitch());
				if (sw.isSpaceEmpty(s1) && sw.isSpaceEmpty(s2)) {
					sw.spawnEntity(s1);
					sw.spawnEntity(s2);
					spawned = true;
				}
			}
			// If all attempts fail, fall back to slight random jitter to avoid hard failure
			if (!spawned) {
				for (int i = 0; i < 2; i++) {
					com.theendupdate.entity.MiniShadowCreakingEntity spawn = new com.theendupdate.entity.MiniShadowCreakingEntity(com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING, sw);
					double ox = baseX + (this.random.nextDouble() - 0.5) * 1.2;
					double oz = baseZ + (this.random.nextDouble() - 0.5) * 1.2;
					spawn.refreshPositionAndAngles(ox, baseY, oz, this.getYaw(), this.getPitch());
					sw.spawnEntity(spawn);
				}
			}
		}
	}

	// Slow down hand swing so attack animation matches vanilla pacing better
	public int getHandSwingDuration() {
		return 12;
	}

	@Override
	public boolean isImmobile() {
		// Respect weeping-angel freeze only if active for this variant/phase
		boolean baseFreeze = this.isWeepingAngelActive() ? super.isImmobile() : false;
		// Prevent movement during emergence and during the post-landing freeze period
		return baseFreeze || this.getPose() == EntityPose.EMERGING || this.postLandFreezeTicks > 0;
	}

	@Override
	public boolean isAiDisabled() {
		// If weeping is not active for this variant/phase, force AI to remain enabled
		return this.isWeepingAngelActive() ? super.isAiDisabled() : false;
	}

	@Override
	public void tick() {
		super.tick();

		// Drive initial emerging pose when spawned
		if (!this.isRemoved()) {
			if (this.age == 1) {
				this.setPose(EntityPose.EMERGING);
				this.playSound(SoundEvents.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
			}
			if (this.getPose() == EntityPose.EMERGING && this.age >= EMERGE_DURATION_TICKS) {
				this.setPose(EntityPose.STANDING);
			}
		}

		// Proactively strip vanilla gaze-freeze goals when weeping is not active (base <50% hp) or for mini/tiny
		if (!this.getWorld().isClient && !this.prunedGazeFreezeGoals && this.age > 0) {
			boolean neverWeep = (this instanceof com.theendupdate.entity.MiniShadowCreakingEntity) || (this instanceof com.theendupdate.entity.TinyShadowCreakingEntity);
			if (neverWeep || !this.isWeepingAngelActive()) {
				this.prunedGazeFreezeGoals = true;
				try {
					removePotentialGazeFreezeGoals();
				} catch (Throwable ignored) {}
			}
		}

		if (this.getPose() == EntityPose.EMERGING) {
			// Limit motion and control while emerging
			try {
				this.getNavigation().stop();
			} catch (Throwable ignored) {}
			this.setSprinting(false);
			this.setJumping(false);
			this.setVelocity(0.0, this.getVelocity().y, 0.0);
			// Invulnerable while emerging for all variants
			this.setInvulnerable(true);
		}

		// Server: start and run post-spawn levitation immediately after emerging finishes
		if (!this.getWorld().isClient) {
			if (!this.playedSpawnLevitation && this.getPose() != EntityPose.EMERGING && this.age >= EMERGE_DURATION_TICKS) {
				this.playedSpawnLevitation = true;
				this.levitateTicksRemaining = LEVITATE_DURATION_TICKS;
				this.setLevitating(true);
				this.setNoGravity(true);
				this.setInvulnerable(true);
			}

			// Trigger a second levitation when dropping below half health (phase 2), then drop weeping-angel restriction
			if (this.shouldTriggerHalfHealthLevitation()
				&& !this.halfHealthLevitationTriggered
				&& this.getHealth() <= this.getMaxHealth() * 0.5f
				&& this.getPose() != EntityPose.EMERGING
				&& !this.isLevitating()) {
				this.halfHealthLevitationTriggered = true;
				this.levitateTicksRemaining = LEVITATE_DURATION_TICKS;
				this.setLevitating(true);
				this.setNoGravity(true);
				this.setInvulnerable(true);
			}

			if (this.isLevitating()) {
				// Halt navigation and horizontal motion while levitating
				try { this.getNavigation().stop(); } catch (Throwable ignored) {}
				this.setSprinting(false);
				this.setJumping(false);
				// Ensure invulnerability remains active during levitation
				this.setInvulnerable(true);

				// Phase timing: first 2s = arms-out only, then 5s hover upwards
				int elapsedTicks = LEVITATE_DURATION_TICKS - this.levitateTicksRemaining;
				double yMove = 0.0;
				if (elapsedTicks >= 40) {
					yMove = LEVITATE_SPEED_PER_TICK;
				}
				// Prevent moving into ceilings
				if (!this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0.0, yMove, 0.0))) {
					yMove = 0.0;
				}
				// Move vertically; keep horizontal still
				this.setVelocity(0.0, yMove, 0.0);
				this.velocityDirty = true;
				this.move(net.minecraft.entity.MovementType.SELF, new Vec3d(0.0, yMove, 0.0));

				// Spawn endermites at hands when arms reach full T-pose (~2s into levitation)
				if (!this.spawnedLevitationEndermites && elapsedTicks >= 40) {
					this.spawnedLevitationEndermites = true;
					if (this.getWorld() instanceof ServerWorld sw2) {
						double yawRad = Math.toRadians(this.getYaw());
						double rightX = Math.cos(yawRad);
						double rightZ = Math.sin(yawRad);
						double side = 0.7; // hand horizontal offset from center
						double handY = this.getY() + 1.8; // approximate hand height

						double leftPx = this.getX() - rightX * side;
						double leftPz = this.getZ() - rightZ * side;
						double rightPx = this.getX() + rightX * side;
						double rightPz = this.getZ() + rightZ * side;

						net.minecraft.entity.mob.EndermiteEntity m1 = new net.minecraft.entity.mob.EndermiteEntity(net.minecraft.entity.EntityType.ENDERMITE, sw2);
						net.minecraft.entity.mob.EndermiteEntity m2 = new net.minecraft.entity.mob.EndermiteEntity(net.minecraft.entity.EntityType.ENDERMITE, sw2);
						if (m1 != null) {
							m1.refreshPositionAndAngles(leftPx, handY, leftPz, this.getYaw(), 0.0f);
							sw2.spawnEntity(m1);
						}
						if (m2 != null) {
							m2.refreshPositionAndAngles(rightPx, handY, rightPz, this.getYaw(), 0.0f);
							sw2.spawnEntity(m2);
						}
					}
				}

				if (--this.levitateTicksRemaining <= 0) {
					this.setLevitating(false);
					this.setNoGravity(false);
					// Drop back to floor with a nudge downward
					this.setVelocity(this.getVelocity().x, Math.min(this.getVelocity().y, -0.35), this.getVelocity().z);
					this.velocityDirty = true;
					this.waitingForPostLandFreeze = true;
					this.pendingLevitationLandingBlast = true; // trigger VFX+damage when we actually touch ground
					// no-op: weeping state now depends directly on current health and variant
				}
			}

			// After landing, freeze and make invulnerable for 3 seconds
			if (this.waitingForPostLandFreeze && this.isOnGround()) {
				this.waitingForPostLandFreeze = false;
				this.postLandFreezeTicks = 60; // 3 seconds
				try { this.getNavigation().stop(); } catch (Throwable ignored) {}
				this.setVelocity(0.0, 0.0, 0.0);
				this.velocityDirty = true;
				this.setInvulnerable(true);
				// Landing-only soul burst + area damage
				if (this.pendingLevitationLandingBlast) {
					this.pendingLevitationLandingBlast = false;
					spawnSoulBurstAndDamage();
				}
			}

			if (this.postLandFreezeTicks > 0) {
				try { this.getNavigation().stop(); } catch (Throwable ignored) {}
				this.setVelocity(0.0, 0.0, 0.0);
				this.velocityDirty = true;
				this.postLandFreezeTicks--;
				if (this.postLandFreezeTicks == 0) {
					this.setInvulnerable(false);
				}
			}

			// If weeping is not active for this variant/phase, ensure AI is enabled even if
			// vanilla logic attempted to disable it due to gaze, and actively drive navigation so
			// movement animations play normally (no manual sliding).
			if (!this.isWeepingAngelActive()) {
				try { this.setAiDisabled(false); } catch (Throwable ignored) {}
				if (this.gazeOverrideAttackCooldownTicks > 0) this.gazeOverrideAttackCooldownTicks--;
				if (this.getPose() != EntityPose.EMERGING && !this.isLevitating() && this.postLandFreezeTicks <= 0) {
					var tgt = this.getTarget();
					if (tgt != null && tgt.isAlive()) {
						double dx = tgt.getX() - this.getX();
						double dz = tgt.getZ() - this.getZ();
						double dd = Math.sqrt(dx * dx + dz * dz);
						// Use path navigation to preserve animations where possible
						if (!this.isNavigating()) {
							this.getNavigation().startMovingTo(tgt, 1.0);
						}
						this.getLookControl().lookAt(tgt, 30.0f, 30.0f);
						// Detect lack of progress and apply a brief self-propelled step with forced run overlay
						double moved = Math.hypot(this.getX() - this.gazeLastX, this.getZ() - this.gazeLastZ);
						if (dd > 1.0 && moved < 0.01) {
							this.gazeNoProgressTicks++;
						} else {
							this.gazeNoProgressTicks = 0;
						}
						if (this.gazeNoProgressTicks >= 6 && dd > 1.0E-4) { // ~0.3s without progress
							dx /= dd; dz /= dd;
							// Face the movement direction to avoid diagonal mismatch
							float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
							this.setYaw(desiredYaw);
							this.setBodyYaw(desiredYaw);
							// Small step forward; keep vertical unchanged
							double base = this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
							double v = Math.max(0.12, base * 0.65);
							this.setVelocity(dx * v, this.getVelocity().y, dz * v);
							this.velocityDirty = true;
							this.move(net.minecraft.entity.MovementType.SELF, new Vec3d(dx * v, 0.0, dz * v));
							this.setSprinting(true);
							this.gazeNoProgressTicks = 0;
							// Trigger short client-side run overlay
							this.forceRunOverlayTicks = 8; // ~0.4s
						}
						this.gazeLastX = this.getX();
						this.gazeLastZ = this.getZ();
						// Manual attack only as a fallback, with a cooldown matching typical melee pacing
						if (dd <= 2.6 && this.gazeOverrideAttackCooldownTicks <= 0 && this.getWorld() instanceof ServerWorld swClose) {
							if (this.tryAttack(swClose, tgt)) {
								this.gazeOverrideAttackCooldownTicks = 20; // ~1s between swings
							}
						}
					}
				}
			}
			// Maintain run overlay tracking and sync to client
			if (this.forceRunOverlayTicks > 0) {
				this.forceRunOverlayTicks--;
				this.dataTracker.set(FORCE_RUNNING, Boolean.TRUE);
			} else {
				this.dataTracker.set(FORCE_RUNNING, Boolean.FALSE);
			}
		}

		// Client-side particles: soul swirl while EMERGING/DIGGING and during post-spawn levitation
		if (this.getWorld().isClient) {
			switch (this.getPose()) {
				case EMERGING:
					// Near-feet swirl while emerging
					this.addSoulSwirlParticles(this.emergingAnimationState, 0.12f, 0.40f, 12, 0.045f);
					break;
				case DIGGING:
					// Near-feet swirl while digging
					this.addSoulSwirlParticles(this.diggingAnimationState, 0.12f, 0.40f, 12, 0.045f);
					break;
				default:
					break;
			}
			if (this.isLevitating()) {
				this.addSoulSwirlParticles(this.levitatingAnimationState, 1.2f, 0.75f, 12, 0.055f);
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void removePotentialGazeFreezeGoals() {
		try {
			// Access the goal selector via our accessor mixin
			net.minecraft.entity.ai.goal.GoalSelector selector = ((com.theendupdate.mixin.MobEntityAccessor)(Object)this).theendupdate$getGoalSelector();
			if (selector == null) return;
			java.util.Set entriesSet = null;
			for (java.lang.reflect.Field f : selector.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				Object val = f.get(selector);
				if (val instanceof java.util.Set s) {
					entriesSet = s; break;
				}
			}
			if (entriesSet == null || entriesSet.isEmpty()) return;
			java.util.List toRemove = new java.util.ArrayList();
			for (Object entry : entriesSet) {
				if (entry == null) continue;
				Object goalObj = null;
				for (java.lang.reflect.Field ef : entry.getClass().getDeclaredFields()) {
					ef.setAccessible(true);
					Object v = ef.get(entry);
					if (v instanceof net.minecraft.entity.ai.goal.Goal) { goalObj = v; break; }
				}
				if (goalObj == null) continue;
				String cn = goalObj.getClass().getName();
				// Heuristic: any creaking-specific look/freeze/observe goals
				if (cn.contains("Creaking") || cn.contains("creaking")) {
					String ln = goalObj.getClass().getSimpleName().toLowerCase();
					if (ln.contains("look") || ln.contains("gaze") || ln.contains("observe") || ln.contains("freeze") || ln.contains("stare")) {
						toRemove.add(entry);
					}
				}
			}
			if (!toRemove.isEmpty()) {
				entriesSet.removeAll(toRemove);
			}
		} catch (Throwable ignored) {}
	}

	// Hook: whether we should trigger the below-half-health levitation on this variant
	protected boolean shouldTriggerHalfHealthLevitation() {
		return true;
	}

	// Hook: whether this entity should spawn children on death if killed by player
	protected boolean shouldSpawnOnDeath() {
		return true;
	}

	protected static boolean wasKilledByPlayer(DamageSource source) {
		try {
			Entity attacker = source.getAttacker();
			if (attacker instanceof PlayerEntity) return true;
			if (attacker instanceof ProjectileEntity proj) {
				Entity owner = proj.getOwner();
				return owner instanceof PlayerEntity;
			}
		} catch (Throwable ignored) {}
		return false;
	}

	// Hook: whether weeping-angel freeze should be active for this variant/phase
protected boolean isWeepingAngelActive() {
	// Base: weeping only while above half health; minis/tinies override to false
	return this.shouldTriggerHalfHealthLevitation() && this.getHealth() > this.getMaxHealth() * 0.5f;
}

	@Override
	public boolean isInvisibleTo(PlayerEntity player) {
		// When weeping is not active (base <= half health, or mini/tiny), hide from gaze checks
		// so vanilla Creaking logic does not freeze movement while looked at.
		if (!this.isWeepingAngelActive()) return true;
		return super.isInvisibleTo(player);
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		// Do not perform attacks while emerging
		if (this.getPose() == EntityPose.EMERGING) return false;
		if (this.isLevitating()) return false;
		if (this.postLandFreezeTicks > 0) return false;
		return super.tryAttack(world, target);
	}

	@Override
	public boolean isPushable() {
		// Prevent pushing/collisions while emerging to match immobility
		if (this.getPose() == EntityPose.EMERGING) return false;
		if (this.isLevitating()) return false;
		if (this.postLandFreezeTicks > 0) return false;
		return super.isPushable();
	}

	// Start/stop animation clocks when POSE or LEVITATING tracked data changes (vanilla pattern)
	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (Entity.POSE.equals(data)) {
			switch (this.getPose()) {
				case EMERGING:
					this.emergingAnimationState.start(this.age);
					break;
				case DIGGING:
					this.diggingAnimationState.start(this.age);
					break;
				default:
					break;
			}
		}
		if (LEVITATING.equals(data)) {
			if (this.isLevitating()) {
				this.levitatingAnimationState.start(this.age);
			} else {
				this.levitatingAnimationState.stop();
			}
		}
		super.onTrackedDataSet(data);
	}

	private void addSoulSwirlParticles(AnimationState animationState, float yOffset, float radius, int count, float tangentialVelocity) {
		// Mirror original timing window: emit for first ~4.5s of the EMERGING/DIGGING animations; no limit for levitating state
		boolean unlimited = animationState == this.levitatingAnimationState;
		if (!unlimited && (float)animationState.getTimeInMilliseconds(this.age) >= 4500.0F) return;

		Random random = this.getRandom();
		float ms = (float)animationState.getTimeInMilliseconds(this.age);
		float base = (ms / 1000.0f) * 2.2f; // angular speed (rad/s) for swirl
		float scale = getVariantParticleScale();
		double cx = this.getX();
		double cy = this.getY() + (yOffset * scale);
		double cz = this.getZ();
		int scaledCount = Math.max(4, (int)(count * (0.5f + 0.5f * scale))); // reduce count for smaller variants
		float scaledRadius = radius * scale;
		float scaledTangential = tangentialVelocity * Math.max(0.5f, scale);
		for (int i = 0; i < scaledCount; i++) {
			float t = base + (float)(i * (Math.PI * 2.0) / (float)count);
			double dx = Math.cos(t) * scaledRadius;
			double dz = Math.sin(t) * scaledRadius;
			double px = cx + dx + MathHelper.nextBetween(random, -0.05f, 0.05f);
			double py = cy + MathHelper.nextBetween(random, -0.05f, 0.05f);
			double pz = cz + dz + MathHelper.nextBetween(random, -0.05f, 0.05f);
			double vx = -Math.sin(t) * scaledTangential;
			double vy = 0.01 + MathHelper.nextBetween(random, -0.005f, 0.005f);
			double vz = Math.cos(t) * scaledTangential;
			this.getWorld().addParticleClient(ParticleTypes.SOUL, px, py, pz, vx, vy, vz);
		}
	}

	private float getVariantParticleScale() {
		if (this instanceof com.theendupdate.entity.TinyShadowCreakingEntity) return 0.25f; // quarter of mini
		if (this instanceof com.theendupdate.entity.MiniShadowCreakingEntity) return 0.5f;
		return 1.0f;
	}

	private void spawnSoulBurstAndDamage() {
		if (!(this.getWorld() instanceof ServerWorld sw)) return;
		Random r = this.getRandom();
		// 1) Spawn dense sphere of soul particles around the entity
		double cx = this.getX();
		double cy = this.getY() + 1.2; // center roughly at chest height
		double cz = this.getZ();
		for (int i = 0; i < 200; i++) {
			double theta = r.nextDouble() * Math.PI * 2.0;
			double phi = Math.acos(2.0 * r.nextDouble() - 1.0);
			double radius = 0.9 + r.nextDouble() * 0.6; // 0.9..1.5
			double sx = cx + radius * Math.sin(phi) * Math.cos(theta);
			double sy = cy + radius * Math.cos(phi);
			double sz = cz + radius * Math.sin(phi) * Math.sin(theta);
			// Outward velocity from center
			double vx = (sx - cx) * 0.55;
			double vy = (sy - cy) * 0.55;
			double vz = (sz - cz) * 0.55;
			sw.spawnParticles(ParticleTypes.SOUL, sx, sy, sz, 1, 0.0, 0.0, 0.0, 0.0);
			sw.spawnParticles(ParticleTypes.SOUL, sx, sy, sz, 0, vx, vy, vz, 0.0);
		}
		// 2) Apply custom area damage and knockback (exclude Endermites and self; no block damage)
		float damage = 14.0f; // roughly TNT-like to mobs nearby
		double radius = 4.25;
		Box box = new Box(cx - radius, this.getY() - 1.5, cz - radius, cx + radius, this.getY() + 3.5, cz + radius);
		for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class, box, (le) -> le.isAlive() && le != this)) {
			if (e instanceof net.minecraft.entity.mob.EndermiteEntity) continue; // explicitly ignore Endermites
			// Damage using mobAttack to ensure damage is applied
			e.damage(sw, sw.getDamageSources().mobAttack(this), damage);
			// Knockback away from center
			double dx = e.getX() - cx;
			double dz = e.getZ() - cz;
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len > 1.0E-4) {
				dx /= len; dz /= len;
				float kb = 1.0f + (float)(Math.max(0.0, radius - len) / radius) * 0.8f;
				e.takeKnockback(kb, dx, dz);
			}
		}
	}
}


