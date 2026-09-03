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

/** Tiny stationary flicker using two sprites from the atlas set */
@Environment(EnvType.CLIENT)
public class TetherTrailParticle extends SingleQuadParticle {

    private static final int FRAME_COUNT = 2;

    private final TextureAtlasSprite[] frames;

    private TetherTrailParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider) {
        super(world, x, y, z, spriteProvider.first());
        this.frames = new TextureAtlasSprite[FRAME_COUNT];
        int spriteMaxAge = Math.max(1, FRAME_COUNT - 1);
        for (int i = 0; i < FRAME_COUNT; i++) {
            this.frames[i] = spriteProvider.get(i, spriteMaxAge);
        }
        this.setLifetime(6);
        this.setParticleSpeed(0.0, 0.0, 0.0);
        this.gravity = 0.0f;
        this.quadSize = 0.28f;
        this.setAlpha(0.42f);
        this.setSprite(this.frames[0]);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.frames[this.age % FRAME_COUNT]);
            this.setAlpha(0.22f + 0.28f * (1.0f - (float) this.age / (float) this.lifetime));
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
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, RandomSource random) {
            return new TetherTrailParticle(world, x, y, z, this.spriteProvider);
        }
    }
}
