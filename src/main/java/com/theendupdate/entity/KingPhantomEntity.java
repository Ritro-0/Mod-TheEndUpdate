package com.theendupdate.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * A phantom boss 4x the size of a normal phantom, with a custom texture set and a
 * two-phase fight: hovers above players and alternates swoop dives, a ranged beam
 * that can be deflected by melee, and (phase 2 only) a summon-4-phantoms dive attack.
 */
public class KingPhantomEntity extends Phantom {
    
    private static final EntityDataAccessor<Integer> CURRENT_PHASE = SynchedEntityData.defineId(KingPhantomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_TRIGGERED_PHASE_TRANSITION = SynchedEntityData.defineId(KingPhantomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SWOOPING = SynchedEntityData.defineId(KingPhantomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SUMMONING = SynchedEntityData.defineId(KingPhantomEntity.class, EntityDataSerializers.BOOLEAN);
    
    public KingPhantomBossBarManager bossBarManager;
    
    private static final int PHASE_1 = 1;
    private static final int PHASE_2 = 2;
    
    private boolean isInPhaseTransition = false;
    private int phaseTransitionTicks = 0;
    private static final int PHASE_TRANSITION_DURATION = 100; // 5s
    private Vec3 phaseTransitionPosition = null;
    
    private static final int ATTACK_INTERVAL_PHASE_1 = 200; // 10s
    private static final int ATTACK_INTERVAL_PHASE_2 = 100; // 5s
    private int attackCooldown = ATTACK_INTERVAL_PHASE_1; // full cooldown so it hovers a bit before the first attack
    
    private int summonPhase = 0; // 0 = idle, 1 = descending, 2 = ascending
    private Player summonTarget = null;
    private Vec3 summonTargetPos = null;
    private Vec3 summonReturnPos = null;
    private boolean hasSummoned = false;
    
    private int rangedBeamTravelTicks = 0;
    private Vec3 rangedBeamStart;
    private Vec3 rangedBeamEnd;
    private double rangedBeamSpeedPerTick = 40.0 / 60.0; // ~0.666 blocks/tick
    private Vec3 rangedBeamCurrentPos; // head position, used for deflection checks
    private boolean rangedBeamDeflected = false;
    private Vec3 rangedBeamDeflectedDirection = null;
    
    public KingPhantomEntity(EntityType<? extends Phantom> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 35; 
        this.setPersistenceRequired(); // don't despawn on chunk unload; checkDespawn() still handles peaceful mode
        this.setNoGravity(true);
        
        // boss bar init happens lazily in tick(), same as Shadow Creaking
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURRENT_PHASE, PHASE_1);
        builder.define(HAS_TRIGGERED_PHASE_TRANSITION, Boolean.FALSE);
        builder.define(IS_SWOOPING, Boolean.FALSE);
        builder.define(IS_SUMMONING, Boolean.FALSE);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        
        // lower number = higher priority, attacks win over hovering
        this.goalSelector.addGoal(1, new PeriodicAttackGoal(this));
        this.goalSelector.addGoal(2, new HoverAbovePlayerGoal(this));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                Player target = KingPhantomEntity.this.level().getNearestPlayer(
                    KingPhantomEntity.this, 64.0
                );
                return target != null && !target.isCreative() && !target.isSpectator();
            }
        });
    }
    
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(com.theendupdate.registry.ModItems.KING_PHANTOM_SPAWN_EGG);
    }
    
    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        
        // no super call - this boss doesn't use vanilla despawn logic at all
    }
    
    @Override
    public boolean fireImmune() {
        return true;
    }
    
    @Override
    public boolean hurtServer(ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // if it's suffocating in a block, dig it out instead of just taking damage forever
        try {
            if (source != null && source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
                clearBlocksInHitbox(world);
            }
        } catch (Throwable ignored) {}
        return super.hurtServer(world, source, amount);
    }
    
    private void clearBlocksInHitbox(ServerLevel sw) {
        if (sw == null) return;
        AABB box = this.getBoundingBox();
        int minX = (int)Math.floor(box.minX);
        int minY = (int)Math.floor(box.minY);
        int minZ = (int)Math.floor(box.minZ);
        int maxX = (int)Math.ceil(box.maxX) - 1;
        int maxY = (int)Math.ceil(box.maxY) - 1;
        int maxZ = (int)Math.ceil(box.maxZ) - 1;
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    net.minecraft.world.level.block.state.BlockState state = sw.getBlockState(pos);
                    if (state.isAir()) continue;
                    // no drops, otherwise spawning inside terrain can dump a pile of items
                    sw.destroyBlock(pos, false, this);
                }
            }
        }
    }
    
    @Override
    public boolean isInvulnerable() {
        return super.isInvulnerable() || this.isInPhaseTransition;
    }
    
    @Override
    public void travel(Vec3 movementInput) {
        // vanilla phantom flight physics get in the way, so we take over movement entirely
        
        if (this.level().isClientSide()) {
            super.travel(movementInput);
            return;
        }
        
        Vec3 velocity = this.getDeltaMovement();
        
        if (!this.isSwooping() && !this.isSummoning()) {
            // clamp downward drift so it doesn't sink into the ground, especially right after load/reload
            if (velocity.y < -0.1) {
                velocity = new Vec3(velocity.x, -0.1, velocity.z);
            }
            
            if (this.onGround() || this.getY() - Math.floor(this.getY()) < 0.5) {
                if (velocity.y < 0) {
                    velocity = new Vec3(velocity.x, 0, velocity.z);
                }
            }
        }
        
        // skip friction during the summon ascent, otherwise it can get stuck fighting drag on the way up
        if (this.isSummoning() && this.summonPhase == 2) {
            velocity = new Vec3(velocity.x, 0.3, velocity.z);
        } else {
            velocity = velocity.multiply(0.91, 0.91, 0.91);
        }
        
        // vanilla move() for collision, but it can also touch our velocity, so re-set it after
        this.move(net.minecraft.world.entity.MoverType.SELF, velocity);
        this.setDeltaMovement(velocity);
    }
    
    @Override
    public void aiStep() {
        super.aiStep();
        // pitch is handled at the end of tick(), after all AI-goal velocity changes for the tick
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // must run after super.tick() and re-force velocity, or the ascent gets clobbered
        if (!this.level().isClientSide() && this.isSummoning()) {
            handleSummonAttack();
            if (this.summonPhase == 2) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.3, this.getDeltaMovement().z);
            }
        }
        
        // command-tag reads were dropped in the 26.1.2 migration; entity data drives phase state now
        
        // never let it land, except mid-swoop or mid-summon
        if (!this.isSwooping() && !this.isSummoning()) {
            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.5, 0));
            } else if (!this.level().isClientSide()) {
                net.minecraft.core.BlockPos posBelow = this.blockPosition().below();
                if (!this.level().getBlockState(posBelow).isAir()) {
                    double distanceToGround = this.getY() - posBelow.getY() - 1.0;
                    if (distanceToGround < 2.0) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.1, 0));
                    }
                }
            }
        }
        
        // KingPhantomBossBarRegistry owns ticking the boss bar, not this class
        if (!this.level().isClientSide()) {
            if (this.bossBarManager == null && this.tickCount <= 5) {
                this.initializeBossBar(); // fallback in case some spawn path skipped the usual init
            }
        }
        
        if (!this.level().isClientSide()) {
            if (this.entityData.get(CURRENT_PHASE) == PHASE_1 && !this.entityData.get(HAS_TRIGGERED_PHASE_TRANSITION)) {
                float healthPercent = this.getHealth() / this.getMaxHealth();
                if (healthPercent <= 0.5f) {
                    startPhaseTransition();
                }
            }
            
            if (this.isInPhaseTransition) {
                handlePhaseTransition();
            } else {
                if (this.rangedBeamTravelTicks > 0 && this.rangedBeamStart != null && this.rangedBeamEnd != null) {
                    advanceRangedBeam();
                }
                
                if (this.attackCooldown > 0) {
                    this.attackCooldown--;
                }
            }
            
            // pitch is set here, last, using direct assignment (not lerp) so it can't get overridden
            // and the client sees the change immediately
            if (this.isSwooping() || this.isSummoning()) {
                Vec3 velocity = this.getDeltaMovement();
                double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                
                if (horizSpeed > 1.0E-5D) {
                    float targetPitch = (float) (Math.atan2(velocity.y, horizSpeed) * 180.0D / Math.PI * -1.0D);
                    targetPitch = Mth.clamp(targetPitch, -45.0f, 45.0f);
                    this.setXRot(targetPitch);
                } else {
                    this.setXRot(0.0f);
                }
            } else if (!this.isInPhaseTransition) {
                Vec3 velocity = this.getDeltaMovement();
                double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                double vertSpeed = Math.abs(velocity.y);
                
                // keep tilting if it's still climbing/descending fast, e.g. right after a summon attack ends
                if (horizSpeed > 1.0E-5D && vertSpeed > 0.1) {
                    float targetPitch = (float) (Math.atan2(velocity.y, horizSpeed) * 180.0D / Math.PI * -1.0D);
                    targetPitch = Mth.clamp(targetPitch, -45.0f, 45.0f);
                    this.setXRot(targetPitch);
                } else {
                    this.setXRot(0.0f);
                }
            }
        }
    }
    
    private void startPhaseTransition() {
        this.entityData.set(HAS_TRIGGERED_PHASE_TRANSITION, true);
        this.isInPhaseTransition = true;
        this.phaseTransitionTicks = 0;
        this.phaseTransitionPosition = new Vec3(this.getX(), this.getY(), this.getZ());
        
        if (!this.level().isClientSide()) {
            this.addTag("theendupdate:phase_transition_triggered"); // so this survives a save/reload mid-transition
        }
        
        this.setDeltaMovement(Vec3.ZERO);
    }
    
    private void handlePhaseTransition() {
        if (!(this.level() instanceof ServerLevel sw)) return;
        
        this.phaseTransitionTicks++;
        
        this.setDeltaMovement(Vec3.ZERO);
        
        // re-snap every tick so it can't drift during the animation
        if (this.phaseTransitionPosition != null) {
            this.setPos(this.phaseTransitionPosition.x, this.phaseTransitionPosition.y, this.phaseTransitionPosition.z);
        }
        
        double radius = 3.0;
        int particlesPerTick = 10;
        
        for (int i = 0; i < particlesPerTick; i++) {
            // uniform random points on a sphere surface
            double theta = this.getRandom().nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * this.getRandom().nextDouble() - 1);
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            
            sw.sendParticles(
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                1, 0, 0, 0, 0.0
            );
            
            sw.sendParticles(
                ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                1, 0, 0, 0, 0.0
            );
        }
        
        if (this.phaseTransitionTicks >= PHASE_TRANSITION_DURATION) {
            for (int i = 0; i < 100; i++) {
                double theta = this.getRandom().nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * this.getRandom().nextDouble() - 1);
                
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.sin(phi) * Math.sin(theta);
                double z = radius * Math.cos(phi);
                
                double vx = x * 0.2;
                double vy = y * 0.2;
                double vz = z * 0.2;
                
                sw.sendParticles(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    this.getX() + x,
                    this.getY() + y,
                    this.getZ() + z,
                    0, vx, vy, vz, 1.0
                );
                
                sw.sendParticles(
                    ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getX() + x,
                    this.getY() + y,
                    this.getZ() + z,
                    0, vx, vy, vz, 1.0
                );
            }
            
            sw.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
            sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
            
            this.entityData.set(CURRENT_PHASE, PHASE_2);
            this.isInPhaseTransition = false;
            this.phaseTransitionPosition = null;
            
            this.addTag("theendupdate:phase_2");
            
            this.attackCooldown = ATTACK_INTERVAL_PHASE_2;
        }
    }
    
    public boolean isInPhaseTransition() {
        return this.isInPhaseTransition;
    }
    
    public int getCurrentPhase() {
        return this.entityData.get(CURRENT_PHASE);
    }
    
    public int getAttackInterval() {
        return this.entityData.get(CURRENT_PHASE) == PHASE_1 ? ATTACK_INTERVAL_PHASE_1 : ATTACK_INTERVAL_PHASE_2;
    }
    
    public void startRangedBeamAttack(Vec3 targetPos) {
        if (!(this.level() instanceof ServerLevel)) return;
        if (this.rangedBeamTravelTicks > 0) return; // already firing
        
        this.rangedBeamStart = new Vec3(this.getX(), this.getY(), this.getZ());
        this.rangedBeamEnd = targetPos;
        
        double distance = this.rangedBeamStart.distanceTo(this.rangedBeamEnd);
        this.rangedBeamTravelTicks = Math.max(1, (int)Math.ceil(distance / this.rangedBeamSpeedPerTick));
    }
    
    public void startSummonAttack(Player target) {
        if (target == null || !target.isAlive()) return;
        if (this.level().isClientSide()) return;
        if (target.isCreative() || target.isSpectator()) return;
        
        this.setSummoning(true);
        this.summonPhase = 1;
        this.summonTarget = target;
        this.hasSummoned = false;
        
        this.summonReturnPos = new Vec3(this.getX(), this.getY(), this.getZ());
        
        // predictive targeting: aim a bit ahead of the player's current velocity
        Vec3 playerPos = new Vec3(target.getX(), target.getY(), target.getZ());
        Vec3 playerVel = target.getDeltaMovement();
        
        this.summonTargetPos = playerPos.add(playerVel.scale(0.5));
        
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 50, 1.0, 1.0, 1.0, 0.1);
        }
    }
    
    private void handleSummonAttack() {
        if (!this.isSummoning() || this.summonPhase == 0) return;
        
        if (this.summonPhase == 1) {
            handleSummonDescent();
        } else if (this.summonPhase == 2) {
            handleSummonAscent();
        }
    }
    
    private void handleSummonDescent() {
        if (this.summonTarget == null || !this.summonTarget.isAlive() || this.summonTargetPos == null) {
            endSummonAttack();
            return;
        }
        
        if (this.summonTarget.isCreative() || this.summonTarget.isSpectator()) {
            endSummonAttack();
            return;
        }
        
        Vec3 currentPos = new Vec3(this.getX(), this.getY(), this.getZ());
        Vec3 direction = this.summonTargetPos.subtract(currentPos).normalize();
        
        double speed = 0.8; // fast dive
        Vec3 velocity = direction.scale(speed);
        this.setDeltaMovement(velocity);
        this.needsSync = true;
        
        double yaw = Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI) - 90.0;
        this.setYRot((float)yaw);
        this.yHeadRot = (float)yaw;
        this.yBodyRot = (float)yaw;
        
        double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizSpeed > 1.0E-5D) {
            float targetPitch = (float) (Math.atan2(velocity.y, horizSpeed) * 180.0D / Math.PI * -1.0D);
            targetPitch = Mth.clamp(targetPitch, -45.0f, 45.0f);
            this.setXRot(targetPitch);
        }
        
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
            sw.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            
            double distance = this.distanceToSqr(this.summonTarget);
            if (distance < 16.0) { // within 4 blocks
                sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
                sw.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
            }
            
            if (distance < 4.0 && !this.hasSummoned) { // within 2 blocks (distance is squared)
                summonPhantoms();
                this.hasSummoned = true;
                
                this.summonPhase = 2;
                return;
            }
        }
        
        if (currentPos.distanceTo(this.summonTargetPos) < 1.0 || this.onGround()) {
            endSummonAttack();
        }
    }
    
    private void summonPhantoms() {
        if (this.summonTarget == null || !this.summonTarget.isAlive()) return;
        if (!(this.level() instanceof ServerLevel serverWorld)) return;
        
        Vec3 playerPos = new Vec3(this.summonTarget.getX(), this.summonTarget.getY(), this.summonTarget.getZ());
        
        double radius = 5.0;
        double heightAbove = 8.0;
        
        for (int i = 0; i < 4; i++) {
            double angle = (i * Math.PI / 2.0); // 0, 90, 180, 270 degrees
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            
            double spawnX = playerPos.x + xOffset;
            double spawnY = playerPos.y + heightAbove;
            double spawnZ = playerPos.z + zOffset;
            
            Phantom phantom = new Phantom(net.minecraft.world.entity.EntityTypes.PHANTOM, serverWorld);
            phantom.snapTo(spawnX, spawnY, spawnZ, 0.0f, 0.0f);
            phantom.setTarget(this.summonTarget);
            serverWorld.addFreshEntity(phantom);
            
            serverWorld.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, 
                spawnX, spawnY, spawnZ, 
                20, 0.5, 0.5, 0.5, 0.1);
            serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK, 
                spawnX, spawnY, spawnZ, 
                15, 0.3, 0.3, 0.3, 0.05);
        }
        
        // damage-only explosion, block destruction disabled below
        serverWorld.explode(
            this,
            null, // no explicit damage source, use the default
            new net.minecraft.world.level.ExplosionDamageCalculator() {
                @Override
                public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion, 
                                               net.minecraft.world.level.BlockGetter world, 
                                               net.minecraft.core.BlockPos pos, 
                                               net.minecraft.world.level.block.state.BlockState state, 
                                               float power) {
                    return false;
                }
            },
            playerPos.x,
            playerPos.y,
            playerPos.z,
            3.0F, // slightly under TNT's 4.0
            false,
            net.minecraft.world.level.Level.ExplosionInteraction.MOB
        );
        
        serverWorld.sendParticles(ParticleTypes.CLOUD, 
            playerPos.x, playerPos.y, playerPos.z, 
            50, 1.0, 1.0, 1.0, 0.2);
        serverWorld.sendParticles(ParticleTypes.EXPLOSION, 
            playerPos.x, playerPos.y, playerPos.z, 
            10, 0.5, 0.5, 0.5, 0.0);
        serverWorld.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
            playerPos.x, playerPos.y, playerPos.z, 
            3, 0.0, 0.0, 0.0, 0.0);
        
        serverWorld.playSound(null, 
            playerPos.x, playerPos.y, playerPos.z,
            net.minecraft.sounds.SoundEvents.PHANTOM_AMBIENT, 
            net.minecraft.sounds.SoundSource.HOSTILE, 
            2.0F, 0.5F); // low pitch, more ominous
    }
    
    private void handleSummonAscent() {
        Vec3 currentPos = new Vec3(this.getX(), this.getY(), this.getZ());
        if (this.summonReturnPos == null) {
            endSummonAttack();
            return;
        }
        
        Vec3 direction = this.summonReturnPos.subtract(currentPos);
        double distanceToReturn = direction.length();
        
        if (distanceToReturn < 2.0) {
            endSummonAttack();
            return;
        }
        
        // normalize() shrinks the Y component to almost nothing when horizontal distance is large,
        // so vertical and horizontal speed are computed separately to guarantee it actually climbs
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double verticalDistance = Math.abs(direction.y);
        
        double horizontalSpeed = 0.3;
        double verticalSpeed = 0.4;
        
        double velocityX = 0;
        double velocityZ = 0;
        double velocityY = verticalSpeed;
        
        if (horizontalDistance > 1.0) {
            velocityX = (direction.x / horizontalDistance) * horizontalSpeed;
            velocityZ = (direction.z / horizontalDistance) * horizontalSpeed;
        }
        
        Vec3 velocity = new Vec3(velocityX, velocityY, velocityZ);
        this.setDeltaMovement(velocity);
        this.needsSync = true;
        
        double yaw = Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI) - 90.0;
        this.setYRot((float)yaw);
        this.yHeadRot = (float)yaw;
        this.yBodyRot = (float)yaw;
        
        double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizSpeed > 1.0E-5D) {
            float targetPitch = (float) (Math.atan2(velocity.y, horizSpeed) * 180.0D / Math.PI * -1.0D);
            targetPitch = Mth.clamp(targetPitch, -45.0f, 45.0f);
            this.setXRot(targetPitch);
        }
    }
    
    private void endSummonAttack() {
        this.setSummoning(false);
        this.summonPhase = 0;
        this.summonTarget = null;
        this.summonTargetPos = null;
        this.summonReturnPos = null;
        this.hasSummoned = false;
        
        this.attackCooldown = this.getAttackInterval();
    }
    
    public boolean isSwooping() {
        return this.getEntityData().get(IS_SWOOPING);
    }
    
    public void setSwooping(boolean swooping) {
        this.getEntityData().set(IS_SWOOPING, swooping);
    }
    
    public boolean isSummoning() {
        return this.getEntityData().get(IS_SUMMONING);
    }
    
    public void setSummoning(boolean summoning) {
        this.getEntityData().set(IS_SUMMONING, summoning);
    }
    
    /** Called from a mixin when a player attacks, to let melee swat the beam off course. */
    public boolean tryDeflectBeam(Player player) {
        if (this.rangedBeamTravelTicks <= 0 || this.rangedBeamCurrentPos == null) {
            return false; // no active beam
        }
        
        if (this.rangedBeamDeflected) {
            return false; // already deflected once
        }
        
        double reachDistance = 3.0; // typical player attack reach
        double distance = this.rangedBeamCurrentPos.distanceTo(player.getEyePosition());
        
        if (distance <= reachDistance) {
            Vec3 playerLook = player.getViewVector(1.0f).normalize();
            this.rangedBeamDeflected = true;
            this.rangedBeamDeflectedDirection = playerLook;
            
            this.rangedBeamStart = this.rangedBeamCurrentPos;
            
            // keep whatever travel time was left, just aim it at the new direction
            double remainingDistance = this.rangedBeamTravelTicks * this.rangedBeamSpeedPerTick;
            this.rangedBeamEnd = this.rangedBeamStart.add(playerLook.scale(remainingDistance));
            
            if (this.level() instanceof ServerLevel sw) {
                sw.playSound(null, this.rangedBeamCurrentPos.x, this.rangedBeamCurrentPos.y, this.rangedBeamCurrentPos.z,
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
            }
            
            return true;
        }
        
        return false;
    }
    
    private void advanceRangedBeam() {
        if (!(this.level() instanceof ServerLevel sw)) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        if (this.rangedBeamStart == null || this.rangedBeamEnd == null) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        
        double totalDistance = this.rangedBeamStart.distanceTo(this.rangedBeamEnd);
        if (totalDistance < 1.0E-6) {
            this.rangedBeamTravelTicks = 0;
            return;
        }
        
        Vec3 dir = this.rangedBeamEnd.subtract(this.rangedBeamStart).normalize();
        int ticksRemaining = this.rangedBeamTravelTicks;
        int ticksElapsed = Math.max(0, (int)Math.ceil(totalDistance / this.rangedBeamSpeedPerTick) - ticksRemaining);
        double headDistance = Math.min(totalDistance, ticksElapsed * this.rangedBeamSpeedPerTick);
        Vec3 head = this.rangedBeamStart.add(dir.scale(headDistance));
        
        this.rangedBeamCurrentPos = head;
        
        double nextHeadDistance = Math.min(totalDistance, (ticksElapsed + 1) * this.rangedBeamSpeedPerTick);
        Vec3 nextHead = this.rangedBeamStart.add(dir.scale(nextHeadDistance));
        net.minecraft.world.phys.HitResult blockHit = sw.clip(new net.minecraft.world.level.ClipContext(
            head,
            nextHead,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            this
        ));
        
        if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            Vec3 impactPos = blockHit.getLocation();
            int segmentPoints = 20;
            for (int i = 0; i < segmentPoints; i++) {
                double t = i / (double)segmentPoints;
                Vec3 p = head.lerp(impactPos, t);
                sw.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.0);
                sw.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            spawnBeamExplosion(impactPos.x, impactPos.y, impactPos.z);
            this.rangedBeamStart = null;
            this.rangedBeamEnd = null;
            this.rangedBeamTravelTicks = 0;
            this.rangedBeamCurrentPos = null;
            this.rangedBeamDeflected = false;
            this.rangedBeamDeflectedDirection = null;
            return;
        }
        
        int segmentPoints = 20;
        for (int i = 0; i < segmentPoints; i++) {
            double offset = (i / (double)segmentPoints) * this.rangedBeamSpeedPerTick;
            Vec3 p = head.add(dir.scale(offset));
            
            sw.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.0);
            sw.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            sw.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        
        this.rangedBeamTravelTicks--;
        if (this.rangedBeamTravelTicks <= 0) {
            spawnBeamExplosion(this.rangedBeamEnd.x, this.rangedBeamEnd.y, this.rangedBeamEnd.z);
            this.rangedBeamStart = null;
            this.rangedBeamEnd = null;
            this.rangedBeamCurrentPos = null;
            this.rangedBeamDeflected = false;
            this.rangedBeamDeflectedDirection = null;
        }
    }
    
    private void spawnBeamExplosion(double cx, double cy, double cz) {
        if (!(this.level() instanceof ServerLevel sw)) return;
        
        sw.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 3, 0.0, 0.0, 0.0, 0.0);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        sw.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 50, 0.5, 0.5, 0.5, 0.1);
        sw.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 30, 0.5, 0.5, 0.5, 0.05);
        
        float baseDamage = 10.0f;
        float damage = getDifficultyScaledDamage(baseDamage);
        double radius = 4.0;
        AABB box = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
        
        // players only, same as Shadow Creaking's AoE
        for (Player player : sw.getEntitiesOfClass(Player.class, box, 
                (pe) -> pe.isAlive() && !pe.isCreative() && !pe.isSpectator())) {
            Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
            double distance = playerPos.distanceTo(new Vec3(cx, cy, cz));
            if (distance <= radius) {
                // generic damage source tends to land more reliably for AoE than mobAttack
                boolean hit = player.hurtServer(sw, sw.damageSources().generic(), damage);
                if (!hit) {
                    hit = player.hurtServer(sw, sw.damageSources().mobAttack(this), damage);
                }
                
                if (hit) {
                    double dx = player.getX() - cx;
                    double dz = player.getZ() - cz;
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len > 1.0E-4) {
                        dx /= len;
                        dz /= len;
                        float kb = 1.0f;
                        player.knockback(kb, -dx, -dz, sw.damageSources().generic(), damage);
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
    
    public void initializeBossBar() {
        if (this.bossBarManager != null) return;
        
        try {
            this.bossBarManager = KingPhantomBossBarRegistry.createBossBar(this);
        } catch (Exception e) {
            // silent fail is intentional, a missing boss bar shouldn't break the fight
        }
    }
    
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        super.die(damageSource);
        
        if (!this.level().isClientSide()) {
            KingPhantomBossBarRegistry.removeBossBar(this.getUUID());
            
            if (this.level() instanceof ServerLevel serverWorld) {
                for (int i = 0; i < 12; i++) {
                    ItemStack essence = new ItemStack(com.theendupdate.registry.ModItems.KING_PHANTOM_ESSENCE);
                    this.spawnAtLocation(serverWorld, essence);
                }
            }
        }
    }
    
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        
        if (!this.level().isClientSide() && this.bossBarManager != null) {
            if (reason != net.minecraft.world.entity.Entity.RemovalReason.KILLED) {
                // peaceful mode, dimension change, etc: the registry notices it's gone and cleans up on its own
            }
        }
    }
    
    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel world, net.minecraft.world.entity.Entity target) {
        // bypasses Phantom's normal attack restrictions entirely
        if (target instanceof LivingEntity livingTarget) {
            if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return false;
            }
            
            float baseDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float damage = getDifficultyScaledDamage(baseDamage);
            
            boolean hit = livingTarget.hurtServer(world, world.damageSources().generic(), damage);
            
            if (!hit) {
                hit = livingTarget.hurtServer(world, world.damageSources().mobAttack(this), damage);
            }
            
            if (hit) {
                livingTarget.knockback(0.4,
                    this.getX() - livingTarget.getX(),
                    this.getZ() - livingTarget.getZ(),
                    world.damageSources().generic(),
                    damage);
                return true;
            }
        }
        return false;
    }
    
    /** Easy 50%, normal 100%, hard 150%. */
    private float getDifficultyScaledDamage(float baseDamage) {
        if (this.level() == null) return baseDamage;
        
        return switch (this.level().getDifficulty()) {
            case PEACEFUL -> 0.0f; // shouldn't be reachable, the mob despawns in peaceful anyway
            case EASY -> baseDamage * 0.5f;
            case NORMAL -> baseDamage;
            case HARD -> baseDamage * 1.5f;
        };
    }
    
    public static AttributeSupplier.Builder createKingPhantomAttributes() {
        // vanilla phantom is 20 HP / 6 damage / 0.4 follow range; this is 32x health, 2x damage
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 640.0)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.FLYING_SPEED, 0.4); // slower, more majestic
    }
    
    /** Hovers roughly 20 blocks above the nearest player's head, circling lazily. */
    static class HoverAbovePlayerGoal extends Goal {
        private final KingPhantomEntity phantom;
        private static final double HOVER_HEIGHT = 20.0;
        private static final double HOVER_RADIUS = 20.0;
        private Vec3 targetPosition;
        private Vec3 idleCircleCenter;
        // randomized per instance so multiple King Phantoms don't wobble in lockstep
        private final double phaseA;
        private final double phaseB;
        private final double phaseC;
        
        public HoverAbovePlayerGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.idleCircleCenter = null;
            this.phaseA = this.phantom.getRandom().nextDouble() * Math.PI * 2.0;
            this.phaseB = this.phantom.getRandom().nextDouble() * Math.PI * 2.0;
            this.phaseC = this.phantom.getRandom().nextDouble() * Math.PI * 2.0;
        }
        
        private double findGroundLevel(double x, double y, double z) {
            net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(x, y, z);
            
            for (int i = 0; i < 50; i++) {
                pos.setY((int)y - i);
                if (!this.phantom.level().getBlockState(pos).isAir()) {
                    return pos.getY() + 1.0;
                }
            }
            
            return y;
        }
        
        @Override
        public boolean canUse() {
            return true;
        }
        
        @Override
        public boolean canContinueToUse() {
            return true;
        }
        
        @Override
        public void tick() {
            if (this.phantom.isInPhaseTransition() || this.phantom.isSummoning()) {
                return;
            }
            
            // re-pick nearest player every tick (excluding creative/spectator)
            Player targetPlayer = null;
            double closestDistance = 64.0;
            
            for (Player player : this.phantom.level().players()) {
                if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
                double distance = this.phantom.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetPlayer = player;
                }
            }
            
            Vec3 circleCenter;
            
            if (targetPlayer == null) {
                if (this.idleCircleCenter == null) {
                    double groundY = findGroundLevel(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                    this.idleCircleCenter = new Vec3(
                        this.phantom.getX(), 
                        groundY + HOVER_HEIGHT, 
                        this.phantom.getZ()
                    );
                }
                circleCenter = this.idleCircleCenter;
            } else {
                // center above the player's eye height so it hovers over the head, not the feet
                circleCenter = new Vec3(
                    targetPlayer.getX(),
                    targetPlayer.getEyeY() + HOVER_HEIGHT,
                    targetPlayer.getZ()
                );
                // carry over so idle circling picks up near the player once they leave
                this.idleCircleCenter = circleCenter;
            }
            
            int t = this.phantom.tickCount;
            // ~0.05-0.06 gives a clearly visible circle at this radius
            double baseAngleSpeed = 0.05;
            double angleJitter = 0.003 * Math.sin(t * 0.015 + phaseA);
            double angle = (t * (baseAngleSpeed + angleJitter)) % (2 * Math.PI);

            // small wobble, kept close to HOVER_RADIUS so the circle still reads as circular
            double radiusJitter = 0.3 * Math.sin(t * 0.05 + phaseB) + 0.2 * Math.sin(t * 0.03 + phaseC);
            double radius = HOVER_RADIUS + radiusJitter;
            double minRadius = HOVER_RADIUS * 0.95;
            double maxRadius = HOVER_RADIUS * 1.05;
            if (radius < minRadius) radius = minRadius;
            if (radius > maxRadius) radius = maxRadius;

            double ellipseX = 1.0 + 0.01 * Math.sin(t * 0.02 + phaseA);
            double ellipseZ = 1.0 - 0.01 * Math.sin(t * 0.02 + phaseA);
            double wobbleX = 0.1 * Math.sin(t * 0.08 + phaseB);
            double wobbleZ = 0.1 * Math.cos(t * 0.06 + phaseC);

            // lead the target point ahead on the circle, otherwise it chases its own tail and spins in place
            double lead = 1.0;
            double leadAngle = angle + lead;
            double offsetX = Math.cos(leadAngle) * radius * ellipseX + wobbleX;
            double offsetZ = Math.sin(leadAngle) * radius * ellipseZ + wobbleZ;

            double bobY = 0.2 * Math.sin(t * 0.05 + phaseC);
            
            this.targetPosition = new Vec3(
                circleCenter.x + offsetX,
                circleCenter.y + bobY,
                circleCenter.z + offsetZ
            );
            
            Vec3 currentPos = new Vec3(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
            Vec3 direction = this.targetPosition.subtract(currentPos);
            double distance = direction.length();
            
            if (distance > 0.1) {
                direction = direction.normalize();
                
                // needs ~1.0-1.5 blocks/tick to actually keep pace with a 20-block circle
                double speed = Math.min(1.5, distance * 0.2);
                speed = Math.max(0.8, speed);
                
                Vec3 desiredVelocity = direction.scale(speed);
                Vec3 currentVelocity = this.phantom.getDeltaMovement();
                Vec3 blendedVelocity = currentVelocity.lerp(desiredVelocity, 0.5);
                if (blendedVelocity.lengthSqr() < 0.25) {
                    blendedVelocity = desiredVelocity.normalize().scale(0.8);
                }
                this.phantom.setDeltaMovement(blendedVelocity);
                
                double yaw = Math.atan2(blendedVelocity.z, blendedVelocity.x) * (180.0 / Math.PI) - 90.0;
                this.phantom.setYRot((float)yaw);
                this.phantom.yHeadRot = (float)yaw;
                this.phantom.yBodyRot = (float)yaw;
                
                this.phantom.setXRot(0.0f); // stays flat while hovering/circling, no tilt
            }
            
            if (targetPlayer != null) {
                this.phantom.getLookControl().setLookAt(
                    targetPlayer.getX(),
                    targetPlayer.getEyeY(),
                    targetPlayer.getZ()
                );
            } else {
                this.phantom.getLookControl().setLookAt(
                    circleCenter.x,
                    circleCenter.y,
                    circleCenter.z
                );
            }
        }
    }
    
    /** Picks between swoop, beam, and (phase 2 only) summon attacks on a cooldown. */
    static class PeriodicAttackGoal extends Goal {
        private final KingPhantomEntity phantom;
        private Player targetPlayer;
        private boolean isSwooping = false;
        private Vec3 swoopTarget;
        private double swoopTargetInitialY = Double.NaN;
        private int swoopTimer = 0;
        private boolean hasDamagedThisSwoop = false;
        private static final int SWOOP_DURATION = 40; // 2s
        private AttackType lastAttack = AttackType.NONE;
        private int phase2SequenceIndex = 0;
        
        private enum AttackType {
            NONE,
            SWOOP,
            BEAM,
            SUMMON
        }
        
        public PeriodicAttackGoal(KingPhantomEntity phantom) {
            this.phantom = phantom;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
        
        @Override
        public boolean canUse() {
            if (this.phantom.isInPhaseTransition() || this.phantom.isSummoning()) return false;
            if (this.phantom.getAttackCooldown() > 0) return false;
            
            Player closestPlayer = null;
            double closestDistance = 64.0;
            
            for (Player player : this.phantom.level().players()) {
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
        public boolean canContinueToUse() {
            // also true while summoning, so this goal doesn't get interrupted mid-summon
            return (this.isSwooping && this.swoopTimer > 0) || this.phantom.isSummoning();
        }
        
        @Override
        public void start() {
            if (this.targetPlayer == null) return;
            
            int currentPhase = this.phantom.getCurrentPhase();
            AttackType selectedAttack;

            if (currentPhase == PHASE_1) {
                // just alternate so it doesn't spam the same attack twice in a row
                selectedAttack = (lastAttack == AttackType.SWOOP) ? AttackType.BEAM : AttackType.SWOOP;
            } else {
                AttackType[] sequence = new AttackType[]{AttackType.SWOOP, AttackType.BEAM, AttackType.SUMMON};
                selectedAttack = sequence[phase2SequenceIndex % sequence.length];
                phase2SequenceIndex++;
                if (selectedAttack == lastAttack) {
                    selectedAttack = sequence[phase2SequenceIndex % sequence.length];
                    phase2SequenceIndex++;
                }
            }

            switch (selectedAttack) {
                case SWOOP -> {
                    this.isSwooping = true;
                    this.phantom.setSwooping(true);
                    this.swoopTimer = SWOOP_DURATION;
                    this.hasDamagedThisSwoop = false;
                    
                    // aim slightly ahead of the player to account for their movement
                    Vec3 playerVel = this.targetPlayer.getDeltaMovement();
                    Vec3 playerPos = new Vec3(this.targetPlayer.getX(), this.targetPlayer.getY(), this.targetPlayer.getZ());
                    this.swoopTarget = playerPos.add(playerVel.scale(0.5));
                    // remembered so the miss-detection check later has a Y baseline
                    this.swoopTargetInitialY = this.targetPlayer.getY();
                }
                case BEAM -> {
                    this.isSwooping = false;
                    this.phantom.setSwooping(false);
                    Vec3 targetPos = new Vec3(
                        this.targetPlayer.getX(),
                        this.targetPlayer.getY() + this.targetPlayer.getEyeHeight() * 0.5,
                        this.targetPlayer.getZ()
                    );
                    this.phantom.startRangedBeamAttack(targetPos);
                }
                case SUMMON -> {
                    this.isSwooping = false;
                    this.phantom.setSwooping(false);
                    this.phantom.startSummonAttack(this.targetPlayer);
                    lastAttack = AttackType.SUMMON;
                    return; // cooldown gets set when the summon attack actually finishes instead
                }
                case NONE -> {
                    // no-op
                }
            }
            lastAttack = selectedAttack;
            this.phantom.setAttackCooldown(this.phantom.getAttackInterval());
        }
        
        @Override
        public void tick() {
            if (this.phantom.isSummoning()) return;
            
            if (!this.isSwooping) return;
            
            this.swoopTimer--;
            
            if (this.swoopTarget != null && this.targetPlayer != null) {
                Vec3 currentPos = new Vec3(this.phantom.getX(), this.phantom.getY(), this.phantom.getZ());
                Vec3 direction = this.swoopTarget.subtract(currentPos).normalize();
                double speed = 1.2;
                
                Vec3 velocity = direction.scale(speed);
                this.phantom.setDeltaMovement(velocity);
                
                // once it passes the player's original height without landing a hit, count it as a miss
                if (!Double.isNaN(this.swoopTargetInitialY) && !this.hasDamagedThisSwoop) {
                    double currentY = this.phantom.getY();
                    if (currentY <= this.swoopTargetInitialY + 1.0) {
                        this.swoopTimer = 0;
                        this.isSwooping = false;
                        this.phantom.setSwooping(false);
                        return;
                    }
                }
                
                double yaw = Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI) - 90.0;
                this.phantom.setYRot((float)yaw);
                this.phantom.yHeadRot = (float)yaw;
                this.phantom.yBodyRot = (float)yaw;
                
                double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizSpeed > 1.0E-5D) {
                    float targetPitch = (float) (Math.atan2(velocity.y, horizSpeed) * 180.0D / Math.PI * -1.0D);
                    targetPitch = Mth.clamp(targetPitch, -45.0f, 45.0f);
                    this.phantom.setXRot(targetPitch);
                }
                
                if (!this.hasDamagedThisSwoop) {
                    double distance = this.phantom.distanceToSqr(this.targetPlayer);
                    if (distance < 9.0) { // 3 block radius
                        if (this.phantom.level() instanceof ServerLevel serverWorld) {
                            boolean didDamage = this.phantom.doHurtTarget(serverWorld, this.targetPlayer);
                            if (didDamage) {
                                this.hasDamagedThisSwoop = true;
                                this.swoopTimer = 0;
                                this.isSwooping = false;
                                this.phantom.setSwooping(false);
                            }
                        }
                    }
                }
            }
            
            if (this.swoopTimer <= 0) {
                this.isSwooping = false;
                this.phantom.setSwooping(false);
            }
        }
        
        @Override
        public void stop() {
            this.isSwooping = false;
            this.phantom.setSwooping(false);
            this.swoopTimer = 0;
            this.swoopTarget = null;
            this.swoopTargetInitialY = Double.NaN;
            this.hasDamagedThisSwoop = false;
            
            // otherwise it keeps coasting on the old swoop velocity after the goal ends
            this.phantom.setDeltaMovement(Vec3.ZERO);
        }
    }
}
