package com.theendupdate.mixin.client;

import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;

// quantum-gateway beacon visuals targeted older beacon renderer APIs - stub kept so
// registration stays stable while effects get reimplemented for 26.x render-state pipelines
@Mixin(BeaconRenderer.class)
public abstract class BeaconBlockEntityRendererMixin {
}
