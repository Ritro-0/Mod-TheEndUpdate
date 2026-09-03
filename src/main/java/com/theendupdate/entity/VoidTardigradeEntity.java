package com.theendupdate.entity;

import com.theendupdate.registry.ModItems;
import com.theendupdate.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A gentle void-dwelling tardigrade that glides just above the abyss.
 * <p>
 * Behaviour goals kept intentionally simple so the creature always prefers to
 * hover over empty space and resists drifting back onto solid islands.
 */
public class VoidTardigradeEntity extends PathfinderMob {
    private static final double MIN_ALTITUDE_OFFSET = 18.0;
    private static final double MAX_ALTITUDE_OFFSET = 32.0;
    private static final double EDGE_SEARCH_MIN_DISTANCE = 4.0;
    private static final double EDGE_SEARCH_RADIUS = 12.0;
    private static final double EDGE_OUTWARD_OFFSET = 1.75;
    private static final double EDGE_HOVER_HEIGHT = 5.0;
    private static final int EDGE_ATTEMPTS = 20;
    private static final int EDGE_VOID_CHECK_DEPTH = 12;
    private static final int EDGE_REQUIRED_CLEAR_DEPTH = 5;
    private static final int SURFACE_FALLBACK_SCAN = 12;
    private static final double SURFACE_HOVER_MIN_OFFSET = 3.0;
    private static final double SURFACE_HOVER_MAX_OFFSET = 5.0;
    private static final double SURFACE_SEARCH_MIN_DISTANCE = 4.0;
    private static final double SURFACE_SEARCH_RADIUS = 10.0;
    private static final int SURFACE_ATTEMPTS = 16;
    private static final int TARGET_REEVALUATE_MIN = 40;
    private static final int TARGET_REEVALUATE_MAX = 80;

    private Vec3 hoverTarget;
    private int hoverCooldown;
    private float bodyBobPhase;
    private double surfaceHoverBaseY = Double.NaN;
    @Nullable
    private EtherealOrbEntity chasingOrb;
    private boolean isTrapped = false;

    public final AnimationState idleAnimationState = new AnimationState();

    public VoidTardigradeEntity(EntityType<? extends PathfinderMob> entityType, Level world) {
        super(entityType, world);
        this.moveControl = new FlyingMoveControl(this, 30, true);
        this.setNoGravity(true);
        this.xpReward = 1;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ConsumeOrbGoal(this));
        this.goalSelector.addGoal(2, new HoverOverVoidGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
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
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (!this.idleAnimationState.isStarted()) {
                this.idleAnimationState.start(this.tickCount);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.fallDistance = 0.0F;

        Vec3 currentVelocity = this.getDeltaMovement();
        if (currentVelocity.y < -0.01) {
            currentVelocity = new Vec3(currentVelocity.x, -0.01, currentVelocity.z);
            this.setDeltaMovement(currentVelocity);
        }

        if (this.isTrapped) {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
            return;
        }
        
        EtherealOrbEntity activeChaseTarget = this.chasingOrb;
        boolean chasing = activeChaseTarget != null && activeChaseTarget.isAlive() && !activeChaseTarget.isRemoved();
        if (!chasing) {
            this.chasingOrb = null;
        }

        if (!this.level().isClientSide()) {
            if (chasing) {
                this.hoverTarget = null;
                this.hoverCooldown = 0;
            } else {
                if (this.hoverTarget == null || this.hoverCooldown <= 0 || this.reachedHoverTarget()) {
                    this.hoverTarget = this.pickHoverTarget();
                    int range = TARGET_REEVALUATE_MAX - TARGET_REEVALUATE_MIN;
                    this.hoverCooldown = TARGET_REEVALUATE_MIN + this.random.nextInt(range + 1);
                } else {
                    this.hoverCooldown--;
                }
            }

            if (chasing && activeChaseTarget != null) {
                Vec3 currentPos = new Vec3(this.getX(), this.getY(), this.getZ());
                Vec3 targetPos = new Vec3(activeChaseTarget.getX(), activeChaseTarget.getY(), activeChaseTarget.getZ());
                Vec3 toTarget = targetPos.subtract(currentPos);
                double distance = toTarget.length();
                double speed = Mth.clamp(distance * 0.2, 1.1, 2.6);
                this.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, speed);

                double altitudeDelta = toTarget.y;
                double desiredVerticalSpeed = Mth.clamp(altitudeDelta * 0.2, -0.35, 0.35);
                Vec3 velocity = this.getDeltaMovement();
                double smoothedY = Mth.lerp(0.45, velocity.y, desiredVerticalSpeed);
                this.setDeltaMovement(velocity.x, smoothedY, velocity.z);
            } else if (this.hoverTarget != null) {
                Vec3 currentPos = new Vec3(this.getX(), this.getY(), this.getZ());
                Vec3 toTarget = this.hoverTarget.subtract(currentPos);
                double speed = Mth.clamp(toTarget.length(), 0.6, 1.25);
                this.getMoveControl().setWantedPosition(this.hoverTarget.x, this.hoverTarget.y, this.hoverTarget.z, speed);

                double altitudeDelta = this.hoverTarget.y - this.getY();
                double desiredVerticalSpeed = Mth.clamp(altitudeDelta * 0.08, -0.05, 0.05);
                Vec3 velocity = this.getDeltaMovement();
                double smoothedY = Mth.lerp(0.25, velocity.y, desiredVerticalSpeed);
                this.setDeltaMovement(velocity.x, smoothedY, velocity.z);
            }

            Vec3 velocity = this.getDeltaMovement();
            if (velocity.lengthSqr() > 1.0E-4D) {
                float targetYaw = (float)(Mth.atan2(velocity.z, velocity.x) * (180.0F / Math.PI)) - 90.0F;
                this.setYRot(this.approachAngle(this.getYRot(), targetYaw, 6.0F));
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
            }
        }

        this.bodyBobPhase += 0.08F;
    }

    private boolean reachedHoverTarget() {
        return this.hoverTarget == null || this.distanceToSqr(this.hoverTarget) < 1.5;
    }

    private Vec3 pickHoverTarget() {
        Vec3 edgeTarget = this.findEdgeHoverTarget();
        if (edgeTarget != null) {
            this.surfaceHoverBaseY = Double.NaN;
            return edgeTarget;
        }
        if (!this.hasVoidBelow()) {
            Vec3 surfaceTarget = this.findSurfaceHoverTarget();
            if (surfaceTarget != null) {
                return surfaceTarget;
            }
        }
        this.surfaceHoverBaseY = Double.NaN;
        return this.findVoidHoverTarget();
    }

    private boolean isVoidBelow(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
            Mth.floor(x),
            Mth.floor(y) - 1,
            Mth.floor(z)
        );
        Level world = this.level();
        int bottomY = world.getMinY();

        for (int i = 0; i < 16 && pos.getY() >= bottomY; i++) {
            if (!world.getBlockState(pos).isAir()) {
                return false;
            }
            pos.move(Direction.DOWN);
        }
        return true;
    }

    private Vec3 findEdgeHoverTarget() {
        Level world = this.level();
        int bottomY = world.getMinY();

        for (int attempt = 0; attempt < EDGE_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            double distance = EDGE_SEARCH_MIN_DISTANCE + this.random.nextDouble() * EDGE_SEARCH_RADIUS;
            double baseX = this.getX() + Math.cos(angle) * distance;
            double baseZ = this.getZ() + Math.sin(angle) * distance;

            BlockPos surfacePos = this.findIslandSurface(baseX, baseZ, bottomY);
            if (surfacePos == null) {
                continue;
            }

            Direction edgeDirection = this.findEdgeDirection(surfacePos, bottomY);
            if (edgeDirection == null) {
                continue;
            }

            double hoverX = surfacePos.getX() + 0.5 + edgeDirection.getStepX() * EDGE_OUTWARD_OFFSET;
            double hoverZ = surfacePos.getZ() + 0.5 + edgeDirection.getStepZ() * EDGE_OUTWARD_OFFSET;
            double hoverY = surfacePos.getY() + EDGE_HOVER_HEIGHT;

            if (this.isVoidBelow(hoverX, hoverY, hoverZ)) {
                double wobble = (this.random.nextDouble() - 0.5) * 1.2;
                return new Vec3(hoverX, hoverY + wobble, hoverZ);
            }
        }

        return null;
    }

    private Vec3 findVoidHoverTarget() {
        Level world = this.level();
        double bottomY = world.getMinY();

        double minY = bottomY + MIN_ALTITUDE_OFFSET;
        double maxY = bottomY + MAX_ALTITUDE_OFFSET;
        double preferredY = Mth.clamp(this.getY(), minY, maxY);

        for (int attempt = 0; attempt < 12; attempt++) {
            double offsetX = (this.random.nextDouble() * 2.0 - 1.0) * 12.0;
            double offsetZ = (this.random.nextDouble() * 2.0 - 1.0) * 12.0;
            double offsetY = (this.random.nextDouble() * 2.0 - 1.0) * 6.0;

            double targetX = this.getX() + offsetX;
            double targetZ = this.getZ() + offsetZ;
            double targetY = Mth.clamp(preferredY + offsetY, minY, maxY);

            if (isVoidBelow(targetX, targetY, targetZ)) {
                return new Vec3(targetX, targetY, targetZ);
            }
        }

        double failsafeY = Math.max(minY, Math.min(maxY, preferredY));
        return new Vec3(this.getX(), failsafeY, this.getZ());
    }

    private BlockPos findIslandSurface(double x, double z, int bottomY) {
        Level world = this.level();
        BlockPos topPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));

        if (topPos.getY() <= bottomY) {
            return null;
        }

        BlockPos surfacePos = topPos.below();
        if (!world.getBlockState(surfacePos).isAir()) {
            return surfacePos;
        }

        BlockPos.MutableBlockPos mutable = surfacePos.mutable();
        for (int i = 0; i < SURFACE_FALLBACK_SCAN && mutable.getY() > bottomY; i++) {
            if (!world.getBlockState(mutable).isAir()) {
                return mutable.immutable();
            }
            mutable.move(Direction.DOWN);
        }

        return null;
    }

    private Direction findEdgeDirection(BlockPos surfacePos, int bottomY) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.hasSteepDrop(surfacePos, direction, bottomY)) {
                return direction;
            }
        }
        return null;
    }

    private boolean hasSteepDrop(BlockPos surfacePos, Direction direction, int bottomY) {
        Level world = this.level();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(
            surfacePos.getX() + direction.getStepX(),
            surfacePos.getY(),
            surfacePos.getZ() + direction.getStepZ()
        );

        int clearDepth = 0;
        while (checkPos.getY() >= bottomY && clearDepth < EDGE_VOID_CHECK_DEPTH) {
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
            checkPos.move(Direction.DOWN);
            clearDepth++;
        }

        return clearDepth >= EDGE_REQUIRED_CLEAR_DEPTH;
    }

    private BlockPos findSurfaceDirectlyBelow(double x, double y, double z, int bottomY) {
        Level world = this.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
            Mth.floor(x),
            Mth.floor(y),
            Mth.floor(z)
        );

        for (int i = 0; i < 32 && pos.getY() >= bottomY; i++) {
            if (!world.getBlockState(pos).isAir()) {
                return pos.immutable();
            }
            pos.move(Direction.DOWN);
        }

        return null;
    }

    private Vec3 findSurfaceHoverTarget() {
        Level world = this.level();
        int bottomY = world.getMinY();

        BlockPos baseSurface = this.findSurfaceDirectlyBelow(this.getX(), this.getY(), this.getZ(), bottomY);
        if (baseSurface != null) {
            double offset = (SURFACE_HOVER_MIN_OFFSET + SURFACE_HOVER_MAX_OFFSET) * 0.5;
            double candidateBase = baseSurface.getY() + offset;
            if (Double.isNaN(this.surfaceHoverBaseY)) {
                this.surfaceHoverBaseY = candidateBase;
            } else if (Math.abs(candidateBase - this.surfaceHoverBaseY) <= 3.5) {
                this.surfaceHoverBaseY = Mth.lerp(0.35, this.surfaceHoverBaseY, candidateBase);
            } else {
                this.surfaceHoverBaseY = candidateBase;
            }
        }
        double referenceBase = this.surfaceHoverBaseY;

        for (int attempt = 0; attempt < SURFACE_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            double distance = SURFACE_SEARCH_MIN_DISTANCE + this.random.nextDouble() * SURFACE_SEARCH_RADIUS;
            double sampleX = this.getX() + Math.cos(angle) * distance;
            double sampleZ = this.getZ() + Math.sin(angle) * distance;

            BlockPos surfacePos = this.findIslandSurface(sampleX, sampleZ, bottomY);
            if (surfacePos == null) {
                continue;
            }

            if (baseSurface != null && Math.abs(surfacePos.getY() - baseSurface.getY()) > 4) {
                continue;
            }

            double targetX = surfacePos.getX() + 0.5;
            double targetZ = surfacePos.getZ() + 0.5;
            double elevationOffset = SURFACE_HOVER_MIN_OFFSET + this.random.nextDouble() * (SURFACE_HOVER_MAX_OFFSET - SURFACE_HOVER_MIN_OFFSET);
            double targetY = surfacePos.getY() + elevationOffset;

            if (!Double.isNaN(referenceBase)) {
                double diff = targetY - referenceBase;
                if (Math.abs(diff) > 4.0) {
                    continue;
                }

                double lerpFactor = Math.abs(diff) > 2.2 ? 0.55 : 0.35;
                referenceBase = Mth.lerp(lerpFactor, referenceBase, targetY);
                this.surfaceHoverBaseY = referenceBase;

                double clampRange = 1.4;
                targetY = Mth.clamp(targetY, referenceBase - clampRange, referenceBase + clampRange);
            } else {
                referenceBase = targetY;
                this.surfaceHoverBaseY = targetY;
            }

            if (!this.isVoidBelow(targetX, targetY, targetZ)) {
                Vec3 jitter = new Vec3(
                    (this.random.nextDouble() - 0.5) * 1.2,
                    (this.random.nextDouble() - 0.5) * 0.3,
                    (this.random.nextDouble() - 0.5) * 1.2
                );
                return new Vec3(targetX, targetY, targetZ).add(jitter);
            }
        }

        return null;
    }

    private float approachAngle(float current, float target, float maxChange) {
        float difference = Mth.wrapDegrees(target - current);
        float clamped = Mth.clamp(difference, -maxChange, maxChange);
        return current + clamped;
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
    protected SoundEvent getAmbientSound() {
        return ModSounds.VOID_TARDIGRADE_IDLE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VOID_TARDIGRADE_DEATH;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.VOID_TARDIGRADE_SPAWN_EGG);
    }

    public Vec3 getHoverTarget() {
        return this.hoverTarget;
    }

    public boolean hasVoidBelow() {
        return isVoidBelow(this.getX(), this.getY(), this.getZ());
    }

    public float getBodyBobPhase() {
        return this.bodyBobPhase;
    }

    public float getHorizontalFlightSpeed() {
        return (float)this.getDeltaMovement().horizontalDistance();
    }

    @Override
    public void die(DamageSource damageSource) {
        EtherealOrbEntity chasedOrb = this.chasingOrb;
        Player killer = getPlayerFromDamageSource(damageSource);
        super.die(damageSource);
        if (!this.level().isClientSide()
            && killer != null
            && chasedOrb != null
            && chasedOrb.isAlive()
            && !chasedOrb.isRemoved()
            && !chasedOrb.isTamed()
            && chasedOrb.level() == this.level()) {
            chasedOrb.tameBy(killer);
        }
    }

    @Nullable
    private static Player getPlayerFromDamageSource(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            return player;
        }
        if (attacker instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Player playerOwner) {
                return playerOwner;
            }
        }
        return null;
    }

    public static AttributeSupplier.Builder createVoidTardigradeAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0D)
            .add(Attributes.FLYING_SPEED, 0.35D)
            .add(Attributes.MOVEMENT_SPEED, 0.2D)
            .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    private void consumeOrb(EtherealOrbEntity orb) {
        if (orb == null || orb.isRemoved()) {
            return;
        }
        Level world = this.level();
        
        // clear trapped state if this orb was the one boxing us, otherwise we'd be stuck after eating it
        if (this.isTrapped() && orb.isTamed()) {
            this.setTrapped(false);
        }
        
        // name must be read before discard() wipes the entity
        Component orbNameText = orb.getName();
        String orbNameString = orbNameText.getString();
        boolean hasCustomName = orb.hasCustomName();
        
        orb.discard();
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        serverWorld.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.9F, 0.9F + this.random.nextFloat() * 0.25F);
        
        ItemStack bitStack = new ItemStack(ModItems.TARDIGRADE_SHELL_BIT);
        
        if (hasCustomName && orbNameString != null && !orbNameString.isEmpty()) {
            Component bitName = Component.literal(orbNameString + " Bit");
            bitStack.set(DataComponents.CUSTOM_NAME, bitName);
        }
        
        this.spawnAtLocation(serverWorld, bitStack);
        this.setOrbChaseTarget(null);
    }

    @Nullable
    public EtherealOrbEntity getChasingOrb() {
        return this.chasingOrb;
    }

    public void setChasingOrb(@Nullable EtherealOrbEntity orb) {
        this.setOrbChaseTarget(orb);
    }

    public void setOrbChaseTarget(@Nullable EtherealOrbEntity orb) {
        if (orb == null || !orb.isAlive() || orb.isRemoved()) {
            this.chasingOrb = null;
        } else {
            this.chasingOrb = orb;
            this.hoverTarget = null;
            this.hoverCooldown = 0;
        }
    }

    @Nullable
    public EtherealOrbEntity getOrbChaseTarget() {
        return this.chasingOrb;
    }
    
    public void setTrapped(boolean trapped) {
        this.isTrapped = trapped;
        if (trapped) {
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
        } else {
            // normal aiStep movement logic picks back up on its own
        }
    }
    
    public boolean isTrapped() {
        return this.isTrapped;
    }

    private static final class ConsumeOrbGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private static final double SEARCH_RANGE = 18.0;
        private static final double CONSUME_DISTANCE_SQ = 1.2;
        private static final double APPROACH_SPEED = 1.6;

        private final VoidTardigradeEntity tardigrade;
        private EtherealOrbEntity target;
        private int cooldown;

        ConsumeOrbGoal(VoidTardigradeEntity tardigrade) {
            this.tardigrade = tardigrade;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (this.tardigrade.level().isClientSide()) {
                return false;
            }
            List<EtherealOrbEntity> candidates = this.tardigrade.level().getEntitiesOfClass(
                EtherealOrbEntity.class,
                this.tardigrade.getBoundingBox().inflate(SEARCH_RANGE),
                orb -> orb != null && orb.isAlive() && !orb.isRemoved()
            );
            if (candidates.isEmpty()) {
                return false;
            }
            EtherealOrbEntity closest = null;
            double closestSq = Double.MAX_VALUE;
            for (EtherealOrbEntity orb : candidates) {
                double distSq = this.tardigrade.distanceToSqr(orb);
                if (distSq < closestSq) {
                    closestSq = distSq;
                    closest = orb;
                }
            }
            if (closest == null || closestSq > SEARCH_RANGE * SEARCH_RANGE) {
                return false;
            }
            this.target = closest;
            this.tardigrade.setOrbChaseTarget(closest);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            // abandon the chase if the target has us boxed (isTamed + isTrapped)
            return this.target != null && this.target.isAlive() && !this.target.isRemoved() && 
                   !(this.target.isTamed() && this.tardigrade.isTrapped());
        }

        @Override
        public void stop() {
            if (this.target != null && !this.target.isRemoved()) {
                this.cooldown = 20;
            }
            this.target = null;
            this.tardigrade.getNavigation().stop();
            this.tardigrade.setOrbChaseTarget(null);
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            this.tardigrade.getLookControl().setLookAt(this.target, 45.0F, 45.0F);
            this.tardigrade.getMoveControl().setWantedPosition(
                this.target.getX(),
                this.target.getY(),
                this.target.getZ(),
                APPROACH_SPEED
            );

            double distSq = this.tardigrade.distanceToSqr(this.target);
            if (distSq <= CONSUME_DISTANCE_SQ) {
                // works even if it's currently boxing us; consumeOrb clears the trapped state
                this.tardigrade.consumeOrb(this.target);
                this.target = null;
                this.cooldown = 60;
                this.tardigrade.getNavigation().stop();
            }
        }
    }

    private static final class HoverOverVoidGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final VoidTardigradeEntity tardigrade;

        HoverOverVoidGoal(VoidTardigradeEntity tardigrade) {
            this.tardigrade = tardigrade;
            this.setFlags(EnumSet.of(Flag.MOVE));
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
            if (this.tardigrade.hoverTarget == null) {
                this.tardigrade.hoverTarget = this.tardigrade.pickHoverTarget();
            }
        }
    }
}

