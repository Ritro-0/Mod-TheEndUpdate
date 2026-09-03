package com.theendupdate.entity;

import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModEntities;
import com.theendupdate.registry.ModItems;
import com.theendupdate.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class EtherealOrbEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BREED_READY = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GROWING_AGE = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BREEDING = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUNTED = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BULB_PRESENT = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER_TRACKED = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> AIR_SITTING = SynchedEntityData.defineId(EtherealOrbEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int BREED_COOLDOWN = 3 * 60 * 20;
    private static final int BABY_GROW_TICKS = 24000;
    private static final int ROTATE_ANIMATION_TICKS = 56;
    private static final int PANIC_TICKS = 40;
    private static final int TAME_RUSH_TICKS = 50;
    private static final int TRAP_DURATION = 18 * 20;
    private static final int TRAP_COOLDOWN = 15 * 20;
    private static final double OWNER_ORBIT_RADIUS = 4.5;
    private static final double OWNER_ORBIT_ENTER = OWNER_ORBIT_RADIUS * 1.1;
    private static final double OWNER_ORBIT_EXIT = OWNER_ORBIT_RADIUS * 1.35;
    private static final double TRAP_RANGE = 20.0;
    private static final double TRAP_BOX = 1.2;

    public final AnimationState rotateAnimationState = new AnimationState();
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();

    // breeding / growth
    private int breedCooldownTicks;
    private int growingAgeTicks;
    private boolean pendingBabySpawn;
    private int babySpawnAge = -1;

    // taming / owner
    private int rushTicks;
    private float ownerOrbitAngle;
    private boolean ownerOrbiting;
    @Nullable private Vec3 sitAnchor;
    @Nullable private Vec3 lostOwnerAnchor;

    // tardigrade trap
    private int trapTicks;
    private int trapCooldown;
    @Nullable private VoidTardigradeEntity trappedTardigrade;
    @Nullable private Vec3 trapCenter;
    @Nullable private Vec3 castPos;

    // misc
    private boolean moving;
    private int panicTicks;
    private int bloodTicks;

    public EtherealOrbEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createEtherealOrbAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 3.5)
            .add(Attributes.MOVEMENT_SPEED, 0.4)
            .add(Attributes.FLYING_SPEED, 0.7)
            .add(Attributes.FOLLOW_RANGE, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGED, false);
        builder.define(BABY, false);
        builder.define(BREED_READY, false);
        builder.define(GROWING_AGE, 0);
        builder.define(BREEDING, false);
        builder.define(STUNTED, false);
        builder.define(BULB_PRESENT, true);
        builder.define(TAMED, false);
        builder.define(OWNER_TRACKED, "");
        builder.define(AIR_SITTING, false);
    }

    public boolean isCharged() { return this.entityData.get(CHARGED); }
    public void setCharged(boolean value) { this.entityData.set(CHARGED, value); }
    public boolean isStunted() { return this.entityData.get(STUNTED); }
    public void setStunted(boolean value) { this.entityData.set(STUNTED, value); }
    public boolean hasBulb() { return this.entityData.get(BULB_PRESENT); }
    public void setBulbPresent(boolean value) { this.entityData.set(BULB_PRESENT, value); }
    @Override public boolean isBaby() { return this.entityData.get(BABY); }
    public boolean isTamed() { return this.entityData.get(TAMED); }
    public boolean isAirSitting() { return this.entityData.get(AIR_SITTING); }

    @Nullable
    public UUID getOwnerUuid() {
        String raw = this.entityData.get(OWNER_TRACKED);
        if (raw == null || raw.isEmpty()) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException e) { return null; }
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.entityData.set(OWNER_TRACKED, uuid == null ? "" : uuid.toString());
    }

    public void setAirSitting(boolean value) {
        this.entityData.set(AIR_SITTING, value);
        if (!value) this.sitAnchor = null;
    }

    public void tameBy(Player player) {
        if (player == null || this.level().isClientSide() || this.isTamed()) return;
        this.setOwnerUuid(player.getUUID());
        this.setTamed(true);
        this.setAirSitting(false);
        this.sitAnchor = null;
        this.lostOwnerAnchor = null;
        this.rushTicks = TAME_RUSH_TICKS;
        this.trapCooldown = TRAP_COOLDOWN;
        this.ownerOrbitAngle = this.random.nextFloat() * Mth.TWO_PI;
        this.ownerOrbiting = false;
        this.getNavigation().stop();
        Vec3 toOwner = player.position().add(0, player.getEyeHeight(), 0).subtract(this.position());
        if (toOwner.lengthSqr() > 1.0E-4) {
            this.setDeltaMovement(toOwner.normalize().scale(1.6));
        }
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.6, this.getZ(), 40, 0.5, 0.5, 0.5, 0.02);
            sw.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 0.6, this.getZ(), 20, 0.4, 0.4, 0.4, 0.01);
            sw.playSound(null, this.blockPosition(), ModSounds.ETHEREAL_ORB_TAMED, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    public void setTamed(boolean value) {
        this.entityData.set(TAMED, value);
        if (!value) {
            this.setAirSitting(false);
            this.sitAnchor = null;
        }
    }

    public boolean isTrapping() {
        return this.trappedTardigrade != null && this.trapTicks > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowAdultGoal());
        this.goalSelector.addGoal(2, new FollowOwnerGoal());
        this.goalSelector.addGoal(3, new OrbitHomeGoal());
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, world);
        nav.setCanFloat(false);
        return nav;
    }

    @Override
    protected boolean omnidirectionalAirMover() { return true; }

    @Override
    public ItemStack getPickResult() { return new ItemStack(ModItems.ETHEREAL_ORB_SPAWN_EGG); }

    @Override
    public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
        return source.is(DamageTypes.FALL) || super.isInvulnerableTo(world, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide()) this.endTrap();
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0F;
        this.updateAnimations();

        if (this.level().isClientSide()) return;

        if (this.panicTicks > 0) this.panicTicks--;
        if (this.bloodTicks > 0 && this.level() instanceof ServerLevel sw) {
            this.bloodTicks--;
            sw.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, this.getX(), this.getY() + 0.9, this.getZ(), 10, 0.3, 0.3, 0.3, 0.15);
        }

        if (this.isCharged() && this.tickCount % 20 == 0 && this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }

        if (this.isTamed()) {
            this.tickTrap();
        }

        int trackedAge = this.entityData.get(GROWING_AGE);
        if (this.growingAgeTicks == 0 && this.isBaby() && trackedAge < 0) {
            this.growingAgeTicks = trackedAge;
        }
        if (this.breedCooldownTicks > 0) this.breedCooldownTicks--;

        boolean breedReady = !this.isBaby() && !this.pendingBabySpawn && this.breedCooldownTicks <= 0;
        if (this.entityData.get(BREED_READY) != breedReady) {
            this.entityData.set(BREED_READY, breedReady);
        }

        if (this.isRotatingForSpawn() || this.isTrapping()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
        }

        if (this.growingAgeTicks < 0) { 
            if (!this.isStunted()) {
                this.growingAgeTicks++;  // Removing the bulb from a baby Orb stunts the growth, keeping it a baby and dropping an "Ethereal Bulb" item.
                this.entityData.set(GROWING_AGE, this.growingAgeTicks);
                this.entityData.set(BABY, true);
                if (this.growingAgeTicks == 0) this.onGrowUp();
            } else {
                this.entityData.set(BABY, true);
            }
        }

        AttributeInstance walk = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance fly = this.getAttribute(Attributes.FLYING_SPEED);
        if (walk != null && fly != null) {
            double scale = this.isBaby() ? 1.25 : 1.0;
            walk.setBaseValue(0.4 * scale);
            fly.setBaseValue(0.7 * scale);
        }

        if (this.pendingBabySpawn && this.tickCount >= this.babySpawnAge && this.level() instanceof ServerLevel sw) {
            this.pendingBabySpawn = false;
            this.rotateAnimationState.stop();
            this.entityData.set(BREEDING, false);
            this.spawnBaby(sw);
            this.breedCooldownTicks = BREED_COOLDOWN;
        }

        if (this.isPanicking() && !this.isTamed()) {
            Vec3 pos = this.position();
            Vec3 flee = pos.add((this.random.nextDouble() - 0.5) * 6.0, this.random.nextDouble() * 3.0, (this.random.nextDouble() - 0.5) * 6.0);
            Vec3 target = clampToFreeSpace(this, pos, flee);
            this.getMoveControl().setWantedPosition(target.x, target.y, target.z, 2.8);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isPanicking() ? null : ModSounds.ETHEREAL_ORB_IDLE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ETHEREAL_ORB_DEATH;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.isTamed() && player.getUUID().equals(this.getOwnerUuid()) && stack.isEmpty()) {
            if (!this.level().isClientSide()) {
                boolean sitting = this.isAirSitting();
                this.setAirSitting(!sitting);
                if (!sitting) {
                    this.sitAnchor = this.position();
                    this.ownerOrbitAngle = this.random.nextFloat() * Mth.TWO_PI;
                }
                float pitch = sitting ? 0.8F : 1.2F;
                this.level().playSound(null, this.blockPosition(), SoundEvents.PARROT_IMITATE_SKELETON, SoundSource.NEUTRAL, 0.5F, pitch);
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (stack.is(ModItems.ETHEREAL_ORB_SPAWN_EGG) && !this.isBaby()) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel sw) {
                this.spawnBaby(sw);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (this.isBaby() && !this.isStunted() && stack.is(Items.SHEARS)) {
            if (!this.level().isClientSide()) {
                this.setStunted(true);
                this.setBulbPresent(false);
                this.panicTicks = PANIC_TICKS;
                this.bloodTicks = 20;
                if (this.level() instanceof ServerLevel sw) this.spawnAtLocation(sw, new ItemStack(ModBlocks.ETHEREAL_BULB));
                if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand);
                this.level().playSound(null, this.blockPosition(), ModSounds.ETHEREAL_ORB_LOSES_BULB, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (this.isBaby() && this.isStunted() && !this.hasBulb() && stack.is(ModBlocks.ETHEREAL_BULB.asItem())) {   // Putting the bulb back on a baby Orb unstunts it, making it grow again.
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.setBulbPresent(true);
                this.setStunted(false);
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                if (this.level() instanceof ServerLevel sw) {
                    sw.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9, this.getZ(), 8, 0.15, 0.15, 0.15, 0.0);
                }
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (this.isCharged() && stack.is(Items.BRUSH)) {
            if (!this.level().isClientSide()) {
                if (this.level() instanceof ServerLevel sw) {
                    this.spawnAtLocation(sw, new ItemStack(ModItems.SPECTRAL_DEBRIS));
                    sw.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.9, this.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
                }
                this.setCharged(false);
                if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand);
                this.level().playSound(null, this.blockPosition(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.9F, 1.0F);
            }
            return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }

        if (!this.hasBulb()) return InteractionResult.PASS;

        if (stack.is(ModBlocks.VOIDSTAR_BLOCK.asItem())) {
            if (this.isBaby() || this.pendingBabySpawn || this.breedCooldownTicks > 0) return InteractionResult.PASS;
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.pendingBabySpawn = true;
                this.babySpawnAge = this.tickCount + ROTATE_ANIMATION_TICKS;
                this.entityData.set(BREEDING, true);
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.9F);
                return InteractionResult.CONSUME;
            }
            return this.entityData.get(BREED_READY) && stack.getCount() > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (this.isBaby() && stack.is(ModItems.VOIDSTAR_NUGGET)) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild && stack.isEmpty()) return InteractionResult.PASS;
                int remaining = -this.growingAgeTicks;
                this.growingAgeTicks = Math.min(0, this.growingAgeTicks + Math.max(1, Mth.ceil(remaining * 0.10F)));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.BLOCKS, 0.8F, 1.2F);
                return InteractionResult.CONSUME;
            }
            return stack.getCount() > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!this.isCharged() && stack.is(ModItems.VOIDSTAR_NUGGET)) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild && stack.isEmpty()) return InteractionResult.PASS;
                this.setCharged(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.CONSUME;
            }
            return stack.getCount() > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    private void updateAnimations() {
        if (this.entityData.get(BREEDING)) {
            this.rotateAnimationState.startIfStopped(this.tickCount);
        } else {
            this.rotateAnimationState.stop();
        }
        if (this.isRotatingForSpawn() || this.isTrapping()) {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.stop();
            this.moving = false;
            return;
        }
        boolean going = this.getDeltaMovement().lengthSqr() > 0.001;
        if (going) {
            this.finishmovementAnimationState.stop();
            if (!this.moving) {
                this.moving = true;
                this.moveAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.startIfStopped(this.tickCount);
            this.moving = false;
        }
    }

    private boolean isRotatingForSpawn() {
        return this.pendingBabySpawn && this.tickCount < this.babySpawnAge;
    }

    public boolean isPanicking() { return this.panicTicks > 0; }

    @Nullable
    private Player getOwner() {
        UUID id = this.getOwnerUuid();
        if (id == null || !(this.level() instanceof ServerLevel sw)) return null;
        return sw.getPlayerByUUID(id);
    }

    private void tickTrap() {
        if (this.trapCooldown > 0) this.trapCooldown--;

        if (this.isTrapping()) {
            this.trapTicks--;
            VoidTardigradeEntity tardigrade = this.trappedTardigrade;
            if (tardigrade == null || !tardigrade.isAlive() || tardigrade.isRemoved() || tardigrade.level() != this.level()) {
                this.endTrap();
                return;
            }

            Vec3 tardigradePos = tardigrade.position();
            if (this.trapCenter != null) {
                Vec3 offset = tardigradePos.subtract(this.trapCenter);
                if (Math.abs(offset.x) > TRAP_BOX || Math.abs(offset.y) > TRAP_BOX || Math.abs(offset.z) > TRAP_BOX) {
                    this.endTrap();
                    return;
                }
                if (tardigradePos.distanceTo(this.trapCenter) < 1.0) {
                    this.trapCenter = this.trapCenter.lerp(tardigradePos, 0.05);
                }
            }

            if (this.castPos != null && this.position().distanceTo(this.castPos) > 0.5) {
                this.snapTo(this.castPos.x, this.castPos.y, this.castPos.z, this.getYRot(), this.getXRot());
            }
            if (this.trapCenter != null) {
                lookAt(this.trapCenter);
            }

            if (this.level() instanceof ServerLevel sw && this.trapCenter != null && this.castPos != null && this.tickCount % 3 == 0) {
                beamParticles(sw, this.castPos, this.trapCenter);
                boxParticles(sw, this.trapCenter, TRAP_BOX);
            }

            if (this.trapTicks <= 0) this.endTrap();
            return;
        }

        if (this.trapCooldown <= 0) {
            VoidTardigradeEntity threat = findChasingTardigrade();
            if (threat != null) startTrap(threat);
        }
    }

    @Nullable
    private VoidTardigradeEntity findChasingTardigrade() {
        List<VoidTardigradeEntity> nearby = this.level().getEntitiesOfClass(
            VoidTardigradeEntity.class,
            this.getBoundingBox().inflate(TRAP_RANGE),
            t -> t.isAlive() && !t.isRemoved() && !t.isTrapped()
        );
        for (VoidTardigradeEntity tardigrade : nearby) {
            if (this.distanceToSqr(tardigrade) > TRAP_RANGE * TRAP_RANGE) continue;
            if (tardigrade.getOrbChaseTarget() == this) return tardigrade;
        }
        return null;
    }

    private void startTrap(VoidTardigradeEntity tardigrade) {
        this.trappedTardigrade = tardigrade;
        this.trapTicks = TRAP_DURATION;
        this.trapCenter = tardigrade.position();
        this.castPos = this.position();
        tardigrade.setTrapped(true);
        lookAt(this.trapCenter);
        if (this.level() instanceof ServerLevel sw) {
            sw.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.5F);
        }
    }

    private void endTrap() {
        if (this.trappedTardigrade != null) this.trappedTardigrade.setTrapped(false);
        this.trappedTardigrade = null;
        this.trapTicks = 0;
        this.trapCenter = null;
        this.castPos = null;
        this.trapCooldown = TRAP_COOLDOWN;
    }

    private void lookAt(Vec3 target) {
        Vec3 diff = target.subtract(this.position());
        if (diff.lengthSqr() < 1.0E-4) return;
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * (180.0F / Math.PI)) - 90.0F;
        this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - this.getYRot()), -10.0F, 10.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private static void beamParticles(ServerLevel world, Vec3 from, Vec3 to) {
        int steps = Math.max(3, (int) (from.distanceTo(to) * 0.5));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            world.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void boxParticles(ServerLevel world, Vec3 center, double size) {
        for (int edge = 0; edge < 12; edge++) {
            for (int i = 0; i <= 3; i++) {
                double t = i / 3.0;
                Vec3 p = trapEdgePoint(center, size, edge, t);
                world.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    // Generates particles along the 12 edges of the light trap box.
    private static Vec3 trapEdgePoint(Vec3 c, double s, int edge, double t) {
        double[] xs = {-s, s, s, -s, -s, s, s, -s};
        double[] ys = {-s, -s, -s, -s, s, s, s, s};
        double[] zs = {-s, -s, s, s, -s, -s, s, s};
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        int a = edges[edge][0], b = edges[edge][1];
        return new Vec3(
            Mth.lerp(t, c.x + xs[a], c.x + xs[b]),
            Mth.lerp(t, c.y + ys[a], c.y + ys[b]),
            Mth.lerp(t, c.z + zs[a], c.z + zs[b])
        );
    }

    private void onGrowUp() {
        this.entityData.set(BABY, false);
        if (this.level() instanceof ServerLevel sw) {
            sw.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.15, 0.15, 0.15, 0.0);
            this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.3F);
        }
    }

    private void spawnBaby(ServerLevel world) {
        EtherealOrbEntity baby = new EtherealOrbEntity(ModEntities.ETHEREAL_ORB, world);
        Vec3 pos = findBabySpawnPos(world);
        baby.snapTo(pos.x, pos.y, pos.z, this.getYRot(), this.getXRot());
        baby.growingAgeTicks = -BABY_GROW_TICKS;
        baby.entityData.set(BABY, true);
        baby.entityData.set(GROWING_AGE, -BABY_GROW_TICKS);
        if (this.isTamed()) {
            baby.setTamed(true);
            baby.setOwnerUuid(this.getOwnerUuid());
        }
        world.addFreshEntity(baby);
        world.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.4, pos.z, 10, 0.2, 0.2, 0.2, 0.0);
        this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private Vec3 findBabySpawnPos(ServerLevel world) {
        float w = 0.4375f * 0.6f;
        float h = 0.6875f * 0.6f;
        for (double[] offset : new double[][] {{0,1,0},{0.3,1,0},{-0.3,1,0},{0,1,0.3},{0,1,-0.3},{0,2,0}}) {
            double x = this.getX() + offset[0], y = this.getY() + offset[1], z = this.getZ() + offset[2];
            AABB box = new AABB(x - w / 2, y, z - w / 2, x + w / 2, y + h, z + w / 2);
            if (world.noCollision(box)) return new Vec3(x, y, z);
        }
        return new Vec3(this.getX(), this.getY() + 1.0, this.getZ());
    }

    static boolean isHomeBlock(BlockState state) {
        return state.is(ModBlocks.STELLARITH_CRYSTAL) || state.is(ModBlocks.ASTRAL_REMNANT);
    }

    static Vec3 clampToFreeSpace(PathfinderMob mob, Vec3 from, Vec3 to) {
        Vec3 last = from;
        for (int i = 1; i <= 8; i++) {
            Vec3 step = from.lerp(to, i / 8.0);
            Vec3 delta = step.subtract(mob.position());
            if (mob.level().noCollision(mob, mob.getBoundingBox().move(delta))) last = step;
            else break;
        }
        return last;    // Crystal Spikes generate at all angles, making the geometry potentiallydifficult to navigate. This prevents the orb from pushing itself into solid blocks.
    }

    class FollowAdultGoal extends Goal {
        private static final double RANGE = 16.0;
        private EtherealOrbEntity parent;

        FollowAdultGoal() { this.setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            return EtherealOrbEntity.this.isBaby() && !EtherealOrbEntity.this.isPanicking() && !EtherealOrbEntity.this.isRotatingForSpawn()
                && (this.parent = findNearestAdult()) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.parent != null && this.parent.isAlive() && EtherealOrbEntity.this.isBaby()
                && EtherealOrbEntity.this.distanceToSqr(this.parent) > 4.0;
        }

        @Override
        public void stop() { this.parent = null; }

        @Override
        public void tick() {
            Vec3 from = EtherealOrbEntity.this.position();
            Vec3 target = clampToFreeSpace(EtherealOrbEntity.this, from, this.parent.position().add(0, 0.2, 0));
            EtherealOrbEntity.this.getMoveControl().setWantedPosition(target.x, target.y, target.z, 2.6);
            EtherealOrbEntity.this.getLookControl().setLookAt(this.parent);
        }

        private EtherealOrbEntity findNearestAdult() {
            return EtherealOrbEntity.this.level().getEntitiesOfClass(
                EtherealOrbEntity.class, EtherealOrbEntity.this.getBoundingBox().inflate(RANGE),
                orb -> orb != EtherealOrbEntity.this && !orb.isBaby()
            ).stream().min(Comparator.comparingDouble(orb -> orb.distanceToSqr(EtherealOrbEntity.this))).orElse(null);
        }
    }

    class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() { this.setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            return EtherealOrbEntity.this.isTamed() && !EtherealOrbEntity.this.isTrapping() && !EtherealOrbEntity.this.isRotatingForSpawn();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            EtherealOrbEntity orb = EtherealOrbEntity.this;
            orb.getNavigation().stop();
            Vec3 from = orb.position();

            if (orb.isAirSitting()) {
                if (orb.sitAnchor == null) {
                    orb.sitAnchor = from;
                    orb.ownerOrbitAngle = orb.random.nextFloat() * Mth.TWO_PI;
                }
                orb.ownerOrbitAngle = (orb.ownerOrbitAngle + 0.05F) % Mth.TWO_PI;
                double r = 2.5;
                Vec3 target = new Vec3(
                    orb.sitAnchor.x + Math.cos(orb.ownerOrbitAngle) * r,
                    orb.sitAnchor.y,
                    orb.sitAnchor.z + Math.sin(orb.ownerOrbitAngle) * r
                );
                moveToward(from, clampToFreeSpace(orb, from, target), 1.4);
                lookAt(orb.sitAnchor);
                return;
            }

            Player owner = orb.getOwner();
            Vec3 center;
            if (owner != null && owner.isAlive() && owner.level() == orb.level()) {
                orb.lostOwnerAnchor = null;
                center = owner.position().add(0, owner.getEyeHeight(), 0);  // If the owner is found, resume following/orbiting them.
            } else {
                if (orb.lostOwnerAnchor == null) orb.lostOwnerAnchor = from;
                center = orb.lostOwnerAnchor; // If the owner is lost, orbit around the last known position instead of wandering aimlessly.
            }

            double dist = from.distanceTo(center);

            if (owner != null && orb.rushTicks > 0) {
                orb.rushTicks--;
                moveToward(from, clampToFreeSpace(orb, from, center), 2.2);
                orb.ownerOrbiting = false;
                return;
            }

            if (owner != null && dist > 12.0) {
                orb.snapTo(center.x, center.y + 0.5, center.z, orb.getYRot(), orb.getXRot());
                orb.setDeltaMovement(Vec3.ZERO);
                orb.ownerOrbiting = false;
                return;
            }

            //Use separate enter/exit distances so the orb doesn't loop between following/orbiting when near the boundary.
            if (orb.ownerOrbiting) {
                if (dist > OWNER_ORBIT_EXIT) orb.ownerOrbiting = false;
            } else if (dist <= OWNER_ORBIT_ENTER) {
                orb.ownerOrbiting = true;
                orb.ownerOrbitAngle = (float) Mth.atan2(from.z - center.z, from.x - center.x);
            }

            Vec3 target;
            double speed;
            if (orb.ownerOrbiting) {
                orb.ownerOrbitAngle = (orb.ownerOrbitAngle + 0.06F) % Mth.TWO_PI;
                target = new Vec3(
                    center.x + Math.cos(orb.ownerOrbitAngle) * OWNER_ORBIT_RADIUS,
                    center.y + 0.5,
                    center.z + Math.sin(orb.ownerOrbitAngle) * OWNER_ORBIT_RADIUS
                );
                speed = 1.75;
            } else {
                target = center;
                speed = dist > 6.0 ? 2.2 : 1.9;
            }

            moveToward(from, clampToFreeSpace(orb, from, target), speed);
            lookAt(center);
        }

        private void moveToward(Vec3 from, Vec3 target, double speed) {
            EtherealOrbEntity.this.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
        }
    }

    class OrbitHomeGoal extends Goal {
        private static final int SCAN_RADIUS = 20;
        private static final double ORBIT_RADIUS = 3.5;
        private static final double STAY_RADIUS = 8.0;
        private static final double MAX_HOME_DISTANCE = 128.0;

        private BlockPos home;
        private float orbitAngle;
        private double orbitDirection = 1.0;
        private Vec3 wanderTarget;
        private int repathCooldown;

        OrbitHomeGoal() { this.setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            return !EtherealOrbEntity.this.level().isClientSide() && !EtherealOrbEntity.this.isTamed()
                && !EtherealOrbEntity.this.isPanicking() && !EtherealOrbEntity.this.isRotatingForSpawn();
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            if (this.repathCooldown > 0) this.repathCooldown--;

            if (this.home == null || !isHomeBlock(EtherealOrbEntity.this.level().getBlockState(this.home))) {
                this.home = findHomeCrystal();
                if (this.home != null) {
                    BlockState state = EtherealOrbEntity.this.level().getBlockState(this.home);
                    this.orbitDirection = state.is(ModBlocks.ASTRAL_REMNANT) ? -1.0 : 1.0;  // Astral Remnant blocks cause Orbs to orbit the opposite direction.
                    Vec3 toHome = Vec3.atCenterOf(this.home).subtract(EtherealOrbEntity.this.position());
                    this.orbitAngle = (float) Mth.atan2(toHome.z, toHome.x);
                }
            }

            Vec3 from = EtherealOrbEntity.this.position();
            Vec3 target;

            if (this.home == null) {
                target = pickWanderTarget(from);
            } else {
                Vec3 homeCenter = Vec3.atCenterOf(this.home);
                double dist = from.distanceTo(homeCenter);
                if (dist > MAX_HOME_DISTANCE) {
                    this.home = null;
                    this.wanderTarget = null;   // If the orb gets extremely far from its home, assume it has lost the crystal and abandon it in hopes to find a new one.
                    target = pickWanderTarget(from);
                } else if (dist > STAY_RADIUS) {
                    target = homeCenter;
                } else {
                    this.orbitAngle = (float) (this.orbitAngle + 0.08 * this.orbitDirection);
                    target = new Vec3(
                        homeCenter.x + Math.cos(this.orbitAngle) * ORBIT_RADIUS,
                        homeCenter.y,
                        homeCenter.z + Math.sin(this.orbitAngle) * ORBIT_RADIUS
                    );
                }
            }

            target = clampToFreeSpace(EtherealOrbEntity.this, from, target);
            double speed = this.home == null ? 1.1 : (from.distanceToSqr(target) > 64 ? 2.4 : 2.0);
            EtherealOrbEntity.this.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
            EtherealOrbEntity.this.getLookControl().setLookAt(target.x, target.y, target.z);
        }

        private Vec3 pickWanderTarget(Vec3 from) {
            if (this.wanderTarget == null || from.distanceToSqr(this.wanderTarget) < 1.5 || this.repathCooldown == 0) {
                this.wanderTarget = from.add(
                    (EtherealOrbEntity.this.random.nextDouble() - 0.5) * 10.0,
                    (EtherealOrbEntity.this.random.nextDouble() * 4.0) - 1.0,
                    (EtherealOrbEntity.this.random.nextDouble() - 0.5) * 10.0
                );
                this.repathCooldown = 15;
            }
            return this.wanderTarget;
        }

        private BlockPos findHomeCrystal() {
            BlockPos origin = EtherealOrbEntity.this.blockPosition();
            BlockPos closest = null;
            double closestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.withinManhattan(origin, SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)) {
                if (!isHomeBlock(EtherealOrbEntity.this.level().getBlockState(pos))) continue;
                double dist = origin.distSqr(pos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = pos.immutable();
                }
            }
            return closest;
        }
    }
}
