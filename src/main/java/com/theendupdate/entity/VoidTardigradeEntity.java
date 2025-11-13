package com.theendupdate.entity;

import com.theendupdate.registry.ModItems;
import com.theendupdate.registry.ModSounds;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * A gentle void-dwelling tardigrade that glides just above the abyss.
 * <p>
 * Behaviour goals kept intentionally simple so the creature always prefers to
 * hover over empty space and resists drifting back onto solid islands.
 */
public class VoidTardigradeEntity extends PathAwareEntity implements Flutterer {
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

    private Vec3d hoverTarget;
    private int hoverCooldown;
    private float bodyBobPhase;
    private double surfaceHoverBaseY = Double.NaN;
    @Nullable
    private EtherealOrbEntity chasingOrb;
    private boolean isTrapped = false;

    public final AnimationState idleAnimationState = new AnimationState();

    public VoidTardigradeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 30, true);
        this.setNoGravity(true);
        this.experiencePoints = 1;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ConsumeEtherealOrbGoal(this));
        this.goalSelector.add(2, new HoverOverVoidGoal(this));
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
    public boolean isInAir() {
        return !this.isOnGround();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getEntityWorld().isClient()) {
            if (!this.idleAnimationState.isRunning()) {
                this.idleAnimationState.start(this.age);
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        this.fallDistance = 0.0F;

        Vec3d currentVelocity = this.getVelocity();
        if (currentVelocity.y < -0.01) {
            currentVelocity = new Vec3d(currentVelocity.x, -0.01, currentVelocity.z);
            this.setVelocity(currentVelocity);
        }

        // If trapped, prevent movement
        if (this.isTrapped) {
            this.setVelocity(Vec3d.ZERO);
            this.getNavigation().stop();
            return;
        }
        
        EtherealOrbEntity activeChaseTarget = this.chasingOrb;
        boolean chasing = activeChaseTarget != null && activeChaseTarget.isAlive() && !activeChaseTarget.isRemoved();
        if (!chasing) {
            this.chasingOrb = null;
        }

        if (!this.getEntityWorld().isClient()) {
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
                Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
                Vec3d targetPos = new Vec3d(activeChaseTarget.getX(), activeChaseTarget.getY(), activeChaseTarget.getZ());
                Vec3d toTarget = targetPos.subtract(currentPos);
                double distance = toTarget.length();
                double speed = MathHelper.clamp(distance * 0.2, 1.1, 2.6);
                this.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);

                double altitudeDelta = toTarget.y;
                double desiredVerticalSpeed = MathHelper.clamp(altitudeDelta * 0.2, -0.35, 0.35);
                Vec3d velocity = this.getVelocity();
                double smoothedY = MathHelper.lerp(0.45, velocity.y, desiredVerticalSpeed);
                this.setVelocity(velocity.x, smoothedY, velocity.z);
            } else if (this.hoverTarget != null) {
                Vec3d currentPos = new Vec3d(this.getX(), this.getY(), this.getZ());
                Vec3d toTarget = this.hoverTarget.subtract(currentPos);
                double speed = MathHelper.clamp(toTarget.length(), 0.6, 1.25);
                this.getMoveControl().moveTo(this.hoverTarget.x, this.hoverTarget.y, this.hoverTarget.z, speed);

                double altitudeDelta = this.hoverTarget.y - this.getY();
                double desiredVerticalSpeed = MathHelper.clamp(altitudeDelta * 0.08, -0.05, 0.05);
                Vec3d velocity = this.getVelocity();
                double smoothedY = MathHelper.lerp(0.25, velocity.y, desiredVerticalSpeed);
                this.setVelocity(velocity.x, smoothedY, velocity.z);
            }

            Vec3d velocity = this.getVelocity();
            if (velocity.lengthSquared() > 1.0E-4D) {
                float targetYaw = (float)(MathHelper.atan2(velocity.z, velocity.x) * (180.0F / Math.PI)) - 90.0F;
                this.setYaw(this.approachAngle(this.getYaw(), targetYaw, 6.0F));
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();
            }
        }

        this.bodyBobPhase += 0.08F;
    }

    private boolean reachedHoverTarget() {
        return this.hoverTarget == null || this.squaredDistanceTo(this.hoverTarget) < 1.5;
    }

    private Vec3d pickHoverTarget() {
        Vec3d edgeTarget = this.findEdgeHoverTarget();
        if (edgeTarget != null) {
            this.surfaceHoverBaseY = Double.NaN;
            return edgeTarget;
        }
        if (!this.hasVoidBelow()) {
            Vec3d surfaceTarget = this.findSurfaceHoverTarget();
            if (surfaceTarget != null) {
                return surfaceTarget;
            }
        }
        this.surfaceHoverBaseY = Double.NaN;
        return this.findVoidHoverTarget();
    }

    private boolean isVoidBelow(double x, double y, double z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(
            MathHelper.floor(x),
            MathHelper.floor(y) - 1,
            MathHelper.floor(z)
        );
        World world = this.getEntityWorld();
        int bottomY = world.getBottomY();

        for (int i = 0; i < 16 && pos.getY() >= bottomY; i++) {
            if (!world.getBlockState(pos).isAir()) {
                return false;
            }
            pos.move(Direction.DOWN);
        }
        return true;
    }

    private Vec3d findEdgeHoverTarget() {
        World world = this.getEntityWorld();
        int bottomY = world.getBottomY();

        for (int attempt = 0; attempt < EDGE_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * MathHelper.TAU;
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

            double hoverX = surfacePos.getX() + 0.5 + edgeDirection.getOffsetX() * EDGE_OUTWARD_OFFSET;
            double hoverZ = surfacePos.getZ() + 0.5 + edgeDirection.getOffsetZ() * EDGE_OUTWARD_OFFSET;
            double hoverY = surfacePos.getY() + EDGE_HOVER_HEIGHT;

            if (this.isVoidBelow(hoverX, hoverY, hoverZ)) {
                double wobble = (this.random.nextDouble() - 0.5) * 1.2;
                return new Vec3d(hoverX, hoverY + wobble, hoverZ);
            }
        }

        return null;
    }

    private Vec3d findVoidHoverTarget() {
        World world = this.getEntityWorld();
        double bottomY = world.getBottomY();

        double minY = bottomY + MIN_ALTITUDE_OFFSET;
        double maxY = bottomY + MAX_ALTITUDE_OFFSET;
        double preferredY = MathHelper.clamp(this.getY(), minY, maxY);

        for (int attempt = 0; attempt < 12; attempt++) {
            double offsetX = (this.random.nextDouble() * 2.0 - 1.0) * 12.0;
            double offsetZ = (this.random.nextDouble() * 2.0 - 1.0) * 12.0;
            double offsetY = (this.random.nextDouble() * 2.0 - 1.0) * 6.0;

            double targetX = this.getX() + offsetX;
            double targetZ = this.getZ() + offsetZ;
            double targetY = MathHelper.clamp(preferredY + offsetY, minY, maxY);

            if (isVoidBelow(targetX, targetY, targetZ)) {
                return new Vec3d(targetX, targetY, targetZ);
            }
        }

        double failsafeY = Math.max(minY, Math.min(maxY, preferredY));
        return new Vec3d(this.getX(), failsafeY, this.getZ());
    }

    private BlockPos findIslandSurface(double x, double z, int bottomY) {
        World world = this.getEntityWorld();
        BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, BlockPos.ofFloored(x, 0, z));

        if (topPos.getY() <= bottomY) {
            return null;
        }

        BlockPos surfacePos = topPos.down();
        if (!world.getBlockState(surfacePos).isAir()) {
            return surfacePos;
        }

        BlockPos.Mutable mutable = surfacePos.mutableCopy();
        for (int i = 0; i < SURFACE_FALLBACK_SCAN && mutable.getY() > bottomY; i++) {
            if (!world.getBlockState(mutable).isAir()) {
                return mutable.toImmutable();
            }
            mutable.move(Direction.DOWN);
        }

        return null;
    }

    private Direction findEdgeDirection(BlockPos surfacePos, int bottomY) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (this.hasSteepDrop(surfacePos, direction, bottomY)) {
                return direction;
            }
        }
        return null;
    }

    private boolean hasSteepDrop(BlockPos surfacePos, Direction direction, int bottomY) {
        World world = this.getEntityWorld();
        BlockPos.Mutable checkPos = new BlockPos.Mutable(
            surfacePos.getX() + direction.getOffsetX(),
            surfacePos.getY(),
            surfacePos.getZ() + direction.getOffsetZ()
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
        World world = this.getEntityWorld();
        BlockPos.Mutable pos = new BlockPos.Mutable(
            MathHelper.floor(x),
            MathHelper.floor(y),
            MathHelper.floor(z)
        );

        for (int i = 0; i < 32 && pos.getY() >= bottomY; i++) {
            if (!world.getBlockState(pos).isAir()) {
                return pos.toImmutable();
            }
            pos.move(Direction.DOWN);
        }

        return null;
    }

    private Vec3d findSurfaceHoverTarget() {
        World world = this.getEntityWorld();
        int bottomY = world.getBottomY();

        BlockPos baseSurface = this.findSurfaceDirectlyBelow(this.getX(), this.getY(), this.getZ(), bottomY);
        if (baseSurface != null) {
            double offset = (SURFACE_HOVER_MIN_OFFSET + SURFACE_HOVER_MAX_OFFSET) * 0.5;
            double candidateBase = baseSurface.getY() + offset;
            if (Double.isNaN(this.surfaceHoverBaseY)) {
                this.surfaceHoverBaseY = candidateBase;
            } else if (Math.abs(candidateBase - this.surfaceHoverBaseY) <= 3.5) {
                this.surfaceHoverBaseY = MathHelper.lerp(0.35, this.surfaceHoverBaseY, candidateBase);
            } else {
                this.surfaceHoverBaseY = candidateBase;
            }
        }
        double referenceBase = this.surfaceHoverBaseY;

        for (int attempt = 0; attempt < SURFACE_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * MathHelper.TAU;
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
                referenceBase = MathHelper.lerp(lerpFactor, referenceBase, targetY);
                this.surfaceHoverBaseY = referenceBase;

                double clampRange = 1.4;
                targetY = MathHelper.clamp(targetY, referenceBase - clampRange, referenceBase + clampRange);
            } else {
                referenceBase = targetY;
                this.surfaceHoverBaseY = targetY;
            }

            if (!this.isVoidBelow(targetX, targetY, targetZ)) {
                Vec3d jitter = new Vec3d(
                    (this.random.nextDouble() - 0.5) * 1.2,
                    (this.random.nextDouble() - 0.5) * 0.3,
                    (this.random.nextDouble() - 0.5) * 1.2
                );
                return new Vec3d(targetX, targetY, targetZ).add(jitter);
            }
        }

        return null;
    }

    private float approachAngle(float current, float target, float maxChange) {
        float difference = MathHelper.wrapDegrees(target - current);
        float clamped = MathHelper.clamp(difference, -maxChange, maxChange);
        return current + clamped;
    }

    @Override
    public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
        if (source.isOf(DamageTypes.FALL)) {
            return true;
        }
        return super.isInvulnerableTo(world, source);
    }

    @Override
    public boolean handleFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
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
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModItems.VOID_TARDIGRADE_SPAWN_EGG);
    }

    public Vec3d getHoverTarget() {
        return this.hoverTarget;
    }

    public boolean hasVoidBelow() {
        return isVoidBelow(this.getX(), this.getY(), this.getZ());
    }

    public float getBodyBobPhase() {
        return this.bodyBobPhase;
    }

    public float getHorizontalFlightSpeed() {
        return (float)this.getVelocity().horizontalLength();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        EtherealOrbEntity chasedOrb = this.getChasingOrb();
        PlayerEntity killer = getPlayerFromDamageSource(damageSource);
        super.onDeath(damageSource);
        if (!this.getEntityWorld().isClient()
            && killer != null
            && chasedOrb != null
            && chasedOrb.isAlive()
            && !chasedOrb.isRemoved()
            && !chasedOrb.isTamed()
            && chasedOrb.getEntityWorld() == this.getEntityWorld()) {
            chasedOrb.tameBy(killer);
        }
    }

    @Nullable
    private static PlayerEntity getPlayerFromDamageSource(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player) {
            return player;
        }
        if (attacker instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof PlayerEntity playerOwner) {
                return playerOwner;
            }
        }
        return null;
    }

    public static DefaultAttributeContainer.Builder createVoidTardigradeAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 16.0D)
            .add(EntityAttributes.FLYING_SPEED, 0.35D)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.2D)
            .add(EntityAttributes.FOLLOW_RANGE, 32.0D);
    }

    private void consumeOrb(EtherealOrbEntity orb) {
        if (orb == null || orb.isRemoved()) {
            return;
        }
        World world = this.getEntityWorld();
        
        // If this orb was trapping us, clear the trapped state before discarding it
        // This ensures we can move again after eating it
        if (this.isTrapped() && orb.isTamed()) {
            this.setTrapped(false);
        }
        
        // Get the orb's custom name BEFORE discarding it (important!)
        // Use getName() which returns the custom name if present, otherwise the default name
        Text orbNameText = orb.getName();
        String orbNameString = orbNameText.getString();
        boolean hasCustomName = orb.hasCustomName();
        
        orb.discard();
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT.value(), SoundCategory.NEUTRAL, 0.9F, 0.9F + this.random.nextFloat() * 0.25F);
        
        // Create the Tard Shell Bit item
        ItemStack bitStack = new ItemStack(ModItems.TARD_SHELL_BIT);
        
        // If the orb had a custom name, name the bit after it (e.g., "Bob Bit")
        if (hasCustomName && orbNameString != null && !orbNameString.isEmpty()) {
            // Combine the orb's name with " Bit"
            Text bitName = Text.literal(orbNameString + " Bit");
            bitStack.set(DataComponentTypes.CUSTOM_NAME, bitName);
        }
        
        this.dropStack(serverWorld, bitStack);
        this.setChasingOrb(null);
    }

    public void setChasingOrb(@Nullable EtherealOrbEntity orb) {
        if (orb == null || !orb.isAlive() || orb.isRemoved()) {
            this.chasingOrb = null;
        } else {
            this.chasingOrb = orb;
            this.hoverTarget = null;
            this.hoverCooldown = 0;
        }
    }

    @Nullable
    public EtherealOrbEntity getChasingOrb() {
        return this.chasingOrb;
    }
    
    public void setTrapped(boolean trapped) {
        this.isTrapped = trapped;
        if (trapped) {
            this.setVelocity(Vec3d.ZERO);
            this.getNavigation().stop();
        } else {
            // When no longer trapped, allow movement again
            // The normal movement code in tickMovement will handle it from here
        }
    }
    
    public boolean isTrapped() {
        return this.isTrapped;
    }

    private static final class ConsumeEtherealOrbGoal extends net.minecraft.entity.ai.goal.Goal {
        private static final double SEARCH_RANGE = 18.0;
        private static final double CONSUME_DISTANCE_SQ = 1.2;
        private static final double APPROACH_SPEED = 1.6;

        private final VoidTardigradeEntity tardigrade;
        private EtherealOrbEntity target;
        private int cooldown;

        ConsumeEtherealOrbGoal(VoidTardigradeEntity tardigrade) {
            this.tardigrade = tardigrade;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            if (this.tardigrade.getEntityWorld().isClient()) {
                return false;
            }
            List<EtherealOrbEntity> candidates = this.tardigrade.getEntityWorld().getEntitiesByClass(
                EtherealOrbEntity.class,
                this.tardigrade.getBoundingBox().expand(SEARCH_RANGE),
                orb -> orb != null && orb.isAlive() && !orb.isRemoved()
            );
            if (candidates.isEmpty()) {
                return false;
            }
            EtherealOrbEntity closest = null;
            double closestSq = Double.MAX_VALUE;
            for (EtherealOrbEntity orb : candidates) {
                double distSq = this.tardigrade.squaredDistanceTo(orb);
                if (distSq < closestSq) {
                    closestSq = distSq;
                    closest = orb;
                }
            }
            if (closest == null || closestSq > SEARCH_RANGE * SEARCH_RANGE) {
                return false;
            }
            this.target = closest;
            this.tardigrade.setChasingOrb(closest);
            return true;
        }

        @Override
        public boolean shouldContinue() {
            // Stop chasing if target is trapped (being boxed by another orb) or no longer valid
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
            this.tardigrade.setChasingOrb(null);
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            this.tardigrade.getLookControl().lookAt(this.target, 45.0F, 45.0F);
            this.tardigrade.getMoveControl().moveTo(
                this.target.getX(),
                this.target.getY(),
                this.target.getZ(),
                APPROACH_SPEED
            );

            double distSq = this.tardigrade.squaredDistanceTo(this.target);
            if (distSq <= CONSUME_DISTANCE_SQ) {
                // Consume the orb (both tamed and untamed)
                // If it's tamed and boxing us, the trapped state will be cleared in consumeOrb
                this.tardigrade.consumeOrb(this.target);
                this.target = null;
                this.cooldown = 60;
                this.tardigrade.getNavigation().stop();
            }
        }
    }

    private static final class HoverOverVoidGoal extends net.minecraft.entity.ai.goal.Goal {
        private final VoidTardigradeEntity tardigrade;

        HoverOverVoidGoal(VoidTardigradeEntity tardigrade) {
            this.tardigrade = tardigrade;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public boolean shouldContinue() {
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

