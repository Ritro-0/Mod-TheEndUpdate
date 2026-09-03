package com.theendupdate.entity.renderer.feature;

import com.theendupdate.entity.model.KingPhantomEntityModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;

/** Glowing eyes layer for the King Phantom, same as vanilla phantom eyes */
public class KingPhantomEyesFeatureRenderer extends EyesLayer<PhantomRenderState, KingPhantomEntityModel> {
    private final Identifier eyesTexture;

    public KingPhantomEyesFeatureRenderer(RenderLayerParent<PhantomRenderState, KingPhantomEntityModel> context, Identifier eyesTexture) {
        super(context);
        this.eyesTexture = eyesTexture;
    }

    @Override
    public net.minecraft.client.renderer.rendertype.RenderType renderType() {
        return RenderTypes.eyes(this.eyesTexture);
    }
}

