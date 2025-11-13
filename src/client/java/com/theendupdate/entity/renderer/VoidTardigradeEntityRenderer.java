package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.VoidTardigradeEntity;
import com.theendupdate.entity.model.VoidTardigradeEntityModel;
import com.theendupdate.entity.state.VoidTardigradeRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class VoidTardigradeEntityRenderer extends MobEntityRenderer<VoidTardigradeEntity, VoidTardigradeRenderState, VoidTardigradeEntityModel> {
    private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/void_tardigrade.png");

    public VoidTardigradeEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new VoidTardigradeEntityModel(context.getPart(VoidTardigradeEntityModel.LAYER_LOCATION)), 0.35f);
    }

    @Override
    public VoidTardigradeRenderState createRenderState() {
        return new VoidTardigradeRenderState();
    }

    @Override
    public void updateRenderState(VoidTardigradeEntity entity, VoidTardigradeRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.horizontalSpeed = entity.getHorizontalFlightSpeed();
        state.animationSeed = (entity.getId() % 97) / 97.0f;
        float bobPhase = entity.getBodyBobPhase() + tickDelta * 0.08F;
        state.hoverBob = MathHelper.sin(bobPhase) * 0.06F;
    }

    @Override
    public Identifier getTexture(VoidTardigradeRenderState state) {
        return TEXTURE;
    }

    @Override
    public void render(VoidTardigradeRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState) {
        matrices.push();
        matrices.translate(0.0, state.hoverBob, 0.0);
        matrices.scale(1.5F, 1.5F, 1.5F);
        matrices.translate(0.0, 0.1F, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        super.render(state, matrices, commandQueue, cameraState);
        matrices.pop();
    }
}

