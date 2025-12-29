package com.theendupdate.entity;

import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModItems;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.Box;
import com.theendupdate.registry.ModEntities;
import org.jetbrains.annotations.Nullable;

/**
 * EtherealOrbEntity - A floating, glowing orb creature that inhabits The End
 * 
 * Key features:
 * - Floats through the air (extends PathAwareEntity with no gravity)
 * - Emits light particles
 * - Passive behavior, but flees when hurt
 * - Can pass through certain blocks
 */
public class EtherealOrbEntity extends PathAwareEntity implements Flutterer {
    private static final TrackedData<Boolean> CHARGED = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BREED_READY = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GROWING_AGE = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> BREEDING = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> STUNTED = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> BULB_PRESENT = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> TAMED = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> OWNER_TRACKED = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> AIR_SITTING = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final int BREED_COOLDOWN_TICKS = 3 * 60 * 20; // 3 minutes
    private static final double TAMED_ORBIT_RADIUS = 5.0;
    private static final double TAMED_ORBIT_HEIGHT_OFFSET = 0.5;
    private static final double TAMED_ORBIT_ENTER_DISTANCE = TAMED_ORBIT_RADIUS * 1.1;
    private static final double TAMED_ORBIT_EXIT_DISTANCE = TAMED_ORBIT_RADIUS * 1.35;
    private static final int TAMED_RUSH_DURATION = 50;
    private static final String TAMED_OWNER_TAG_PREFIX = "theendupdate:tamed_owner:";
    public final AnimationState rotateAnimationState = new AnimationState();
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    private boolean hasStartedMoving = false;
    private int breedCooldownTicks = 0;
    private int panicTicks = 0;
    private int bloodTicks = 0;
    private int tamedRushTicks = 0;
    private float tamedOrbitAngle = 0.0F;
    private boolean tamedOrbiting;
    @Nullable
    private Vec3d tamedOrbitAnchor;
    @Nullable
    private Vec3d airSitAnchor;
    
    // Tardigrade trapping system
    @Nullable
    private VoidTardigradeEntity trappedTardigrade;
    private int trapBoxTicks = 0;
    private int trapCooldownTicks = 0;
    private static final int TRAP_BOX_DURATION = 18 * 20; // 18 seconds (15-20 range, using 18)
    private static final int TRAP_COOLDOWN = 15 * 20; // 15 seconds
    private static final double TARDIGRADE_DETECTION_RANGE = 20.0;
    private static final double TRAP_BOX_SIZE = 1.2; // Smaller box size
    @Nullable
    private Vec3d trapBoxCenter;
    @Nullable
    private Vec3d castingPosition;

    // Baby/growth system
    private static final int BABY_GROW_TICKS = 24000; // 20 minutes like vanilla
    private int growingAgeTicks = 0; // negative when baby, counts up to 0
    // Baby speed is handled by adjusting base attribute values while baby

    // Rotate animation → delayed baby spawn
    private static final int ROTATE_ANIMATION_TICKS = 56; // ≈ 2.8s based on ANIMATION3 length (2.7916765f * 20)
    private boolean pendingBabySpawn = false;
    private int babySpawnAge = -1;

    public EtherealOrbEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 30, true); // Doubled from 15 to 30
        this.setNoGravity(true);
        this.setPersistent();
    }
    
    @Override
    public ItemStack getPickBlockStack() {
        // Return the spawn egg when middle-clicked in creative mode
        return new ItemStack(com.theendupdate.registry.ModItems.ETHEREAL_ORB_SPAWN_EGG);
    }
    
    @Override
    public boolean isInAir() {
        return !this.isOnGround() && !this.isTouchingWater();
    }
    
    // Be invulnerable to fall, but NOT to in-wall damage
    @Override
    public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
        if (source.isOf(DamageTypes.FALL)) return true;
        return super.isInvulnerableTo(world, source);
    }
    
    @Override
    public boolean handleFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }
    
    public static DefaultAttributeContainer.Builder createEtherealOrbAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 3.5)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.4) // Doubled from 0.2
            .add(EntityAttributes.FLYING_SPEED, 0.7) // Doubled from 0.35
            .add(EntityAttributes.FOLLOW_RANGE, 10.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.ATTACK_SPEED, 1.0)
            .add(EntityAttributes.ARMOR, 0.0)
            .add(EntityAttributes.ARMOR_TOUGHNESS, 0.0)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.0);
    }
    
    // Mapping-safe init for tracked data (1.21 uses builder-based init)
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CHARGED, Boolean.FALSE);
        builder.add(BABY, Boolean.FALSE);
        builder.add(BREED_READY, Boolean.FALSE);
        builder.add(GROWING_AGE, 0);
        builder.add(BREEDING, Boolean.FALSE);
        builder.add(STUNTED, Boolean.FALSE);
		builder.add(BULB_PRESENT, Boolean.TRUE);
        builder.add(TAMED, Boolean.FALSE);
        builder.add(OWNER_TRACKED, "");
        builder.add(AIR_SITTING, Boolean.FALSE);
    }

    public boolean isCharged() {
        return this.dataTracker.get(CHARGED);
    }

    public void setCharged(boolean value) {
        this.dataTracker.set(CHARGED, value);
        if (!this.getEntityWorld().isClient()) {
            if (value) this.addCommandTag("theendupdate:charged");
            else this.removeCommandTag("theendupdate:charged");
        }
    }

	public boolean isStunted() {
		return this.dataTracker.get(STUNTED);
	}

	public void setStunted(boolean value) {
		this.dataTracker.set(STUNTED, value);
		if (!this.getEntityWorld().isClient()) {
			if (value) this.addCommandTag("theendupdate:stunted");
			else this.removeCommandTag("theendupdate:stunted");
		}
	}

	public boolean hasBulb() {
		return this.dataTracker.get(BULB_PRESENT);
	}

	public void setBulbPresent(boolean value) {
		this.dataTracker.set(BULB_PRESENT, value);
	}
    
    public boolean isTamed() {
        return this.dataTracker.get(TAMED);
    }

    public void setTamed(boolean value) {
        this.dataTracker.set(TAMED, value);
        if (!this.getEntityWorld().isClient()) {
            if (value) {
                this.addCommandTag("theendupdate:tamed");
            } else {
                this.removeCommandTag("theendupdate:tamed");
                // Clear air sitting when untamed
                this.setAirSitting(false);
                this.airSitAnchor = null;
            }
        }
    }

    @Nullable
    public UUID getOwnerUuid() {
        String stored = this.dataTracker.get(OWNER_TRACKED);
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        String stored = ownerUuid == null ? "" : ownerUuid.toString();
        this.dataTracker.set(OWNER_TRACKED, stored);
        if (!this.getEntityWorld().isClient()) {
            this.clearOwnerTags();
            if (ownerUuid != null) {
                try {
                    this.addCommandTag(TAMED_OWNER_TAG_PREFIX + ownerUuid);
                } catch (Throwable ignored) {}
            }
        }
    }

    public boolean isAirSitting() {
        return this.dataTracker.get(AIR_SITTING);
    }

    public void setAirSitting(boolean value) {
        this.dataTracker.set(AIR_SITTING, value);
        if (!value) {
            this.airSitAnchor = null;
        }
    }

    private void clearOwnerTags() {
        if (this.getEntityWorld().isClient()) {
            return;
        }
        List<String> toRemove = new ArrayList<>();
        for (String tag : this.getCommandTags()) {
            if (tag.startsWith(TAMED_OWNER_TAG_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String removeTag : toRemove) {
            try {
                this.removeCommandTag(removeTag);
            } catch (Throwable ignored) {}
        }
    }

    private void syncOwnerFromTags() {
        if (this.getEntityWorld().isClient()) {
            return;
        }
        if (this.getOwnerUuid() != null) {
            return;
        }
        for (String tag : this.getCommandTags()) {
            if (!tag.startsWith(TAMED_OWNER_TAG_PREFIX)) {
                continue;
            }
            String uuidString = tag.substring(TAMED_OWNER_TAG_PREFIX.length());
            try {
                UUID parsed = UUID.fromString(uuidString);
                this.setOwnerUuid(parsed);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed tags from older saves or manual edits
            }
            break;
        }
    }

    @Nullable
    private PlayerEntity getOwnerPlayer() {
        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        return serverWorld.getPlayerByUuid(ownerUuid);
    }

    public void tameBy(PlayerEntity player) {
        if (player == null || this.getEntityWorld().isClient() || this.isTamed()) {
            return;
        }
        this.setOwnerUuid(player.getUuid());
        this.setTamed(true);
        this.tamedRushTicks = TAMED_RUSH_DURATION;
        this.tamedOrbitAngle = this.random.nextFloat() * MathHelper.TAU;
        this.tamedOrbiting = false;
        this.tamedOrbitAnchor = null;
        this.airSitAnchor = null;
        this.setAirSitting(false);
        this.panicTicks = 0;
        this.bloodTicks = 0;
        // Set initial cooldown when tamed - orb must wait before it can box
        this.trapCooldownTicks = TRAP_COOLDOWN;
        this.getNavigation().stop();
        Vec3d ownerHead = new Vec3d(player.getX(), player.getY() + player.getStandingEyeHeight(), player.getZ());
        Vec3d orbPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d toOwner = ownerHead.subtract(orbPos);
        if (toOwner.lengthSquared() > 1.0E-4) {
            this.setVelocity(toOwner.normalize().multiply(1.8));
        }
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            spawnTamingBurst(serverWorld);
            serverWorld.playSound(null, this.getBlockPos(), com.theendupdate.registry.ModSounds.ETHEREAL_ORB_TAMED, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private void spawnTamingBurst(ServerWorld world) {
        double x = this.getX();
        double y = this.getY() + 0.6;
        double z = this.getZ();
        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 60, 0.6, 0.6, 0.6, 0.02);
        world.spawnParticles(ParticleTypes.GLOW, x, y, z, 30, 0.5, 0.5, 0.5, 0.01);
    }
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FollowAdultGoal(this));
        this.goalSelector.add(2, new MaintainHomeGoal(this));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanSwim(false);
        return navigation;
    }
    
    @Override
    public void tick() {
        super.tick();
        this.updateAnimations();
        // Ensure we never accumulate fall distance
        this.fallDistance = 0.0F;
        
        // Server-side subtle particle hint when charged (moderate frequency)
        if (!this.getEntityWorld().isClient() && this.isCharged()) {
            // ~1 particle per second on average per orb
            if (this.age % 10 == 0 && this.getRandom().nextFloat() < 0.5f) {
                double x = this.getX() + (this.getRandom().nextDouble() - 0.5) * 0.15;
                double y = this.getY() + 0.9;
                double z = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 0.15;
                if (this.getEntityWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        // Panic & blood timers (server) and blood particle spew
        if (!this.getEntityWorld().isClient()) {
            if (this.panicTicks > 0) this.panicTicks--;
            if (this.bloodTicks > 0) {
                this.bloodTicks--;
                if (this.getEntityWorld() instanceof ServerWorld sw) {
                    double cx = this.getX();
                    double cy = this.getY() + 0.9;
                    double cz = this.getZ();
                    // Red block-dust spew using powered redstone wire for active-wire color
                    net.minecraft.block.BlockState wire = net.minecraft.block.Blocks.REDSTONE_WIRE.getDefaultState().with(net.minecraft.state.property.Properties.POWER, 15);
                    net.minecraft.particle.BlockStateParticleEffect redDust = new net.minecraft.particle.BlockStateParticleEffect(
                        ParticleTypes.BLOCK,
                        wire
                    );
                    sw.spawnParticles(redDust, cx, cy, cz, 14, 0.35, 0.35, 0.35, 0.20);
                }
            }
        }

        // Sync charged from scoreboard tags on server (persists across saves)
        if (!this.getEntityWorld().isClient()) {
            if (this.getCommandTags().contains("theendupdate:charged") && !this.isCharged()) {
                this.setCharged(true);
            }
            if (this.getCommandTags().contains("theendupdate:stunted") && !this.isStunted()) {
                this.setStunted(true);
            }
            if (this.getCommandTags().contains("theendupdate:tamed") && !this.isTamed()) {
                this.setTamed(true);
            }
            this.syncOwnerFromTags();

            if (this.isTamed()) {
                // Check if casting trap - if so, skip normal behavior (handled in serverTickTardigradeDefense)
                if (this.trappedTardigrade == null || this.trapBoxTicks <= 0) {
                    this.serverTickTamedBehavior();
                }
                this.serverTickTardigradeDefense();
            } else {
                this.tamedOrbitAnchor = null;
            }
        }
        
        // Particle rendering is handled server-side in serverTickTardigradeDefense

        // No custom suffocation logic; rely on vanilla in-wall checks only
        if (!this.getEntityWorld().isClient()) {
            // Restore growth counter from tracked data after reload
            int trackedAge = this.dataTracker.get(GROWING_AGE);
            if (this.growingAgeTicks == 0 && this.dataTracker.get(BABY) && trackedAge < 0) {
                this.growingAgeTicks = trackedAge;
            }
            if (this.breedCooldownTicks > 0) {
                this.breedCooldownTicks--;
            }
            // Synchronize breed eligibility to clients to avoid false-positive hand swings
            boolean eligible = !this.isBaby() && !this.pendingBabySpawn && this.breedCooldownTicks <= 0;
            if (this.dataTracker.get(BREED_READY) != eligible) {
                this.dataTracker.set(BREED_READY, eligible);
            }
            // Freeze movement during rotate animation for baby spawn
            if (this.isRotatingForSpawn()) {
                this.setVelocity(Vec3d.ZERO);
                this.getNavigation().stop();
            }

		// Growth ticking for babies and speed/base adjustments
		if (this.growingAgeTicks < 0) {
			if (!this.isStunted()) {
				this.growingAgeTicks++;
				this.dataTracker.set(GROWING_AGE, this.growingAgeTicks);
				// Ensure client knows we are a baby
				if (!this.dataTracker.get(BABY)) this.dataTracker.set(BABY, Boolean.TRUE);
				if (this.growingAgeTicks == 0) {
					this.onGrowUp();
				}
			} else {
				// Remain a baby forever when stunted
				if (!this.dataTracker.get(BABY)) this.dataTracker.set(BABY, Boolean.TRUE);
			}
		}

            // Adjust base speed attributes for babies for a slight boost
            EntityAttributeInstance walk = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            EntityAttributeInstance fly = this.getAttributeInstance(EntityAttributes.FLYING_SPEED);
			if (walk != null && fly != null) {
				if (this.isBaby()) {
					// Mirror vanilla baby animals: modest speed boost over adults
					walk.setBaseValue(0.4 * 1.25);
					fly.setBaseValue(0.7 * 1.25);
				} else {
					walk.setBaseValue(0.4);
					fly.setBaseValue(0.7);
				}
			}

            // Delayed baby spawn after rotate animation completes
            if (this.pendingBabySpawn && this.age >= this.babySpawnAge) {
                this.pendingBabySpawn = false;
                this.rotateAnimationState.stop();
                this.dataTracker.set(BREEDING, Boolean.FALSE);
                if (this.getEntityWorld() instanceof ServerWorld sw) {
                    this.spawnBaby(sw);
                    // Start breeding cooldown after successful spawn
                    this.breedCooldownTicks = BREED_COOLDOWN_TICKS;
                }
            }
        }

        // While panicking, move erratically at higher speed to simulate fear
        if (!this.getEntityWorld().isClient() && this.isPanicking() && !this.isTamed()) {
            Vec3d pos = new Vec3d(this.getX(), this.getY(), this.getZ());
            double range = 4.0;
            double dx = (this.getRandom().nextDouble() * 2.0 - 1.0) * range;
            // Bias upward when panicking to avoid ground-hugging
            double dy = Math.abs(this.getRandom().nextDouble() * 2.0 - 1.0) * (range * 0.6);
            double dz = (this.getRandom().nextDouble() * 2.0 - 1.0) * range;
            Vec3d target = new Vec3d(
                pos.x + dx,
                pos.y + dy,
                pos.z + dz
            );
            Vec3d safe = this.clampTargetToFreeSpace(pos, target);
            this.getMoveControl().moveTo(safe.x, safe.y, safe.z, 3.2);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isPanicking() ? null : com.theendupdate.registry.ModSounds.ETHEREAL_ORB_IDLE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.theendupdate.registry.ModSounds.ETHEREAL_ORB_DEATH;
    }

    private void updateAnimations() {
        // Ensure rotate animation runs while breeding flag is true (client-visible)
        if (this.dataTracker.get(BREEDING)) {
            this.rotateAnimationState.startIfNotRunning(this.age);
        } else {
            this.rotateAnimationState.stop();
        }
        if (this.isRotatingForSpawn()) {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.stop();
            hasStartedMoving = false;
            return;
        }
        if (isGoingForward()) {
            this.finishmovementAnimationState.stop();
            if (!hasStartedMoving) {
                hasStartedMoving = true;
                this.moveAnimationState.startIfNotRunning(this.age);
            }
        } else {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.startIfNotRunning(this.age);
            hasStartedMoving = false;
        }
    }

    private boolean isGoingForward() {
        return this.getVelocity().z > 0; 
    }

    // Clamp a desired point along the segment to the last free position to avoid commanding
    // movement into solid blocks. This does not teleport; it's only used for choosing waypoints.
    private Vec3d clampTargetToFreeSpace(Vec3d from, Vec3d to) {
        final int steps = 12;
        Vec3d lastFree = from;
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d stepPos = from.lerp(to, t);
            Vec3d delta = stepPos.subtract(new Vec3d(this.getX(), this.getY(), this.getZ()));
            Box test = this.getBoundingBox().offset(delta);
            if (this.getEntityWorld().isSpaceEmpty(this, test)) {
                lastFree = stepPos;
            } else {
                break;
            }
        }
        return lastFree;
    }

    private void serverTickTamedBehavior() {
        // Check for air sitting mode first (takes priority over following parent)
        if (this.isAirSitting()) {
            if (this.airSitAnchor == null) {
                // Initialize air sit anchor if not set
                this.airSitAnchor = new Vec3d(this.getX(), this.getY(), this.getZ());
                this.tamedOrbitAngle = this.random.nextFloat() * MathHelper.TAU;
            }
            // Orbit around the air sit anchor with a small 2-3 block radius
            Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
            this.getNavigation().stop();
            
            double airSitRadius = 2.5; // 2-3 block radius (using 2.5 as middle)
            this.tamedOrbitAngle = (this.tamedOrbitAngle + 0.1F) % MathHelper.TAU;
            double offsetX = Math.cos(this.tamedOrbitAngle) * airSitRadius;
            double offsetZ = Math.sin(this.tamedOrbitAngle) * airSitRadius;
            double desiredY = this.airSitAnchor.y;
            Vec3d desired = new Vec3d(this.airSitAnchor.x + offsetX, desiredY, this.airSitAnchor.z + offsetZ);
            Vec3d safeTarget = this.clampTargetToFreeSpace(currentPos, desired);
            this.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, 2.6);
            Vec3d velocity = this.getVelocity();
            double verticalDelta = safeTarget.y - this.getY();
            double desiredVerticalSpeed = MathHelper.clamp(verticalDelta * 0.25, -0.25, 0.25);
            Vec3d adjusted = new Vec3d(
                MathHelper.lerp(0.35, velocity.x, (safeTarget.x - this.getX()) * 0.4),
                MathHelper.lerp(0.25, velocity.y, desiredVerticalSpeed),
                MathHelper.lerp(0.35, velocity.z, (safeTarget.z - this.getZ()) * 0.4)
            );
            double speedCap = 0.7;
            if (adjusted.lengthSquared() > speedCap * speedCap) {
                adjusted = adjusted.normalize().multiply(speedCap);
            }
            this.setVelocity(adjusted.x, adjusted.y, adjusted.z);
            
            // Face the orbit center
            Vec3d direction = this.airSitAnchor.subtract(currentPos);
            if (direction.lengthSquared() > 1.0E-3) {
                float targetYaw = (float)(MathHelper.atan2(direction.z, direction.x) * (180.0F / Math.PI)) - 90.0F;
                float wrapped = MathHelper.wrapDegrees(targetYaw - this.getYaw());
                this.setYaw(this.getYaw() + MathHelper.clamp(wrapped, -8.0F, 8.0F));
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();
            }
            return; // Exit early when air sitting
        }
        
        // For tamed babies: FollowAdultGoal handles following parents (priority 1)
        // If no parent is available, the goal won't be active and we'll follow owner here
        // For tamed adults: follow owner normally
        
        PlayerEntity owner = this.getOwnerPlayer();
        Vec3d orbitCenter;
        
        if (owner == null || owner.isRemoved() || owner.getEntityWorld() != this.getEntityWorld()) {
            if (this.tamedOrbitAnchor == null) {
                this.tamedOrbitAnchor = new Vec3d(this.getX(), this.getY(), this.getZ());
                this.tamedOrbiting = true;
                this.tamedOrbitAngle = 0.0F;
            }
            orbitCenter = this.tamedOrbitAnchor;
        } else {
            this.tamedOrbitAnchor = null;
            orbitCenter = new Vec3d(owner.getX(), owner.getY() + owner.getStandingEyeHeight(), owner.getZ());
        }
        Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        this.getNavigation().stop();

        if (owner != null && !owner.isRemoved() && owner.getEntityWorld() == this.getEntityWorld()) {
            if (this.tamedRushTicks > 0) {
                this.tamedRushTicks--;
                Vec3d rushTarget = this.clampTargetToFreeSpace(currentPos, orbitCenter);
                this.getMoveControl().moveTo(rushTarget.x, rushTarget.y, rushTarget.z, 3.6);
                this.tamedOrbiting = false;
            } else {
                double distance = currentPos.distanceTo(orbitCenter);
                if (distance > 12.0 && !this.getEntityWorld().isClient()) {
                    Vec3d teleportTarget = this.clampTargetToFreeSpace(orbitCenter, orbitCenter.add(0.0, TAMED_ORBIT_HEIGHT_OFFSET, 0.0));
                    this.refreshPositionAndAngles(teleportTarget.x, teleportTarget.y, teleportTarget.z, this.getYaw(), this.getPitch());
                    this.setVelocity(Vec3d.ZERO);
                    this.tamedOrbiting = false;
                    return;
                }
                if (this.tamedOrbiting) {
                    if (distance > TAMED_ORBIT_EXIT_DISTANCE) {
                        this.tamedOrbiting = false;
                    }
                } else if (distance <= TAMED_ORBIT_ENTER_DISTANCE) {
                    this.tamedOrbiting = true;
                    this.tamedOrbitAngle = (float) MathHelper.atan2(currentPos.z - orbitCenter.z, currentPos.x - orbitCenter.x);
                }

                if (this.tamedOrbiting) {
                    this.tamedOrbitAngle = (this.tamedOrbitAngle + 0.1F) % MathHelper.TAU;
                    double offsetX = Math.cos(this.tamedOrbitAngle) * TAMED_ORBIT_RADIUS;
                    double offsetZ = Math.sin(this.tamedOrbitAngle) * TAMED_ORBIT_RADIUS;
                    double desiredY = orbitCenter.y + TAMED_ORBIT_HEIGHT_OFFSET;
                    Vec3d desired = new Vec3d(orbitCenter.x + offsetX, desiredY, orbitCenter.z + offsetZ);
                    Vec3d safeTarget = this.clampTargetToFreeSpace(currentPos, desired);
                    this.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, 2.6);
                    Vec3d velocity = this.getVelocity();
                    double verticalDelta = safeTarget.y - this.getY();
                    double desiredVerticalSpeed = MathHelper.clamp(verticalDelta * 0.25, -0.25, 0.25);
                    Vec3d adjusted = new Vec3d(
                        MathHelper.lerp(0.35, velocity.x, (safeTarget.x - this.getX()) * 0.4),
                        MathHelper.lerp(0.25, velocity.y, desiredVerticalSpeed),
                        MathHelper.lerp(0.35, velocity.z, (safeTarget.z - this.getZ()) * 0.4)
                    );
                    double speedCap = 0.7;
                    if (adjusted.lengthSquared() > speedCap * speedCap) {
                        adjusted = adjusted.normalize().multiply(speedCap);
                    }
                    this.setVelocity(adjusted.x, adjusted.y, adjusted.z);
                } else {
                    Vec3d followTarget = this.clampTargetToFreeSpace(currentPos, orbitCenter);
                    this.getMoveControl().moveTo(followTarget.x, followTarget.y, followTarget.z, 3.4);
                    Vec3d towardOwner = orbitCenter.subtract(currentPos);
                    Vec3d velocity = this.getVelocity();
                    Vec3d desiredVelocity = Vec3d.ZERO;
                    if (towardOwner.lengthSquared() > 1.0E-4) {
                        desiredVelocity = towardOwner.normalize().multiply(1.05);
                    }
                    Vec3d blended = new Vec3d(
                        MathHelper.lerp(0.55, velocity.x, desiredVelocity.x),
                        velocity.y,
                        MathHelper.lerp(0.55, velocity.z, desiredVelocity.z)
                    );
                    double verticalDelta = followTarget.y - this.getY();
                    double desiredVerticalSpeed = MathHelper.clamp(verticalDelta * 0.35, -0.3, 0.3);
                    blended = new Vec3d(blended.x, MathHelper.lerp(0.3, blended.y, desiredVerticalSpeed), blended.z);
                    double speedCap = 1.15;
                    if (blended.lengthSquared() > speedCap * speedCap) {
                        blended = blended.normalize().multiply(speedCap);
                    }
                    this.setVelocity(blended.x, blended.y, blended.z);
                    if (distance > 0.35) {
                        this.tamedOrbitAngle = (float) MathHelper.atan2(currentPos.z - orbitCenter.z, currentPos.x - orbitCenter.x);
                    }
                }
            }
        } else {
            this.tamedRushTicks = 0;
            this.tamedOrbiting = true;
            this.tamedOrbitAngle = (this.tamedOrbitAngle + 0.1F) % MathHelper.TAU;
            double offsetX = Math.cos(this.tamedOrbitAngle) * TAMED_ORBIT_RADIUS;
            double offsetZ = Math.sin(this.tamedOrbitAngle) * TAMED_ORBIT_RADIUS;
            double desiredY = orbitCenter.y + TAMED_ORBIT_HEIGHT_OFFSET;
            Vec3d desired = new Vec3d(orbitCenter.x + offsetX, desiredY, orbitCenter.z + offsetZ);
            Vec3d safeTarget = this.clampTargetToFreeSpace(currentPos, desired);
            this.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, 2.6);
            Vec3d velocity = this.getVelocity();
            double verticalDelta = safeTarget.y - this.getY();
            double desiredVerticalSpeed = MathHelper.clamp(verticalDelta * 0.25, -0.25, 0.25);
            Vec3d adjusted = new Vec3d(
                MathHelper.lerp(0.35, velocity.x, (safeTarget.x - this.getX()) * 0.4),
                MathHelper.lerp(0.25, velocity.y, desiredVerticalSpeed),
                MathHelper.lerp(0.35, velocity.z, (safeTarget.z - this.getZ()) * 0.4)
            );
            double speedCap = 0.7;
            if (adjusted.lengthSquared() > speedCap * speedCap) {
                adjusted = adjusted.normalize().multiply(speedCap);
            }
            this.setVelocity(adjusted.x, adjusted.y, adjusted.z);
        }

        Vec3d direction = orbitCenter.subtract(currentPos);
        if (direction.lengthSquared() > 1.0E-3) {
            float targetYaw = (float)(MathHelper.atan2(direction.z, direction.x) * (180.0F / Math.PI)) - 90.0F;
            float wrapped = MathHelper.wrapDegrees(targetYaw - this.getYaw());
            this.setYaw(this.getYaw() + MathHelper.clamp(wrapped, -8.0F, 8.0F));
            this.bodyYaw = this.getYaw();
            this.headYaw = this.getYaw();
        }
    }

    private void serverTickTardigradeDefense() {
        // Handle cooldown
        if (this.trapCooldownTicks > 0) {
            this.trapCooldownTicks--;
        }
        
        // If currently trapping, update trap state
        if (this.trappedTardigrade != null && this.trapBoxTicks > 0) {
            this.trapBoxTicks--;
            
            // Check if trapped Tardigrade is still valid
            if (this.trappedTardigrade.isRemoved() || !this.trappedTardigrade.isAlive() || 
                this.trappedTardigrade.getEntityWorld() != this.getEntityWorld()) {
                // Tardigrade died or despawned, end trap early
                this.endTrap();
                return;
            }
            
            // Check if Tardigrade has escaped the box
            if (this.trapBoxCenter != null) {
                Vec3d tardigradePos = new Vec3d(this.trappedTardigrade.getX(), this.trappedTardigrade.getY(), this.trappedTardigrade.getZ());
                Vec3d offset = tardigradePos.subtract(this.trapBoxCenter);
                
                // Check if Tardigrade is outside the box bounds
                if (Math.abs(offset.x) > TRAP_BOX_SIZE || Math.abs(offset.y) > TRAP_BOX_SIZE || Math.abs(offset.z) > TRAP_BOX_SIZE) {
                    // Tardigrade escaped! End trap and enter cooldown
                    this.endTrap();
                    return;
                }
                
                // Update trap box center to follow Tardigrade (but keep it constrained)
                // Only update if Tardigrade hasn't moved too far (prevent escape)
                double distance = tardigradePos.distanceTo(this.trapBoxCenter);
                if (distance < 1.0) {
                    // Allow slight movement but keep center relatively stable
                    this.trapBoxCenter = this.trapBoxCenter.lerp(tardigradePos, 0.05);
                }
            }
            
            // Keep orb in place while casting
            if (this.castingPosition != null) {
                this.setVelocity(Vec3d.ZERO);
                this.getNavigation().stop();
                Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
                if (currentPos.distanceTo(this.castingPosition) > 0.5) {
                    // Teleport back to casting position if drifted
                    this.refreshPositionAndAngles(this.castingPosition.x, this.castingPosition.y, this.castingPosition.z, this.getYaw(), this.getPitch());
                }
                
                // Face the direction of the beam toward the Tardigrade
                if (this.trapBoxCenter != null) {
                    Vec3d toTardigrade = this.trapBoxCenter.subtract(currentPos);
                    if (toTardigrade.lengthSquared() > 1.0E-3) {
                        float targetYaw = (float)(MathHelper.atan2(toTardigrade.z, toTardigrade.x) * (180.0F / Math.PI)) - 90.0F;
                        float wrapped = MathHelper.wrapDegrees(targetYaw - this.getYaw());
                        this.setYaw(this.getYaw() + MathHelper.clamp(wrapped, -10.0F, 10.0F));
                        this.bodyYaw = this.getYaw();
                        this.headYaw = this.getYaw();
                    }
                }
            }
            
            // Spawn particle beam from orb to box and box particles (every 3 ticks, reduced particles)
            if (this.getEntityWorld() instanceof ServerWorld sw && this.castingPosition != null && this.trapBoxCenter != null && this.age % 3 == 0) {
                this.spawnBeamParticles(sw, this.castingPosition, this.trapBoxCenter);
                this.spawnBoxParticles(sw, this.trapBoxCenter);
            }
            
            // End trap when duration expires
            if (this.trapBoxTicks <= 0) {
                this.endTrap();
            }
            return;
        }
        
        // If not trapping and cooldown expired, check for threats
        if (this.trapCooldownTicks <= 0 && this.trappedTardigrade == null) {
            VoidTardigradeEntity threat = this.findThreateningTardigrade();
            if (threat != null) {
                this.startTrap(threat);
            }
        }
    }
    
    @Nullable
    private VoidTardigradeEntity findThreateningTardigrade() {
        if (this.getEntityWorld().isClient()) {
            return null;
        }
        
        Box searchBox = this.getBoundingBox().expand(TARDIGRADE_DETECTION_RANGE);
        List<VoidTardigradeEntity> nearbyTardigrades = this.getEntityWorld().getEntitiesByClass(
            VoidTardigradeEntity.class,
            searchBox,
            tardigrade -> tardigrade != null && tardigrade.isAlive() && !tardigrade.isRemoved() &&
                         tardigrade.getEntityWorld() == this.getEntityWorld() &&
                         !tardigrade.isTrapped() // Don't target Tardigrades already being trapped
        );
        
        // Find Tardigrades that are actively tracking THIS orb to eat it
        // Only check if the Tardigrade has this orb as its chase target - nothing else
        for (VoidTardigradeEntity tardigrade : nearbyTardigrades) {
            double distSq = this.squaredDistanceTo(tardigrade);
            if (distSq > TARDIGRADE_DETECTION_RANGE * TARDIGRADE_DETECTION_RANGE) {
                continue;
            }
            
            // Only return this Tardigrade if it's actively tracking THIS orb as its target
            EtherealOrbEntity chaseTarget = tardigrade.getChasingOrb();
            if (chaseTarget == this) {
                return tardigrade;
            }
        }
        
        return null;
    }
    
    private void startTrap(VoidTardigradeEntity tardigrade) {
        this.trappedTardigrade = tardigrade;
        this.trapBoxTicks = TRAP_BOX_DURATION;
        this.trapBoxCenter = new Vec3d(tardigrade.getX(), tardigrade.getY(), tardigrade.getZ());
        this.castingPosition = new Vec3d(this.getX(), this.getY(), this.getZ());
        
        // Mark Tardigrade as trapped (prevents other orbs from also trapping it)
        tardigrade.setTrapped(true);
        
        // Face the direction of the beam immediately when starting
        Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d toTardigrade = this.trapBoxCenter.subtract(currentPos);
        if (toTardigrade.lengthSquared() > 1.0E-3) {
            float targetYaw = (float)(MathHelper.atan2(toTardigrade.z, toTardigrade.x) * (180.0F / Math.PI)) - 90.0F;
            this.setYaw(targetYaw);
            this.bodyYaw = targetYaw;
            this.headYaw = targetYaw;
        }
        
        // Play sound
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.NEUTRAL, 0.8f, 1.5f);
        }
    }
    
    private void endTrap() {
        if (this.trappedTardigrade != null) {
            // Release Tardigrade
            this.trappedTardigrade.setTrapped(false);
            this.trappedTardigrade = null;
        }
        this.trapBoxTicks = 0;
        this.trapBoxCenter = null;
        this.castingPosition = null;
        this.trapCooldownTicks = TRAP_COOLDOWN;
    }
    
    private void spawnBeamParticles(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d direction = to.subtract(from);
        double distance = direction.length();
        // Reduced particle count - fewer steps for the beam
        int steps = Math.max(3, Math.min(8, (int)(distance * 0.5)));
        
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3d pos = from.lerp(to, t);
            world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    private void spawnBoxParticles(ServerWorld world, Vec3d center) {
        double boxSize = TRAP_BOX_SIZE; // Use the constant for box size
        int particlesPerEdge = 5; // Reduced for performance
        
        // Bottom face edges (cage frame)
        for (int i = 0; i <= particlesPerEdge; i++) {
            double t = i / (double) particlesPerEdge;
            // X edges
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y - boxSize, center.z - boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y - boxSize, center.z + boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            // Z edges
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize, center.y - boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x + boxSize, center.y - boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Top face edges (cage frame)
        for (int i = 0; i <= particlesPerEdge; i++) {
            double t = i / (double) particlesPerEdge;
            // X edges
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y + boxSize, center.z - boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y + boxSize, center.z + boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            // Z edges
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize, center.y + boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x + boxSize, center.y + boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Vertical edges (cage frame)
        for (int i = 0; i <= particlesPerEdge; i++) {
            double t = i / (double) particlesPerEdge;
            double y = center.y - boxSize + t * boxSize * 2;
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize, y, center.z - boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x + boxSize, y, center.z - boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize, y, center.z + boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, center.x + boxSize, y, center.z + boxSize, 1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Add cage bars - vertical bars on each face
        int barsPerFace = 3; // Number of bars per face
        for (int bar = 1; bar < barsPerFace; bar++) {
            double barPos = -boxSize + (bar / (double) barsPerFace) * boxSize * 2;
            
            // Front and back faces (X direction bars)
            for (int i = 0; i <= particlesPerEdge; i++) {
                double t = i / (double) particlesPerEdge;
                double y = center.y - boxSize + t * boxSize * 2;
                world.spawnParticles(ParticleTypes.GLOW, center.x + barPos, y, center.z - boxSize, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.GLOW, center.x + barPos, y, center.z + boxSize, 1, 0.0, 0.0, 0.0, 0.0);
            }
            
            // Left and right faces (Z direction bars)
            for (int i = 0; i <= particlesPerEdge; i++) {
                double t = i / (double) particlesPerEdge;
                double y = center.y - boxSize + t * boxSize * 2;
                world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize, y, center.z + barPos, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.GLOW, center.x + boxSize, y, center.z + barPos, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        
        // Horizontal bars (top and bottom faces)
        for (int bar = 1; bar < barsPerFace; bar++) {
            double barPos = -boxSize + (bar / (double) barsPerFace) * boxSize * 2;
            
            // Top and bottom faces - horizontal bars
            for (int i = 0; i <= particlesPerEdge; i++) {
                double t = i / (double) particlesPerEdge;
                // X direction bars on top/bottom
                world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y - boxSize, center.z + barPos, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.GLOW, center.x - boxSize + t * boxSize * 2, center.y + boxSize, center.z + barPos, 1, 0.0, 0.0, 0.0, 0.0);
                // Z direction bars on top/bottom
                world.spawnParticles(ParticleTypes.GLOW, center.x + barPos, center.y - boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticles(ParticleTypes.GLOW, center.x + barPos, center.y + boxSize, center.z - boxSize + t * boxSize * 2, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
		// Shearing a baby orb to remove bulb and stunt growth
		ItemStack stack = player.getStackInHand(hand);
		// Using the Ethereal Orb spawn egg on an adult should spawn a baby (vanilla parity)
		if (stack != null && stack.isOf(com.theendupdate.registry.ModItems.ETHEREAL_ORB_SPAWN_EGG)) {
			if (!this.isBaby()) {
				if (!this.getEntityWorld().isClient()) {
					if (this.getEntityWorld() instanceof ServerWorld sw) {
						this.spawnBaby(sw);
					}
					if (!player.getAbilities().creativeMode) {
						stack.decrement(1);
					}
				}
				return this.getEntityWorld().isClient() ? ActionResult.SUCCESS : ActionResult.CONSUME;
			}
			// If already a baby, do nothing special; let other handlers decide
			return ActionResult.PASS;
		}
        if (this.isBaby() && !this.isStunted() && stack != null && stack.isOf(Items.SHEARS)) {
			if (!this.getEntityWorld().isClient()) {
				this.setStunted(true);
				this.panicTicks = 40; // 2 seconds of panic
				this.bloodTicks = 20; // brief spew of dust
                this.setBulbPresent(false);
				// Drop ethereal bulb block item
				if (this.getEntityWorld() instanceof ServerWorld sw) {
					this.dropStack(sw, new ItemStack(ModBlocks.ETHEREAL_BULB));
				}
				// Damage shears
				if (!player.getAbilities().creativeMode) {
					stack.damage(1, player, hand);
				}
				// Play lose bulb sound
				this.getEntityWorld().playSound(null, this.getBlockPos(), com.theendupdate.registry.ModSounds.ETHEREAL_ORB_LOSES_BULB, SoundCategory.NEUTRAL, 1.0f, 1.0f);
			}
			return this.getEntityWorld().isClient() ? ActionResult.SUCCESS : ActionResult.CONSUME;
		}

		// Reattach bulb to a stunted baby using the ethereal bulb item
		if (this.isBaby() && this.isStunted() && !this.hasBulb() && stack != null && stack.isOf(ModBlocks.ETHEREAL_BULB.asItem())) {
			if (!this.getEntityWorld().isClient()) {
				if (!player.getAbilities().creativeMode) {
					stack.decrement(1);
				}
				this.setBulbPresent(true);
				// Resume normal behavior immediately upon reattachment
				this.setStunted(false);
				this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
				if (this.getEntityWorld() instanceof ServerWorld sw) {
					sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9, this.getZ(), 8, 0.15, 0.15, 0.15, 0.0);
				}
			}
			return this.getEntityWorld().isClient() ? ActionResult.SUCCESS : ActionResult.CONSUME;
		}

		// Priority: brushing to harvest spectral debris (and remove glow)
        if (this.isCharged() && stack != null && stack.isOf(Items.BRUSH)) {
            if (!this.getEntityWorld().isClient()) {
                if (this.getEntityWorld() instanceof ServerWorld sw) {
                    this.dropStack(sw, new ItemStack(ModItems.SPECTRAL_DEBRIS));
                }
                this.setCharged(false);
                // small server particle to indicate harvesting
                if (this.getEntityWorld() instanceof ServerWorld sw2) {
                    sw2.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.9, this.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                }
                if (!player.getAbilities().creativeMode) {
                    // Simulate minor brush wear
                    stack.damage(1, player, hand);
                }
                // Play resin breaking-like sound
                this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.BLOCKS, 0.9f, 1.0f);
            }
            if (this.getEntityWorld().isClient()) return ActionResult.SUCCESS;
            return ActionResult.CONSUME;
        }
        return this.theendupdate$handleFeed(player, hand);
    }

	private ActionResult theendupdate$handleFeed(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
		// Refuse feeding only when missing bulb
		if (!this.hasBulb()) {
			return ActionResult.PASS;
		}
        // Feed voidstar block to initiate rotate animation and delayed baby spawn
        if (stack != null && stack.isOf(com.theendupdate.registry.ModBlocks.VOIDSTAR_BLOCK.asItem())) {
            // Must be adult, not already spawning, and not on cooldown
            boolean canBreed = !this.isBaby() && !this.pendingBabySpawn && this.breedCooldownTicks <= 0;
            if (!canBreed) return ActionResult.PASS;

            if (!this.getEntityWorld().isClient()) {
                // Server: allow breeding in both survival and creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume) {
                    stack.decrement(1);
                }
                this.pendingBabySpawn = true;
                this.babySpawnAge = this.age + ROTATE_ANIMATION_TICKS;
                this.dataTracker.set(BREEDING, Boolean.TRUE);
                // Subtle resonate sound
                this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, 0.9f);
                return ActionResult.CONSUME;
            } else {
                // Client: play hand swing when orb is breed-ready and player holds the block
                boolean willConsume = this.dataTracker.get(BREED_READY) && stack.getCount() > 0;
                return willConsume ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }

        // Accelerate baby growth with voidstar nuggets (10% of remaining time)
        if (this.isBaby() && stack != null && stack.isOf(ModItems.VOIDSTAR_NUGGET)) {
            if (!this.getEntityWorld().isClient()) {
                // Server: allow effect in creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume || player.getAbilities().creativeMode) {
                    int remaining = -this.growingAgeTicks;
                    int reduce = Math.max(1, MathHelper.ceil(remaining * 0.10f));
                    this.growingAgeTicks = Math.min(0, this.growingAgeTicks + reduce);
                    if (survivalConsume) stack.decrement(1);
                    this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS, 0.8f, 1.2f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            } else {
                // Client: swing when baby and player holds a nugget
                boolean willConsume = this.isBaby() && stack.getCount() > 0;
                return willConsume ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }

        if (!this.isCharged() && stack != null && stack.isOf(ModItems.VOIDSTAR_NUGGET)) {
            if (!this.getEntityWorld().isClient()) {
                // Server: allow effect in creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume || player.getAbilities().creativeMode) {
                    this.setCharged(true);
                    if (survivalConsume) stack.decrement(1);
                    // Play a single amethyst step-like sound
                    this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            } else {
                // Client: swing when not charged and player holds a nugget
                boolean willConsume = !this.isCharged() && stack.getCount() > 0;
                return willConsume ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }
        
        // Air sit toggle for tamed orbs (only when owner right-clicks with empty hand or non-consumable item)
        if (this.isTamed() && this.getOwnerUuid() != null && this.getOwnerUuid().equals(player.getUuid())) {
            if (!this.getEntityWorld().isClient()) {
                boolean wasSitting = this.isAirSitting();
                this.setAirSitting(!wasSitting);
                if (!wasSitting) {
                    // Entering air sit mode - set anchor at current position
                    this.airSitAnchor = new Vec3d(this.getX(), this.getY(), this.getZ());
                    this.tamedOrbitAngle = this.random.nextFloat() * MathHelper.TAU;
                    if (this.getEntityWorld() instanceof ServerWorld sw) {
                        sw.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_PARROT_IMITATE_SKELETON, SoundCategory.NEUTRAL, 0.5f, 1.2f);
                    }
                } else {
                    // Exiting air sit mode - resume following
                    this.airSitAnchor = null;
                    if (this.getEntityWorld() instanceof ServerWorld sw) {
                        sw.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_PARROT_IMITATE_SKELETON, SoundCategory.NEUTRAL, 0.5f, 0.8f);
                    }
                }
                return ActionResult.CONSUME;
            } else {
                return ActionResult.SUCCESS;
            }
        }
        
        return ActionResult.PASS;
    }

    // No special drops on death; discourage killing (use empty loot table to avoid item drops)

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        // Clean up trap when orb is removed - release any trapped Tardigrade
        if (!this.getEntityWorld().isClient() && this.trappedTardigrade != null) {
            this.endTrap();
        }
    }

    @Override
    public boolean isBaby() {
        return this.dataTracker.get(BABY);
    }

    // NBT persistence handled via command tag sync in tick/setter for compatibility with current mappings

    // Visual scale left default; baby speed differentiates behavior

    private void setBabyTicks(int ticks) {
        this.growingAgeTicks = ticks;
    }

    private void onGrowUp() {
        // Remove any baby modifiers; goal system will naturally keep running
        EntityAttributeInstance walk = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (walk != null) walk.setBaseValue(0.4);
        EntityAttributeInstance fly = this.getAttributeInstance(EntityAttributes.FLYING_SPEED);
        if (fly != null) fly.setBaseValue(0.7);
        this.dataTracker.set(BABY, Boolean.FALSE);
        // Small poof
        if (this.getEntityWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.15, 0.15, 0.15, 0.0);
            this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.6f, 1.3f);
        }
    }

    private void spawnBaby(ServerWorld world) {
        EtherealOrbEntity baby = new EtherealOrbEntity(ModEntities.ETHEREAL_ORB, world);
        
        // Find a safe spawn position to avoid suffocation
        Vec3d safePos = findSafeSpawnPosition(world);
        baby.refreshPositionAndAngles(safePos.x, safePos.y, safePos.z, this.getYaw(), this.getPitch());
        baby.setBabyTicks(-BABY_GROW_TICKS);
        baby.dataTracker.set(BABY, Boolean.TRUE);
        baby.dataTracker.set(GROWING_AGE, -BABY_GROW_TICKS);
        
        // If parent is tamed, baby inherits taming from parent
        if (this.isTamed() && this.getOwnerUuid() != null) {
            baby.setOwnerUuid(this.getOwnerUuid());
            baby.setTamed(true);
        }
        
        world.spawnEntity(baby);
        world.spawnParticles(ParticleTypes.END_ROD, safePos.x, safePos.y + 0.4, safePos.z, 10, 0.2, 0.2, 0.2, 0.0);
        this.getEntityWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
    }
    
    /**
     * Find a safe position to spawn a baby orb, ensuring it won't be inside blocks.
     * Tries positions near the parent, progressively searching farther if needed.
     */
    private Vec3d findSafeSpawnPosition(ServerWorld world) {
        // Baby dimensions when scaled (0.6 scale factor from renderer)
        float babyWidth = 0.4375f * 0.6f;
        float babyHeight = 0.6875f * 0.6f;
        
        // Try positions in expanding radius
        double[][] offsets = {
            // First try slightly above and to the side
            {0.0, 1.0, 0.0},
            {0.3, 1.0, 0.0},
            {-0.3, 1.0, 0.0},
            {0.0, 1.0, 0.3},
            {0.0, 1.0, -0.3},
            // Try farther positions
            {0.5, 1.2, 0.0},
            {-0.5, 1.2, 0.0},
            {0.0, 1.2, 0.5},
            {0.0, 1.2, -0.5},
            // Try positions at parent level
            {0.5, 0.0, 0.0},
            {-0.5, 0.0, 0.0},
            {0.0, 0.0, 0.5},
            {0.0, 0.0, -0.5},
            // Last resort: directly above parent
            {0.0, 2.0, 0.0},
            {0.0, 3.0, 0.0}
        };
        
        for (double[] offset : offsets) {
            double testX = this.getX() + offset[0];
            double testY = this.getY() + offset[1];
            double testZ = this.getZ() + offset[2];
            
            // Create a bounding box for the baby at this position
            Box testBox = new Box(
                testX - babyWidth / 2, testY, testZ - babyWidth / 2,
                testX + babyWidth / 2, testY + babyHeight, testZ + babyWidth / 2
            );
            
            // Check if this position is safe (no block collisions)
            if (world.isSpaceEmpty(testBox)) {
                return new Vec3d(testX, testY, testZ);
            }
        }
        
        // Fallback: spawn at parent position + upward offset (better than suffocating)
        return new Vec3d(this.getX(), this.getY() + 1.0, this.getZ());
    }

    private boolean isRotatingForSpawn() {
        return this.pendingBabySpawn && this.age < this.babySpawnAge;
    }

    // Persistence omitted for simplicity; DataTracker can be added later if needed

    /**
     * Maintain a persistent crystal "home"; if none found, wander and continuously scan.
     */
    /**
     * Babies prefer to follow nearest adult within range; loosely modeled after vanilla FollowParentGoal.
     */
    class FollowAdultGoal extends Goal {
        private static final double RANGE = 16.0;
        private static final double STOP_DISTANCE = 2.0;

        private final EtherealOrbEntity orb;
        private EtherealOrbEntity targetAdult;
        private int repathCooldown;
        private static final int HOME_RESCAN_INTERVAL = 30;
        private int homeRescanTicks;

        FollowAdultGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!orb.isBaby() || orb.isPanicking()) return false;
            // Allow tamed babies to follow parents too (they prioritize parents over owner)
            this.targetAdult = findNearestAdult();
            return this.targetAdult != null;
        }

        @Override
        public boolean shouldContinue() {
            return orb.isBaby() && !orb.isPanicking() && this.targetAdult != null && this.targetAdult.isAlive() && orb.squaredDistanceTo(this.targetAdult) > (STOP_DISTANCE * STOP_DISTANCE);
        }

        @Override
        public void stop() {
            this.targetAdult = null;
        }

        @Override
        public void tick() {
            if (this.targetAdult == null) return;
				// Recalculate frequently so babies are responsive like vanilla FollowParent
				repathCooldown = Math.max(0, repathCooldown - 1);
				Vec3d pos = new Vec3d(this.targetAdult.getX(), this.targetAdult.getY(), this.targetAdult.getZ());
				// Slightly faster than adults so babies can catch up
				double speed = 2.6;
				Vec3d from = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
				Vec3d desired = new Vec3d(pos.x, pos.y + 0.2, pos.z);
				Vec3d safeTarget = orb.clampTargetToFreeSpace(from, desired);
				orb.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
				orb.getLookControl().lookAt(this.targetAdult);
        }

        private EtherealOrbEntity findNearestAdult() {
            Box box = orb.getBoundingBox().expand(RANGE);
            List<EtherealOrbEntity> list = orb.getEntityWorld().getEntitiesByClass(EtherealOrbEntity.class, box, e -> e != orb && !e.isBaby());
            if (list.isEmpty()) return null;
            return list.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(orb))).orElse(null);
        }
    }

    class MaintainHomeGoal extends Goal {
        private static final int HOME_RADIUS = 8;
        private static final int SCAN_CHUNKS = 6;
        private static final int REPATH_TICKS = 10;
        private static final int HOME_MAX_DISTANCE = 128;
		
		// Orbit radius progression - from default down to minimum
		private static final double RADIUS_DEFAULT = 3.5;  // Normal comfortable orbit
		private static final double RADIUS_MEDIUM = 2.5;   // Tighter when default blocked
		private static final double RADIUS_SMALL = 2.0;    // Tighter
		private static final double RADIUS_MINIMUM = 1.5;  // Surrounding 8 blocks (one block away from home)
        private static final int HOME_RESCAN_INTERVAL = 30;

        private final EtherealOrbEntity orb;
        private BlockPos homeCrystalPos;
		private BlockPos lastHomeCrystalPos;
        private Vec3d intermediateWaypoint;
        private Vec3d lastPosition;
        private int stuckCounter;
        private int repathCooldown;
        private int homeRescanTicks;
		
		// Orbit mode: false = horizontal, true = vertical
		private boolean verticalOrbitMode;
		// Current orbit radius index (0=default, 1=medium, 2=small, 3=minimum)
		private int currentRadiusLevel;
		// Track failed radiuses for current mode
		private boolean[] blockedRadiuses;
		// Obstruction counter for current radius
		private int obstructionCounter;
		// Success counter to track if orbit is actually working
		private int successCounter;
		// Cooldown before trying to switch modes
		private int modeSwitchCooldown;
		// Cooldown before changing radius to prevent rapid cycling
		private int radiusChangeCooldown;
		// 0 = orbit in X-Y plane (vary x,y; z near home), 1 = orbit in Y-Z plane (vary y,z; x near home)
		private int verticalPlaneAxis;
		// Track consecutive mode switches to detect total blockage
		private int modeSwitchCount;
		// Bounce recovery state - disables navigation temporarily after radius change
		private int bounceRecoveryTicks;
		// Orbit direction: 1.0 for normal (stellarith), -1.0 for reversed (astral remnant)
		private double orbitDirection;

        MaintainHomeGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return !orb.getEntityWorld().isClient() && orb.getTarget() == null && !orb.isPanicking() && !orb.isTamed();
        }

        @Override
        public boolean shouldContinue() {
            return !orb.isPanicking() && !orb.isTamed(); // pause while panicking or when tamed
        }

        @Override
        public void start() {
            this.lastPosition = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
            this.stuckCounter = 0;
            this.repathCooldown = 0;
			this.verticalOrbitMode = false;
			this.currentRadiusLevel = 0;
			this.blockedRadiuses = new boolean[4];
			this.obstructionCounter = 0;
			this.successCounter = 0;
			this.modeSwitchCooldown = 0;
			this.radiusChangeCooldown = 0;
			this.verticalPlaneAxis = orb.getRandom().nextBoolean() ? 0 : 1;
			this.modeSwitchCount = 0;
			this.bounceRecoveryTicks = 0;
			this.orbitDirection = 1.0; // Default direction
        }

        private void ensureHomeReference() {
            if (homeCrystalPos != null && !isTargetBlock(homeCrystalPos)) {
                homeCrystalPos = null;
                homeRescanTicks = 0;
            }
            if (homeRescanTicks > 0 && homeCrystalPos != null) {
                homeRescanTicks--;
                return;
            }
            BlockPos found = findNearbyCrystal();
            if (found == null) {
                found = scanChunksForCrystal(SCAN_CHUNKS);
            }
            if (found != null) {
                if (homeCrystalPos == null || !found.equals(homeCrystalPos)) {
                    configureOrbitModeForHome(found);
                    lastHomeCrystalPos = found.toImmutable();
                }
                homeCrystalPos = found.toImmutable();
            } else {
                homeCrystalPos = null;
            }
            homeRescanTicks = HOME_RESCAN_INTERVAL;
        }

        @Override
        public void tick() {
            if (repathCooldown > 0) repathCooldown--;
			if (modeSwitchCooldown > 0) modeSwitchCooldown--;
			if (radiusChangeCooldown > 0) radiusChangeCooldown--;
			if (bounceRecoveryTicks > 0) bounceRecoveryTicks--;
			
			// During bounce recovery, skip waypoint recalculation and obstruction detection
			// but still navigate to the existing waypoint
			if (bounceRecoveryTicks > 0) {
				// Continue navigating to the waypoint
				Vec3d cur = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
				Vec3d desired = intermediateWaypoint != null ? intermediateWaypoint : Vec3d.ofCenter(homeCrystalPos);
				double d2 = cur.squaredDistanceTo(desired);
				double speed = d2 > 64 ? 2.8 : 2.0;
				Vec3d safeTarget = orb.clampTargetToFreeSpace(cur, desired);
				orb.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
				return; // Skip normal orbit logic during recovery
			}

			// Validate or acquire home
            ensureHomeReference();
			// Configure orbit mode when home changes
			if (homeCrystalPos != null) {
				if (lastHomeCrystalPos == null || 
					lastHomeCrystalPos.getX() != homeCrystalPos.getX() || 
					lastHomeCrystalPos.getY() != homeCrystalPos.getY() || 
					lastHomeCrystalPos.getZ() != homeCrystalPos.getZ()) {
					configureOrbitModeForHome(homeCrystalPos);
					lastHomeCrystalPos = homeCrystalPos.toImmutable();
				}
			}

            Vec3d desired;
            if (homeCrystalPos != null) {
				Vec3d home = Vec3d.ofCenter(homeCrystalPos);
                double dist = new Vec3d(orb.getX(), orb.getY(), orb.getZ()).distanceTo(home);
                // Detach if dragged too far from home
                if (dist > HOME_MAX_DISTANCE) {
                    homeCrystalPos = null;
                }
                if (homeCrystalPos != null && dist > HOME_RADIUS) {
                    // Fly back to within HOME_RADIUS of home
                    Vec3d dir = home.subtract(new Vec3d(orb.getX(), orb.getY(), orb.getZ())).normalize();
                    Vec3d target = home.add(dir.multiply(-Math.max(1.5, 0.5)));
                    desired = target;
                } else {
					// Idle around home: choose an orbit waypoint with progressive radius fallback
					// Skip waypoint recalculation during bounce recovery
					if (bounceRecoveryTicks == 0 && (repathCooldown == 0 || intermediateWaypoint == null || new Vec3d(orb.getX(), orb.getY(), orb.getZ()).squaredDistanceTo(intermediateWaypoint) < 1.0)) {
						Vec3d curPos = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
						Vec3d candidate = null;
						
						// Get current radius based on level
						double radius = getRadiusForLevel(currentRadiusLevel);
						
						// Calculate angle based on current position relative to home
						// This ensures the orb picks the "next" point along the circle from where it currently is
						Vec3d toOrb = curPos.subtract(home);
						double currentAngle;
						if (verticalOrbitMode) {
							if (verticalPlaneAxis == 0) {
								// X-Y plane: use atan2(y, x)
								currentAngle = Math.atan2(toOrb.y, toOrb.x);
							} else {
								// Y-Z plane: use atan2(y, z)
								currentAngle = Math.atan2(toOrb.y, toOrb.z);
							}
						} else {
							// Horizontal: use atan2(z, x)
							currentAngle = Math.atan2(toOrb.z, toOrb.x);
						}
						// Move forward along the circle (add ~30 degrees, or subtract if reversed)
						double angle = currentAngle + (0.5 * orbitDirection);
						
						// Try to compute orbit path for current mode and radius
						// Use square pattern for minimum radius (surrounding 8 blocks)
						if (currentRadiusLevel == 3) {
							// Minimum radius: use square orbit around home
							candidate = computeSquareOrbit(home, curPos, orbitDirection);
						} else if (verticalOrbitMode) {
							candidate = computeVerticalOrbit(home, radius, angle, verticalPlaneAxis);
							// Try alternate vertical axis if first is blocked
							if (!isSegmentClear(curPos, candidate)) {
								int otherAxis = verticalPlaneAxis == 0 ? 1 : 0;
								Vec3d alt = computeVerticalOrbit(home, radius, angle, otherAxis);
								if (isSegmentClear(curPos, alt)) {
									verticalPlaneAxis = otherAxis;
									candidate = alt;
								}
							}
								} else {
							candidate = computeHorizontalOrbit(home, radius, angle);
						}
						
						// Check if path is clear (ignoring home block itself)
						boolean pathClear = isSegmentClearExcludingHome(curPos, candidate, homeCrystalPos);
						
						if (!pathClear) {
							// Path is obstructed by blocks other than home
							obstructionCounter++;
							successCounter = 0;
							
							// Require sustained obstruction (10 consecutive failures) AND cooldown expired before changing radius
							if (obstructionCounter >= 10 && radiusChangeCooldown == 0) {
								blockedRadiuses[currentRadiusLevel] = true;
								obstructionCounter = 0;
								
								// Try next smaller radius
								if (tryNextRadius()) {
									// Successfully moved to next radius level
									radius = getRadiusForLevel(currentRadiusLevel);
									
									// Apply bounce when switching radius
									Vec3d toHome = home.subtract(curPos).normalize();
									
									if (verticalOrbitMode) {
										// Vertical orbit: apply horizontal bounce to get around obstacle
										Vec3d lateral = new Vec3d(-toHome.z, 0, toHome.x).normalize();
										if (orb.getRandom().nextBoolean()) lateral = lateral.multiply(-1.0);
										Vec3d bounceVelocity = new Vec3d(
											lateral.x * 0.6,
											0.0,
											lateral.z * 0.6
										);
										orb.setVelocity(bounceVelocity);
						} else {
										// Horizontal orbit: apply bounce perpendicular to home with upward component
										Vec3d lateral = new Vec3d(-toHome.z, 0, toHome.x).normalize();
										if (orb.getRandom().nextBoolean()) lateral = lateral.multiply(-1.0);
										Vec3d bounceVelocity = new Vec3d(
											lateral.x * 0.6,
											0.3, // Add upward component
											lateral.z * 0.6
										);
										orb.setVelocity(bounceVelocity);
									}
									
									// Stop current navigation
									orb.getNavigation().stop();
									
									// Skip ahead to next waypoint on the new orbit (well past the blocked area)
									Vec3d offsetFromHome = curPos.subtract(home);
									double orbAngle;
									if (verticalOrbitMode) {
										orbAngle = verticalPlaneAxis == 0 ? Math.atan2(offsetFromHome.y, offsetFromHome.x) : Math.atan2(offsetFromHome.y, offsetFromHome.z);
									} else {
										orbAngle = Math.atan2(offsetFromHome.z, offsetFromHome.x);
									}
									double skipAngle = orbAngle + Math.PI; // Skip ahead 180 degrees - opposite side of orbit
									
									if (currentRadiusLevel == 3) {
										intermediateWaypoint = computeSquareOrbit(home, curPos, orbitDirection);
									} else if (verticalOrbitMode) {
										intermediateWaypoint = computeVerticalOrbit(home, radius, skipAngle, verticalPlaneAxis);
									} else {
										intermediateWaypoint = computeHorizontalOrbit(home, radius, skipAngle);
									}
									
									// Enter bounce recovery state (3 seconds)
									bounceRecoveryTicks = 60;
									radiusChangeCooldown = 40;
								} else {
									// All radiuses blocked in current mode - switch modes
									if (modeSwitchCooldown == 0) {
										switchOrbitMode();
										radius = getRadiusForLevel(currentRadiusLevel);
										if (verticalOrbitMode) {
											candidate = computeVerticalOrbit(home, radius, angle, verticalPlaneAxis);
										} else {
											candidate = computeHorizontalOrbit(home, radius, angle);
										}
									}
									}
								}
							} else {
							// Path is clear - increment success counter
							successCounter++;
							// Reset obstruction counter after sustained success
							if (successCounter >= 5) {
								obstructionCounter = Math.max(0, obstructionCounter - 1);
								successCounter = 0;
							}
						}
						
						intermediateWaypoint = candidate;
						repathCooldown = REPATH_TICKS;
					}
					desired = intermediateWaypoint;
                }
            } else {
                // No home; wander and keep scanning
                if (repathCooldown == 0 || intermediateWaypoint == null || new Vec3d(orb.getX(), orb.getY(), orb.getZ()).squaredDistanceTo(intermediateWaypoint) < 1.0) {
                    Vec3d pos = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
                    double range = 6.0;
                    double dx = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;
                    
                    // Bias upward to prevent ground-hugging when homeless
                    // Use absolute value to always be positive, then occasionally allow downward movement
                    double dy;
                    double minFloatHeight = orb.getEntityWorld().getBottomY() + 8.0;
                    if (pos.y < minFloatHeight) {
                        // Below minimum height: always go up
                        dy = Math.abs(orb.getRandom().nextDouble()) * (range * 0.8);
                    } else if (pos.y < minFloatHeight + 10.0) {
                        // Near minimum: strong upward bias (80% chance to go up)
                        dy = (orb.getRandom().nextDouble() < 0.8 ? 1 : -1) * Math.abs(orb.getRandom().nextDouble()) * (range * 0.5);
                    } else {
                        // At good height: gentle wandering with slight upward bias (60% chance to go up)
                        dy = (orb.getRandom().nextDouble() < 0.6 ? 1 : -1) * Math.abs(orb.getRandom().nextDouble()) * (range * 0.4);
                    }
                    
                    double dz = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;
                    intermediateWaypoint = new Vec3d(
                        pos.x + dx,
                        pos.y + dy,
                        pos.z + dz
                    );
                    repathCooldown = REPATH_TICKS;
                }
                desired = intermediateWaypoint;
            }

            // Movement
            Vec3d cur = new Vec3d(orb.getX(), orb.getY(), orb.getZ());
            double d2 = cur.squaredDistanceTo(desired);
            double speed = d2 > 64 ? 2.8 : 2.0;

			// Track position for stuck detection (only used during recovery)
			if (bounceRecoveryTicks == 0) {
				lastPosition = cur;
			}

            Vec3d safeTarget = orb.clampTargetToFreeSpace(cur, desired);
            orb.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
            orb.getLookControl().lookAt(desired.x, desired.y, desired.z);
        }

		private void configureOrbitModeForHome(BlockPos homePos) {
			// Always start with horizontal orbit mode
			// Vertical will only be used if horizontal gets completely blocked
			verticalOrbitMode = false;
			verticalPlaneAxis = orb.getRandom().nextBoolean() ? 0 : 1;
			
			// Reset radius tracking when home changes
			currentRadiusLevel = 0;
			blockedRadiuses = new boolean[4];
			obstructionCounter = 0;
			successCounter = 0;
			radiusChangeCooldown = 0;
			bounceRecoveryTicks = 0;
			
			// Set orbit direction based on home block type
			var homeState = orb.getEntityWorld().getBlockState(homePos);
			if (homeState.isOf(ModBlocks.ASTRAL_REMNANT)) {
				orbitDirection = -1.0; // Reverse direction for astral remnant
			} else {
				orbitDirection = 1.0; // Normal direction for stellarith crystal
			}
		}
		
		/**
		 * Get the orbit radius for a given level (0=default, 1=medium, 2=small, 3=minimum)
		 * Progressively tries smaller radiuses when obstructed.
		 */
		private double getRadiusForLevel(int level) {
			switch(level) {
				case 0: return RADIUS_DEFAULT;
				case 1: return RADIUS_MEDIUM;
				case 2: return RADIUS_SMALL;
				case 3: return RADIUS_MINIMUM;
				default: return RADIUS_MINIMUM;
			}
		}
		
		/**
		 * Try to move to the next smaller radius level.
		 * Returns true if a smaller radius is available, false if all radiuses have been tried.
		 */
		private boolean tryNextRadius() {
			// Look for next available (non-blocked) radius
			for (int i = currentRadiusLevel + 1; i < 4; i++) {
				if (!blockedRadiuses[i]) {
					currentRadiusLevel = i;
					obstructionCounter = 0;
					successCounter = 0;
					return true;
				}
			}
			// All radiuses blocked or tried
			return false;
		}
		
		/**
		 * Switch between horizontal and vertical orbit modes.
		 * Resets blocked radius memory for the new mode and always starts at default radius.
		 */
		private void switchOrbitMode() {
			// Toggle mode
			verticalOrbitMode = !verticalOrbitMode;
			
			// Always reset to default radius (Level 0) when switching modes
			currentRadiusLevel = 0;
			blockedRadiuses = new boolean[4];
			obstructionCounter = 0;
			successCounter = 0;
			bounceRecoveryTicks = 0;
			
			// Set cooldown to prevent rapid mode switching
			modeSwitchCooldown = 100; // 5 seconds
			
			// Track mode switches
			modeSwitchCount++;
		}

		private Vec3d computeSquareOrbit(Vec3d home, Vec3d currentPos, double direction) {
			// Create a square orbit around the home block (surrounding 8 blocks)
			// Positions: directly adjacent blocks in X/Z, keeping Y at home level
			double dx = currentPos.x - home.x;
			double dz = currentPos.z - home.z;
			
			// Define the 8 positions around home in clockwise order
			// Each position is 1.0 block from home center (tighter to avoid corner clipping)
			Vec3d[] squarePositions = {
				new Vec3d(home.x + 1.0, home.y, home.z),      // East
				new Vec3d(home.x + 1.0, home.y, home.z + 1.0), // Southeast
				new Vec3d(home.x, home.y, home.z + 1.0),       // South
				new Vec3d(home.x - 1.0, home.y, home.z + 1.0), // Southwest
				new Vec3d(home.x - 1.0, home.y, home.z),       // West
				new Vec3d(home.x - 1.0, home.y, home.z - 1.0), // Northwest
				new Vec3d(home.x, home.y, home.z - 1.0),       // North
				new Vec3d(home.x + 1.0, home.y, home.z - 1.0)  // Northeast
			};
			
			// Find which corner we're closest to
			int closestIndex = 0;
			double closestDist = currentPos.squaredDistanceTo(squarePositions[0]);
			for (int i = 1; i < squarePositions.length; i++) {
				double dist = currentPos.squaredDistanceTo(squarePositions[i]);
				if (dist < closestDist) {
					closestDist = dist;
					closestIndex = i;
				}
			}
			
			// Move to the next corner in sequence (clockwise if direction > 0, counter-clockwise if direction < 0)
			int nextIndex;
			if (direction > 0) {
				nextIndex = (closestIndex + 1) % squarePositions.length;
			} else {
				nextIndex = (closestIndex - 1 + squarePositions.length) % squarePositions.length;
			}
			return squarePositions[nextIndex];
		}

		private Vec3d computeHorizontalOrbit(Vec3d home, double radius, double angle) {
			double x = home.x + Math.cos(angle) * radius;
			double y = home.y; // Keep at home height for horizontal orbit
			double z = home.z + Math.sin(angle) * radius;
			return new Vec3d(x, y, z);
		}

		private Vec3d computeVerticalOrbit(Vec3d home, double radius, double angle, int axis) {
		// axis 0: X-Y plane (vary x,y; z = constant), axis 1: Y-Z plane (vary y,z; x = constant)
			if (axis == 0) {
				double x = home.x + Math.cos(angle) * radius;
			double y = home.y + Math.sin(angle) * radius;
			double z = home.z; // Keep Z constant for X-Y plane orbit
				return new Vec3d(x, y, z);
			} else {
			double x = home.x; // Keep X constant for Y-Z plane orbit
			double y = home.y + Math.sin(angle) * radius;
				double z = home.z + Math.cos(angle) * radius;
				return new Vec3d(x, y, z);
			}
		}

		private boolean isSegmentClear(Vec3d from, Vec3d to) {
			// Raycast for hard blockers first
			HitResult hit = orb.getEntityWorld().raycast(new RaycastContext(
				from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb));
			if (hit.getType() == HitResult.Type.BLOCK) return false;
			// Step with the entity's bounding box to ensure there is enough clearance to move
			final int steps = 8;
			for (int i = 1; i <= steps; i++) {
				double t = i / (double) steps;
				Vec3d step = from.lerp(to, t);
				Vec3d delta = step.subtract(new Vec3d(orb.getX(), orb.getY(), orb.getZ()));
				Box test = orb.getBoundingBox().offset(delta);
				if (!orb.getEntityWorld().isSpaceEmpty(orb, test)) return false;
			}
			return true;
		}
		
		private boolean isSegmentClearExcludingHome(Vec3d from, Vec3d to, BlockPos homePos) {
			// Check path clearance but ignore collisions with the home crystal block
			// Step with the entity's bounding box to ensure there is enough clearance to move
			final int steps = 8;
			for (int i = 1; i <= steps; i++) {
				double t = i / (double) steps;
				Vec3d step = from.lerp(to, t);
				Vec3d delta = step.subtract(new Vec3d(orb.getX(), orb.getY(), orb.getZ()));
				Box test = orb.getBoundingBox().offset(delta);
				
				// Check if this box collides with any blocks
				int minX = MathHelper.floor(test.minX);
				int minY = MathHelper.floor(test.minY);
				int minZ = MathHelper.floor(test.minZ);
				int maxX = MathHelper.floor(test.maxX);
				int maxY = MathHelper.floor(test.maxY);
				int maxZ = MathHelper.floor(test.maxZ);
				
				BlockPos.Mutable mutable = new BlockPos.Mutable();
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						for (int z = minZ; z <= maxZ; z++) {
							mutable.set(x, y, z);
							// Skip the home block itself
							if (mutable.equals(homePos)) continue;
							
							// Check if this block would collide
							var state = orb.getEntityWorld().getBlockState(mutable);
							if (!state.isAir() && !state.getCollisionShape(orb.getEntityWorld(), mutable).isEmpty()) {
								return false; // Obstruction found (not home block)
							}
						}
					}
				}
			}
			return true;
		}

        private boolean isTargetBlock(BlockPos pos) {
            var state = orb.getEntityWorld().getBlockState(pos);
            return state.isOf(ModBlocks.ASTRAL_REMNANT) || state.isOf(ModBlocks.STELLARITH_CRYSTAL);
        }

        private BlockPos findNearbyCrystal() {
            BlockPos origin = orb.getBlockPos();
            int r = 32;
            for (BlockPos p : BlockPos.iterateOutwards(origin, r, r, r)) {
                if (isTargetBlock(p)) return p.toImmutable();
            }
            return null;
        }

        private BlockPos scanChunksForCrystal(int maxChunks) {
            ChunkPos center = orb.getChunkPos();
            int[][] offsets = new int[][] { {0,0}, {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {-1,-1}, {1,-1}, {-1,1} };
            int visited = 0;
            for (int i = 0; i < offsets.length && visited < maxChunks; i++) {
                int cx = center.x + offsets[i][0];
                int cz = center.z + offsets[i][1];
                if (!orb.getEntityWorld().isChunkLoaded(cx, cz)) continue;
                visited++;
                Chunk chunk = orb.getEntityWorld().getChunk(cx, cz);
                int minX = chunk.getPos().getStartX();
                int minZ = chunk.getPos().getStartZ();
                int maxX = minX + 15;
                int maxZ = minZ + 15;
                // Use dimension's actual build height limits (Overworld: -64 to 320, Nether: 0 to 256, End: 0 to 256)
                int bottomY = orb.getEntityWorld().getBottomY();
                int topY = bottomY + orb.getEntityWorld().getHeight();
                // Scan every Y level (not y+=2) to ensure we don't miss crystals at odd coordinates
                for (int x = minX; x <= maxX; x += 2) {
                    for (int z = minZ; z <= maxZ; z += 2) {
                        for (int y = bottomY; y < topY; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (isTargetBlock(p)) return p;
                        }
                    }
                }
            }
            return null;
        }
    }

    public boolean isPanicking() {
        return this.panicTicks > 0;
    }
}
