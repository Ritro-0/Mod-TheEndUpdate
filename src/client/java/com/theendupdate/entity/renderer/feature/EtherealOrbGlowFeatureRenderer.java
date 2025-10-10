package com.theendupdate.entity.renderer.feature;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.state.EtherealOrbRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the emissive glow layer for ethereal orb bulbs, similar to how glow squids work.
 * Uses the EyesFeatureRenderer pattern which handles the emissive rendering automatically.
 */
public class EtherealOrbGlowFeatureRenderer extends EyesFeatureRenderer<EtherealOrbRenderState, EtherealOrbEntityModel> {
    private static final RenderLayer GLOW_LAYER;
    private final Identifier glowTexture;

    public EtherealOrbGlowFeatureRenderer(FeatureRendererContext<EtherealOrbRenderState, EtherealOrbEntityModel> context, Identifier glowTexture) {
        super(context);
        this.glowTexture = glowTexture;
    }
    
    static {
        // Initialize the glow layer - will be set per-instance via getEyesTexture
        GLOW_LAYER = null;
    }

    @Override
    public RenderLayer getEyesTexture() {
        // Return the eyes render layer with our glow texture
        // This automatically renders at full brightness
        return RenderLayer.getEyes(this.glowTexture);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, EtherealOrbRenderState state, float limbAngle, float limbDistance) {
        // Only render the glow when the bulb is present AND the orb is charged
        // (charged = has spectral debris and can be brushed/harvested)
        if (!state.bulbPresent || !state.charged) {
            return;
        }
        
        // Let the parent EyesFeatureRenderer handle the actual rendering
        // It knows how to work with the new OrderedRenderCommandQueue API
        super.render(matrices, commandQueue, light, state, limbAngle, limbDistance);
    }
}


