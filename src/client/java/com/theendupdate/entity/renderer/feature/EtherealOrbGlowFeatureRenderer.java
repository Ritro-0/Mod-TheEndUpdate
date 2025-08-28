package com.theendupdate.entity.renderer.feature;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class EtherealOrbGlowFeatureRenderer extends FeatureRenderer<EtherealOrbRenderState, EtherealOrbEntityModel> {
    private final Identifier glowTexture;

    public EtherealOrbGlowFeatureRenderer(FeatureRendererContext<EtherealOrbRenderState, EtherealOrbEntityModel> context, Identifier glowTexture) {
        super(context);
        this.glowTexture = glowTexture;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EtherealOrbRenderState state, float limbAngle, float limbDistance) {
        if (!state.charged) return;
        EtherealOrbEntityModel model = this.getContextModel();
        var layer = RenderLayer.getEyes(glowTexture);
        var consumer = vertexConsumers.getBuffer(layer);
        model.render(matrices, consumer, net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
    }
}


