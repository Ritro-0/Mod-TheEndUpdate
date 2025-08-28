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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.particle.ParticleTypes;
// no NBT base overrides needed in 1.21 for simple tracked data persistence here
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.Box;
import com.theendupdate.registry.ModEntities;
import net.minecraft.nbt.NbtCompound;

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
    private static final int BREED_COOLDOWN_TICKS = 3 * 60 * 20; // 3 minutes
    public final AnimationState rotateAnimationState = new AnimationState();
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    private boolean hasStartedMoving = false;
    private int breedCooldownTicks = 0;

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

        // Sync charged from scoreboard tags on server (persists across saves)
        if (!this.getWorld().isClient) {
            if (this.getCommandTags().contains("theendupdate:charged") && !this.isCharged()) {
                this.setCharged(true);
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
                this.growingAgeTicks++;
                this.dataTracker.set(GROWING_AGE, this.growingAgeTicks);
                // Ensure client knows we are a baby
                if (!this.dataTracker.get(BABY)) this.dataTracker.set(BABY, Boolean.TRUE);
                if (this.growingAgeTicks == 0) {
                    this.onGrowUp();
                }
            }

            // Adjust base speed attributes for babies for a slight boost
            EntityAttributeInstance walk = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            EntityAttributeInstance fly = this.getAttributeInstance(EntityAttributes.FLYING_SPEED);
            if (walk != null && fly != null) {
                if (this.isBaby()) {
                    walk.setBaseValue(0.4 * 1.2); // +20%
                    fly.setBaseValue(0.7 * 1.2);  // +20%
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
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.theendupdate.registry.ModSounds.ETHEREAL_ORB_IDLE;
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
        // Priority: brushing to harvest spectral debris (and remove glow)
        ItemStack stack = player.getStackInHand(hand);
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
        if (baby == null) return;
        double ox = this.getX() + (this.getRandom().nextDouble() - 0.5) * 0.6;
        double oy = this.getY() + 0.5;
        double oz = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 0.6;
        baby.refreshPositionAndAngles(ox, oy, oz, this.getYaw(), this.getPitch());
        baby.setBabyTicks(-BABY_GROW_TICKS);
        baby.dataTracker.set(BABY, Boolean.TRUE);
        baby.dataTracker.set(GROWING_AGE, -BABY_GROW_TICKS);
        world.spawnEntity(baby);
        world.spawnParticles(ParticleTypes.END_ROD, ox, oy + 0.4, oz, 10, 0.2, 0.2, 0.2, 0.0);
        this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.0f);
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
        private static final int REPATH_COOLDOWN_TICKS = 10;

        private final EtherealOrbEntity orb;
        private EtherealOrbEntity targetAdult;
        private int repathCooldown;

        FollowAdultGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (!orb.isBaby()) return false;
            this.targetAdult = findNearestAdult();
            return this.targetAdult != null;
        }

        @Override
        public boolean shouldContinue() {
            return orb.isBaby() && this.targetAdult != null && this.targetAdult.isAlive() && orb.squaredDistanceTo(this.targetAdult) > (STOP_DISTANCE * STOP_DISTANCE);
        }

        @Override
        public void stop() {
            this.targetAdult = null;
        }

        @Override
        public void tick() {
            if (this.targetAdult == null) return;
            if (repathCooldown > 0) {
                repathCooldown--;
                return;
            }
            repathCooldown = REPATH_COOLDOWN_TICKS;
            Vec3d pos = this.targetAdult.getPos();
            // Move towards adult using flight control for natural pathing
            double speed = 2.2; // slower to reduce overshoot into blocks
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
        private Vec3d intermediateWaypoint;
        private Vec3d lastPosition;
        private int stuckCounter;
        private int repathCooldown;

        MaintainHomeGoal(EtherealOrbEntity orb) {
            this.orb = orb;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return !orb.getWorld().isClient && orb.getTarget() == null;
        }

        @Override
        public boolean shouldContinue() {
            return true; // runs continuously
        }

        @Override
        public void start() {
            this.lastPosition = orb.getPos();
            this.stuckCounter = 0;
            this.repathCooldown = 0;
        }

        @Override
        public void tick() {
            if (repathCooldown > 0) repathCooldown--;

            // Validate or acquire home
            if (homeCrystalPos == null || !isTargetBlock(homeCrystalPos)) {
                BlockPos found = findNearbyCrystal();
                if (found == null) found = scanChunksForCrystal(SCAN_CHUNKS);
                homeCrystalPos = found; // may be null
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
                    // Idle around home: pick a small offset ring target every few ticks
                    if (repathCooldown == 0 || intermediateWaypoint == null || orb.getPos().squaredDistanceTo(intermediateWaypoint) < 1.0) {
                        double angle = (orb.age % 360) * 0.0174533;
                        double radius = 3.0 + (orb.getRandom().nextDouble() * 2.0);
                        double x = home.x + Math.cos(angle) * radius;
                        double y = home.y + 0.5 + (orb.getRandom().nextDouble() * 1.5 - 0.75);
                        double z = home.z + Math.sin(angle) * radius;
                        intermediateWaypoint = new Vec3d(x, y, z);
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
                        desired = new Vec3d(desired.x, Math.max(desired.y + 4.0, cur.y + 6.0), desired.z);
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
}
