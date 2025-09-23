package com.theendupdate.entity;

// removed unused imports after replacing dig particles with soul particles
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
// no direct usage; spawned via fully qualified names below
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ShadowCreakingEntity extends CreakingEntity {
    private static final int EMERGE_DURATION_TICKS = 134; // match Warden emerging duration
    private static final int LEVITATE_DURATION_TICKS = 140; // 7s total: 2s arms-out + 5s hover
    private static final double LEVITATE_SPEED_PER_TICK = 0.02; // ~2 blocks over 5s

    private static final TrackedData<Boolean> LEVITATING = DataTracker.registerData(ShadowCreakingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Pose-driven animation states (client uses these to time animations)
    public final AnimationState emergingAnimationState = new AnimationState();
    public final AnimationState diggingAnimationState = new AnimationState();
    public final AnimationState levitatingAnimationState = new AnimationState();

    private boolean playedSpawnLevitation;
    private int levitateTicksRemaining;
    private boolean waitingForPostLandFreeze;
    private int postLandFreezeTicks;
    private boolean spawnedLevitationEndermites;

    public ShadowCreakingEntity(EntityType<? extends CreakingEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 7;
    }

    public static DefaultAttributeContainer.Builder createShadowCreakingAttributes() {
        // Base on hostile default, then tweak for Warden-equivalent punch and half Warden health
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, 250.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 30.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.FOLLOW_RANGE, 32.0)
            .add(EntityAttributes.ARMOR, 2.0)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LEVITATING, Boolean.FALSE);
    }

    public boolean isLevitating() {
        return this.dataTracker.get(LEVITATING);
    }

    private void setLevitating(boolean value) {
        this.dataTracker.set(LEVITATING, value);
    }

    // Keep duplication behavior only; everything else remains vanilla
    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        for (int i = 0; i < 2; i++) {
            ShadowCreakingEntity spawn = new ShadowCreakingEntity(com.theendupdate.registry.ModEntities.SHADOW_CREAKING, sw);
            double ox = this.getX() + (this.random.nextDouble() - 0.5) * 0.6;
            double oy = this.getY();
            double oz = this.getZ() + (this.random.nextDouble() - 0.5) * 0.6;
            spawn.refreshPositionAndAngles(ox, oy, oz, this.getYaw(), this.getPitch());
            sw.spawnEntity(spawn);
        }
    }

    // Slow down hand swing so attack animation matches vanilla pacing better
    public int getHandSwingDuration() {
        return 12;
    }

    @Override
    public boolean isImmobile() {
        // Prevent movement during emergence and during the post-landing freeze period
        return super.isImmobile() || this.getPose() == EntityPose.EMERGING || this.postLandFreezeTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();

        // Drive initial emerging pose when spawned
        if (!this.isRemoved()) {
            if (this.age == 1) {
                this.setPose(EntityPose.EMERGING);
                this.playSound(SoundEvents.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
            }
            if (this.getPose() == EntityPose.EMERGING && this.age >= EMERGE_DURATION_TICKS) {
                this.setPose(EntityPose.STANDING);
            }
        }

        if (this.getPose() == EntityPose.EMERGING) {
            // Limit motion and control while emerging
            try {
                this.getNavigation().stop();
            } catch (Throwable ignored) {}
            this.setSprinting(false);
            this.setJumping(false);
            this.setVelocity(0.0, this.getVelocity().y, 0.0);
        }

        // Server: start and run post-spawn levitation immediately after emerging finishes
        if (!this.getWorld().isClient) {
            if (!this.playedSpawnLevitation && this.getPose() != EntityPose.EMERGING && this.age >= EMERGE_DURATION_TICKS) {
                this.playedSpawnLevitation = true;
                this.levitateTicksRemaining = LEVITATE_DURATION_TICKS;
                this.setLevitating(true);
                this.setNoGravity(true);
            }

            if (this.isLevitating()) {
                // Halt navigation and horizontal motion while levitating
                try { this.getNavigation().stop(); } catch (Throwable ignored) {}
                this.setSprinting(false);
                this.setJumping(false);

                // Phase timing: first 2s = arms-out only, then 5s hover upwards
                int elapsedTicks = LEVITATE_DURATION_TICKS - this.levitateTicksRemaining;
                double yMove = 0.0;
                if (elapsedTicks >= 40) {
                    yMove = LEVITATE_SPEED_PER_TICK;
                }
                // Prevent moving into ceilings
                if (!this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0.0, yMove, 0.0))) {
                    yMove = 0.0;
                }
                // Move vertically; keep horizontal still
                this.setVelocity(0.0, yMove, 0.0);
                this.velocityDirty = true;
                this.move(net.minecraft.entity.MovementType.SELF, new Vec3d(0.0, yMove, 0.0));

                // Spawn endermites at hands when arms reach full T-pose (~2s into levitation)
                if (!this.spawnedLevitationEndermites && elapsedTicks >= 40) {
                    this.spawnedLevitationEndermites = true;
                    if (this.getWorld() instanceof ServerWorld sw2) {
                        double yawRad = Math.toRadians(this.getYaw());
                        double rightX = Math.cos(yawRad);
                        double rightZ = Math.sin(yawRad);
                        double side = 0.7; // hand horizontal offset from center
                        double handY = this.getY() + 1.8; // approximate hand height

                        double leftPx = this.getX() - rightX * side;
                        double leftPz = this.getZ() - rightZ * side;
                        double rightPx = this.getX() + rightX * side;
                        double rightPz = this.getZ() + rightZ * side;

                        net.minecraft.entity.mob.EndermiteEntity m1 = new net.minecraft.entity.mob.EndermiteEntity(net.minecraft.entity.EntityType.ENDERMITE, sw2);
                        net.minecraft.entity.mob.EndermiteEntity m2 = new net.minecraft.entity.mob.EndermiteEntity(net.minecraft.entity.EntityType.ENDERMITE, sw2);
                        if (m1 != null) {
                            m1.refreshPositionAndAngles(leftPx, handY, leftPz, this.getYaw(), 0.0f);
                            sw2.spawnEntity(m1);
                        }
                        if (m2 != null) {
                            m2.refreshPositionAndAngles(rightPx, handY, rightPz, this.getYaw(), 0.0f);
                            sw2.spawnEntity(m2);
                        }
                    }
                }

                if (--this.levitateTicksRemaining <= 0) {
                    this.setLevitating(false);
                    this.setNoGravity(false);
                    // Drop back to floor with a nudge downward
                    this.setVelocity(this.getVelocity().x, Math.min(this.getVelocity().y, -0.35), this.getVelocity().z);
                    this.velocityDirty = true;
                    this.waitingForPostLandFreeze = true;
                }
            }

            // After landing, freeze and make invulnerable for 3 seconds
            if (this.waitingForPostLandFreeze && this.isOnGround()) {
                this.waitingForPostLandFreeze = false;
                this.postLandFreezeTicks = 60; // 3 seconds
                try { this.getNavigation().stop(); } catch (Throwable ignored) {}
                this.setVelocity(0.0, 0.0, 0.0);
                this.velocityDirty = true;
                this.setInvulnerable(true);
            }

            if (this.postLandFreezeTicks > 0) {
                try { this.getNavigation().stop(); } catch (Throwable ignored) {}
                this.setVelocity(0.0, 0.0, 0.0);
                this.velocityDirty = true;
                this.postLandFreezeTicks--;
                if (this.postLandFreezeTicks == 0) {
                    this.setInvulnerable(false);
                }
            }
        }

        // Client-side particles: soul swirl while EMERGING/DIGGING and during post-spawn levitation
        if (this.getWorld().isClient) {
            switch (this.getPose()) {
                case EMERGING:
                    // Near-feet swirl while emerging
                    this.addSoulSwirlParticles(this.emergingAnimationState, 0.12f, 0.40f, 12, 0.045f);
                    break;
                case DIGGING:
                    // Near-feet swirl while digging
                    this.addSoulSwirlParticles(this.diggingAnimationState, 0.12f, 0.40f, 12, 0.045f);
                    break;
                default:
                    break;
            }
            if (this.isLevitating()) {
                this.addSoulSwirlParticles(this.levitatingAnimationState, 1.2f, 0.75f, 12, 0.055f);
            }
        }
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        // Do not perform attacks while emerging
        if (this.getPose() == EntityPose.EMERGING) return false;
        if (this.isLevitating()) return false;
        if (this.postLandFreezeTicks > 0) return false;
        return super.tryAttack(world, target);
    }

    @Override
    public boolean isPushable() {
        // Prevent pushing/collisions while emerging to match immobility
        if (this.getPose() == EntityPose.EMERGING) return false;
        if (this.isLevitating()) return false;
        if (this.postLandFreezeTicks > 0) return false;
        return super.isPushable();
    }

    // Start/stop animation clocks when POSE or LEVITATING tracked data changes (vanilla pattern)
    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (Entity.POSE.equals(data)) {
            switch (this.getPose()) {
                case EMERGING:
                    this.emergingAnimationState.start(this.age);
                    break;
                case DIGGING:
                    this.diggingAnimationState.start(this.age);
                    break;
                default:
                    break;
            }
        }
        if (LEVITATING.equals(data)) {
            if (this.isLevitating()) {
                this.levitatingAnimationState.start(this.age);
            } else {
                this.levitatingAnimationState.stop();
            }
        }
        super.onTrackedDataSet(data);
    }

    private void addSoulSwirlParticles(AnimationState animationState, float yOffset, float radius, int count, float tangentialVelocity) {
        // Mirror original timing window: emit for first ~4.5s of the EMERGING/DIGGING animations; no limit for levitating state
        boolean unlimited = animationState == this.levitatingAnimationState;
        if (!unlimited && (float)animationState.getTimeInMilliseconds(this.age) >= 4500.0F) return;

        Random random = this.getRandom();
        float ms = (float)animationState.getTimeInMilliseconds(this.age);
        float base = (ms / 1000.0f) * 2.2f; // angular speed (rad/s) for swirl
        double cx = this.getX();
        double cy = this.getY() + yOffset;
        double cz = this.getZ();
        for (int i = 0; i < count; i++) {
            float t = base + (float)(i * (Math.PI * 2.0) / (float)count);
            double dx = Math.cos(t) * radius;
            double dz = Math.sin(t) * radius;
            double px = cx + dx + MathHelper.nextBetween(random, -0.05f, 0.05f);
            double py = cy + MathHelper.nextBetween(random, -0.05f, 0.05f);
            double pz = cz + dz + MathHelper.nextBetween(random, -0.05f, 0.05f);
            double vx = -Math.sin(t) * tangentialVelocity;
            double vy = 0.01 + MathHelper.nextBetween(random, -0.005f, 0.005f);
            double vz = Math.cos(t) * tangentialVelocity;
            this.getWorld().addParticleClient(ParticleTypes.SOUL, px, py, pz, vx, vy, vz);
        }
    }
}


