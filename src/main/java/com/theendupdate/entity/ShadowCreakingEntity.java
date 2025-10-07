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
import net.minecraft.util.math.Box;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class ShadowCreakingEntity extends CreakingEntity {
	private static final int EMERGE_DURATION_TICKS = 134; // match Warden emerging duration
	private static final int LEVITATE_DURATION_TICKS = 140; // 7s total: 2s arms-out + 5s hover
	private static final double LEVITATE_SPEED_PER_TICK = 0.02; // ~2 blocks over 5s

	private static final TrackedData<Boolean> LEVITATING = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> FORCE_RUNNING = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> SPAWN_EMERGED = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    protected static final TrackedData<Boolean> LEVITATION_INTRO_PLAYED = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	// Pose-driven animation states (client uses these to time animations)
	public final AnimationState emergingAnimationState = new AnimationState();
	public final AnimationState diggingAnimationState = new AnimationState();
	public final AnimationState levitatingAnimationState = new AnimationState();

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
	// Cooldown to prevent repeated jumping
	private int jumpCooldownTicks;
	// Track if entity is currently jumping to preserve momentum
	private boolean isJumping;
	// Timeout to prevent infinite jumping state
	private int jumpStateTicks;
	
	// Boss bar management
	protected ShadowCreakingBossBarManager bossBarManager;
	protected boolean isMainEntity = true; // Only true for the original entity, false for spawned children

	public ShadowCreakingEntity(EntityType<? extends CreakingEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 7;
		this.setPersistent();
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
        builder.add(SPAWN_EMERGED, Boolean.FALSE);
        builder.add(LEVITATION_INTRO_PLAYED, Boolean.FALSE);
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
		
		// Handle boss bar cleanup for all entities
		if (this.bossBarManager != null) {
			this.bossBarManager.removeEntity(this.getUuid());
		}
		
		if (!this.shouldSpawnOnDeath()) return;
		if (wasKilledByPlayer(damageSource)) {
			// Pre-assign tiny drop roles: one cover, one pages, two wood chips
			int[] roles = new int[] {
				com.theendupdate.entity.TinyShadowCreakingEntity.DROP_ENCHANTED_BOOK_COVER,
				com.theendupdate.entity.TinyShadowCreakingEntity.DROP_ENCHANTED_PAGES,
				com.theendupdate.entity.TinyShadowCreakingEntity.DROP_WOOD_CHIP,
				com.theendupdate.entity.TinyShadowCreakingEntity.DROP_WOOD_CHIP
			};
			
			// Create the two mini entities to spawn
			com.theendupdate.entity.MiniShadowCreakingEntity s1 = new com.theendupdate.entity.MiniShadowCreakingEntity(com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING, sw);
			com.theendupdate.entity.MiniShadowCreakingEntity s2 = new com.theendupdate.entity.MiniShadowCreakingEntity(com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING, sw);
			s1.setChildTinyDropRoles(roles[0], roles[1]);
			s2.setChildTinyDropRoles(roles[2], roles[3]);
			try { s1.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
			try { s2.addCommandTag("theendupdate:spawned_by_parent"); } catch (Throwable ignored) {}
			
			// Set up boss bar for spawned mini entities
			if (this.bossBarManager != null) {
				s1.bossBarManager = this.bossBarManager;
				s2.bossBarManager = this.bossBarManager;
				s1.isMainEntity = false;
				s2.isMainEntity = false;
				
				// Add mini entities to boss bar tracking
				this.bossBarManager.addMiniEntity(s1);
				this.bossBarManager.addMiniEntity(s2);
			}
			
			// Find valid spawn positions and spawn entities
			java.util.List<com.theendupdate.entity.ShadowCreakingEntity> toSpawn = new java.util.ArrayList<>();
			toSpawn.add(s1);
			toSpawn.add(s2);
			spawnEntitiesWithValidPositions(sw, toSpawn, this.getX(), this.getY(), this.getZ());
		}
	}

	/**
	 * Helper method to find valid spawn positions within 5 blocks and spawn entities.
	 * If only one valid position is found, all entities spawn there.
	 * If multiple positions are found, entities are distributed across them.
	 */
	protected void spawnEntitiesWithValidPositions(ServerWorld sw, java.util.List<ShadowCreakingEntity> entities, double baseX, double baseY, double baseZ) {
		if (entities.isEmpty()) return;
		
		// Get the entity dimensions for collision checking
		ShadowCreakingEntity sampleEntity = entities.get(0);
		float width = sampleEntity.getWidth();
		float height = sampleEntity.getHeight();
		
		// Search for valid positions within 5 blocks, starting at minimum distance of 1 block
		java.util.List<Vec3d> validPositions = new java.util.ArrayList<>();
		int searchRadius = 5;
		int neededPositions = entities.size(); // Try to find one position per entity
		
		// Start with positions at least 1 block away from death location, spiraling outward
		for (double radius = 1.0; radius <= searchRadius && validPositions.size() < neededPositions; radius += 0.5) {
			int angleSteps = Math.max(8, (int)(radius * 8)); // More angles for larger radii
			for (int i = 0; i < angleSteps && validPositions.size() < neededPositions; i++) {
				double angle = (2.0 * Math.PI * i) / angleSteps;
				double offsetX = Math.cos(angle) * radius;
				double offsetZ = Math.sin(angle) * radius;
				
				// Check positions at different heights (same level, slightly above, slightly below)
				for (double yOffset = -1.0; yOffset <= 1.0 && validPositions.size() < neededPositions; yOffset += 0.5) {
					double testX = baseX + offsetX;
					double testY = baseY + yOffset;
					double testZ = baseZ + offsetZ;
					
					// Create a bounding box at this position to test for collisions
					Box testBox = new Box(
						testX - width / 2, testY, testZ - width / 2,
						testX + width / 2, testY + height, testZ + width / 2
					);
					
					// Check if the position is valid (no collisions, on solid ground or close to it)
					if (sw.isSpaceEmpty(testBox) && isValidSpawnPosition(sw, testBox, testY)) {
						// Make sure this position isn't too close to already found positions
						// (at least 1 block apart to ensure separate spawns)
						boolean tooClose = false;
						for (Vec3d existing : validPositions) {
							double dist = Math.sqrt(
								Math.pow(testX - existing.x, 2) + 
								Math.pow(testZ - existing.z, 2)
							);
							if (dist < 1.0) { // Less than 1 block apart horizontally
								tooClose = true;
								break;
							}
						}
						
						if (!tooClose) {
							validPositions.add(new Vec3d(testX, testY, testZ));
						}
					}
				}
			}
		}
		
		// If we still need more positions and haven't found enough, relax the distance requirement
		if (validPositions.size() < neededPositions) {
			for (double radius = 1.0; radius <= searchRadius && validPositions.size() < neededPositions; radius += 0.5) {
				int angleSteps = Math.max(8, (int)(radius * 8));
				for (int i = 0; i < angleSteps && validPositions.size() < neededPositions; i++) {
					double angle = (2.0 * Math.PI * i) / angleSteps;
					double offsetX = Math.cos(angle) * radius;
					double offsetZ = Math.sin(angle) * radius;
					
					for (double yOffset = -1.0; yOffset <= 1.0 && validPositions.size() < neededPositions; yOffset += 0.5) {
						double testX = baseX + offsetX;
						double testY = baseY + yOffset;
						double testZ = baseZ + offsetZ;
						
						Box testBox = new Box(
							testX - width / 2, testY, testZ - width / 2,
							testX + width / 2, testY + height, testZ + width / 2
						);
						
						// Check if already added
						boolean alreadyAdded = false;
						for (Vec3d existing : validPositions) {
							if (existing.x == testX && existing.y == testY && existing.z == testZ) {
								alreadyAdded = true;
								break;
							}
						}
						
						if (!alreadyAdded && sw.isSpaceEmpty(testBox) && isValidSpawnPosition(sw, testBox, testY)) {
							validPositions.add(new Vec3d(testX, testY, testZ));
						}
					}
				}
			}
		}
		
		// If no valid positions found after searching, try the immediate area (0.5 block radius)
		if (validPositions.isEmpty()) {
			for (int i = 0; i < 8; i++) {
				double angle = (2.0 * Math.PI * i) / 8.0;
				double testX = baseX + Math.cos(angle) * 0.5;
				double testZ = baseZ + Math.sin(angle) * 0.5;
				
				Box testBox = new Box(
					testX - width / 2, baseY, testZ - width / 2,
					testX + width / 2, baseY + height, testZ + width / 2
				);
				
				if (sw.isSpaceEmpty(testBox)) {
					validPositions.add(new Vec3d(testX, baseY, testZ));
					if (validPositions.size() >= neededPositions) break;
				}
			}
		}
		
		// Last resort: spawn at parent's position if still no valid position found
		if (validPositions.isEmpty()) {
			validPositions.add(new Vec3d(baseX, baseY, baseZ));
		}
		
		// Distribute entities across valid positions
		// Each entity gets its own position if possible, or they share if only one position available
		for (int i = 0; i < entities.size(); i++) {
			ShadowCreakingEntity entity = entities.get(i);
			// Use different positions for each entity, wrapping around if we have fewer positions than entities
			Vec3d spawnPos = validPositions.get(i % validPositions.size());
			entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, this.getYaw(), this.getPitch());
			sw.spawnEntity(entity);
		}
	}
	
	/**
	 * Check if a position is suitable for spawning (entity should be on ground or close to it)
	 */
	private boolean isValidSpawnPosition(ServerWorld sw, Box entityBox, double yPos) {
		// Check if there's ground within 2 blocks below this position
		Box groundCheckBox = new Box(
			entityBox.minX, yPos - 2.0, entityBox.minZ,
			entityBox.maxX, yPos, entityBox.maxZ
		);
		
		// There should be solid ground below (not all air)
		boolean hasGroundBelow = !sw.isSpaceEmpty(groundCheckBox);
		
		// Also check that the space directly below isn't too far (void check)
		Box immediateGroundBox = new Box(
			entityBox.minX, yPos - 0.5, entityBox.minZ,
			entityBox.maxX, yPos, entityBox.maxZ
		);
		boolean hasImmediateGround = !sw.isSpaceEmpty(immediateGroundBox);
		
		return hasGroundBelow || hasImmediateGround;
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
		
		// Handle boss bar initialization and updates for main entity
		if (!this.getWorld().isClient && this.isMainEntity) {
			// Initialize boss bar if not already done (fallback for any spawn method)
			if (this.bossBarManager == null && this.age <= 5) {
				boolean isEmerging = this.getPose() == EntityPose.EMERGING;
				com.theendupdate.TemplateMod.LOGGER.info("Initializing boss bar at age {}, pose: {}, emerging: {}", 
					this.age, this.getPose(), isEmerging);
				this.initializeBossBar(isEmerging);
			}
			
			// Update boss bar
			if (this.bossBarManager != null) {
				this.bossBarManager.tick((ServerWorld) this.getWorld());
			}
		}

		// Restore persisted one-time state on fresh loads so rejoin does not replay cinematics
		if (!this.getWorld().isClient) {
            try {
                if (this.age <= 1) {
                    java.util.Set<String> tags = this.getCommandTags();
                    if (tags != null && tags.contains("theendupdate:emerged") && !this.dataTracker.get(SPAWN_EMERGED)) {
                        this.dataTracker.set(SPAWN_EMERGED, Boolean.TRUE);
                        if (this.getPose() == EntityPose.EMERGING) this.setPose(EntityPose.STANDING);
                    }
                    if (tags != null && tags.contains("theendupdate:levitation_intro_played") && !this.dataTracker.get(LEVITATION_INTRO_PLAYED)) {
                        this.dataTracker.set(LEVITATION_INTRO_PLAYED, Boolean.TRUE);
                    }
                    if (tags != null && tags.contains("theendupdate:half_health_levitation_triggered")) {
                        this.halfHealthLevitationTriggered = true;
                    }
                }
            } catch (Throwable ignored) {}
		}

		// If not spawned by altar or by a parent, never run spawn cinematics (emerge/levitation)
		if (!this.getWorld().isClient && this.age <= 1) {
			try {
				java.util.Set<String> tags = this.getCommandTags();
				boolean fromAltar = tags != null && tags.contains("theendupdate:spawned_by_altar");
				boolean fromParent = tags != null && tags.contains("theendupdate:spawned_by_parent");
				if (!(fromAltar || fromParent)) {
					if (this.getPose() == EntityPose.EMERGING) this.setPose(EntityPose.STANDING);
					this.dataTracker.set(SPAWN_EMERGED, Boolean.TRUE);
					this.dataTracker.set(LEVITATION_INTRO_PLAYED, Boolean.TRUE);
				}
			} catch (Throwable ignored) {}
		}

		// Drive initial emerging pose once when first spawned; persist via tracked flag to clients
		if (!this.isRemoved()) {
			if (!this.dataTracker.get(SPAWN_EMERGED) && this.age == 1) {
				this.setPose(EntityPose.EMERGING);
				this.playSound(SoundEvents.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
			}
            if (this.getPose() == EntityPose.EMERGING && this.age >= EMERGE_DURATION_TICKS) {
                this.setPose(EntityPose.STANDING);
                this.dataTracker.set(SPAWN_EMERGED, Boolean.TRUE);
                try { this.addCommandTag("theendupdate:emerged"); } catch (Throwable ignored) {}
                // Start levitation immediately and mark as played if and only if spawned by altar
                try {
                    java.util.Set<String> tags = this.getCommandTags();
                    boolean fromAltar = tags != null && tags.contains("theendupdate:spawned_by_altar");
                    boolean fromParent = tags != null && tags.contains("theendupdate:spawned_by_parent");
                    if ((fromAltar || fromParent) && !this.dataTracker.get(LEVITATION_INTRO_PLAYED)) {
                        this.dataTracker.set(LEVITATION_INTRO_PLAYED, Boolean.TRUE);
                        this.levitateTicksRemaining = LEVITATE_DURATION_TICKS;
                        this.setLevitating(true);
                        this.setNoGravity(true);
                        this.setInvulnerable(true);
                        try { this.addCommandTag("theendupdate:levitation_intro_played"); } catch (Throwable ignored2) {}
                    }
                } catch (Throwable ignored) {}
            }
			// Safety: if persisted as emerged, ensure pose is not EMERGING after reload
			if (this.dataTracker.get(SPAWN_EMERGED) && this.getPose() == EntityPose.EMERGING) {
				this.setPose(EntityPose.STANDING);
			}
            // Additional safety: if age is far beyond emerge window but somehow pose is EMERGING, correct it
            if (this.age > (EMERGE_DURATION_TICKS + 5) && this.getPose() == EntityPose.EMERGING) {
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

        // Server: run levitation if already active; do not auto-start by time on reload
        if (!this.getWorld().isClient) {
            // Removed age-window auto-start. Levitation is started only at emerge completion (above).

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
				try { this.addCommandTag("theendupdate:half_health_levitation_triggered"); } catch (Throwable ignored) {}
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
				if (this.jumpCooldownTicks > 0) this.jumpCooldownTicks--;
				
				// Detect landing after jump to reset jump state
				if (this.isJumping) {
					this.jumpStateTicks++;
					// Reset jump state if: landed on ground, or timeout (20 ticks = 1 second), or velocity is very low
					if (this.isOnGround() || this.jumpStateTicks > 20 || 
					    (Math.abs(this.getVelocity().y) < 0.1 && this.jumpStateTicks > 5)) {
						this.isJumping = false;
						this.jumpStateTicks = 0;
					}
				}
				
				if (this.getPose() != EntityPose.EMERGING && !this.isLevitating() && this.postLandFreezeTicks <= 0) {
					var tgt = this.getTarget();
					if (tgt != null && tgt.isAlive()) {
						double dx = tgt.getX() - this.getX();
						double dz = tgt.getZ() - this.getZ();
						double dd = Math.sqrt(dx * dx + dz * dz);
						
						// Enhanced pathfinding with obstacle avoidance and jumping
						if (!this.isNavigating()) {
							this.getNavigation().startMovingTo(tgt, 1.0);
						}
						this.getLookControl().lookAt(tgt, 30.0f, 30.0f);
						
						// Check if we need to jump to reach the target
						boolean shouldJump = this.shouldJumpToReachTarget(tgt);
						if (shouldJump && this.isOnGround() && this.jumpCooldownTicks <= 0 && !this.isJumping) {
							this.performJump();
							this.jumpCooldownTicks = 20; // 1 second cooldown
						}
						
						// Only apply stuck detection and movement override if not jumping
						if (!this.isJumping) {
							// Detect lack of progress and apply enhanced pathfinding
							double moved = Math.hypot(this.getX() - this.gazeLastX, this.getZ() - this.gazeLastZ);
							if (dd > 1.0 && moved < 0.005) { // Reduced threshold for stuck detection
								this.gazeNoProgressTicks++;
							} else {
								this.gazeNoProgressTicks = 0;
							}
							
							// Enhanced movement with obstacle avoidance
							if (this.gazeNoProgressTicks >= 10 && dd > 1.0E-4) { // ~0.5s without progress
								// First try direct path, only use alternative if blocked
								dx /= dd; 
								dz /= dd;
								
								// Only try alternative path if direct path is blocked
								if (this.isBlockingPath(dx, dz)) {
									Vec3d betterDirection = this.findBetterPathDirection(tgt);
									if (betterDirection != null) {
										dx = betterDirection.x;
										dz = betterDirection.z;
										dd = Math.sqrt(dx * dx + dz * dz);
									}
								}
								
								// Face the movement direction to avoid diagonal mismatch
								float desiredYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
								this.setYaw(desiredYaw);
								this.setBodyYaw(desiredYaw);
								
								// Enhanced movement with jumping capability
								double base = this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
								double v = Math.max(0.12, base * 0.65);
								
								// Check if we need to jump over a block
								if (this.isBlockingPath(dx, dz) && this.isOnGround()) {
									this.performJump();
								}
								
								this.setVelocity(dx * v, this.getVelocity().y, dz * v);
								this.velocityDirty = true;
								this.move(net.minecraft.entity.MovementType.SELF, new Vec3d(dx * v, 0.0, dz * v));
								this.setSprinting(true);
								this.gazeNoProgressTicks = 0;
								// Trigger short client-side run overlay
								this.forceRunOverlayTicks = 8; // ~0.4s
							}
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
	
	/**
	 * Initializes the boss bar for this entity
	 */
	protected void initializeBossBar(boolean isEmergingFromAltar) {
		if (!this.isMainEntity || this.bossBarManager != null) return;
		
		try {
			this.bossBarManager = ShadowCreakingBossBarRegistry.createBossBar(this, isEmergingFromAltar);
			com.theendupdate.TemplateMod.LOGGER.info("Shadow Creaking boss bar initialized for entity {} (emerging: {})", 
				this.getUuid(), isEmergingFromAltar);
		} catch (Exception e) {
			com.theendupdate.TemplateMod.LOGGER.error("Failed to initialize Shadow Creaking boss bar", e);
		}
	}
	
	/**
	 * Sets whether this entity is the main entity (affects boss bar management)
	 */
	public void setMainEntity(boolean isMainEntity) {
		this.isMainEntity = isMainEntity;
	}
	
	/**
	 * Gets the boss bar manager for this entity
	 */
	public ShadowCreakingBossBarManager getBossBarManager() {
		return this.bossBarManager;
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
		
		// Check reach distance and line of sight
		if (!canReachTarget(target)) {
			return false;
		}
		
		return super.tryAttack(world, target);
	}
	
	/**
	 * Check if the target is within reach distance and line of sight
	 */
	private boolean canReachTarget(Entity target) {
		if (target == null) return false;
		
		// Calculate 3D distance
		double dx = this.getX() - target.getX();
		double dy = this.getY() - target.getY();
		double dz = this.getZ() - target.getZ();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		
		// Get attack reach distance (default is 2.0 blocks for most mobs)
		double reachDistance = 2.0;
		
		// Check if target is within reach distance
		if (distance > reachDistance) {
			return false;
		}
		
		// Check line of sight - ensure there are no solid blocks between attacker and target
		Vec3d attackerPos = this.getEyePos();
		Vec3d targetPos = target.getBoundingBox().getCenter();
		
		// Use world.raycast to check for obstructions
		return this.getWorld().raycast(new RaycastContext(
			attackerPos, 
			targetPos, 
			RaycastContext.ShapeType.COLLIDER, 
			RaycastContext.FluidHandling.NONE, 
			this
		)).getType() == HitResult.Type.MISS;
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
					// Only play once per lifetime; prevent replay after reload/unload using tracked flag
					if (!this.dataTracker.get(SPAWN_EMERGED) && this.age <= EMERGE_DURATION_TICKS) {
						this.emergingAnimationState.start(this.age);
					} else {
						try { this.setPose(EntityPose.STANDING); } catch (Throwable ignored) {}
					}
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

	/**
	 * Check if the entity should jump to reach the target
	 */
	private boolean shouldJumpToReachTarget(Entity target) {
		if (target == null) return false;
		
		Vec3d start = this.getPos();
		Vec3d end = new Vec3d(target.getX(), this.getY(), target.getZ());
		Vec3d direction = end.subtract(start).normalize();
		double distance = start.distanceTo(end);
		
		// Only consider jumping if target is at least 0.5 blocks away
		if (distance < 0.5) return false;
		
		// Check vertical distance to target
		double dy = target.getY() - this.getY();
		
		// Check if target is higher - definitely need to jump
		if (dy > 0.3) { // Target is higher (even slightly)
			// Check if there's a block blocking the direct path
			for (double i = 0.5; i < Math.min(2.5, distance); i += 0.4) {
				Vec3d checkPos = start.add(direction.multiply(i));
				
				// Check for blocking blocks at various heights
				for (double yOffset = 0; yOffset <= 1.5; yOffset += 0.5) {
					Vec3d testPos = checkPos.add(0, yOffset, 0);
					boolean hasBlock = !this.getWorld().isSpaceEmpty(this, new Box(
						testPos.subtract(0.3, 0, 0.3), 
						testPos.add(0.3, 0.5, 0.3)
					));
					
					if (hasBlock) {
						// Check if there's space above to jump through
						Vec3d abovePos = testPos.add(0, 1.5, 0);
						boolean hasSpaceAbove = this.getWorld().isSpaceEmpty(this, new Box(
							abovePos.subtract(0.3, 0, 0.3), 
							abovePos.add(0.3, 1.5, 0.3)
						));
						
						if (hasSpaceAbove) {
							return true;
						}
					}
				}
			}
		}
		
		// Check for blocks directly in front that need to be jumped over (works for climbing and horizontal)
		Vec3d frontCheckPos = start.add(direction.multiply(0.8));
		boolean hasBlockAhead = !this.getWorld().isSpaceEmpty(this, new Box(
			frontCheckPos.subtract(0.3, 0, 0.3), 
			frontCheckPos.add(0.3, 1.0, 0.3)
		));
		
		if (hasBlockAhead) {
			// Check if there's space above the block
			Vec3d abovePos = frontCheckPos.add(0, 1.0, 0);
			boolean hasSpaceAbove = this.getWorld().isSpaceEmpty(this, new Box(
				abovePos.subtract(0.3, 0, 0.3), 
				abovePos.add(0.3, 1.5, 0.3)
			));
			
			if (hasSpaceAbove) {
				return true;
			}
		}
		
		// If entity hasn't made progress recently, consider jumping to break the cycle
		// Reduced from 15 to 10 ticks to be more responsive
		if (this.gazeNoProgressTicks >= 10) { // Stuck for ~0.5 seconds
			// Check if jumping would help (could be descending or just getting unstuck)
			Vec3d jumpLandPos = start.add(direction.multiply(1.5));
			boolean landingClear = this.getWorld().isSpaceEmpty(this, new Box(
				jumpLandPos.subtract(0.3, 0, 0.3), 
				jumpLandPos.add(0.3, 2, 0.3)
			));
			
			if (landingClear) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Find a better path direction around obstacles
	 */
	private Vec3d findBetterPathDirection(Entity target) {
		if (target == null) return null;
		
		Vec3d currentPos = this.getPos();
		Vec3d targetPos = target.getPos();
		Vec3d directDirection = targetPos.subtract(currentPos).normalize();
		
		// Try directions closer to the direct path first (45-degree increments)
		double[] angles = {
			Math.atan2(directDirection.z, directDirection.x), // Direct path
			Math.atan2(directDirection.z, directDirection.x) + Math.PI/4, // 45 degrees right
			Math.atan2(directDirection.z, directDirection.x) - Math.PI/4, // 45 degrees left
			Math.atan2(directDirection.z, directDirection.x) + Math.PI/2, // 90 degrees right
			Math.atan2(directDirection.z, directDirection.x) - Math.PI/2, // 90 degrees left
			Math.atan2(directDirection.z, directDirection.x) + 3*Math.PI/4, // 135 degrees right
			Math.atan2(directDirection.z, directDirection.x) - 3*Math.PI/4, // 135 degrees left
			Math.atan2(directDirection.z, directDirection.x) + Math.PI // 180 degrees (back)
		};
		
		Vec3d bestDirection = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		
		for (double angle : angles) {
			Vec3d testDirection = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
			
			// Try multiple distances to find the best path
			for (double distance : new double[]{1.5, 2.5, 3.5}) {
				Vec3d testPos = currentPos.add(testDirection.multiply(distance));
				
				// Check if this direction is clear
				if (this.getWorld().isSpaceEmpty(this, new Box(testPos.subtract(0.3, 0, 0.3), testPos.add(0.3, 2, 0.3)))) {
					// Calculate score: prioritize directions that are both clear and closer to target
					double currentDist = currentPos.distanceTo(targetPos);
					double testDist = testPos.distanceTo(targetPos);
					double distanceImprovement = currentDist - testDist;
					
					// Prefer directions closer to the direct path (lower angle difference)
					double angleDiff = Math.abs(angle - Math.atan2(directDirection.z, directDirection.x));
					if (angleDiff > Math.PI) angleDiff = 2*Math.PI - angleDiff;
					double angleScore = 1.0 - (angleDiff / Math.PI); // 1.0 for direct, 0.0 for opposite
					
					// Bonus for longer clear paths
					double pathLengthBonus = distance * 0.1;
					
					// Combined score: distance improvement + angle preference + path length
					double score = distanceImprovement * 0.6 + angleScore * 0.3 + pathLengthBonus * 0.1;
					
					if (score > bestScore) {
						bestScore = score;
						bestDirection = testDirection;
					}
				}
			}
		}
		
		return bestDirection;
	}
	
	/**
	 * Check if there's a blocking block in the path
	 */
	private boolean isBlockingPath(double dx, double dz) {
		Vec3d currentPos = this.getPos();
		Vec3d checkPos = currentPos.add(dx * 1.5, 0, dz * 1.5);
		
		// Check for solid blocks in the path
		return !this.getWorld().isSpaceEmpty(this, new Box(checkPos.subtract(0.3, 0, 0.3), checkPos.add(0.3, 2, 0.3)));
	}
	
	/**
	 * Make the entity jump with forward momentum toward target
	 */
	private void performJump() {
		if (this.isOnGround()) {
			// Mark as jumping and reset the timer
			this.isJumping = true;
			this.jumpStateTicks = 0;
			
			// Always calculate forward momentum toward target when jumping
			var target = this.getTarget();
			double vx = 0;
			double vz = 0;
			
			if (target != null) {
				// Find the best direction to jump toward the target
				Vec3d start = this.getPos();
				Vec3d end = new Vec3d(target.getX(), this.getY(), target.getZ());
				double distance = start.distanceTo(end);
				
				if (distance > 0.1) {
					// Try direct path first
					Vec3d directDirection = end.subtract(start).normalize();
					
					// Check if direct path is blocked
					boolean directBlocked = false;
					for (double i = 0.5; i < Math.min(1.5, distance); i += 0.3) {
						Vec3d checkPos = start.add(directDirection.multiply(i));
						if (!this.getWorld().isSpaceEmpty(this, new Box(checkPos.subtract(0.3, 0, 0.3), checkPos.add(0.3, 2, 0.3)))) {
							directBlocked = true;
							break;
						}
					}
					
					if (!directBlocked) {
						// Use direct path with stronger momentum
						// Scale speed based on distance - jump further toward distant targets
						double baseSpeed = 0.5; // Increased from 0.4
						double distanceBonus = Math.min(distance * 0.1, 0.3); // Up to 0.3 extra speed for distant targets
						double speed = baseSpeed + distanceBonus;
						vx = directDirection.x * speed;
						vz = directDirection.z * speed;
					} else {
						// Find best alternative direction with stronger momentum
						Vec3d bestDirection = findBestJumpDirection(target);
						if (bestDirection != null) {
							double baseSpeed = 0.5;
							double distanceBonus = Math.min(distance * 0.1, 0.3);
							double speed = baseSpeed + distanceBonus;
							vx = bestDirection.x * speed;
							vz = bestDirection.z * speed;
						} else {
							// If no good direction found, jump straight up and slightly forward
							vx = directDirection.x * 0.3;
							vz = directDirection.z * 0.3;
						}
					}
				}
			}
			
			// Higher jump velocity to clear blocks more easily
			// Vertical velocity increased from 0.5 to 0.6 for better block clearing
			this.setVelocity(vx, 0.6, vz);
			this.velocityDirty = true;
		}
	}
	
	/**
	 * Find the best direction to jump toward the target
	 */
	private Vec3d findBestJumpDirection(Entity target) {
		if (target == null) return null;
		
		Vec3d start = this.getPos();
		Vec3d end = new Vec3d(target.getX(), this.getY(), target.getZ());
		Vec3d directDirection = end.subtract(start).normalize();
		double directAngle = Math.atan2(directDirection.z, directDirection.x);
		
		// Try multiple angles with preference for directions closer to the target
		// Start with smaller deviations from direct path
		double[] angleOffsets = {0, Math.PI/6, -Math.PI/6, Math.PI/4, -Math.PI/4, Math.PI/3, -Math.PI/3, 
		                         Math.PI/2, -Math.PI/2, 2*Math.PI/3, -2*Math.PI/3, 3*Math.PI/4, -3*Math.PI/4};
		
		Vec3d bestDirection = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		
		for (double angleOffset : angleOffsets) {
			double angle = directAngle + angleOffset;
			Vec3d testDirection = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
			
			// Test multiple jump distances to find the best landing spot
			for (double jumpDist = 1.5; jumpDist <= 3.0; jumpDist += 0.5) {
				Vec3d landingPos = start.add(testDirection.multiply(jumpDist));
				
				// Check if landing area is clear
				boolean landingClear = this.getWorld().isSpaceEmpty(this, new Box(
					landingPos.subtract(0.3, 0, 0.3), 
					landingPos.add(0.3, 2, 0.3)
				));
				
				if (!landingClear) continue;
				
				// Check if there's ground to land on (within 2 blocks down)
				boolean hasGround = false;
				for (double checkDown = 0; checkDown <= 2.0; checkDown += 0.5) {
					Vec3d groundCheck = landingPos.subtract(0, checkDown, 0);
					boolean groundExists = !this.getWorld().isSpaceEmpty(this, new Box(
						groundCheck.subtract(0.3, -0.1, 0.3), 
						groundCheck.add(0.3, 0, 0.3)
					));
					if (groundExists) {
						hasGround = true;
						break;
					}
				}
				
				if (!hasGround) continue;
				
				// Calculate score based on multiple factors
				double targetDist = landingPos.distanceTo(end);
				double currentDist = start.distanceTo(end);
				double distanceImprovement = currentDist - targetDist;
				
				// Prefer directions closer to direct path
				double angleDiff = Math.abs(angleOffset);
				double angleScore = 1.0 - (angleDiff / Math.PI);
				
				// Prefer longer jumps (more momentum)
				double jumpDistScore = jumpDist / 3.0;
				
				// Avoid landing in previously visited spots by adding slight randomness
				double randomFactor = this.getRandom().nextDouble() * 0.1;
				
				// Combined score: heavily weight distance improvement, then angle, then jump distance
				double score = distanceImprovement * 0.5 + angleScore * 0.25 + jumpDistScore * 0.15 + randomFactor * 0.1;
				
				if (score > bestScore) {
					bestScore = score;
					bestDirection = testDirection;
				}
			}
		}
		
		return bestDirection;
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
		for (PlayerEntity e : sw.getEntitiesByClass(PlayerEntity.class, box, (pe) -> pe.isAlive())) {
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


