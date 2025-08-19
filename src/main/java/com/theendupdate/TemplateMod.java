package com.theendupdate;

import net.fabricmc.api.ModInitializer;
// removed unused lifecycle/command imports
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
// unused imports removed
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
// entity attribute registry called from ModEntities
// debug-related imports removed
// (no server tick hooks used currently)

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        

    @Override
    public void onInitialize() {
        // Initialize mod content
        com.theendupdate.registry.ModBlocks.registerModBlocks();
        com.theendupdate.registry.ModBlockEntities.register();
        com.theendupdate.registry.ModScreenHandlers.register();
        com.theendupdate.registry.ModItems.registerModItems();
        com.theendupdate.registry.ModEntities.registerModEntities();
        
        // Entity attributes are registered inside ModEntities.registerModEntities()
        // Fuels: make ethereal wood a poor fuel source (~half normal wood burn time)
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            final int ETHEREAL_FUEL_TICKS = context.baseSmeltTime() / 2; // usually 100 ticks
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SPOROCARP, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PUSTULE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_STAIRS, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_SLAB, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_FENCE_GATE, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_DOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_TRAPDOOR, ETHEREAL_FUEL_TICKS);
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_BUTTON, context.baseSmeltTime() / 4); // very low
            builder.add(com.theendupdate.registry.ModBlocks.ETHEREAL_PRESSURE_PLATE, ETHEREAL_FUEL_TICKS);
        });

        // Composting: mirror vanilla chances
        // - Moss Block: 65%
        // - Twisting Vines: 50%
        // - Tall Grass: 30%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_BLOCK.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_CRAWL.asItem(), 0.50f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.VOID_BLOOM.asItem(), 0.30f);
        // Mold plants composting (match vanilla equivalents):
        // - Nether Sprouts ~30%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE.asItem(), 0.30f);
        // - Warped Roots ~65%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_TUFT.asItem(), 0.65f);
        // - Large Fern (double-tall) ~65%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_SPORE_SPROUT.asItem(), 0.65f);

        // Tendril plants: match Warped Fungus (~65%)
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_SPROUT.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_THREAD.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.TENDRIL_CORE.asItem(), 0.65f);

        // Remove dev-only logging and commands for release

        // Global hooks to ensure mold_crawl reacts even if vanilla neighbor updates are skipped by renderer state:
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient) {
                // Clicked block position
                var clickedPos = hitResult.getBlockPos();
                // Intended placed position is one block in the clicked face direction
                var placedPos = clickedPos.offset(hitResult.getSide());
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, clickedPos);
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, placedPos);
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClient) {
                com.theendupdate.block.MoldcrawlBlock.reactToExternalChange(world, pos);
            }
        });

        // (loot table event debug removed to avoid API signature differences; block-break diagnostics remain)

        // Worldgen registration
        com.theendupdate.registry.ModWorldgen.registerAll();
        
        // No debug startup logging in release builds
        
    }
}


