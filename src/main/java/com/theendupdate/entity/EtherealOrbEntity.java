package com.theendupdate.entity;

import net.minecraft.entity.AnimationState;
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
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.Box;
import com.theendupdate.registry.ModEntities;

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
    private static final int BREED_COOLDOWN_TICKS = 3 * 60 * 20; // 3 minutes
    public final AnimationState rotateAnimationState = new AnimationState();
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    private boolean hasStartedMoving = false;
    private int breedCooldownTicks = 0;
    private int panicTicks = 0;
    private int bloodTicks = 0;

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
    }

    public boolean isCharged() {
        return this.dataTracker.get(CHARGED);
    }

    public void setCharged(boolean value) {
        this.dataTracker.set(CHARGED, value);
        if (!this.getWorld().isClient) {
            if (value) this.addCommandTag("theendupdate:charged");
            else this.removeCommandTag("theendupdate:charged");
        }
    }

	public boolean isStunted() {
		return this.dataTracker.get(STUNTED);
	}

	public void setStunted(boolean value) {
		this.dataTracker.set(STUNTED, value);
		if (!this.getWorld().isClient) {
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
        // Let vanilla collision resolution handle floor/wall/ceiling interactions with no manual nudging

        // Server-side subtle particle hint when charged (moderate frequency)
        if (!this.getWorld().isClient && this.isCharged()) {
            // ~1 particle per second on average per orb
            if (this.age % 10 == 0 && this.getRandom().nextFloat() < 0.5f) {
                double x = this.getX() + (this.getRandom().nextDouble() - 0.5) * 0.15;
                double y = this.getY() + 0.9;
                double z = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 0.15;
                if (this.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        // Panic & blood timers (server) and blood particle spew
        if (!this.getWorld().isClient) {
            if (this.panicTicks > 0) this.panicTicks--;
            if (this.bloodTicks > 0) {
                this.bloodTicks--;
                if (this.getWorld() instanceof ServerWorld sw) {
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
        if (!this.getWorld().isClient) {
            if (this.getCommandTags().contains("theendupdate:charged") && !this.isCharged()) {
                this.setCharged(true);
            }
            if (this.getCommandTags().contains("theendupdate:stunted") && !this.isStunted()) {
                this.setStunted(true);
            }
        }

        // No custom suffocation logic; rely on vanilla in-wall checks only
        if (!this.getWorld().isClient) {
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
                if (this.getWorld() instanceof ServerWorld sw) {
                    this.spawnBaby(sw);
                    // Start breeding cooldown after successful spawn
                    this.breedCooldownTicks = BREED_COOLDOWN_TICKS;
                }
            }
        }

        // While panicking, move erratically at higher speed to simulate fear
        if (!this.getWorld().isClient && this.isPanicking()) {
            Vec3d pos = this.getPos();
            double range = 4.0;
            double dx = (this.getRandom().nextDouble() * 2.0 - 1.0) * range;
            double dy = (this.getRandom().nextDouble() * 2.0 - 1.0) * (range * 0.6);
            double dz = (this.getRandom().nextDouble() * 2.0 - 1.0) * range;
            Vec3d target = new Vec3d(
                pos.x + dx,
                MathHelper.clamp(pos.y + dy, this.getWorld().getBottomY() + 5, this.getWorld().getBottomY() + 100),
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
            Vec3d delta = stepPos.subtract(this.getPos());
            Box test = this.getBoundingBox().offset(delta);
            if (this.getWorld().isSpaceEmpty(this, test)) {
                lastFree = stepPos;
            } else {
                break;
            }
        }
        return lastFree;
    }

    public ActionResult interactMob(PlayerEntity player, Hand hand) {
		// Shearing a baby orb to remove bulb and stunt growth
		ItemStack stack = player.getStackInHand(hand);
		// Using the Ethereal Orb spawn egg on an adult should spawn a baby (vanilla parity)
		if (stack != null && stack.isOf(com.theendupdate.registry.ModItems.ETHEREAL_ORB_SPAWN_EGG)) {
			if (!this.isBaby()) {
				if (!this.getWorld().isClient) {
					if (this.getWorld() instanceof ServerWorld sw) {
						this.spawnBaby(sw);
					}
					if (!player.getAbilities().creativeMode) {
						stack.decrement(1);
					}
				}
				return this.getWorld().isClient ? ActionResult.SUCCESS : ActionResult.CONSUME;
			}
			// If already a baby, do nothing special; let other handlers decide
			return ActionResult.PASS;
		}
        if (this.isBaby() && !this.isStunted() && stack != null && stack.isOf(Items.SHEARS)) {
			if (!this.getWorld().isClient) {
				this.setStunted(true);
				this.panicTicks = 40; // 2 seconds of panic
				this.bloodTicks = 20; // brief spew of dust
                this.setBulbPresent(false);
				// Drop ethereal bulb block item
				if (this.getWorld() instanceof ServerWorld sw) {
					this.dropStack(sw, new ItemStack(ModBlocks.ETHEREAL_BULB));
				}
				// Damage shears
				if (!player.getAbilities().creativeMode) {
					stack.damage(1, player, hand);
				}
				// Play lose bulb sound
				this.getWorld().playSound(null, this.getBlockPos(), com.theendupdate.registry.ModSounds.ETHEREAL_ORB_LOSES_BULB, SoundCategory.NEUTRAL, 1.0f, 1.0f);
			}
			return this.getWorld().isClient ? ActionResult.SUCCESS : ActionResult.CONSUME;
		}

		// Reattach bulb to a stunted baby using the ethereal bulb item
		if (this.isBaby() && this.isStunted() && !this.hasBulb() && stack != null && stack.isOf(ModBlocks.ETHEREAL_BULB.asItem())) {
			if (!this.getWorld().isClient) {
				if (!player.getAbilities().creativeMode) {
					stack.decrement(1);
				}
				this.setBulbPresent(true);
				// Resume normal behavior immediately upon reattachment
				this.setStunted(false);
				this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
				if (this.getWorld() instanceof ServerWorld sw) {
					sw.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9, this.getZ(), 8, 0.15, 0.15, 0.15, 0.0);
				}
			}
			return this.getWorld().isClient ? ActionResult.SUCCESS : ActionResult.CONSUME;
		}

		// Priority: brushing to harvest spectral debris (and remove glow)
        if (this.isCharged() && stack != null && stack.isOf(Items.BRUSH)) {
            if (!this.getWorld().isClient) {
                if (this.getWorld() instanceof ServerWorld sw) {
                    this.dropStack(sw, new ItemStack(ModItems.SPECTRAL_DEBRIS));
                }
                this.setCharged(false);
                // small server particle to indicate harvesting
                if (this.getWorld() instanceof ServerWorld sw2) {
                    sw2.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.9, this.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                }
                if (!player.getAbilities().creativeMode) {
                    // Simulate minor brush wear
                    stack.damage(1, player, hand);
                }
                // Play resin breaking-like sound
                this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.BLOCKS, 0.9f, 1.0f);
            }
            if (this.getWorld().isClient) return ActionResult.SUCCESS;
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

            if (!this.getWorld().isClient) {
                // Server: allow breeding in both survival and creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume) {
                    stack.decrement(1);
                }
                this.pendingBabySpawn = true;
                this.babySpawnAge = this.age + ROTATE_ANIMATION_TICKS;
                this.dataTracker.set(BREEDING, Boolean.TRUE);
                // Subtle resonate sound
                this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, 0.9f);
                return ActionResult.CONSUME;
            } else {
                // Client: play hand swing when orb is breed-ready and player holds the block
                boolean willConsume = this.dataTracker.get(BREED_READY) && stack.getCount() > 0;
                return willConsume ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }

        // Accelerate baby growth with voidstar nuggets (10% of remaining time)
        if (this.isBaby() && stack != null && stack.isOf(ModItems.VOIDSTAR_NUGGET)) {
            if (!this.getWorld().isClient) {
                // Server: allow effect in creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume || player.getAbilities().creativeMode) {
                    int remaining = -this.growingAgeTicks;
                    int reduce = Math.max(1, MathHelper.ceil(remaining * 0.10f));
                    this.growingAgeTicks = Math.min(0, this.growingAgeTicks + reduce);
                    if (survivalConsume) stack.decrement(1);
                    this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS, 0.8f, 1.2f);
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
            if (!this.getWorld().isClient) {
                // Server: allow effect in creative; consume only in survival
                boolean survivalConsume = !player.getAbilities().creativeMode && stack.getCount() > 0;
                if (survivalConsume || player.getAbilities().creativeMode) {
                    this.setCharged(true);
                    if (survivalConsume) stack.decrement(1);
                    // Play a single amethyst step-like sound
                    this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.BLOCKS, 0.8f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            } else {
                // Client: swing when not charged and player holds a nugget
                boolean willConsume = !this.isCharged() && stack.getCount() > 0;
                return willConsume ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }
        return ActionResult.PASS;
    }

    // No special drops on death; discourage killing (use empty loot table to avoid item drops)

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
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.15, 0.15, 0.15, 0.0);
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.6f, 1.3f);
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
        world.spawnEntity(baby);
        world.spawnParticles(ParticleTypes.END_ROD, safePos.x, safePos.y + 0.4, safePos.z, 10, 0.2, 0.2, 0.2, 0.0);
        this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
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

        FollowAdultGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!orb.isBaby() || orb.isPanicking()) return false;
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
				Vec3d pos = this.targetAdult.getPos();
				// Slightly faster than adults so babies can catch up
				double speed = 2.6;
				Vec3d from = orb.getPos();
				Vec3d desired = new Vec3d(pos.x, pos.y + 0.2, pos.z);
				Vec3d safeTarget = orb.clampTargetToFreeSpace(from, desired);
				orb.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
				orb.getLookControl().lookAt(this.targetAdult);
        }

        private EtherealOrbEntity findNearestAdult() {
            Box box = orb.getBoundingBox().expand(RANGE);
            List<EtherealOrbEntity> list = orb.getWorld().getEntitiesByClass(EtherealOrbEntity.class, box, e -> e != orb && !e.isBaby());
            if (list.isEmpty()) return null;
            return list.stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(orb))).orElse(null);
        }
    }

    class MaintainHomeGoal extends Goal {
        private static final int HOME_RADIUS = 8;
        private static final int SCAN_CHUNKS = 6;
        private static final int REPATH_TICKS = 10;
        private static final int HOME_MAX_DISTANCE = 128;

        private final EtherealOrbEntity orb;
        private BlockPos homeCrystalPos;
		private BlockPos lastHomeCrystalPos;
        private Vec3d intermediateWaypoint;
        private Vec3d lastPosition;
        private int stuckCounter;
        private int repathCooldown;
		// When horizontal circling is obstructed, allow a vertical orbit mode
		private boolean verticalOrbitMode;
		private boolean verticalOrbitLock;
		private int orbitModeCooldown;
		private int obstructionCounter;
		// 0 = orbit in X-Y plane (vary x,y; z near home), 1 = orbit in Y-Z plane (vary y,z; x near home)
		private int verticalPlaneAxis;

        MaintainHomeGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return !orb.getWorld().isClient && orb.getTarget() == null && !orb.isPanicking();
        }

        @Override
        public boolean shouldContinue() {
            return !orb.isPanicking(); // pause while panicking
        }

        @Override
        public void start() {
            this.lastPosition = orb.getPos();
            this.stuckCounter = 0;
            this.repathCooldown = 0;
			this.verticalOrbitMode = false;
			this.verticalOrbitLock = false;
			this.obstructionCounter = 0;
			this.orbitModeCooldown = 0;
			this.verticalPlaneAxis = orb.getRandom().nextBoolean() ? 0 : 1;
        }

        @Override
        public void tick() {
            if (repathCooldown > 0) repathCooldown--;
			if (orbitModeCooldown > 0) orbitModeCooldown--;

			// Validate or acquire home
            if (homeCrystalPos == null || !isTargetBlock(homeCrystalPos)) {
                BlockPos found = findNearbyCrystal();
                if (found == null) found = scanChunksForCrystal(SCAN_CHUNKS);
                homeCrystalPos = found; // may be null
            }
			// Configure orbit mode when home changes
			if (homeCrystalPos != null && (lastHomeCrystalPos == null || !homeCrystalPos.equals(lastHomeCrystalPos))) {
				configureOrbitModeForHome(homeCrystalPos);
				lastHomeCrystalPos = homeCrystalPos.toImmutable();
			}

            Vec3d desired;
            if (homeCrystalPos != null) {
				Vec3d home = Vec3d.ofCenter(homeCrystalPos);
                double dist = orb.getPos().distanceTo(home);
                // Detach if dragged too far from home
                if (dist > HOME_MAX_DISTANCE) {
                    homeCrystalPos = null;
                }
                if (homeCrystalPos != null && dist > HOME_RADIUS) {
                    // Fly back to within HOME_RADIUS of home
                    Vec3d dir = home.subtract(orb.getPos()).normalize();
                    Vec3d target = home.add(dir.multiply(-Math.max(1.5, 0.5)));
                    desired = target;
                } else {
					// Idle around home: choose an orbit waypoint with clearance preference
					if (repathCooldown == 0 || intermediateWaypoint == null || orb.getPos().squaredDistanceTo(intermediateWaypoint) < 1.0) {
						double angle = (orb.age % 360) * 0.0174533;
						// Keep same radius regardless of orbit mode
						double radius = 3.0 + (orb.getRandom().nextDouble() * 2.0);
						Vec3d curPos = orb.getPos();
						Vec3d candidate;
						if (verticalOrbitMode) {
							candidate = computeVerticalOrbit(home, radius, angle, verticalPlaneAxis);
							// If chosen axis is not clear, try the other vertical axis before falling back
							if (!isSegmentClear(curPos, candidate)) {
								int other = verticalPlaneAxis == 0 ? 1 : 0;
								Vec3d alt = computeVerticalOrbit(home, radius, angle, other);
								if (isSegmentClear(curPos, alt)) {
									verticalPlaneAxis = other;
									candidate = alt;
								} else {
									// If both vertical planes are blocked, stay in place radius but nudge forward slightly
									candidate = computeHorizontalOrbit(home, Math.max(2.5, radius - 1.0), angle);
								}
						} else {
							// Once vertical is enabled and locked, do not revert to horizontal circling
							// even if horizontal appears clear now.
							if (verticalOrbitLock) {
								// no-op; keep candidate as current vertical orbit
							}
						}
						} else {
							candidate = computeHorizontalOrbit(home, radius, angle);
							// If horizontal path is repeatedly obstructed, prefer switching to vertical when possible
							if (!isSegmentClear(curPos, candidate)) {
								obstructionCounter++;
								if (obstructionCounter >= 3 && orbitModeCooldown == 0) {
									Vec3d v0 = computeVerticalOrbit(home, radius, angle, 0);
									Vec3d v1 = computeVerticalOrbit(home, radius, angle, 1);
									if (isSegmentClear(curPos, v0) || isSegmentClear(curPos, v1)) {
										verticalOrbitMode = true;
										verticalPlaneAxis = isSegmentClear(curPos, v0) ? 0 : 1;
										verticalOrbitLock = true; // stick with vertical once switched due to obstruction
										orbitModeCooldown = 100;
										candidate = verticalPlaneAxis == 0 ? v0 : v1;
										obstructionCounter = 0;
									}
								}
							} else {
								obstructionCounter = 0;
							}
						}
						intermediateWaypoint = candidate;
						repathCooldown = REPATH_TICKS;
					}
					desired = intermediateWaypoint;
                }
            } else {
                // No home; wander and keep scanning
                if (repathCooldown == 0 || intermediateWaypoint == null || orb.getPos().squaredDistanceTo(intermediateWaypoint) < 1.0) {
                    Vec3d pos = orb.getPos();
                    double range = 6.0;
                    double dx = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;
                    double dy = (orb.getRandom().nextDouble() * 2.0 - 1.0) * (range * 0.6);
                    double dz = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;
                    intermediateWaypoint = new Vec3d(
                        pos.x + dx,
                        MathHelper.clamp(pos.y + dy, orb.getWorld().getBottomY() + 5, orb.getWorld().getBottomY() + 100),
                        pos.z + dz
                    );
                    repathCooldown = REPATH_TICKS;
                }
                desired = intermediateWaypoint;
            }

            // Movement + avoidance
            Vec3d cur = orb.getPos();
            double d2 = cur.squaredDistanceTo(desired);
            double speed = d2 > 64 ? 2.8 : 2.0;

            HitResult hit = orb.getWorld().raycast(new RaycastContext(
                cur, desired, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d dir = desired.subtract(cur);
                Vec3d horiz = new Vec3d(dir.x, 0.0, dir.z);
                Vec3d lateral;
                if (horiz.lengthSquared() < 1.0E-4) {
                    lateral = new Vec3d(1.0, 0.0, 0.0);
                } else {
                    lateral = new Vec3d(-horiz.z, 0.0, horiz.x).normalize();
                }
                if (orb.getRandom().nextBoolean()) lateral = lateral.multiply(-1.0);
                Vec3d forward = horiz.lengthSquared() > 1.0E-4 ? horiz.normalize().multiply(1.0) : Vec3d.ZERO;
                desired = cur.add(forward).add(lateral.multiply(2.0));
                // Avoid climbing into ceilings when rerouting
                desired = new Vec3d(desired.x, Math.min(desired.y, cur.y + 0.5), desired.z);
            }

			// Stuck detection
            if (lastPosition != null) {
                double move = cur.squaredDistanceTo(lastPosition);
                if (move < 0.01) {
                    stuckCounter++;
					if (stuckCounter > 10) {
						// If near home and horizontal circling is failing, try enabling vertical orbit
						if (homeCrystalPos != null && !verticalOrbitMode && orbitModeCooldown == 0) {
							Vec3d home = Vec3d.ofCenter(homeCrystalPos);
							double angle = (orb.age % 360) * 0.0174533;
							double radius = 3.5;
							Vec3d v0 = computeVerticalOrbit(home, radius, angle, 0);
							Vec3d v1 = computeVerticalOrbit(home, radius, angle, 1);
							if (isSegmentClear(cur, v0) || isSegmentClear(cur, v1)) {
								verticalOrbitMode = true;
								verticalPlaneAxis = isSegmentClear(cur, v0) ? 0 : 1;
								verticalOrbitLock = true;
								orbitModeCooldown = 120;
								intermediateWaypoint = verticalPlaneAxis == 0 ? v0 : v1;
							} else {
								// As a fallback, climb a bit to escape local obstruction
								desired = new Vec3d(desired.x, Math.max(desired.y + 4.0, cur.y + 6.0), desired.z);
							}
						} else {
							// Default behavior: climb upwards to free space
							desired = new Vec3d(desired.x, Math.max(desired.y + 4.0, cur.y + 6.0), desired.z);
						}
						stuckCounter = 0;
					}
                } else {
                    stuckCounter = 0;
                }
            }
            lastPosition = cur;

            Vec3d safeTarget = orb.clampTargetToFreeSpace(cur, desired);
            orb.getMoveControl().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
            orb.getLookControl().lookAt(desired.x, desired.y, desired.z);
        }

		private void configureOrbitModeForHome(BlockPos homePos) {
			Direction.Axis axis = determineDominantSpikeAxis(homePos);
			// If the crystal/spike is horizontal (X or Z), default to vertical circling and lock it
			if (axis == Direction.Axis.X) {
				verticalOrbitMode = true;
				verticalOrbitLock = true;
				verticalPlaneAxis = 1; // Y-Z plane to wrap around X-oriented spike
			} else if (axis == Direction.Axis.Z) {
				verticalOrbitMode = true;
				verticalOrbitLock = true;
				verticalPlaneAxis = 0; // X-Y plane to wrap around Z-oriented spike
			} else {
				// Likely vertical spike; allow normal behavior (start horizontal)
				verticalOrbitMode = false;
				verticalOrbitLock = false;
			}
		}

		private Direction.Axis determineDominantSpikeAxis(BlockPos center) {
			int lenX = countLine(center, Direction.EAST) + countLine(center, Direction.WEST);
			int lenY = countLine(center, Direction.UP) + countLine(center, Direction.DOWN);
			int lenZ = countLine(center, Direction.SOUTH) + countLine(center, Direction.NORTH);
			if (lenX >= lenY && lenX >= lenZ) return Direction.Axis.X;
			if (lenZ >= lenY && lenZ >= lenX) return Direction.Axis.Z;
			return Direction.Axis.Y;
		}

		private int countLine(BlockPos origin, Direction dir) {
			int max = 6; // small local scan
			int count = 0;
			BlockPos.Mutable p = origin.mutableCopy();
			for (int i = 1; i <= max; i++) {
				p.move(dir);
				if (isTargetBlock(p)) count++;
				else break;
			}
			return count;
		}

		private Vec3d computeHorizontalOrbit(Vec3d home, double radius, double angle) {
			double x = home.x + Math.cos(angle) * radius;
			double y = clampY(home.y + (orb.getRandom().nextDouble() * 1.5 - 0.75));
			double z = home.z + Math.sin(angle) * radius;
			return new Vec3d(x, y, z);
		}

		private Vec3d computeVerticalOrbit(Vec3d home, double radius, double angle, int axis) {
			// axis 0: X-Y plane (vary x,y; z ≈ constant), axis 1: Y-Z plane (vary y,z; x ≈ constant)
			if (axis == 0) {
				double x = home.x + Math.cos(angle) * radius;
				double y = clampY(home.y + Math.sin(angle) * radius);
				double z = home.z + (orb.getRandom().nextDouble() * 1.5 - 0.75);
				return new Vec3d(x, y, z);
			} else {
				double x = home.x + (orb.getRandom().nextDouble() * 1.5 - 0.75);
				double y = clampY(home.y + Math.sin(angle) * radius);
				double z = home.z + Math.cos(angle) * radius;
				return new Vec3d(x, y, z);
			}
		}

		private double clampY(double y) {
			int bottom = orb.getWorld().getBottomY() + 5;
			int top = orb.getWorld().getBottomY() + 100;
			return MathHelper.clamp(y, bottom, top);
		}

		private boolean isSegmentClear(Vec3d from, Vec3d to) {
			// Raycast for hard blockers first
			HitResult hit = orb.getWorld().raycast(new RaycastContext(
				from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb));
			if (hit.getType() == HitResult.Type.BLOCK) return false;
			// Step with the entity's bounding box to ensure there is enough clearance to move
			final int steps = 8;
			for (int i = 1; i <= steps; i++) {
				double t = i / (double) steps;
				Vec3d step = from.lerp(to, t);
				Vec3d delta = step.subtract(orb.getPos());
				Box test = orb.getBoundingBox().offset(delta);
				if (!orb.getWorld().isSpaceEmpty(orb, test)) return false;
			}
			return true;
		}

        private boolean isTargetBlock(BlockPos pos) {
            var state = orb.getWorld().getBlockState(pos);
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
                if (!orb.getWorld().isChunkLoaded(cx, cz)) continue;
                visited++;
                Chunk chunk = orb.getWorld().getChunk(cx, cz);
                int minX = chunk.getPos().getStartX();
                int minZ = chunk.getPos().getStartZ();
                int maxX = minX + 15;
                int maxZ = minZ + 15;
                int bottomY = orb.getWorld().getBottomY();
                int topY = bottomY + 128;
                for (int x = minX; x <= maxX; x += 2) {
                    for (int z = minZ; z <= maxZ; z += 2) {
                        for (int y = bottomY; y < topY; y += 2) {
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
