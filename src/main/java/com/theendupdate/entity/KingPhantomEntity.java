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
 * - Custom AI: hovers 10 blocks above players, alternates between swoop attacks and ranged beam attacks
 */
public class KingPhantomEntity extends PhantomEntity {
    
    // Boss bar management
    public KingPhantomBossBarManager bossBarManager;
    
    // Attack tracking
    private int attackCooldown = 0;
    private static final int ATTACK_INTERVAL = 200; // 10 seconds (200 ticks)
    
    // Ranged beam attack tracking
    private int rangedBeamTravelTicks = 0;
    private Vec3d rangedBeamStart;
    private Vec3d rangedBeamEnd;
    private double rangedBeamSpeedPerTick = 40.0 / 60.0; // ~0.666 blocks/tick
    
    public KingPhantomEntity(EntityType<? extends PhantomEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 20; // More XP than regular phantom (5)
        this.setPersistent();
        this.setNoGravity(true); // Ensure no gravity for proper flying
        
        // Initialize boss bar on server side
        if (!world.isClient()) {
            this.bossBarManager = new KingPhantomBossBarManager(this.getUuid());
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
        
        // Add targeting
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    @Override
    public ItemStack getPickBlockStack() {
        // Return the spawn egg when middle-clicked in creative mode
        return new ItemStack(com.theendupdate.registry.ModItems.KING_PHANTOM_SPAWN_EGG);
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
        
        // Spawn MORE particles along the beam for better visibility
        int segmentPoints = 20; // Increased from 6
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
        
        // Area damage
        float damage = 10.0f;
        double radius = 4.0; // Increased radius
        Box box = new Box(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
        
        // Only target players like the Shadow Creaking does
        for (PlayerEntity player : sw.getEntitiesByClass(PlayerEntity.class, box, (pe) -> pe.isAlive())) {
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
    public void onRemoved() {
        super.onRemoved();
        
        // Clean up boss bar when entity is removed
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            this.bossBarManager.endBossFight();
        }
    }
    
    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        
        // Clean up boss bar when entity dies
        if (!this.getEntityWorld().isClient() && this.bossBarManager != null) {
            this.bossBarManager.endBossFight();
        }
    }
    
    @Override
    public boolean tryAttack(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.Entity target) {
        // Override to ensure we can always attack (bypass phantom's restrictions)
        if (target instanceof LivingEntity livingTarget) {
            float damage = (float)this.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
            
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
    
    public static DefaultAttributeContainer.Builder createKingPhantomAttributes() {
        // Phantom base stats: 20 HP, 6 damage, 0.4 follow range
        // King Phantom: 4x scaling
        return net.minecraft.entity.mob.HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, 80.0) // 4x normal phantom health (20)
            .add(EntityAttributes.ATTACK_DAMAGE, 12.0) // 2x normal phantom damage (6)
            .add(EntityAttributes.FOLLOW_RANGE, 64.0) // Extended range for larger mob
            .add(EntityAttributes.FLYING_SPEED, 0.6); // Flying speed
    }
    
    /**
     * Custom AI Goal: Hovers approximately 10 blocks above the nearest player's head
     */
    static class HoverAbovePlayerGoal extends Goal {
        private final KingPhantomEntity phantom;
        private static final double HOVER_HEIGHT = 10.0;
        private static final double HOVER_RADIUS = 3.0; // Circle around the player
        private Vec3d targetPosition;
        
        public HoverAbovePlayerGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
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
            // Find nearest player every tick to keep tracking
            PlayerEntity targetPlayer = this.phantom.getEntityWorld().getClosestPlayer(
                this.phantom, 64.0
            );
            
            if (targetPlayer == null || !targetPlayer.isAlive()) {
                // No player nearby, just hover in place
                Vec3d currentPos = new Vec3d(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                this.targetPosition = currentPos;
                return;
            }
            
            // Calculate a position 10 blocks above the player
            // Add some horizontal offset so it circles like a vulture
            double angle = (this.phantom.age * 0.05) % (2 * Math.PI);
            double offsetX = Math.cos(angle) * HOVER_RADIUS;
            double offsetZ = Math.sin(angle) * HOVER_RADIUS;
            
            this.targetPosition = new Vec3d(
                targetPlayer.getX() + offsetX,
                targetPlayer.getY() + HOVER_HEIGHT,
                targetPlayer.getZ() + offsetZ
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
            
            // Look at the player
            this.phantom.getLookControl().lookAt(
                targetPlayer.getX(),
                targetPlayer.getEyeY(),
                targetPlayer.getZ()
            );
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
        private static final int SWOOP_DURATION = 40; // 2 seconds
        
        public PeriodicAttackGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            // Only start if attack cooldown is finished
            if (this.phantom.getAttackCooldown() > 0) return false;
            
            this.targetPlayer = this.phantom.getEntityWorld().getClosestPlayer(
                this.phantom, 64.0
            );
            return this.targetPlayer != null && this.targetPlayer.isAlive();
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
                
                // Try to damage player if close enough (increased range)
                double distance = this.phantom.squaredDistanceTo(this.targetPlayer);
                if (distance < 9.0) { // 3 blocks radius
                    if (this.phantom.getEntityWorld() instanceof ServerWorld serverWorld) {
                        // Use the vanilla tryAttack method like other mobs do
                        this.phantom.tryAttack(serverWorld, this.targetPlayer);
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
        }
    }
}

