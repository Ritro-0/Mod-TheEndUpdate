package com.theendupdate.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
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
    
    // Boss bar management
    public KingPhantomBossBarManager bossBarManager;
    
    // Attack tracking
    private static final int ATTACK_INTERVAL = 200; // 10 seconds (200 ticks)
    private int attackCooldown = ATTACK_INTERVAL; // Start with full cooldown for initial hover phase
    
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
        
        // Get or create boss bar manager from registry (prevents duplicates on reload)
        if (!world.isClient()) {
            this.bossBarManager = KingPhantomBossBarManager.getOrCreate(this.getUuid());
        }
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
    public void travel(Vec3d movementInput) {
        // Override travel to have complete control over movement
        // Vanilla phantoms have special flight physics we want to bypass
        
        if (this.getEntityWorld().isClient()) {
            // Client side - just do normal travel
            super.travel(movementInput);
            return;
        }
        
        // Server side - use our velocity directly without phantom flight physics
        Vec3d velocity = this.getVelocity();
        
        // Apply velocity to position
        this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        
        // Apply air friction (0.91 is vanilla air resistance)
        this.setVelocity(velocity.multiply(0.91, 0.91, 0.91));
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Ensure the phantom never lands - always keep it slightly off the ground for animations
        if (this.isOnGround()) {
            // If somehow on ground, push upward
            this.setVelocity(this.getVelocity().add(0, 0.2, 0));
            this.velocityModified = true;
        }
        
        // Update boss bar on server side
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            // Start boss bar on first tick
            if (!this.bossBarManager.isActive() && this.age == 1) {
                this.bossBarManager.startBossFight(this);
            }
            
            // Update boss bar every tick
            if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
                this.bossBarManager.tick(serverWorld, this);
            }
        }
        
        // Server: advance any active ranged beam
        if (!this.getEntityWorld().isClient()) {
            if (this.rangedBeamTravelTicks > 0 && this.rangedBeamStart != null && this.rangedBeamEnd != null) {
                advanceRangedBeam();
            }
            
            // Decrement attack cooldown
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
        }
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
    
    @Override
    public void remove(net.minecraft.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        
        // Handle boss bar cleanup based on removal reason
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            if (reason == net.minecraft.entity.Entity.RemovalReason.KILLED) {
                // Normal death - permanently end boss bar
                this.bossBarManager.permanentlyEnd();
            } else if (reason == net.minecraft.entity.Entity.RemovalReason.DISCARDED) {
                // Entity discarded (commands, peaceful mode, etc.) - permanently end
                this.bossBarManager.permanentlyEnd();
            } else if (reason == net.minecraft.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                // Chunk unloaded - just pause boss bar (keep in registry for reload)
                this.bossBarManager.endBossFight();
            } else if (reason == net.minecraft.entity.Entity.RemovalReason.CHANGED_DIMENSION) {
                // Dimension change - just pause boss bar (entity will reload in new dimension)
                this.bossBarManager.endBossFight();
            } else {
                // Other reasons - permanently end to be safe
                this.bossBarManager.permanentlyEnd();
            }
        }
    }
    
    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        
        // Permanently clean up boss bar when entity dies
        // Note: remove() will also be called with KILLED reason, but this ensures cleanup
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            this.bossBarManager.permanentlyEnd();
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
                // Set idle circle center to current position if not set
                if (this.idleCircleCenter == null) {
                    this.idleCircleCenter = new Vec3d(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                }
                circleCenter = this.idleCircleCenter;
            } else {
                // Player found - circle around player position (25 blocks above)
                circleCenter = new Vec3d(
                    targetPlayer.getX(),
                    targetPlayer.getY() + HOVER_HEIGHT,
                    targetPlayer.getZ()
                );
                // Update idle circle center to current position for when player leaves
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
            return this.isSwooping && this.swoopTimer > 0;
        }
        
        @Override
        public void start() {
            if (this.targetPlayer == null) return;
            
            // 50% chance for each attack type
            boolean useSwoop = this.phantom.getRandom().nextBoolean();
            
            if (useSwoop) {
                // Swoop attack
                this.isSwooping = true;
                this.swoopTimer = SWOOP_DURATION;
                this.hasDamagedThisSwoop = false; // Reset damage flag for new swoop
                
                // Target slightly in front of player to account for movement
                Vec3d playerVel = this.targetPlayer.getVelocity();
                Vec3d playerPos = new Vec3d(this.targetPlayer.getX(), this.targetPlayer.getY(), this.targetPlayer.getZ());
                this.swoopTarget = playerPos.add(playerVel.multiply(0.5));
            } else {
                // Ranged beam attack
                this.isSwooping = false;
                
                // Fire at player's current position
                Vec3d targetPos = new Vec3d(
                    this.targetPlayer.getX(),
                    this.targetPlayer.getY() + this.targetPlayer.getStandingEyeHeight() * 0.5,
                    this.targetPlayer.getZ()
                );
                
                this.phantom.startRangedBeamAttack(targetPos);
            }
            
            // Set cooldown for next attack (10 seconds)
            this.phantom.setAttackCooldown(ATTACK_INTERVAL);
        }
        
        @Override
        public void tick() {
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
                            }
                        }
                    }
                }
            }
            
            // End swoop early if we've passed the target
            if (this.swoopTimer <= 0) {
                this.isSwooping = false;
            }
        }
        
        @Override
        public void stop() {
            this.isSwooping = false;
            this.swoopTimer = 0;
            this.swoopTarget = null;
            this.hasDamagedThisSwoop = false; // Reset damage flag when swoop ends
        }
    }
}

