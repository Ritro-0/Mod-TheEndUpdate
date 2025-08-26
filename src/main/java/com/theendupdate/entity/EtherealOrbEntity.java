package com.theendupdate.entity;

import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.text.Text;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;

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
    public EtherealOrbEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 30, true); // Doubled from 15 to 30
        this.setNoGravity(true);
    }
    
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    
    @Override
    public boolean isInAir() {
        return !this.isOnGround() && !this.isTouchingWater();
    }
    
    // Be invulnerable to fall and in-wall damage
    @Override
    public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
        if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.IN_WALL)) return true;
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
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MaintainHomeGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(3, new LookAroundGoal(this));
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
        // Ensure we never accumulate fall distance and never show a name
        this.fallDistance = 0.0F;
        // On initial ticks after spawn, scrub any default or blank custom name applied by spawn flow
        if (this.age < 5) {
            net.minecraft.text.Text cn = this.getCustomName();
            if (cn != null) {
                String cs = cn.getString();
                if (cs.isBlank() || "Ethereal Orb".equals(cs)) {
                    this.setCustomName(null);
                    this.setCustomNameVisible(false);
                }
            }
        }
        // If touching ground or colliding downward, gently lift to avoid floor damage/stuck
        if ((this.isOnGround() || this.verticalCollision) && this.getVelocity().y <= 0.0) {
            this.setVelocity(this.getVelocity().x, 0.25, this.getVelocity().z);
            this.setPosition(this.getX(), this.getY() + 0.05, this.getZ());
        }
    }

    private void updateAnimations() {
        if (isGoingForward()) {
            this.finishmovementAnimationState.stop();
            this.moveAnimationState.startIfNotRunning(this.age);
        } else {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.startIfNotRunning(this.age);
        }
    }

    private boolean isGoingForward() {
        return this.getVelocity().z > 0; 
    }

    /**
     * Maintain a persistent crystal "home"; if none found, wander and continuously scan.
     */
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
            double speed = d2 > 64 ? 3.75 : 2.7;

            HitResult hit = orb.getWorld().raycast(new RaycastContext(
                cur, desired, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult bhr = (BlockHitResult) hit;
                BlockPos hp = bhr.getBlockPos();
                desired = new Vec3d(desired.x, Math.max(desired.y, hp.getY() + 2.5), desired.z);
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

            orb.getMoveControl().moveTo(desired.x, desired.y, desired.z, speed);
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
