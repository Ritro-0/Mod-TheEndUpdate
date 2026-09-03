package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.TetherlingEntity;
import com.theendupdate.entity.model.TetherlingEntityModel;
import com.theendupdate.entity.state.TetherlingRenderState;
import com.theendupdate.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class TetherlingEntityRenderer extends MobRenderer<TetherlingEntity, TetherlingRenderState, TetherlingEntityModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/tetherling.png");

    public TetherlingEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new TetherlingEntityModel(context.bakeLayer(TetherlingEntityModel.LAYER_LOCATION)), 0.4f);
    }

    @Override
    public TetherlingRenderState createRenderState() {
        return new TetherlingRenderState();
    }

    @Override
    protected RenderType getRenderType(TetherlingRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderTypes.entityTranslucentEmissive(getTextureLocation(state));
    }

    @Override
    protected int getModelTint(TetherlingRenderState state) {
        return ARGB.opaque(super.getModelTint(state));
    }

    @Override
    public void extractRenderState(TetherlingEntity entity, TetherlingRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        byte phase = entity.getSyncPhase();
        state.tentacleYeet = 0.0F;

        float distToPlayer = 0.0F;
        float pitchToPlayer = 0.0F;
        float yawToPlayer = 0.0F;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            distToPlayer = entity.distanceTo(mc.player);

            double dx = mc.player.getX() - entity.getX();
            double dy = mc.player.getEyeY() - entity.getY();
            double dz = mc.player.getZ() - entity.getZ();

            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            pitchToPlayer = (float) Math.atan2(dy, horizontalDistance);
            yawToPlayer = (float) Math.atan2(-dx, dz);
        }

        state.distanceToPlayer = distToPlayer;
        state.pitchToPlayer = pitchToPlayer;
        state.yawToPlayer = yawToPlayer;

        final float BASE_REACH = 2.0F;
        final float MAX_REACH = 12.0F;

        if (phase == TetherlingEntity.SYNC_PHASE_CHARGE) {
            float targetReach = Math.min(distToPlayer, MAX_REACH);
            float reachScale = targetReach / BASE_REACH;
            state.tentacleExtend = Math.max(1.0F, reachScale * 1.2F);
        } else if (phase == TetherlingEntity.SYNC_PHASE_LIFTOFF) {
            float targetReach = Math.min(distToPlayer, MAX_REACH);
            float reachScale = targetReach / BASE_REACH;
            state.tentacleExtend = Math.max(1.5F, reachScale * 1.3F);
        } else if (phase == TetherlingEntity.SYNC_PHASE_THROW) {
            float targetReach = Math.min(distToPlayer, MAX_REACH);
            float reachScale = targetReach / BASE_REACH;
            state.tentacleExtend = Math.max(1.5F, reachScale * 1.3F);
            int aux = entity.getAnimAuxTicks();
            float snap = Mth.clamp(aux / (float) TetherlingEntity.THROW_ANIM_TICKS, 0.0F, 1.0F);
            state.tentacleYeet = snap * snap;
        } else if (phase == TetherlingEntity.SYNC_PHASE_RECOVER) {
            int aux = entity.getAnimAuxTicks();
            float t = Mth.clamp(aux / (float) TetherlingEntity.RECOVER_ANIM_TICKS, 0.0F, 1.0F);
            t = t * t * (3.0F - 2.0F * t);
            float targetReach = Math.min(distToPlayer, MAX_REACH);
            float reachScale = targetReach / BASE_REACH;
            float fullExtend = Math.max(1.5F, reachScale * 1.3F);
            state.tentacleExtend = Mth.lerp(1.0F - t, 0.0F, fullExtend);
        } else if (phase == TetherlingEntity.SYNC_PHASE_APPROACH) {
            float targetReach = Math.min(distToPlayer * 0.8F, MAX_REACH * 0.6F);
            state.tentacleExtend = Math.max(0.5F, targetReach / BASE_REACH);
        } else if (phase == TetherlingEntity.SYNC_PHASE_STARE) {
            float targetReach = Math.min(distToPlayer * 0.4F, MAX_REACH * 0.3F);
            state.tentacleExtend = Math.max(0.2F, targetReach / BASE_REACH);
        } else {
            state.tentacleExtend = 0.0F;
        }

        float bobPhase = entity.tickCount + tickDelta;
        state.hoverBob = Mth.sin(bobPhase * 0.08F) * 0.04F;

        int recoverAux = entity.getAnimAuxTicks();
        if (shouldShowTentacleTrail(phase, recoverAux)) {
            Player trailTarget = resolveTrailTarget(mc, entity);
            if (trailTarget != null) {
                spawnTentacleParticleTrails(mc, entity, trailTarget);
            }
        }
    }

    private static boolean shouldShowTentacleTrail(byte phase, int recoverAux) {
        if (phase == TetherlingEntity.SYNC_PHASE_CHARGE
            || phase == TetherlingEntity.SYNC_PHASE_LIFTOFF
            || phase == TetherlingEntity.SYNC_PHASE_THROW) {
            return true;
        }
        if (phase == TetherlingEntity.SYNC_PHASE_RECOVER) {
            return recoverAux > TetherlingEntity.RECOVER_ANIM_TICKS - 10;
        }
        return false;
    }

    private static Player resolveTrailTarget(Minecraft mc, TetherlingEntity entity) {
        if (mc.level == null) {
            return null;
        }
        Player closest = null;
        double best = 64 * 64;
        for (AbstractClientPlayer p : mc.level.players()) {
            double d = entity.distanceToSqr(p);
            if (d <= best) {
                best = d;
                closest = p;
            }
        }
        return closest != null ? closest : mc.player;
    }

    private static void spawnTentacleParticleTrails(Minecraft mc, TetherlingEntity entity, Player player) {
        ParticleStatus mode = mc.options.particles().get();
        if (mode == ParticleStatus.MINIMAL) {
            return;
        }
        ClientLevel world = mc.level;
        if (world == null) {
            return;
        }

        long worldTime = world.getGameTime();
        if ((worldTime + entity.getId()) % 3 != 0) {
            return;
        }

        double px = player.getX();
        double py = player.getEyeY() - 0.38;
        double pz = player.getZ();

        double ex = entity.getX();
        double ey = entity.getY() - 0.35;
        double ez = entity.getZ();

        double[][] offsets = {
            {-0.45, -0.3}, {0.45, -0.3},
            {0.0, 0.15}
        };
        for (double[] off : offsets) {
            spawnSparseLine(world, mc, ex + off[0], ey, ez + off[1], px, py, pz, mode);
        }
    }

    private static void spawnSparseLine(ClientLevel world, Minecraft mc,
        double x0, double y0, double z0, double x1, double y1, double z1, ParticleStatus mode) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.min(10, Math.max(1, (int) Math.ceil(len / 2.2)));
        float skipChance = mode == ParticleStatus.DECREASED ? 0.94f : 0.88f;
        for (int i = 0; i <= steps; i++) {
            if (world.getRandom().nextFloat() < skipChance) {
                continue;
            }
            double t = (double) i / (double) steps;
            double x = x0 + dx * t;
            double y = y0 + dy * t;
            double z = z0 + dz * t;
            mc.particleEngine.createParticle(ModParticles.TETHER_TRAIL, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public Identifier getTextureLocation(TetherlingRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(TetherlingRenderState state, PoseStack matrices, float animationProgress, float bodyYaw) {
        super.setupRotations(state, matrices, animationProgress, bodyYaw);
    }

    private static final float ALIGN_Y = 0.1875F;

    @Override
    public void submit(TetherlingRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue,
        CameraRenderState cameraState) {
        matrices.pushPose();
        matrices.translate(0.0, state.hoverBob + ALIGN_Y, 0.0);
        super.submit(state, matrices, commandQueue, cameraState);
        matrices.popPose();
    }
}
