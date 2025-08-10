package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import com.theendupdate.registry.ModBlocks;

public class TemplateModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Try ItemBlockRenderTypes approach like BiomesOPlenty uses
        try {
            Class<?> itemBlockRenderTypesClass = Class.forName("net.minecraft.client.renderer.ItemBlockRenderTypes");
            Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.RenderType");
            
            Object cutoutLayer = renderTypeClass.getMethod("cutout").invoke(null);
            
            itemBlockRenderTypesClass.getMethod("setRenderLayer", 
                net.minecraft.block.Block.class, 
                renderTypeClass)
                .invoke(null, ModBlocks.VOID_BLOOM, cutoutLayer);
                
            com.theendupdate.TemplateMod.LOGGER.info("Successfully registered VOID_BLOOM with ItemBlockRenderTypes");
        } catch (Exception e) {
            // Fallback to Fabric API
            try {
                Class<?> blockRenderLayerMapClass = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap");
                Class<?> renderLayerClass = Class.forName("net.minecraft.client.render.RenderLayer");
                
                Object cutoutLayer = renderLayerClass.getMethod("getCutout").invoke(null);
                
                blockRenderLayerMapClass.getMethod("putBlock", 
                    net.minecraft.block.Block.class, 
                    renderLayerClass)
                    .invoke(null, ModBlocks.VOID_BLOOM, cutoutLayer);
                    
                com.theendupdate.TemplateMod.LOGGER.info("Successfully registered VOID_BLOOM with Fabric API fallback");
            } catch (Exception ex) {
                com.theendupdate.TemplateMod.LOGGER.warn("Failed to register transparency: " + ex.getMessage());
            }
        }
    }
}


