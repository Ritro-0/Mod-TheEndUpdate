package com.theendupdate.entity;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.theendupdate.registry.ModItems;
import com.theendupdate.registry.ModSounds;

/**
 * Floating End creature that homes to its spawn position, watches players, and can
 * slingshot them after a short wind-up when eye contact is held.
 */
public class TetherlingEntity extends PathfinderMob {

    public static final byte SYNC_PHASE_IDLE = 0;
    public static final byte SYNC_PHASE_STARE = 1;
    public static final byte SYNC_PHASE_APPROACH = 2;
    public static final byte SYNC_PHASE_CHARGE = 3;
    /** Dramatic upward flight before throwing. */
    public static final byte SYNC_PHASE_LIFTOFF = 4;
    /** Brief tentacle snap after launch (client anim). */
    public static final byte SYNC_PHASE_THROW = 5;
    /** Tentacles easing back to rest after a throw. */
    public static final byte SYNC_PHASE_RECOVER = 6;

    private static final EntityDataAccessor<Byte> SYNC_PHASE = SynchedEntityData.defineId(TetherlingEntity.class, EntityDataSerializers.BYTE);
    /** Remaining wind-up ticks while charging (0 when not in {@link #SYNC_PHASE_CHARGE}). Synced for attach stretch timing. */
    private static final EntityDataAccessor<Byte> CHARGE_TICKS_LEFT = SynchedEntityData.defineId(TetherlingEntity.class, EntityDataSerializers.BYTE);
    /** Countdown during {@link #SYNC_PHASE_THROW} and {@link #SYNC_PHASE_RECOVER}. */
    private static final EntityDataAccessor<Byte> ANIM_AUX = SynchedEntityData.defineId(TetherlingEntity.class, EntityDataSerializers.BYTE);

    private static final int LOOK_ACCUMULATE_TICKS = 60;
    private static final int CHARGE_TICKS = 100;
    /** Same as internal wind-up length; exposed for client attach animation timing. */
    public static final int CHARGE_WINDUP_TICKS = CHARGE_TICKS;
    /** First ticks of charge: tentacles stretch from “approaching” to fully attached. */
    public static final int ATTACH_STRETCH_TICKS = 18;
    /** Dramatic upward flight before throw. */
    public static final int LIFTOFF_TICKS = 25;
    public static final int THROW_ANIM_TICKS = 10;
    public static final int RECOVER_ANIM_TICKS = 38;
    private static final int POST_LAUNCH_COOLDOWN_TICKS = 80;
    private static final double DETECT_RANGE = 20.0;
    private static final double HOME_WANDER = 3.5;
    private static final double HOME_RETURN_DISTANCE = 10.0;
    private static final double MAX_SEQUENCE_RANGE = 48.0;
    /** Maximum distance tentacles can reach. Tetherling moves closer if player is beyond this. */
    public static final double MAX_TENTACLE_REACH = 12.0;
    private static final double LOOK_DOT_MIN = 0.985;
    private static final double LAUNCH_BASE = 20.0;
    private static final double LAUNCH_PER_BLOCK_STRETCH = 8.1;
    /** Extra launch strength per block walked backward during charge (20 blocks → +182 on strength). */
    private static final double BACKWARD_STRENGTH_PER_BLOCK = 182.0 / 20.0;
    private static final double LAUNCH_HORIZ_SCALE = 0.052;
    private static final double LAUNCH_HORIZ_AIM_UP_REDUCTION = 0.25;
    /** Baseline upward push when not aiming up. */
    private static final double LAUNCH_UP_BASE = 0.18;
    /** Gentle vertical scaling from charge/backward strength (kept low to avoid always sky-high throws). */
    private static final double LAUNCH_UP_PER_STRENGTH = 0.0035;
    /** Extra upward push unlocked by aiming upward. */
    private static final double LAUNCH_UP_AIM_BASE_BONUS = 0.75;
    /** Strength-scaled vertical bonus while aiming upward. */
    private static final double LAUNCH_UP_AIM_PER_STRENGTH_BONUS = 0.0045;
    /** Max apex height (blocks) above launch position from vertical impulse alone; horizontal motion can add a little more in flight. */
    private static final double MAX_LAUNCH_PEAK_BLOCKS = 20.0;
    /** Vanilla living-entity aerial gravity per tick (used to estimate peak height; {@link LivingEntity#getGravity()} is protected). */
    private static final double LAUNCH_PEAK_GRAVITY_PER_TICK = 0.08;

    private BlockPos homePos = BlockPos.ZERO;
    private boolean homeInitialized;

    private int lookAccumulateTicks;
    private int chargeTicks;
    private int cooldownTicks;
    private double maxStretchDuringCharge;

    private boolean wasLeashed;

    @Nullable
    private Player sequencePlayer;

    /** Feet position last tick while charging; used to measure backward movement. */
    @Nullable
    private Vec3 chargeLastPlayerPos;
    /** Blocks moved horizontally opposite where the player is looking (WASD back), while attached. */
    private double chargeBackwardBlocks;

    /** World position before liftoff; used to snap back down after the sequence. */
    @Nullable
    private Vec3 preLiftoffPos;

    public TetherlingEntity(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createTetherlingAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.FLYING_SPEED, 0.35)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SYNC_PHASE, SYNC_PHASE_IDLE);
        builder.define(CHARGE_TICKS_LEFT, (byte) 0);
        builder.define(ANIM_AUX, (byte) 0);
    }

    public byte getSyncPhase() {
        return this.entityData.get(SYNC_PHASE);
    }

    private void setSyncPhase(byte phase) {
        this.entityData.set(SYNC_PHASE, phase);
    }

    /** Remaining charge wind-up (for client attach animation). 0 when not charging. */
    public int getChargeTicksRemaining() {
        return Byte.toUnsignedInt(this.entityData.get(CHARGE_TICKS_LEFT));
    }

    /** Throw/recover countdown (meaning depends on {@link #getSyncPhase()}). */
    public int getAnimAuxTicks() {
        return Byte.toUnsignedInt(this.entityData.get(ANIM_AUX));
    }

    private void setChargeTicksRemaining(int ticks) {
        this.entityData.set(CHARGE_TICKS_LEFT, (byte) Math.min(255, Math.max(0, ticks)));
    }

    private void setAnimAux(int ticks) {
        this.entityData.set(ANIM_AUX, (byte) Math.min(255, Math.max(0, ticks)));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level world) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, world);
        navigation.setCanFloat(false);
        return navigation;
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.TETHERLING_SPAWN_EGG);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel world, DamageSource source) {
        if (source.is(DamageTypes.FALL)) {
            return true;
        }
        return super.isInvulnerableTo(world, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putInt("HomeX", this.homePos.getX());
        view.putInt("HomeY", this.homePos.getY());
        view.putInt("HomeZ", this.homePos.getZ());
        view.putBoolean("HomeInit", this.homeInitialized);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        Optional<Integer> ox = view.getInt("HomeX");
        Optional<Integer> oy = view.getInt("HomeY");
        Optional<Integer> oz = view.getInt("HomeZ");
        if (ox.isPresent() && oy.isPresent() && oz.isPresent()) {
            this.homePos = new BlockPos(ox.get(), oy.get(), oz.get());
            this.homeInitialized = view.getBooleanOr("HomeInit", true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.fallDistance = 0.0F;

        if (!this.level().isClientSide()) {
            this.tickServer();
            if (this.level() instanceof ServerLevel sw) {
                this.spawnAmbientEndParticles(sw);
            }
        }
    }

    /** Enderman-style portal motes around the body (server-side). */
    private void spawnAmbientEndParticles(ServerLevel sw) {
        double x = this.getX();
        double y = this.getY(0.55);
        double z = this.getZ();
        sw.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.4, 0.48, 0.4, 0.02);
        if ((this.tickCount + this.getId()) % 3 == 0) {
            sw.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.34, 0.4, 0.34, 0.0);
        }
    }

    private void tickServer() {
        if (!this.homeInitialized) {
            this.homePos = this.blockPosition();
            this.homeInitialized = true;
        }

        if (this.wasLeashed && !this.isLeashed()) {
            this.homePos = this.blockPosition();
        }
        this.wasLeashed = this.isLeashed();

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }

        if (this.isLeashed()) {
            this.sequencePlayer = null;
            this.lookAccumulateTicks = 0;
            this.chargeTicks = 0;
            this.maxStretchDuringCharge = 0.0;
            this.chargeLastPlayerPos = null;
            this.chargeBackwardBlocks = 0.0;
            this.setChargeTicksRemaining(0);
            this.setAnimAux(0);
            this.preLiftoffPos = null;
            this.setSyncPhase(SYNC_PHASE_IDLE);
            return;
        }

        byte phase = this.getSyncPhase();
        if (phase == SYNC_PHASE_LIFTOFF) {
            this.tickLiftoff();
            return;
        }
        if (phase == SYNC_PHASE_THROW) {
            this.tickThrowAnim();
            return;
        }
        if (phase == SYNC_PHASE_RECOVER) {
            this.tickRecoverAnim();
            return;
        }

        switch (phase) {
            case SYNC_PHASE_IDLE -> this.tickIdleSeek();
            case SYNC_PHASE_STARE -> this.tickStare();
            case SYNC_PHASE_APPROACH -> this.tickApproach();
            case SYNC_PHASE_CHARGE -> this.tickCharge();
            default -> this.setSyncPhase(SYNC_PHASE_IDLE);
        }

        this.tickHomeMotion();
    }

    private void tickThrowAnim() {
        int left = this.getAnimAuxTicks();
        if (left <= 1) {
            this.setSyncPhase(SYNC_PHASE_RECOVER);
            this.setAnimAux(RECOVER_ANIM_TICKS);
            return;
        }
        this.setAnimAux(left - 1);
    }

    private void tickRecoverAnim() {
        int left = this.getAnimAuxTicks();

        // wait out the 10-tick tentacle trail window before snapping back down
        if (this.preLiftoffPos != null && left <= RECOVER_ANIM_TICKS - 10) {
            this.tickDescendToPreLiftoff();
        }

        if (left <= 0) {
            if (this.preLiftoffPos != null) {
                this.setPos(this.preLiftoffPos.x, this.preLiftoffPos.y, this.preLiftoffPos.z);
                this.setDeltaMovement(Vec3.ZERO);
            }
            this.preLiftoffPos = null;
            this.setSyncPhase(SYNC_PHASE_IDLE);
            this.setAnimAux(0);
            return;
        }
        this.setAnimAux(left - 1);
    }

    private void tickDescendToPreLiftoff() {
        Vec3 target = this.preLiftoffPos;
        if (target == null) {
            return;
        }
        this.getNavigation().stop();
        Vec3 pos = new Vec3(this.getX(), this.getY(), this.getZ());
        double dist = pos.distanceTo(target);
        if (dist < 0.35) {
            this.setPos(target.x, target.y, target.z);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 dir = target.subtract(pos).normalize();
        double speed = Math.min(5.5, 0.45 + dist * 1.8);
        this.setDeltaMovement(dir.scale(speed));
        this.needsSync = true;
    }

    private void tickIdleSeek() {
        if (this.cooldownTicks > 0) {
            return;
        }
        Player player = this.findNearestPlayer(DETECT_RANGE);
        if (player == null || !isValidTetherlingTarget(player)) {
            return;
        }
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
        if (this.isPlayerLookingAt(player)) {
            this.sequencePlayer = player;
            this.lookAccumulateTicks = 0;
            this.setSyncPhase(SYNC_PHASE_STARE);
        }
    }

    private void tickStare() {
        Player player = this.sequencePlayer;
        if (player == null || !isValidTetherlingTarget(player) || this.distanceToSqr(player) > MAX_SEQUENCE_RANGE * MAX_SEQUENCE_RANGE) {
            this.resetSequence();
            return;
        }
        this.getLookControl().setLookAt(player, 45.0F, 45.0F);
        if (this.isPlayerLookingAt(player)) {
            this.lookAccumulateTicks++;
            if (this.lookAccumulateTicks >= LOOK_ACCUMULATE_TICKS) {
                this.setSyncPhase(SYNC_PHASE_APPROACH);
            }
        } else {
            this.lookAccumulateTicks = 0;
        }
    }

    private void tickApproach() {
        Player player = this.sequencePlayer;
        if (player == null || !isValidTetherlingTarget(player) || this.distanceToSqr(player) > MAX_SEQUENCE_RANGE * MAX_SEQUENCE_RANGE) {
            this.resetSequence();
            return;
        }
        Vec3 head = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
        Vec3 behind = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad)).scale(0.85);
        Vec3 target = head.add(0.0, 0.15, 0.0).add(behind);
        this.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.35);
        this.getLookControl().setLookAt(player, 60.0F, 60.0F);

        if (new Vec3(this.getX(), this.getY(), this.getZ()).distanceTo(target) < 0.85) {
            this.chargeTicks = CHARGE_TICKS;
            this.setChargeTicksRemaining(CHARGE_TICKS);
            this.maxStretchDuringCharge = 0.0;
            this.chargeBackwardBlocks = 0.0;
            this.chargeLastPlayerPos = new Vec3(player.getX(), player.getY(), player.getZ());
            this.setSyncPhase(SYNC_PHASE_CHARGE);
            this.level().playSound(null, this.blockPosition(), ModSounds.TETHERLING_GRAB, SoundSource.NEUTRAL, 0.85F, 1.0F);
        }
    }

    private void tickCharge() {
        Player player = this.sequencePlayer;
        if (player == null || !isValidTetherlingTarget(player) || this.distanceToSqr(player) > MAX_SEQUENCE_RANGE * MAX_SEQUENCE_RANGE) {
            this.resetSequence();
            return;
        }

        Vec3 pPos = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 tPos = new Vec3(this.getX(), this.getY(), this.getZ());
        double dist = pPos.distanceTo(tPos);
        
        if (dist > MAX_TENTACLE_REACH) {
            Vec3 direction = pPos.subtract(tPos).normalize();
            double moveDistance = dist - MAX_TENTACLE_REACH + 0.5; // a bit closer than exactly the reach limit
            Vec3 moveTarget = tPos.add(direction.scale(moveDistance));
            
            this.getMoveControl().setWantedPosition(moveTarget.x, moveTarget.y, moveTarget.z, 1.2);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
        }

        Vec3 currentFeet = new Vec3(player.getX(), player.getY(), player.getZ());
        if (this.chargeLastPlayerPos != null) {
            Vec3 delta = currentFeet.subtract(this.chargeLastPlayerPos);
            Vec3 lookH = horizontalLookDirection(player);
            Vec3 horizDelta = new Vec3(delta.x, 0.0, delta.z);
            double backward = -horizDelta.dot(lookH);
            if (backward > 0.0) {
                this.chargeBackwardBlocks += backward;
            }
        }
        this.chargeLastPlayerPos = currentFeet;

        this.maxStretchDuringCharge = Math.max(this.maxStretchDuringCharge, dist);

        this.chargeTicks--;
        this.setChargeTicksRemaining(this.chargeTicks);
        if (this.chargeTicks <= 0) {
            this.preLiftoffPos = new Vec3(this.getX(), this.getY(), this.getZ());
            this.setSyncPhase(SYNC_PHASE_LIFTOFF);
            this.setAnimAux(LIFTOFF_TICKS);
        }
    }

    private void tickLiftoff() {
        Player player = this.sequencePlayer;
        if (player == null || !isValidTetherlingTarget(player)) {
            this.resetSequence();
            return;
        }

        this.setDeltaMovement(0.0, 1.2, 0.0);
        this.needsSync = true;

        int left = this.getAnimAuxTicks();
        if (left <= 0) {
            this.launchPlayer(player, this.maxStretchDuringCharge, this.chargeBackwardBlocks);
            this.cooldownTicks = POST_LAUNCH_COOLDOWN_TICKS;
            this.beginThrowAnimAfterLaunch();
        } else {
            this.setAnimAux(left - 1);
        }
    }

    private void beginThrowAnimAfterLaunch() {
        this.sequencePlayer = null;
        this.lookAccumulateTicks = 0;
        this.chargeTicks = 0;
        this.maxStretchDuringCharge = 0.0;
        this.chargeLastPlayerPos = null;
        this.chargeBackwardBlocks = 0.0;
        this.setChargeTicksRemaining(0);
        this.setSyncPhase(SYNC_PHASE_THROW);
        this.setAnimAux(THROW_ANIM_TICKS);
    }

    private void launchPlayer(Player player, double stretchBlocks, double backwardBlocks) {
        Vec3 look = player.getViewVector(1.0F);
        double upwardAim = Math.max(0.0, look.y);
        Vec3 lookH = horizontalLookDirection(player);
        double strength = LAUNCH_BASE + LAUNCH_PER_BLOCK_STRETCH * stretchBlocks + BACKWARD_STRENGTH_PER_BLOCK * backwardBlocks;
        double vyDesired = LAUNCH_UP_BASE
            + strength * LAUNCH_UP_PER_STRENGTH
            + upwardAim * (LAUNCH_UP_AIM_BASE_BONUS + strength * LAUNCH_UP_AIM_PER_STRENGTH_BONUS);
        double vy = vyDesired;
        if (peakHeightFromInitialVy(vyDesired, LAUNCH_PEAK_GRAVITY_PER_TICK) > MAX_LAUNCH_PEAK_BLOCKS) {
            vy = maxInitialVyForPeakHeight(MAX_LAUNCH_PEAK_BLOCKS, LAUNCH_PEAK_GRAVITY_PER_TICK);
        }
        double horizontalStrength = strength * LAUNCH_HORIZ_SCALE * (1.0 - upwardAim * LAUNCH_HORIZ_AIM_UP_REDUCTION);
        Vec3 impulse = lookH.scale(horizontalStrength).add(0.0, vy, 0.0);
        player.setDeltaMovement(impulse);
        // Same as NebulaVentBlockEntity.applyPlayerBoost: server must mark dirty or client won't apply the boost.
        player.needsSync = true;
        player.fallDistance = 0.0F;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        if (this.level() instanceof ServerLevel sw) {
            sw.playSound(null, player.blockPosition(), ModSounds.TETHERLING_YEET, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void resetSequence() {
        this.sequencePlayer = null;
        this.lookAccumulateTicks = 0;
        this.chargeTicks = 0;
        this.maxStretchDuringCharge = 0.0;
        this.chargeLastPlayerPos = null;
        this.chargeBackwardBlocks = 0.0;
        this.setChargeTicksRemaining(0);
        this.setAnimAux(0);
        this.preLiftoffPos = null;
        this.setSyncPhase(SYNC_PHASE_IDLE);
    }

    private static boolean isValidTetherlingTarget(Player player) {
        return player.isAlive() && !player.isSpectator();
    }

    /**
     * Discrete climb from initial upward velocity (same tick model as living entities: each tick add v, then v -= gravity).
     */
    private static double peakHeightFromInitialVy(double vy0, double gravityPerTick) {
        if (vy0 <= 0.0) {
            return 0.0;
        }
        if (gravityPerTick <= 1.0E-9) {
            return Double.POSITIVE_INFINITY;
        }
        double h = 0.0;
        double v = vy0;
        while (v > 0.0) {
            h += v;
            v -= gravityPerTick;
        }
        return h;
    }

    private static double maxInitialVyForPeakHeight(double maxPeak, double gravityPerTick) {
        if (maxPeak <= 0.0 || gravityPerTick <= 1.0E-9) {
            return 0.0;
        }
        double lo = 0.0;
        double hi = 8.0;
        while (peakHeightFromInitialVy(hi, gravityPerTick) <= maxPeak) {
            hi += 4.0;
            if (hi > 256.0) {
                return hi;
            }
        }
        for (int i = 0; i < 48; i++) {
            double mid = (lo + hi) * 0.5;
            if (peakHeightFromInitialVy(mid, gravityPerTick) <= maxPeak) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Horizontal unit vector in the direction the player is facing (for WASD "backward" vs look). */
    private static Vec3 horizontalLookDirection(Player player) {
        Vec3 look = player.getViewVector(1.0F);
        double hx = look.x;
        double hz = look.z;
        double lenSq = hx * hx + hz * hz;
        if (lenSq < 1.0E-8) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double inv = 1.0 / Math.sqrt(lenSq);
        return new Vec3(hx * inv, 0.0, hz * inv);
    }

    @Nullable
    private Player findNearestPlayer(double range) {
        AABB box = this.getBoundingBox().inflate(range);
        Player closest = null;
        double best = range * range;
        for (Player p : this.level().getEntitiesOfClass(Player.class, box, pl -> pl != null && isValidTetherlingTarget(pl))) {
            double d = this.distanceToSqr(p);
            if (d < best) {
                best = d;
                closest = p;
            }
        }
        return closest;
    }

    private boolean isPlayerLookingAt(Player player) {
        if (!player.hasLineOfSight(this)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 center = this.getBoundingBox().getCenter();
        Vec3 toEntity = center.subtract(eye);
        double lenSq = toEntity.lengthSqr();
        if (lenSq < 1.0E-4) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F);
        double cos = look.dot(toEntity.normalize());
        return cos > LOOK_DOT_MIN;
    }

    private void tickHomeMotion() {
        if (this.getSyncPhase() != SYNC_PHASE_IDLE) {
            return;
        }
        if (this.isLeashed()) {
            return;
        }
        Vec3 homeCenter = Vec3.atCenterOf(this.homePos);
        Vec3 pos = new Vec3(this.getX(), this.getY(), this.getZ());
        double distHome = pos.distanceTo(homeCenter);
        if (distHome > HOME_RETURN_DISTANCE) {
            Vec3 dir = homeCenter.subtract(pos).normalize();
            Vec3 step = pos.add(dir.scale(0.35));
            this.getMoveControl().setWantedPosition(step.x, step.y, step.z, 0.9);
            return;
        }
        if (distHome > HOME_WANDER || this.getRandom().nextInt(120) == 0) {
            double ox = (this.getRandom().nextDouble() - 0.5) * 2.4;
            double oy = (this.getRandom().nextDouble() - 0.5) * 1.2;
            double oz = (this.getRandom().nextDouble() - 0.5) * 2.4;
            Vec3 wander = homeCenter.add(ox, oy, oz);
            this.getMoveControl().setWantedPosition(wander.x, wander.y, wander.z, 0.45);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TETHERLING_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.TETHERLING_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TETHERLING_DEATH;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        this.resetSequence();
        return super.hurtServer(world, source, amount);
    }
}
