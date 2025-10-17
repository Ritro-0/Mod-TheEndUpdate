package com.theendupdate.entity.renderer.feature;

import com.theendupdate.entity.model.KingPhantomEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.state.PhantomEntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Renders the glowing eyes layer for the King Phantom, similar to vanilla phantom eyes.
 */
public class KingPhantomEyesFeatureRenderer extends EyesFeatureRenderer<PhantomEntityRenderState, KingPhantomEntityModel> {
    private final Identifier eyesTexture;

    public KingPhantomEyesFeatureRenderer(FeatureRendererContext<PhantomEntityRenderState, KingPhantomEntityModel> context, Identifier eyesTexture) {
        super(context);
        this.eyesTexture = eyesTexture;
    }

    @Override
    public RenderLayer getEyesTexture() {
        // Return the eyes render layer with our custom eyes texture
        // This automatically renders at full brightness (glowing effect)
        return RenderLayer.getEyes(this.eyesTexture);
    }
}

