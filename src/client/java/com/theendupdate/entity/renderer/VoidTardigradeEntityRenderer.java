package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.VoidTardigradeEntity;
import com.theendupdate.entity.model.VoidTardigradeEntityModel;
import com.theendupdate.entity.state.VoidTardigradeRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class VoidTardigradeEntityRenderer extends MobRenderer<VoidTardigradeEntity, VoidTardigradeRenderState, VoidTardigradeEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/void_tardigrade.png");

    public VoidTardigradeEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidTardigradeEntityModel(context.bakeLayer(VoidTardigradeEntityModel.LAYER_LOCATION)), 0.35f);
    }

    @Override
    public VoidTardigradeRenderState createRenderState() {
        return new VoidTardigradeRenderState();
    }

    @Override
    public void extractRenderState(VoidTardigradeEntity entity, VoidTardigradeRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.horizontalSpeed = entity.getHorizontalFlightSpeed();
        state.animationSeed = (entity.getId() % 97) / 97.0f;
        float bobPhase = entity.getBodyBobPhase() + tickDelta * 0.08F;
        state.hoverBob = Mth.sin(bobPhase) * 0.06F;
    }

    @Override
    public Identifier getTextureLocation(VoidTardigradeRenderState state) {
        return TEXTURE;
    }

    @Override
    public void submit(VoidTardigradeRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue, CameraRenderState cameraState) {
        matrices.pushPose();
        matrices.translate(0.0, state.hoverBob, 0.0);
        matrices.scale(1.5F, 1.5F, 1.5F);
        matrices.translate(0.0, 0.1F, 0.0);
        matrices.mulPose(new Quaternionf().rotationY(Mth.HALF_PI));
        super.submit(state, matrices, commandQueue, cameraState);
        matrices.popPose();
    }
}

