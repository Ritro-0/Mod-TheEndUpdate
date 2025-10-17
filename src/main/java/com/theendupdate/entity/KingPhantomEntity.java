package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.EnumSet;

/**
 * King Phantom - A massive phantom variant that is 4x the size of a normal phantom.
 * 
 * Key features:
 * - 4x scale of normal phantom
 * - Uses custom king_phantom.png and king_phantom_eyes.png textures
 * - Increased health and damage compared to normal phantoms
 * - Deep blood red boss bar
 * - Custom AI: hovers 25 blocks above players, alternates between swoop attacks and ranged beam attacks
 */
public class KingPhantomEntity extends PhantomEntity {
    
    // Data tracker for persistent phase state
    private static final TrackedData<Integer> CURRENT_PHASE = DataTracker.registerData(KingPhantomEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HAS_TRIGGERED_PHASE_TRANSITION = DataTracker.registerData(KingPhantomEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Boss bar management
    public KingPhantomBossBarManager bossBarManager;
    
    // Phase tracking
    private static final int PHASE_1 = 1;
    private static final int PHASE_2 = 2;
    
    // Phase transition tracking
    private boolean isInPhaseTransition = false;
    private int phaseTransitionTicks = 0;
    private static final int PHASE_TRANSITION_DURATION = 100; // 5 seconds
    private Vec3d phaseTransitionPosition = null;
    
    // Attack tracking
    private static final int ATTACK_INTERVAL_PHASE_1 = 200; // 10 seconds (200 ticks)
    private static final int ATTACK_INTERVAL_PHASE_2 = 100; // 5 seconds (100 ticks)
    private int attackCooldown = ATTACK_INTERVAL_PHASE_1; // Start with full cooldown for initial hover phase
    private boolean isSwooping = false; // Track if currently executing a swoop attack
    
    // Summon attack tracking
    private boolean isSummoning = false;
    private int summonPhase = 0; // 0 = not summoning, 1 = descending, 2 = ascending after summon
    private PlayerEntity summonTarget = null;
    private Vec3d summonTargetPos = null;
    private Vec3d summonReturnPos = null;
    private boolean hasSummoned = false; // Track if we've already summoned phantoms
    
    // Ranged beam attack tracking
    private int rangedBeamTravelTicks = 0;
    private Vec3d rangedBeamStart;
    private Vec3d rangedBeamEnd;
    private double rangedBeamSpeedPerTick = 40.0 / 60.0; // ~0.666 blocks/tick
    
    public KingPhantomEntity(EntityType<? extends PhantomEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 20; // More XP than regular phantom (5)
        this.setPersistent(); // Persistent to avoid chunk unload despawn, but still despawns in peaceful
        this.setNoGravity(true); // Ensure no gravity for proper flying
        
        // Boss bar will be initialized in tick() method (lazy init, like Shadow Creaking)
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CURRENT_PHASE, PHASE_1);
        builder.add(HAS_TRIGGERED_PHASE_TRANSITION, Boolean.FALSE);
    }
    
    @Override
    protected void initGoals() {
        // Clear default phantom goals and add custom ones
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);
        
        // Add custom goals (lower number = higher priority)
        this.goalSelector.add(1, new PeriodicAttackGoal(this)); // Attacks take priority
        this.goalSelector.add(2, new HoverAbovePlayerGoal(this)); // Hover when not attacking
        
        // Add targeting - only target players in Survival/Adventure mode
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
            @Override
            public boolean canStart() {
                // Only target players who are not in creative or spectator mode
                if (!super.canStart()) return false;
                PlayerEntity target = KingPhantomEntity.this.getEntityWorld().getClosestPlayer(
                    KingPhantomEntity.this, 64.0
                );
                return target != null && !target.isCreative() && !target.isSpectator();
            }
        });
    }
    
    @Override
    public ItemStack getPickBlockStack() {
        // Return the spawn egg when middle-clicked in creative mode
        return new ItemStack(com.theendupdate.registry.ModItems.KING_PHANTOM_SPAWN_EGG);
    }
    
    @Override
    public void checkDespawn() {
        // Despawn in peaceful mode (like other hostile mobs)
        if (this.getEntityWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        
        // Otherwise, don't despawn (persistent boss)
        // Don't call super.checkDespawn() to prevent normal despawn logic
    }
    
    @Override
    public boolean isFireImmune() {
        // King Phantom is immune to all fire damage
        return true;
    }
    
    @Override
    public boolean isInvulnerable() {
        // Invulnerable during phase transition
        return super.isInvulnerable() || this.isInPhaseTransition;
    }
    
    @Override
    public void travel(Vec3d movementInput) {
        // Override travel to have complete control over movement
        // Vanilla phantoms have special flight physics we want to bypass
        
        if (this.getEntityWorld().isClient()) {
            // Client side - just do normal travel
            super.travel(movementInput);
            return;
        }
        
        // Server side - use our velocity with proper collision detection
        Vec3d velocity = this.getVelocity();
        
        // Only prevent downward velocity when NOT swooping or summoning
        if (!this.isSwooping && !this.isSummoning) {
            // Prevent downward velocity from pushing the entity into the ground
            // This is especially important on entity load/reload
            if (velocity.y < -0.1) {
                velocity = new Vec3d(velocity.x, -0.1, velocity.z);
            }
            
            // If close to ground, don't apply downward velocity at all
            if (this.isOnGround() || this.getY() - Math.floor(this.getY()) < 0.5) {
                if (velocity.y < 0) {
                    velocity = new Vec3d(velocity.x, 0, velocity.z);
                }
            }
        }
        
        // Update velocity for friction
        this.setVelocity(velocity.multiply(0.91, 0.91, 0.91));
        
        // Use vanilla entity movement with collision detection
        // This respects block collisions while still allowing our custom velocity control
        this.move(net.minecraft.entity.MovementType.SELF, velocity);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Sync phase state from command tags on server (persists across saves)
        if (!this.getEntityWorld().isClient()) {
            if (this.getCommandTags().contains("theendupdate:phase_transition_triggered") && !this.dataTracker.get(HAS_TRIGGERED_PHASE_TRANSITION)) {
                this.dataTracker.set(HAS_TRIGGERED_PHASE_TRANSITION, true);
            }
            if (this.getCommandTags().contains("theendupdate:phase_2") && this.dataTracker.get(CURRENT_PHASE) != PHASE_2) {
                this.dataTracker.set(CURRENT_PHASE, PHASE_2);
            }
        }
        
        // Ensure the phantom never lands - but NOT during swoop or summon attacks!
        if (!this.isSwooping && !this.isSummoning) {
            if (this.isOnGround()) {
                // If somehow on ground, push upward strongly
                this.setVelocity(this.getVelocity().add(0, 0.5, 0));
                this.velocityModified = true;
            } else if (!this.getEntityWorld().isClient()) {
                // Also check if we're too close to blocks below
                net.minecraft.util.math.BlockPos posBelow = this.getBlockPos().down();
                if (!this.getEntityWorld().getBlockState(posBelow).isAir()) {
                    // There's a block directly below - apply gentle upward force
                    double distanceToGround = this.getY() - posBelow.getY() - 1.0;
                    if (distanceToGround < 2.0) {
                        this.setVelocity(this.getVelocity().add(0, 0.1, 0));
                        this.velocityModified = true;
                    }
                }
            }
        }
        
        // Note: Boss bar ticking is handled by KingPhantomBossBarRegistry, not here
        if (!this.getEntityWorld().isClient()) {
            // Initialize boss bar if not already done (fallback for any spawn method)
            if (this.bossBarManager == null && this.age <= 5) {
                this.initializeBossBar();
            }
        }
        
        // Server: Handle phase transition and attacks
        if (!this.getEntityWorld().isClient()) {
            // Check for phase 2 transition (at 50% health)
            if (this.dataTracker.get(CURRENT_PHASE) == PHASE_1 && !this.dataTracker.get(HAS_TRIGGERED_PHASE_TRANSITION)) {
                float healthPercent = this.getHealth() / this.getMaxHealth();
                if (healthPercent <= 0.5f) {
                    startPhaseTransition();
                }
            }
            
            // Handle phase transition
            if (this.isInPhaseTransition) {
                handlePhaseTransition();
            } else {
                // Handle summon attack
                if (this.isSummoning) {
                    handleSummonAttack();
                }
                
                // Normal behavior: advance ranged beam and cooldown
                if (this.rangedBeamTravelTicks > 0 && this.rangedBeamStart != null && this.rangedBeamEnd != null) {
                    advanceRangedBeam();
                }
                
                // Decrement attack cooldown
                if (this.attackCooldown > 0) {
                    this.attackCooldown--;
                }
            }
        }
    }
    
    /**
     * Starts the phase 2 transition sequence
     */
    private void startPhaseTransition() {
        this.dataTracker.set(HAS_TRIGGERED_PHASE_TRANSITION, true);
        this.isInPhaseTransition = true;
        this.phaseTransitionTicks = 0;
        this.phaseTransitionPosition = new Vec3d(this.getX(), this.getY(), this.getZ());
        
        // Add command tag for persistence across saves
        if (!this.getEntityWorld().isClient()) {
            this.addCommandTag("theendupdate:phase_transition_triggered");
        }
        
        // Stop all movement
        this.setVelocity(Vec3d.ZERO);
        this.velocityModified = true;
    }
    
    /**
     * Handles the phase transition animation and effects
     */
    private void handlePhaseTransition() {
        if (!(this.getEntityWorld() instanceof ServerWorld sw)) return;
        
        this.phaseTransitionTicks++;
        
        // Freeze in place
        this.setVelocity(Vec3d.ZERO);
        this.velocityModified = true;
        
        // Snap to transition position to prevent any drift
        if (this.phaseTransitionPosition != null) {
            this.setPosition(this.phaseTransitionPosition.x, this.phaseTransitionPosition.y, this.phaseTransitionPosition.z);
        }
        
        // Spawn particle bubble every tick
        double radius = 3.0; // Radius of particle sphere
        int particlesPerTick = 10;
        
        for (int i = 0; i < particlesPerTick; i++) {
            // Random points on sphere surface
            double theta = this.getRandom().nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * this.getRandom().nextDouble() - 1);
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            
            // Crying obsidian particle (purple drip)
            sw.spawnParticles(
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                1, 0, 0, 0, 0.0
            );
            
            // Spore blossom particle (green falling)
            sw.spawnParticles(
                ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                1, 0, 0, 0, 0.0
            );
        }
        
        // At the end of transition, spawn burst and enter phase 2
        if (this.phaseTransitionTicks >= PHASE_TRANSITION_DURATION) {
            // Big burst of particles
            for (int i = 0; i < 100; i++) {
                double theta = this.getRandom().nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * this.getRandom().nextDouble() - 1);
                
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.sin(phi) * Math.sin(theta);
                double z = radius * Math.cos(phi);
                
                // Velocity outward
                double vx = x * 0.2;
                double vy = y * 0.2;
                double vz = z * 0.2;
                
                sw.spawnParticles(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    this.getX() + x,
                    this.getY() + y,
                    this.getZ() + z,
                    0, vx, vy, vz, 1.0
                );
                
                sw.spawnParticles(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getX() + x,
                    this.getY() + y,
                    this.getZ() + z,
                    0, vx, vy, vz, 1.0
                );
            }
            
            // Add explosion particles
            sw.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
            sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
            
            // Enter phase 2
            this.dataTracker.set(CURRENT_PHASE, PHASE_2);
            this.isInPhaseTransition = false;
            this.phaseTransitionPosition = null;
            
            // Add command tag for persistence across saves
            this.addCommandTag("theendupdate:phase_2");
            
            // Reset attack cooldown to the new phase 2 interval
            this.attackCooldown = ATTACK_INTERVAL_PHASE_2;
        }
    }
    
    /**
     * Returns whether the King Phantom is currently in phase transition
     */
    public boolean isInPhaseTransition() {
        return this.isInPhaseTransition;
    }
    
    /**
     * Returns the current phase (1 or 2)
     */
    public int getCurrentPhase() {
        return this.dataTracker.get(CURRENT_PHASE);
    }
    
    /**
     * Returns the attack interval for the current phase
     */
    public int getAttackInterval() {
        return this.dataTracker.get(CURRENT_PHASE) == PHASE_1 ? ATTACK_INTERVAL_PHASE_1 : ATTACK_INTERVAL_PHASE_2;
    }
    
    /**
     * Starts a ranged beam attack targeting the specified position
     */
    public void startRangedBeamAttack(Vec3d targetPos) {
        if (!(this.getEntityWorld() instanceof ServerWorld)) return;
        if (this.rangedBeamTravelTicks > 0) return; // Already firing
        
        // Snapshot start and end
        this.rangedBeamStart = new Vec3d(this.getX(), this.getY(), this.getZ());
        this.rangedBeamEnd = targetPos;
        
        double distance = this.rangedBeamStart.distanceTo(this.rangedBeamEnd);
        this.rangedBeamTravelTicks = Math.max(1, (int)Math.ceil(distance / this.rangedBeamSpeedPerTick));
    }
    
    /**
     * Starts a summon attack targeting the specified player
     */
    public void startSummonAttack(PlayerEntity target) {
        if (target == null || !target.isAlive()) return;
        if (this.getEntityWorld().isClient()) return;
        
        // Don't summon on creative or spectator players
        if (target.isCreative() || target.isSpectator()) return;
        
        this.isSummoning = true;
        this.summonPhase = 1; // Start descending
        this.summonTarget = target;
        this.hasSummoned = false; // Reset summon flag
        
        // Store current position as return point
        this.summonReturnPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        
        // Dive toward the player with predictive targeting
        Vec3d playerPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        Vec3d playerVel = target.getVelocity();
        
        this.summonTargetPos = playerPos.add(playerVel.multiply(0.5));
        
        // Spawn red particles to show summon attack started
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 50, 1.0, 1.0, 1.0, 0.1);
        }
    }
    
    /**
     * Handles summon attack logic each tick
     */
    private void handleSummonAttack() {
        if (!this.isSummoning || this.summonPhase == 0) return;
        
        if (this.summonPhase == 1) {
            // Phase 1: Descending toward player
            handleSummonDescent();
        } else if (this.summonPhase == 2) {
            // Phase 2: Ascending after summon
            handleSummonAscent();
        }
    }
    
    /**
     * Handles the descent phase of summon attack
     */
    private void handleSummonDescent() {
        if (this.summonTarget == null || !this.summonTarget.isAlive() || this.summonTargetPos == null) {
            endSummonAttack();
            return;
        }
        
        // Safety check: if player became creative/spectator, end summon
        if (this.summonTarget.isCreative() || this.summonTarget.isSpectator()) {
            endSummonAttack();
            return;
        }
        
        // Move toward the target position at high speed
        Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d direction = this.summonTargetPos.subtract(currentPos).normalize();
        
        double speed = 0.8; // Very fast dive
        Vec3d velocity = direction.multiply(speed);
        this.setVelocity(velocity);
        this.velocityDirty = true;
        
        // Spawn dramatic particles during descent
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            
            // When close to player, spawn even more dramatic particles
            double distance = this.squaredDistanceTo(this.summonTarget);
            if (distance < 16.0) { // Within 4 blocks
                sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
            }
            
            // Check for contact with player
            if (distance < 4.0 && !this.hasSummoned) { // Within 2 blocks and haven't summoned yet
                // Summon 4 phantoms around the player!
                summonPhantoms();
                this.hasSummoned = true;
                
                this.summonPhase = 2; // Switch to ascending
                return;
            }
        }
        
        // End attack if we've reached target or hit ground
        if (currentPos.distanceTo(this.summonTargetPos) < 1.0 || this.isOnGround()) {
            endSummonAttack();
        }
    }
    
    /**
     * Summons 4 regular phantoms around the target player
     */
    private void summonPhantoms() {
        if (this.summonTarget == null || !this.summonTarget.isAlive()) return;
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return;
        
        Vec3d playerPos = new Vec3d(this.summonTarget.getX(), this.summonTarget.getY(), this.summonTarget.getZ());
        
        // Spawn 4 regular phantoms in a circle around the player
        double radius = 5.0; // 5 blocks away from player
        double heightAbove = 8.0; // 8 blocks above player
        
        for (int i = 0; i < 4; i++) {
            double angle = (i * Math.PI / 2.0); // 0°, 90°, 180°, 270°
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            
            double spawnX = playerPos.x + xOffset;
            double spawnY = playerPos.y + heightAbove;
            double spawnZ = playerPos.z + zOffset;
            
            // Create and spawn regular phantom
            PhantomEntity phantom = new PhantomEntity(EntityType.PHANTOM, serverWorld);
            phantom.refreshPositionAndAngles(spawnX, spawnY, spawnZ, 0.0f, 0.0f);
            phantom.setTarget(this.summonTarget); // Make them immediately target the player
            serverWorld.spawnEntity(phantom);
            
            // Spawn dramatic summon particles at spawn location
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, 
                spawnX, spawnY, spawnZ, 
                20, 0.5, 0.5, 0.5, 0.1);
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, 
                spawnX, spawnY, spawnZ, 
                15, 0.3, 0.3, 0.3, 0.05);
        }
        
        // Create actual explosion (damage only, no block destruction)
        serverWorld.createExplosion(
            this,
            null, // No damage source - use default
            new net.minecraft.world.explosion.ExplosionBehavior() {
                @Override
                public boolean canDestroyBlock(net.minecraft.world.explosion.Explosion explosion, 
                                               net.minecraft.world.BlockView world, 
                                               net.minecraft.util.math.BlockPos pos, 
                                               net.minecraft.block.BlockState state, 
                                               float power) {
                    return false; // Don't destroy blocks
                }
            },
            playerPos.x,
            playerPos.y,
            playerPos.z,
            3.0F, // TNT explosion power (TNT is 4.0, this is slightly less)
            false, // No fire
            net.minecraft.world.World.ExplosionSourceType.MOB
        );
        
        // Spawn dramatic particles at player location
        serverWorld.spawnParticles(ParticleTypes.CLOUD, 
            playerPos.x, playerPos.y, playerPos.z, 
            50, 1.0, 1.0, 1.0, 0.2);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, 
            playerPos.x, playerPos.y, playerPos.z, 
            10, 0.5, 0.5, 0.5, 0.0);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, 
            playerPos.x, playerPos.y, playerPos.z, 
            3, 0.0, 0.0, 0.0, 0.0);
        
        // Play ominous sound
        serverWorld.playSound(null, 
            playerPos.x, playerPos.y, playerPos.z,
            net.minecraft.sound.SoundEvents.ENTITY_PHANTOM_AMBIENT, 
            net.minecraft.sound.SoundCategory.HOSTILE, 
            2.0F, 0.5F); // Lower pitch for more dramatic effect
    }
    
    /**
     * Handles the ascent phase after summoning phantoms
     */
    private void handleSummonAscent() {
        Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        if (this.summonReturnPos == null) {
            endSummonAttack();
            return;
        }
        
        Vec3d direction = this.summonReturnPos.subtract(currentPos);
        double distanceToReturn = direction.length();
        
        // If close to return position, end attack
        if (distanceToReturn < 2.0) {
            endSummonAttack();
            return;
        }
        
        // Ascend back to hovering position
        double speed = 0.4;
        Vec3d velocity = direction.normalize().multiply(speed);
        this.setVelocity(velocity);
        this.velocityDirty = true;
    }
    
    /**
     * Ends the summon attack and resets state
     */
    private void endSummonAttack() {
        this.isSummoning = false;
        this.summonPhase = 0;
        this.summonTarget = null;
        this.summonTargetPos = null;
        this.summonReturnPos = null;
        this.hasSummoned = false;
    }
    
    /**
     * Returns whether currently executing a summon attack
     */
    public boolean isSummoning() {
        return this.isSummoning;
    }
    
    /**
     * Advances the ranged beam animation and handles collision/damage
     */
    private void advanceRangedBeam() {
        if (!(this.getEntityWorld() instanceof ServerWorld sw)) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        if (this.rangedBeamStart == null || this.rangedBeamEnd == null) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        
        // Compute current head position along the path
        double totalDistance = this.rangedBeamStart.distanceTo(this.rangedBeamEnd);
        if (totalDistance < 1.0E-6) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        
        Vec3d dir = this.rangedBeamEnd.subtract(this.rangedBeamStart).normalize();
        int ticksRemaining = this.rangedBeamTravelTicks;
        int ticksElapsed = Math.max(0, (int)Math.ceil(totalDistance / this.rangedBeamSpeedPerTick) - ticksRemaining);
        double headDistance = Math.min(totalDistance, ticksElapsed * this.rangedBeamSpeedPerTick);
        Vec3d head = this.rangedBeamStart.add(dir.multiply(headDistance));
        
        // Check for block collision along the beam's path for this tick
        double nextHeadDistance = Math.min(totalDistance, (ticksElapsed + 1) * this.rangedBeamSpeedPerTick);
        Vec3d nextHead = this.rangedBeamStart.add(dir.multiply(nextHeadDistance));
        net.minecraft.util.hit.HitResult blockHit = sw.raycast(new net.minecraft.world.RaycastContext(
            head,
            nextHead,
            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            this
        ));
        
        // If we hit a block, detonate at the impact point
        if (blockHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            Vec3d impactPos = blockHit.getPos();
            // Spawn particles along the path up to the impact point
            int segmentPoints = 20;
            for (int i = 0; i < segmentPoints; i++) {
                double t = i / (double)segmentPoints;
                Vec3d p = head.lerp(impactPos, t);
                sw.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.0);
                sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            // Detonate at impact point
            spawnBeamExplosion(impactPos.x, impactPos.y, impactPos.z);
            // Clean up beam state
            this.rangedBeamStart = null;
            this.rangedBeamEnd = null;
            this.rangedBeamTravelTicks = 0;
            return;
        }
        
        // No block hit - continue beam normally
        // Spawn particles along the beam for better visibility
        int segmentPoints = 20;
        for (int i = 0; i < segmentPoints; i++) {
            double offset = (i / (double)segmentPoints) * this.rangedBeamSpeedPerTick;
            Vec3d p = head.add(dir.multiply(offset));
            
            // Multiple particle types for better visibility
            sw.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.0);
            sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            sw.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        
        this.rangedBeamTravelTicks--;
        if (this.rangedBeamTravelTicks <= 0) {
            // Detonate at the end with area damage
            spawnBeamExplosion(this.rangedBeamEnd.x, this.rangedBeamEnd.y, this.rangedBeamEnd.z);
            this.rangedBeamStart = null;
            this.rangedBeamEnd = null;
        }
    }
    
    /**
     * Spawns an explosion at the beam's impact point and damages nearby players
     */
    private void spawnBeamExplosion(double cx, double cy, double cz) {
        if (!(this.getEntityWorld() instanceof ServerWorld sw)) return;
        
        // Bigger particle burst
        sw.spawnParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 3, 0.0, 0.0, 0.0, 0.0);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        sw.spawnParticles(ParticleTypes.END_ROD, cx, cy, cz, 50, 0.5, 0.5, 0.5, 0.1);
        sw.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.05);
        
        // Area damage - scale based on difficulty
        float baseDamage = 10.0f;
        float damage = getDifficultyScaledDamage(baseDamage);
        double radius = 4.0; // Increased radius
        Box box = new Box(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
        
        // Only target players (excluding creative/spectator) like the Shadow Creaking does
        for (PlayerEntity player : sw.getEntitiesByClass(PlayerEntity.class, box, 
                (pe) -> pe.isAlive() && !pe.isCreative() && !pe.isSpectator())) {
            Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            double distance = playerPos.distanceTo(new Vec3d(cx, cy, cz));
            if (distance <= radius) {
                // Try generic damage first (works reliably for AoE)
                boolean hit = player.damage(sw, sw.getDamageSources().generic(), damage);
                if (!hit) {
                    // Fallback to mobAttack if generic didn't work
                    hit = player.damage(sw, sw.getDamageSources().mobAttack(this), damage);
                }
                
                if (hit) {
                    // Knockback away from center
                    double dx = player.getX() - cx;
                    double dz = player.getZ() - cz;
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len > 1.0E-4) {
                        dx /= len;
                        dz /= len;
                        float kb = 1.0f;
                        player.takeKnockback(kb, -dx, -dz);
                    }
                }
            }
        }
    }
    
    public int getAttackCooldown() {
        return this.attackCooldown;
    }
    
    public void setAttackCooldown(int cooldown) {
        this.attackCooldown = cooldown;
    }
    
    /**
     * Initializes the boss bar for this entity
     */
    public void initializeBossBar() {
        if (this.bossBarManager != null) return;
        
        try {
            this.bossBarManager = KingPhantomBossBarRegistry.createBossBar(this);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        
        // Clean up boss bar on death
        if (!this.getEntityWorld().isClient()) {
            KingPhantomBossBarRegistry.removeBossBar(this.getUuid());
        }
    }
    
    @Override
    public void remove(net.minecraft.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        
        // Clean up boss bar when entity is removed for any reason (peaceful mode, dimension change, etc.)
        // Note: Registry handles cleanup when entity is gone
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            if (reason != net.minecraft.entity.Entity.RemovalReason.KILLED) {
                // Removed by peaceful mode, dimension change, etc.
                // Registry will detect entity is gone and clean up
            }
        }
    }
    
    @Override
    public boolean tryAttack(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.Entity target) {
        // Override to ensure we can always attack (bypass phantom's restrictions)
        if (target instanceof LivingEntity livingTarget) {
            // Don't attack creative/spectator players
            if (target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
                return false;
            }
            
            // Scale damage based on difficulty
            float baseDamage = (float)this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
            float damage = getDifficultyScaledDamage(baseDamage);
            
            // Try with generic damage source (works reliably)
            boolean hit = livingTarget.damage(world, world.getDamageSources().generic(), damage);
            
            if (!hit) {
                // Fallback to mob attack damage source
                hit = livingTarget.damage(world, world.getDamageSources().mobAttack(this), damage);
            }
            
            if (hit) {
                // Apply knockback
                livingTarget.takeKnockback(0.4, 
                    this.getX() - livingTarget.getX(), 
                    this.getZ() - livingTarget.getZ());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Scales damage based on world difficulty.
     * Easy: 50% damage, Normal: 100% damage, Hard: 150% damage
     */
    private float getDifficultyScaledDamage(float baseDamage) {
        if (this.getEntityWorld() == null) return baseDamage;
        
        return switch (this.getEntityWorld().getDifficulty()) {
            case PEACEFUL -> 0.0f; // No damage in peaceful (shouldn't happen since mob despawns)
            case EASY -> baseDamage * 0.5f;
            case NORMAL -> baseDamage;
            case HARD -> baseDamage * 1.5f;
        };
    }
    
    public static DefaultAttributeContainer.Builder createKingPhantomAttributes() {
        // Phantom base stats: 20 HP, 6 damage, 0.4 follow range
        // King Phantom: 32x health, 2x damage
        return net.minecraft.entity.mob.HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, 640.0) // 32x normal phantom health (20)
            .add(EntityAttributes.ATTACK_DAMAGE, 12.0) // 2x normal phantom damage (6)
            .add(EntityAttributes.FOLLOW_RANGE, 64.0) // Extended range for larger mob
            .add(EntityAttributes.FLYING_SPEED, 0.6); // Flying speed
    }
    
    /**
     * Custom AI Goal: Hovers approximately 25 blocks above the nearest player's head
     */
    static class HoverAbovePlayerGoal extends Goal {
        private final KingPhantomEntity phantom;
        private static final double HOVER_HEIGHT = 25.0;
        private static final double HOVER_RADIUS = 3.0; // Circle around the player
        private Vec3d targetPosition;
        private Vec3d idleCircleCenter; // Position to circle around when no target
        
        public HoverAbovePlayerGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
            // Initialize idle circle center to phantom's spawn position
            this.idleCircleCenter = null;
        }
        
        /**
         * Finds the ground level below a given position by scanning downward
         */
        private double findGroundLevel(double x, double y, double z) {
            net.minecraft.util.math.BlockPos.Mutable pos = new net.minecraft.util.math.BlockPos.Mutable(x, y, z);
            
            // Scan downward up to 50 blocks to find solid ground
            for (int i = 0; i < 50; i++) {
                pos.setY((int)y - i);
                if (!this.phantom.getEntityWorld().getBlockState(pos).isAir()) {
                    // Found a solid block, return the Y position above it
                    return pos.getY() + 1.0;
                }
            }
            
            // If no ground found within 50 blocks, just use current Y position
            return y;
        }
        
        @Override
        public boolean canStart() {
            return true; // Always active when not attacking
        }
        
        @Override
        public boolean shouldContinue() {
            return true;
        }
        
        @Override
        public void tick() {
            // Don't hover during phase transition or summon attack
            if (this.phantom.isInPhaseTransition() || this.phantom.isSummoning()) {
                return;
            }
            
            // Find nearest valid player every tick to keep tracking (exclude creative/spectator)
            PlayerEntity targetPlayer = null;
            double closestDistance = 64.0;
            
            for (PlayerEntity player : this.phantom.getEntityWorld().getPlayers()) {
                if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
                double distance = this.phantom.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetPlayer = player;
                }
            }
            
            // Determine the center point to circle around
            Vec3d circleCenter;
            
            if (targetPlayer == null) {
                // No player nearby - circle around idle position
                // Set idle circle center to current position if not set, but maintain proper height
                if (this.idleCircleCenter == null) {
                    // Find the ground level below the phantom and hover HOVER_HEIGHT blocks above it
                    double groundY = findGroundLevel(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                    this.idleCircleCenter = new Vec3d(
                        this.phantom.getX(), 
                        groundY + HOVER_HEIGHT, 
                        this.phantom.getZ()
                    );
                }
                circleCenter = this.idleCircleCenter;
            } else {
                // Player found - circle around player position (25 blocks above)
                circleCenter = new Vec3d(
                    targetPlayer.getX(),
                    targetPlayer.getY() + HOVER_HEIGHT,
                    targetPlayer.getZ()
                );
                // Update idle circle center to player's position for when player leaves
                this.idleCircleCenter = circleCenter;
            }
            
            // Calculate a position around the circle center
            // Add some horizontal offset so it circles like a vulture
            double angle = (this.phantom.age * 0.05) % (2 * Math.PI);
            double offsetX = Math.cos(angle) * HOVER_RADIUS;
            double offsetZ = Math.sin(angle) * HOVER_RADIUS;
            
            this.targetPosition = new Vec3d(
                circleCenter.x + offsetX,
                circleCenter.y,
                circleCenter.z + offsetZ
            );
            
            // Calculate direction to target
            Vec3d currentPos = new Vec3d(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
            Vec3d direction = this.targetPosition.subtract(currentPos);
            double distance = direction.length();
            
            if (distance > 0.5) {
                direction = direction.normalize();
                
                // Set velocity based on distance (faster when far, slower when close)
                double speed = Math.min(1.0, distance * 0.1);
                speed = Math.max(0.2, speed); // Minimum speed
                
                Vec3d velocity = direction.multiply(speed);
                this.phantom.setVelocity(velocity);
                this.phantom.velocityModified = true;
                
                // Make the phantom face the direction it's moving
                double yaw = Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI) - 90.0;
                double pitch = Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)) * (180.0 / Math.PI) * -1.0;
                
                this.phantom.setYaw((float)yaw);
                this.phantom.setPitch((float)pitch);
                this.phantom.headYaw = (float)yaw;
                this.phantom.bodyYaw = (float)yaw;
            }
            
            // Look at the player if there is one, otherwise look at circle center
            if (targetPlayer != null) {
                this.phantom.getLookControl().lookAt(
                    targetPlayer.getX(),
                    targetPlayer.getEyeY(),
                    targetPlayer.getZ()
                );
            } else {
                this.phantom.getLookControl().lookAt(
                    circleCenter.x,
                    circleCenter.y,
                    circleCenter.z
                );
            }
        }
    }
    
    /**
     * Custom AI Goal: Periodically attacks with either a swoop or ranged beam attack
     */
    static class PeriodicAttackGoal extends Goal {
        private final KingPhantomEntity phantom;
        private PlayerEntity targetPlayer;
        private boolean isSwooping = false;
        private Vec3d swoopTarget;
        private int swoopTimer = 0;
        private boolean hasDamagedThisSwoop = false; // Track if we've already hit during this swoop
        private static final int SWOOP_DURATION = 40; // 2 seconds
        
        public PeriodicAttackGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            // Don't attack during phase transition or while summoning
            if (this.phantom.isInPhaseTransition() || this.phantom.isSummoning()) return false;
            
            // Only start if attack cooldown is finished
            if (this.phantom.getAttackCooldown() > 0) return false;
            
            // Find closest valid target (exclude creative/spectator)
            PlayerEntity closestPlayer = null;
            double closestDistance = 64.0;
            
            for (PlayerEntity player : this.phantom.getEntityWorld().getPlayers()) {
                if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
                double distance = this.phantom.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
            
            this.targetPlayer = closestPlayer;
            return this.targetPlayer != null;
        }
        
        @Override
        public boolean shouldContinue() {
            // Continue if swooping OR if phantom is summoning (so goal doesn't stop mid-summon)
            return (this.isSwooping && this.swoopTimer > 0) || this.phantom.isSummoning();
        }
        
        @Override
        public void start() {
            if (this.targetPlayer == null) return;
            
            int currentPhase = this.phantom.getCurrentPhase();
            
            if (currentPhase == PHASE_1) {
                // Phase 1: 50% swoop, 50% ranged
                boolean useSwoop = this.phantom.getRandom().nextBoolean();
                
                if (useSwoop) {
                    // Swoop attack
                    this.isSwooping = true;
                    this.phantom.isSwooping = true; // Update entity flag
                    this.swoopTimer = SWOOP_DURATION;
                    this.hasDamagedThisSwoop = false;
                    
                    // Target slightly in front of player to account for movement
                    Vec3d playerVel = this.targetPlayer.getVelocity();
                    Vec3d playerPos = new Vec3d(this.targetPlayer.getX(), this.targetPlayer.getY(), this.targetPlayer.getZ());
                    this.swoopTarget = playerPos.add(playerVel.multiply(0.5));
                } else {
                    // Ranged beam attack
                    this.isSwooping = false;
                    this.phantom.isSwooping = false; // Update entity flag
                    
                    // Fire at player's current position
                    Vec3d targetPos = new Vec3d(
                        this.targetPlayer.getX(),
                        this.targetPlayer.getY() + this.targetPlayer.getStandingEyeHeight() * 0.5,
                        this.targetPlayer.getZ()
                    );
                    
                    this.phantom.startRangedBeamAttack(targetPos);
                }
            } else {
                // Phase 2: 40% swoop, 40% ranged, 20% summon
                int attackChoice = this.phantom.getRandom().nextInt(100);
                
                if (attackChoice < 40) {
                    // Swoop attack (40%)
                    this.isSwooping = true;
                    this.phantom.isSwooping = true; // Update entity flag
                    this.swoopTimer = SWOOP_DURATION;
                    this.hasDamagedThisSwoop = false;
                    
                    // Target slightly in front of player to account for movement
                    Vec3d playerVel = this.targetPlayer.getVelocity();
                    Vec3d playerPos = new Vec3d(this.targetPlayer.getX(), this.targetPlayer.getY(), this.targetPlayer.getZ());
                    this.swoopTarget = playerPos.add(playerVel.multiply(0.5));
                } else if (attackChoice < 80) {
                    // Ranged beam attack (40%)
                    this.isSwooping = false;
                    this.phantom.isSwooping = false; // Update entity flag
                    
                    // Fire at player's current position
                    Vec3d targetPos = new Vec3d(
                        this.targetPlayer.getX(),
                        this.targetPlayer.getY() + this.targetPlayer.getStandingEyeHeight() * 0.5,
                        this.targetPlayer.getZ()
                    );
                    
                    this.phantom.startRangedBeamAttack(targetPos);
                } else {
                    // Summon attack (20%)
                    this.isSwooping = false;
                    this.phantom.isSwooping = false; // Update entity flag
                    this.phantom.startSummonAttack(this.targetPlayer);
                }
            }
            
            // Set cooldown for next attack based on current phase
            this.phantom.setAttackCooldown(this.phantom.getAttackInterval());
        }
        
        @Override
        public void tick() {
            // Don't tick swoop if summoning
            if (this.phantom.isSummoning()) return;
            
            if (!this.isSwooping) return;
            
            this.swoopTimer--;
            
            if (this.swoopTarget != null && this.targetPlayer != null) {
                // Swoop toward target at high speed
                Vec3d currentPos = new Vec3d(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                Vec3d direction = this.swoopTarget.subtract(currentPos).normalize();
                double speed = 1.2; // Fast swoop
                
                Vec3d velocity = direction.multiply(speed);
                this.phantom.setVelocity(velocity);
                this.phantom.velocityModified = true;
                
                // Make the phantom face the swoop direction
                double yaw = Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI) - 90.0;
                double pitch = Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)) * (180.0 / Math.PI) * -1.0;
                
                this.phantom.setYaw((float)yaw);
                this.phantom.setPitch((float)pitch);
                this.phantom.headYaw = (float)yaw;
                this.phantom.bodyYaw = (float)yaw;
                
                // Try to damage player if close enough (increased range) - but only once per swoop!
                if (!this.hasDamagedThisSwoop) {
                    double distance = this.phantom.squaredDistanceTo(this.targetPlayer);
                    if (distance < 9.0) { // 3 blocks radius
                        if (this.phantom.getEntityWorld() instanceof ServerWorld serverWorld) {
                            // Use the vanilla tryAttack method like other mobs do
                            boolean didDamage = this.phantom.tryAttack(serverWorld, this.targetPlayer);
                            if (didDamage) {
                                this.hasDamagedThisSwoop = true; // Mark that we've damaged during this swoop
                                // End the swoop immediately after landing a hit
                                this.swoopTimer = 0;
                                this.isSwooping = false;
                                this.phantom.isSwooping = false; // Update entity flag
                            }
                        }
                    }
                }
            }
            
            // End swoop early if we've passed the target
            if (this.swoopTimer <= 0) {
                this.isSwooping = false;
                this.phantom.isSwooping = false; // Update entity flag
            }
        }
        
        @Override
        public void stop() {
            this.isSwooping = false;
            this.phantom.isSwooping = false; // Update entity flag
            this.swoopTimer = 0;
            this.swoopTarget = null;
            this.hasDamagedThisSwoop = false; // Reset damage flag when swoop ends
        }
    }
}


