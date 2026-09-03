package com.theendupdate.entity.renderer.feature;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.Identifier;

/** Emissive glow layer for ethereal orb bulbs, same EyesLayer pattern as glow squids */
public class EtherealOrbGlowFeatureRenderer extends EyesLayer<EtherealOrbRenderState, EtherealOrbEntityModel> {
    private final Identifier glowTexture;

    public EtherealOrbGlowFeatureRenderer(RenderLayerParent<EtherealOrbRenderState, EtherealOrbEntityModel> context, Identifier glowTexture) {
        super(context);
        this.glowTexture = glowTexture;
    }

    @Override
    public net.minecraft.client.renderer.rendertype.RenderType renderType() {
        return RenderTypes.eyes(this.glowTexture);
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        EtherealOrbRenderState state,
        float yRot,
        float xRot
    ) {
        if (!state.bulbPresent || !state.charged) {
            return;
        }
        super.submit(poseStack, collector, packedLight, state, yRot, xRot);
    }
}


