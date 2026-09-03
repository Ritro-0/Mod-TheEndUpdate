package com.theendupdate.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Campfire-like smoke plume: rises fast, fades, grows, advances texture frames by age */
@Environment(EnvType.CLIENT)
public class NebulaVentSmokeParticle extends SingleQuadParticle {

    private static final int FRAME_COUNT = 4;

    private final TextureAtlasSprite[] frames;
    private final float baseQuadSize;

    private NebulaVentSmokeParticle(
        ClientLevel world,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        RandomSource random,
        SpriteSet spriteProvider
    ) {
        super(world, x, y, z, spriteProvider.first());

        this.frames = new TextureAtlasSprite[FRAME_COUNT];
        int spriteMaxAge = Math.max(1, FRAME_COUNT - 1);
        for (int i = 0; i < FRAME_COUNT; i++) {
            this.frames[i] = spriteProvider.get(i, spriteMaxAge);
        }

        this.setLifetime(22 + random.nextInt(10));
        this.setParticleSpeed(velocityX * 0.6, velocityY * 3.0, velocityZ * 0.6);
        this.gravity = 0.0f;

        this.baseQuadSize = 0.45f + random.nextFloat() * 0.21f;
        this.quadSize = this.baseQuadSize;
        this.setAlpha(0.55f);

        this.setSprite(this.frames[0]);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float progress = (float) this.age / (float) this.lifetime;
        int frameIndex = Math.min(this.frames.length - 1, (int) (progress * this.frames.length));
        this.setSprite(this.frames[frameIndex]);

        float fade = 1.0f - progress;
        this.quadSize = this.baseQuadSize * (0.45f + 0.94f * progress);
        this.setAlpha(0.05f + 0.50f * fade * fade);

        this.xd *= 0.96;
        this.zd *= 0.96;
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
            return new NebulaVentSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ, random, this.spriteProvider);
        }
    }
}
