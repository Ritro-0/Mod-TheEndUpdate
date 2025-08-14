package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import com.theendupdate.registry.ModBlocks;

// The correct imports for Fabric 1.21.8
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer; // This was the key!

@Environment(EnvType.CLIENT)  
public class TemplateModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register transparent blocks
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_SAP, BlockRenderLayer.CUTOUT);
        // Tendril plants (crossed planes)
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_SPROUT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_THREAD, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TENDRIL_CORE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_CRAWL, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE_TUFT, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.MOLD_SPORE_SPROUT, BlockRenderLayer.CUTOUT);
        // Wooden transparent parts
        BlockRenderLayerMap.putBlock(ModBlocks.ETHEREAL_DOOR, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ETHEREAL_TRAPDOOR, BlockRenderLayer.CUTOUT);
        // Quantum gateway uses glass-like rendering; translucent looks better for alpha
        BlockRenderLayerMap.putBlock(ModBlocks.QUANTUM_GATEWAY, BlockRenderLayer.TRANSLUCENT);
        // Client init complete
    }
}


