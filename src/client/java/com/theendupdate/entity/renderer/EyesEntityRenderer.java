package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.EyesEntity;
import com.theendupdate.entity.model.EyesEntityModel;
import com.theendupdate.entity.state.EyesRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class EyesEntityRenderer extends MobRenderer<EyesEntity, EyesRenderState, EyesEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/eyes.png");
    private static final Identifier TEXTURE_DART_1 = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/eyes_dart_1.png");
    private static final Identifier TEXTURE_DART_2 = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/eyes_dart_2.png");
    private static final float MODEL_CENTER_Y = 1.0F;

    public EyesEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new EyesEntityModel(context.bakeLayer(EyesEntityModel.LAYER_LOCATION)), 0.0f);
    }

    @Override
    public EyesRenderState createRenderState() {
        return new EyesRenderState();
    }

    @Override
    public void extractRenderState(EyesEntity entity, EyesRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.vanishStage = entity.getVanishStage();
        state.displayScale = entity.getDisplayScale();
        state.xRot = 0.0F;
        state.yRot = 0.0F;
        state.bodyRot = 0.0F;
    }

    @Override
    public Identifier getTextureLocation(EyesRenderState state) {
        if (state.vanishStage == EyesEntity.STAGE_DART_1) {
            return TEXTURE_DART_1;
        }
        if (state.vanishStage == EyesEntity.STAGE_DART_2) {
            return TEXTURE_DART_2;
        }
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(EyesRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        return RenderTypes.entityTranslucentEmissive(getTextureLocation(state));
    }

    @Override
    protected int getModelTint(EyesRenderState state) {
        return ARGB.opaque(super.getModelTint(state));
    }

    @Override
    protected void setupRotations(EyesRenderState state, PoseStack matrices, float animationProgress, float bodyYaw) {
        // billboard rotation applied in submit() from camera orientation instead
    }

    @Override
    public void submit(EyesRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue, CameraRenderState cameraState) {
        matrices.pushPose();
        matrices.translate(0.0F, MODEL_CENTER_Y, 0.0F);
        matrices.mulPose(cameraState.orientation);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
        matrices.scale(1.6F * state.displayScale, 1.6F * state.displayScale, 1.6F * state.displayScale);
        matrices.translate(0.0F, -MODEL_CENTER_Y, 0.0F);
        super.submit(state, matrices, commandQueue, cameraState);
        matrices.popPose();
    }
}
