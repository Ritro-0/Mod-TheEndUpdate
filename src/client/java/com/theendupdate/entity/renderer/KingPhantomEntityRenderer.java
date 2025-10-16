package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.KingPhantomEntity;
import com.theendupdate.entity.renderer.feature.KingPhantomEyesFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PhantomEntityModel;
import net.minecraft.client.render.entity.state.PhantomEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.util.Identifier;

/**
 * Renderer for the King Phantom entity.
 * 
 * This renderer handles:
 * - Rendering the phantom model at 4x scale
 * - Using custom king_phantom.png and king_phantom_eyes.png textures
 */
public class KingPhantomEntityRenderer extends MobEntityRenderer<KingPhantomEntity, PhantomEntityRenderState, PhantomEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/king_phantom.png");
    private static final Identifier EYES_TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/king_phantom_eyes.png");
    
    public KingPhantomEntityRenderer(EntityRendererFactory.Context context) {
        // Shadow radius of 7.0f (4x the normal phantom shadow of ~1.75f) for proper ground shadow
        super(context, new PhantomEntityModel(context.getPart(EntityModelLayers.PHANTOM)), 7.0f);
        // Add the eyes feature renderer for glowing eyes effect
        this.addFeature(new KingPhantomEyesFeatureRenderer(this, EYES_TEXTURE));
    }
    
    @Override
    public PhantomEntityRenderState createRenderState() {
        return new PhantomEntityRenderState();
    }
    
    @Override
    public void updateRenderState(KingPhantomEntity entity, PhantomEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.size = entity.getPhantomSize();
    }
    
    @Override
    public Identifier getTexture(PhantomEntityRenderState state) {
        return TEXTURE;
    }
    
    @Override
    public void render(PhantomEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState) {
        // Apply 4x scale to match the 4x hitbox dimensions
        // The hitbox is 3.6 x 2.0 (4x the normal 0.9 x 0.5)
        matrices.push();
        
        // Scale first
        matrices.scale(4.0f, 4.0f, 4.0f);
        
        // Translate down to align model with hitbox
        // Fine-tuned based on testing: between -0.375 (too high) and -1.875 (too low)
        matrices.translate(0.0, -1.125, 0.0); // Middle ground for proper alignment
        
        super.render(state, matrices, commandQueue, cameraState);
        matrices.pop();
    }
}

