package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import com.theendupdate.entity.renderer.feature.EtherealOrbGlowFeatureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;


public class EtherealOrbEntityRenderer extends MobRenderer<EtherealOrbEntity, EtherealOrbRenderState, EtherealOrbEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/ethereal_orb.png");
    private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/ethereal_orb_emissive.png");
    
    public EtherealOrbEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new EtherealOrbEntityModel(context.bakeLayer(EtherealOrbEntityModel.ETHEREAL_ORB_LAYER)), 0.3f);
        this.addLayer(new EtherealOrbGlowFeatureRenderer(this, GLOW_TEXTURE));
    }
    
    @Override
    public EtherealOrbRenderState createRenderState() {
        return new EtherealOrbRenderState();
    }
    
    @Override
    public void extractRenderState(EtherealOrbEntity entity, EtherealOrbRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.moveAnimationState.copyFrom(entity.moveAnimationState);
        state.finishmovementAnimationState.copyFrom(entity.finishmovementAnimationState);
        state.rotateAnimationState.copyFrom(entity.rotateAnimationState);
        state.charged = entity.isCharged();
        state.baby = entity.isBaby();
        state.stunted = entity.isStunted();
        state.bulbPresent = entity.hasBulb();
    }
    
    @Override
    public Identifier getTextureLocation(EtherealOrbRenderState state) {
        return TEXTURE;
    }

    @Override
    public void submit(EtherealOrbRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue, CameraRenderState cameraState) {
        if (state.baby) {
            matrices.pushPose();
            matrices.scale(0.6f, 0.6f, 0.6f);
            super.submit(state, matrices, commandQueue, cameraState);
            matrices.popPose();
            return;
        }
        super.submit(state, matrices, commandQueue, cameraState);
    }

    // label logic handled via mixin to core renderer, not overridden here
}
