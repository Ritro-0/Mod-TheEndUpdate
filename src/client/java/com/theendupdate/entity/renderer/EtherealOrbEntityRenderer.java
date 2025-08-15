package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.TemplateModClient;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.util.Identifier;


/**
 * Renderer for the Ethereal Orb entity - Minecraft 1.21.8 version
 * 
 * This renderer handles:
 * - Rendering the orb model
 * - Adding a glowing effect
 * - Rotating animation
 * - Particle trail effects
 */
public class EtherealOrbEntityRenderer extends LivingEntityRenderer<EtherealOrbEntity, EtherealOrbRenderState, EtherealOrbEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/ethereal_orb.png");
    
    public EtherealOrbEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new EtherealOrbEntityModel(context.getPart(TemplateModClient.MODEL_ETHEREAL_ORB_LAYER)), 0.3f);
        
        // Add feature renderers here if needed (e.g., glowing overlay)
        // this.addFeature(new EtherealOrbGlowFeatureRenderer(this));
    }
    
    @Override
    public EtherealOrbRenderState createRenderState() {
        return new EtherealOrbRenderState();
    }
    
    @Override
    public void updateRenderState(EtherealOrbEntity entity, EtherealOrbRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.moveAnimationState.copyFrom(entity.moveAnimationState);
    }
    

    
    @Override
    public Identifier getTexture(EtherealOrbRenderState state) {
        return TEXTURE;
    }

}
