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
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
import com.theendupdate.TemplateMod;
import net.minecraft.particle.ParticleTypes;

/**
 * EtherealOrbEntity - A floating, glowing orb creature that inhabits The End
 * 
 * Key features:
 * - Floats through the air (extends PathAwareEntity with no gravity)
 * - Emits light particles
 * - Passive behavior, but flees when hurt
 * - Can pass through certain blocks
 */
public class EtherealOrbEntity extends AnimalEntity implements Flutterer {
    public EtherealOrbEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 15, true);
        this.setNoGravity(true);
    }
    public final AnimationState moveAnimationState = new AnimationState();
    public final AnimationState finishmovementAnimationState = new AnimationState();
    @Override
    public boolean isInAir() {
        return !this.isOnGround() && !this.isTouchingWater();
    }
    @Override
    public boolean isBreedingItem(ItemStack stack) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isBreedingItem'");
    }
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createChild'");
    }
public static DefaultAttributeContainer.Builder createEtherealOrbAttributes() {
    return AnimalEntity.createMobAttributes()
    .add(EntityAttributes.MAX_HEALTH, 3.5)
    .add(EntityAttributes.MOVEMENT_SPEED, 0.2)
    .add(EntityAttributes.FLYING_SPEED, 0.35)
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
        this.goalSelector.add(1, new SeekCrystalsGoal(this, 1800, 12)); // ~90s, 12-block comfort radius
        this.goalSelector.add(2, new RandomFlyGoal(this, 1.0));
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
    public void tick()
    {
        super.tick();
        this.updateAnimations();

    }

    private void updateAnimations()
    {
        if (isGoingForward())
        {
            this.finishmovementAnimationState.stop();
            this.moveAnimationState.startIfNotRunning(this.age);
        }
        else
        {
            this.moveAnimationState.stop();
            this.finishmovementAnimationState.startIfNotRunning(this.age);
        }

    }

        private boolean isGoingForward()
        {
            return this.getVelocity().z > 0; 
}

/**
 * Simple goal to make the orb wander in the air around its current position.
 */
class RandomFlyGoal extends Goal {
    private final EtherealOrbEntity orb;
    private final double speed;
    private double targetX;
    private double targetY;
    private double targetZ;

    RandomFlyGoal(EtherealOrbEntity orb, double speed) {
        this.orb = orb;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (orb.getTarget() != null) return false;
        if (orb.getNavigation().isFollowingPath()) return false;
        // Pick a new target sometimes
        if (orb.getRandom().nextInt(20) != 0) return false;

        Vec3d pos = orb.getPos();
        double range = 6.0;
        double dx = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;
        double dy = (orb.getRandom().nextDouble() * 2.0 - 1.0) * (range * 0.6);
        double dz = (orb.getRandom().nextDouble() * 2.0 - 1.0) * range;

        targetX = pos.x + dx;
        // Keep within a reasonable vertical band above the bottom of the world
        targetY = MathHelper.clamp(pos.y + dy, orb.getWorld().getBottomY() + 5, orb.getWorld().getBottomY() + 100);
        targetZ = pos.z + dz;
        return true;
    }

    @Override
    public void start() {
        orb.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
    }

    @Override
    public boolean shouldContinue() {
        return orb.getNavigation().isFollowingPath();
    }
}

/**
 * Periodically seeks Astral Remnant or Stellarith Crystal blocks. If none within
 * comfortRadius, scans up to ~6 nearby chunks and flies toward a spot within comfortRadius
 * of the found crystal.
 */
class SeekCrystalsGoal extends Goal {
    private final EtherealOrbEntity orb;
    private final int cooldownTicks;
    private final int comfortRadius;
    private static final int TARGET_RADIUS = 3; // pursue until within 3 blocks
    private int cooldown;

    private BlockPos targetCrystalPos;
    private BlockPos destinationPos;
    private Path currentPath;
    private boolean useDirectFlight;
    private int pursuitTimeout;

    SeekCrystalsGoal(EtherealOrbEntity orb, int cooldownTicks, int comfortRadius) {
        this.orb = orb;
        this.cooldownTicks = cooldownTicks;
        this.comfortRadius = comfortRadius;
        // Short initial cooldown for quick testing; subsequent runs use cooldownTicks
        this.cooldown = orb.getRandom().nextInt(60); // ~3s initial window
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (orb.getWorld().isClient) return false;
        if (orb.getTarget() != null) return false;
        if (cooldown > 0) { cooldown--; return false; }
        cooldown = cooldownTicks;

        // Already satisfied (within 12 blocks)
        if (isNearCrystal(comfortRadius)) {
            TemplateMod.LOGGER.info("[EtherealOrb][seek] {} SKIP reason=already-near radius={}",
                orb.getUuid(), comfortRadius);
            return false;
        }

        // Locate a crystal
        BlockPos found = findNearbyCrystal();
        if (found == null) found = scanChunksForCrystal(6);
        if (found == null) {
            TemplateMod.LOGGER.info("[EtherealOrb][seek] {} SKIP reason=no-crystal-found", orb.getUuid());
            return false;
        }
        this.targetCrystalPos = found;

        // Compute a destination within ~3 blocks of the crystal, along the approach line
        Vec3d from = orb.getPos();
        Vec3d toCrystal = Vec3d.ofCenter(found);
        Vec3d dir = toCrystal.subtract(from);
        double dist = Math.max(dir.length(), 0.001);
        Vec3d dest = toCrystal.add(dir.normalize().multiply(-Math.max(TARGET_RADIUS - 0.5, 1.5)));
        this.destinationPos = BlockPos.ofFloored(dest);

        // Always use direct flight (no walking/path nodes)
        this.currentPath = null;
        this.useDirectFlight = true;
        this.pursuitTimeout = 20 * 20; // 20s safety timeout
        TemplateMod.LOGGER.info("[EtherealOrb][seek] {} START crystal={} dest={}", orb.getUuid(), this.targetCrystalPos, this.destinationPos);
        return true;
    }

    @Override
    public void start() {
        // Ensure walking/pathing doesn't engage
        orb.getNavigation().stop();
    }

    @Override
    public boolean shouldContinue() {
        // Stop only when we are within 3 blocks of a crystal
        if (isNearCrystal(TARGET_RADIUS)) return false;
        if (this.pursuitTimeout-- <= 0) {
            TemplateMod.LOGGER.info("[EtherealOrb][seek] {} STOP reason=timeout", orb.getUuid());
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        this.targetCrystalPos = null;
        this.destinationPos = null;
        this.currentPath = null;
        this.useDirectFlight = false;
        TemplateMod.LOGGER.info("[EtherealOrb][seek] {} STOP reason=finished/reset", orb.getUuid());
    }

    @Override
    public void tick() {
        if (this.destinationPos == null) return;
        if (useDirectFlight) {
            double tx = destinationPos.getX() + 0.5;
            double ty = destinationPos.getY() + 0.5;
            double tz = destinationPos.getZ() + 0.5;
            double dx = tx - orb.getX();
            double dy = ty - orb.getY();
            double dz = tz - orb.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            double speed = distSq > 64 ? 1.25 : 0.9;
            orb.getMoveControl().moveTo(tx, ty, tz, speed);
            orb.getLookControl().lookAt(tx, ty, tz);
            if (!orb.getWorld().isClient && orb.getWorld() instanceof ServerWorld server) {
                if ((orb.age % 20) == 0) {
                    server.spawnParticles(ParticleTypes.END_ROD, tx, ty, tz, 6, 0.2, 0.2, 0.2, 0.01);
                    TemplateMod.LOGGER.info("[EtherealOrb][seek] {} TICK dest={} dist={}", orb.getUuid(), this.destinationPos, String.format("%.2f", Math.sqrt(distSq)));
                }
            }
        }
    }

    private boolean isTargetBlock(BlockPos pos) {
        var state = orb.getWorld().getBlockState(pos);
        return state.isOf(ModBlocks.ASTRAL_REMNANT) || state.isOf(ModBlocks.STELLARITH_CRYSTAL);
    }

    private boolean isNearCrystal(int radius) {
        BlockPos origin = orb.getBlockPos();
        int r = radius;
        for (BlockPos p : BlockPos.iterateOutwards(origin, r, r, r)) {
            if (origin.getSquaredDistance(p) > (long) r * r) continue;
            if (isTargetBlock(p)) return true;
        }
        return false;
    }

    private BlockPos findNearbyCrystal() {
        BlockPos origin = orb.getBlockPos();
        int r = Math.min(24, comfortRadius * 2);
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
