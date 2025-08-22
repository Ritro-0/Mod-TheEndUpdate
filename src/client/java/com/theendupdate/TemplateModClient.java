package com.theendupdate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import com.theendupdate.entity.model.EtherealOrbEntityModel;
import com.theendupdate.entity.renderer.EtherealOrbEntityRenderer;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.registry.ModEntities;

// The correct imports for Fabric 1.21.8
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.BlockRenderLayer; // This was the key!
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
// predicate registration not needed since models use built-in trim_type predicate
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.registry.entry.RegistryEntry;
// predicate registration removed; compass logic handled via mixins

@Environment(EnvType.CLIENT)  
public class TemplateModClient implements ClientModInitializer {
    public static final EntityModelLayer MODEL_ETHEREAL_ORB_LAYER = new EntityModelLayer(Identifier.of(TemplateMod.MOD_ID, "ethereal_orb"), "main");

    @Override
    public void onInitializeClient() 
    {
        // TODO: Add entity renderer when implementing custom renderer for 1.21.8
        // EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, EtherealOrbEntityRenderer::new);
        // EntityModelLayerRegistry.registerModelLayer(EtherealOrbEntityModel.ETHEREAL_ORB_LAYER, EtherealOrbEntityModel::getTexturedModelData);
        
        // Register transparent blocks
        BlockRenderLayerMap.putBlock(ModBlocks.VOID_BLOOM, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.ENDER_CHRYSANTHEMUM, BlockRenderLayer.CUTOUT);
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
        // Entity Initialization
        EntityModelLayerRegistry.registerModelLayer(MODEL_ETHEREAL_ORB_LAYER, EtherealOrbEntityModel :: getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.ETHEREAL_ORB, (context) -> new EtherealOrbEntityRenderer(context));
        // Register custom screen for Quantum Gateway
        HandledScreens.register(com.theendupdate.registry.ModScreenHandlers.GATEWAY, com.theendupdate.screen.GatewayScreen::new);

        // Client init complete
        // cleaned debug log

        // Item models override using built-in trim_type; no explicit predicate registration required

    }

    // no-op
    
}


