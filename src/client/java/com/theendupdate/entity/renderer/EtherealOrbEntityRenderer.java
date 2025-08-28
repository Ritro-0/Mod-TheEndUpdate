package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.TemplateModClient;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import com.theendupdate.entity.renderer.feature.EtherealOrbGlowFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
// no extra state imports
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;


/**
 * Renderer for the Ethereal Orb entity - Minecraft 1.21.8 version
 * 
 * This renderer handles:
 * - Rendering the orb model
 * - Adding a glowing effect
 * - Rotating animation
 * - Particle trail effects
 */
public class EtherealOrbEntityRenderer extends MobEntityRenderer<EtherealOrbEntity, EtherealOrbRenderState, EtherealOrbEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/ethereal_orb.png");
    private static final Identifier GLOW_TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/ethereal_orb_emissive.png");
    
    public EtherealOrbEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new EtherealOrbEntityModel(context.getPart(TemplateModClient.MODEL_ETHEREAL_ORB_LAYER)), 0.3f);
        this.addFeature(new EtherealOrbGlowFeatureRenderer(this, GLOW_TEXTURE));
    }
    
    @Override
    public EtherealOrbRenderState createRenderState() {
        return new EtherealOrbRenderState();
    }
    
    @Override
    public void updateRenderState(EtherealOrbEntity entity, EtherealOrbRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.moveAnimationState.copyFrom(entity.moveAnimationState);
        state.finishmovementAnimationState.copyFrom(entity.finishmovementAnimationState);
        state.rotateAnimationState.copyFrom(entity.rotateAnimationState);
        state.charged = entity.isCharged();
        state.baby = entity.isBaby();
    }
    
    @Override
    public Identifier getTexture(EtherealOrbRenderState state) {
        return TEXTURE;
    }

    @Override
    public void render(EtherealOrbRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (state.baby) {
            matrices.push();
            matrices.scale(0.6f, 0.6f, 0.6f);
            super.render(state, matrices, vertexConsumers, light);
            matrices.pop();
            return;
        }
        super.render(state, matrices, vertexConsumers, light);
    }

    // No label logic overrides here; handled via mixin to core renderer
}
