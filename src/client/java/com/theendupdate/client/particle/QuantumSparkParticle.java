package com.theendupdate.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Small helix spark used along quantum gateway beams */
@Environment(EnvType.CLIENT)
public class QuantumSparkParticle extends SingleQuadParticle {

    private static final int FRAME_COUNT = 2;
    private static final float ORBIT_ANGULAR_SPEED = 0.17f;
    private static final double RISE_PER_TICK = 1.0 / 37.0;
    private static final int FLICKER_STEP_TICKS = 4;

    private final TextureAtlasSprite[] frames;
    private final float baseQuadSize;
    private final float baseAlpha;
    private final int flickerStepTicks;

    private final double centerX;
    private final double centerZ;
    private final double startY;
    private final int targetTopY;
    private final int spritePhaseOffset;
    private final BlockPos gatewayPos;

    private final double radius;
    private float angle;
    private final float angularSpeed;

    private final double yVelocityPerTick;
    private int shutdownCountdown = -1;

    private QuantumSparkParticle(
        ClientLevel world,
        double x,
        double y,
        double z,
        double centerX,
        double targetTopY,
        double centerZ,
        RandomSource random,
        SpriteSet spriteProvider
    ) {
        super(world, x, y, z, spriteProvider.first());

        this.frames = new TextureAtlasSprite[FRAME_COUNT];
        int spriteMaxAge = Math.max(1, FRAME_COUNT - 1);
        for (int i = 0; i < FRAME_COUNT; i++) {
            this.frames[i] = spriteProvider.get(i, spriteMaxAge);
        }

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.startY = y;
        int top = (int) Math.floor(targetTopY);
        this.targetTopY = top;
        this.spritePhaseOffset = (targetTopY - (double) top) > 0.25 ? 1 : 0;
        this.gatewayPos = BlockPos.containing(this.centerX, this.startY - 1.02, this.centerZ);

        double dx = x - this.centerX;
        double dz = z - this.centerZ;
        this.radius = Math.sqrt(dx * dx + dz * dz);
        this.angle = (float) Math.atan2(dz, dx);

        this.baseQuadSize = (0.16f + random.nextFloat() * 0.12f) * 0.5f;
        this.baseAlpha = 0.68f + random.nextFloat() * 0.12f;

        this.angularSpeed = ORBIT_ANGULAR_SPEED;
        this.flickerStepTicks = FLICKER_STEP_TICKS;
        this.yVelocityPerTick = RISE_PER_TICK;

        double dy = (double) this.targetTopY - this.startY;
        int lifetimeTicks = (int) Math.max(1, Math.ceil(dy / this.yVelocityPerTick));
        this.setLifetime(lifetimeTicks);

        this.setParticleSpeed(0.0, 0.0, 0.0);
        this.gravity = 0.0f;

        this.quadSize = this.baseQuadSize;
        this.setAlpha(this.baseAlpha);
        this.setSprite(this.frames[0]);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float progress = (float) this.age / (float) this.lifetime;

        int flickerStep = this.age / Math.max(1, this.flickerStepTicks);
        int baseSprite = flickerStep & 1;
        int spriteIndex = this.spritePhaseOffset == 0 ? baseSprite : (1 - baseSprite);
        this.setSprite(this.frames[spriteIndex]);
        this.setColor(1.0f, 1.0f, 1.0f);

        this.angle += this.angularSpeed;
        this.x = this.centerX + Math.cos(this.angle) * this.radius;
        this.z = this.centerZ + Math.sin(this.angle) * this.radius;
        this.y += this.yVelocityPerTick;

        float fade = 1.0f - progress;
        float alpha = this.baseAlpha * (float) Math.pow(Math.max(0.0f, fade), 0.15f);
        this.setAlpha(alpha);

        this.quadSize = this.baseQuadSize * (0.98f + 0.08f * fade);

        // clear newest -> oldest on power loss: lower age ratio means shorter countdown
        boolean powered = this.level.hasNeighborSignal(this.gatewayPos);
        if (powered) {
            if (this.shutdownCountdown < 0) {
                float ageRatio = (float) this.age / (float) Math.max(1, this.lifetime);
                this.shutdownCountdown = 2 + (int) (ageRatio * 18.0f);
            } else if (--this.shutdownCountdown <= 0) {
                this.remove();
                return;
            }
        } else {
            this.shutdownCountdown = -1;
        }
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
            SimpleParticleType type,
            ClientLevel world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            RandomSource random
        ) {
            if (!shouldSpawnForParticleSetting(random)) {
                return null;
            }
            return new QuantumSparkParticle(world, x, y, z, velocityX, velocityY, velocityZ, random, this.spriteProvider);
        }
    }

    private static boolean shouldSpawnForParticleSetting(RandomSource random) {
        try {
            Object setting = Minecraft.getInstance().options.particles().get();
            String status = String.valueOf(setting).toLowerCase(java.util.Locale.ROOT);
            if (status.contains("minimal")) {
                return random.nextFloat() < 0.15f;
            }
            if (status.contains("decreased")) {
                return random.nextFloat() < 0.45f;
            }
        } catch (Throwable ignored) {
            return true;
        }
        return true;
    }
}
