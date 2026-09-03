package com.theendupdate.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Atmospheric Shadowlands watcher: a floating pair of eyes that turns to face the player,
 * then darts away if stared at, damaged, or left more than 115 blocks away.
 */
public class EyesEntity extends Mob {
    public static final int STAGE_IDLE = 0;
    public static final int STAGE_DART_1 = 1;
    public static final int STAGE_DART_2 = 2;

    private static final EntityDataAccessor<Integer> VANISH_STAGE =
        SynchedEntityData.defineId(EyesEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DISPLAY_SCALE =
        SynchedEntityData.defineId(EyesEntity.class, EntityDataSerializers.FLOAT);

    private static final int LOOK_TICKS_REQUIRED = 20;
    private static final int DART_STAGE_TICKS = 10;
    private static final int LIFETIME_TICKS = 20 * 60;
    private static final double VANISH_DISTANCE = 115.0;
    private static final double SEARCH_DISTANCE = 128.0;

    private int consecutiveLookTicks;
    private int dartTicks;

    public EyesEntity(EntityType<? extends Mob> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FLYING_SPEED, 0.2)
            .add(Attributes.FOLLOW_RANGE, 80.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VANISH_STAGE, STAGE_IDLE);
        builder.define(DISPLAY_SCALE, 1.0F);
    }

    public int getVanishStage() {
        return this.entityData.get(VANISH_STAGE);
    }

    public float getDisplayScale() {
        return this.entityData.get(DISPLAY_SCALE);
    }

    public void setDisplayScale(float scale) {
        this.entityData.set(DISPLAY_SCALE, Math.max(1.0F, scale));
        this.refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DISPLAY_SCALE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return super.getDefaultDimensions(pose).scale(this.getDisplayScale());
    }

    private void setVanishStage(int stage) {
        this.entityData.set(VANISH_STAGE, stage);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        this.noPhysics = true;
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        Player player = this.findNearestPlayer();
        if (player != null) {
            this.getLookControl().setLookAt(player, 360.0F, 360.0F);
            this.setYRot(this.getYHeadRot());
            this.yBodyRot = this.getYRot();
        }
        this.setDeltaMovement(Vec3.ZERO);

        int stage = this.getVanishStage();
        if (stage == STAGE_IDLE) {
            if (this.tickCount >= LIFETIME_TICKS) {
                this.beginDartVanish();
                return;
            }
            boolean tooFar = this.tickCount > 40
                && (player == null || this.distanceToSqr(player) > VANISH_DISTANCE * VANISH_DISTANCE);
            if (tooFar) {
                this.beginDartVanish();
                return;
            }
            if (player != null && this.isPlayerLookingAt(player)) {
                this.consecutiveLookTicks++;
                if (this.consecutiveLookTicks >= LOOK_TICKS_REQUIRED) {
                    this.beginDartVanish();
                }
            } else {
                this.consecutiveLookTicks = 0;
            }
            return;
        }

        this.dartTicks++;
        if (stage == STAGE_DART_1 && this.dartTicks >= DART_STAGE_TICKS) {
            this.setVanishStage(STAGE_DART_2);
            this.dartTicks = 0;
        } else if (stage == STAGE_DART_2 && this.dartTicks >= DART_STAGE_TICKS) {
            this.discard();
        }
    }

    private void beginDartVanish() {
        if (this.getVanishStage() != STAGE_IDLE) {
            return;
        }
        this.setVanishStage(STAGE_DART_1);
        this.dartTicks = 0;
    }

    @Nullable
    private Player findNearestPlayer() {
        AABB box = this.getBoundingBox().inflate(SEARCH_DISTANCE);
        Player closest = null;
        double best = SEARCH_DISTANCE * SEARCH_DISTANCE;
        for (Player player : this.level().getEntitiesOfClass(Player.class, box, this::isValidTarget)) {
            double d = this.distanceToSqr(player);
            if (d < best) {
                best = d;
                closest = player;
            }
        }
        return closest;
    }

    private boolean isValidTarget(Player player) {
        return player != null && player.isAlive() && !player.isSpectator();
    }

    private boolean isPlayerLookingAt(Player player) {
        if (!player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 toEyes = this.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double dist = toEyes.length();
        if (dist < 1.0E-4) {
            return true;
        }
        double cos = player.getViewVector(1.0F).dot(toEyes.normalize());
        return cos > 1.0 - 0.18 / dist;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isRemoved()) {
            return false;
        }
        this.discard();
        return true;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
        return source.is(DamageTypes.FALL) || super.isInvulnerableTo(world, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Unloaded chunks drop this entity; noSave() means it is never written back.
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
