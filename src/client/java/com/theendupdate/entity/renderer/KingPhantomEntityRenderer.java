package com.theendupdate.entity.renderer;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.KingPhantomEntity;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.renderer.feature.KingPhantomEyesFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.PhantomEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class KingPhantomEntityRenderer extends MobEntityRenderer<KingPhantomEntity, PhantomEntityRenderState, KingPhantomEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/king_phantom.png");
    private static final Identifier EYES_TEXTURE = Identifier.of(TemplateMod.MOD_ID, "textures/entity/king_phantom_eyes.png");
    
    public KingPhantomEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new KingPhantomEntityModel(context.getPart(KingPhantomEntityModel.LAYER_LOCATION)), 7.0f);
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
    protected void setupTransforms(PhantomEntityRenderState state, MatrixStack matrices, float animationProgress, float bodyYaw) {
        super.setupTransforms(state, matrices, animationProgress, bodyYaw);
        
        // Flip sign: corrects descent to nose DOWN (positive pitch → negative rotation → nose down)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
    }
    
    @Override
    public void render(PhantomEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState) {
        matrices.push();
        
        // Apply 4x scale to match the 4x hitbox dimensions
        matrices.scale(4.0f, 4.0f, 4.0f);
        
        // Translate down to align model with hitbox
        // Tweak Y (e.g., -1.5) if model floats above hitbox
        matrices.translate(0.0, -1.125, 0.0);
        
        super.render(state, matrices, commandQueue, cameraState);
        
        matrices.pop();
    }
}

