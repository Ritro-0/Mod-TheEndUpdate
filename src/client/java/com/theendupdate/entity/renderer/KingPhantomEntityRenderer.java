package com.theendupdate.entity.renderer;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.KingPhantomEntity;
import com.theendupdate.entity.model.KingPhantomEntityModel;
import com.theendupdate.entity.renderer.feature.KingPhantomEyesFeatureRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class KingPhantomEntityRenderer extends MobRenderer<KingPhantomEntity, PhantomRenderState, KingPhantomEntityModel> {
    
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/king_phantom.png");
    private static final Identifier EYES_TEXTURE = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "textures/entity/king_phantom_eyes.png");
    
    public KingPhantomEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new KingPhantomEntityModel(context.bakeLayer(KingPhantomEntityModel.LAYER_LOCATION)), 7.0f);
        this.addLayer(new KingPhantomEyesFeatureRenderer(this, EYES_TEXTURE));
    }
    
    @Override
    public PhantomRenderState createRenderState() {
        return new PhantomRenderState();
    }
    
    @Override
    public void extractRenderState(KingPhantomEntity entity, PhantomRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.size = entity.getPhantomSize();
    }
    
    @Override
    public Identifier getTextureLocation(PhantomRenderState state) {
        return TEXTURE;
    }
    
    @Override
    protected void setupRotations(PhantomRenderState state, PoseStack matrices, float animationProgress, float bodyYaw) {
        super.setupRotations(state, matrices, animationProgress, bodyYaw);
        
        // flip sign so positive pitch pitches nose down instead of up
        matrices.mulPose(new Quaternionf().rotationX(-state.xRot * Mth.DEG_TO_RAD));
    }
    
    @Override
    public void submit(PhantomRenderState state, PoseStack matrices, SubmitNodeCollector commandQueue, CameraRenderState cameraState) {
        matrices.pushPose();

        matrices.scale(4.0f, 4.0f, 4.0f);

        matrices.translate(0.0, -1.125, 0.0);

        super.submit(state, matrices, commandQueue, cameraState);

        matrices.popPose();
    }
}

