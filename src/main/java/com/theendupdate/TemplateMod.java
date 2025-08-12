package com.theendupdate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TheEndUpdate");
        com.theendupdate.registry.ModBlocks.registerModBlocks();
        com.theendupdate.registry.ModItems.registerModItems();
        LOGGER.info("Moldcrawl registered");

        // Composting: mirror vanilla chances
        // - Moss Block: 65%
        // - Twisting Vines: 50%
        // - Tall Grass: 30%
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_BLOCK.asItem(), 0.65f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.MOLD_CRAWL.asItem(), 0.50f);
        CompostingChanceRegistry.INSTANCE.add(com.theendupdate.registry.ModBlocks.VOID_BLOOM.asItem(), 0.30f);

        // Log all recipes from this mod's namespace when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var recipeManager = server.getRecipeManager();
                int count = 0;
                for (var entry : recipeManager.values()) {
                    RegistryKey<Recipe<?>> key = entry.id();
                    Identifier id = key.getValue();
                    if (MOD_ID.equals(id.getNamespace())) {
                        count++;
                        LOGGER.info("Loaded recipe: {}", id);
                    }
                }
                LOGGER.info("Total '{}' recipes loaded: {}", MOD_ID, count);

                // Debug: verify ethereal_planks is inside #minecraft:planks (items) at runtime
                var itemRegistry = server.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ITEM);
                var planksTag = net.minecraft.registry.tag.ItemTags.PLANKS;
                var nonFlammable = net.minecraft.registry.tag.ItemTags.NON_FLAMMABLE_WOOD;

                var etherealPlanksItem = com.theendupdate.registry.ModBlocks.ETHEREAL_PLANKS.asItem();
                var etherealId = net.minecraft.registry.Registries.ITEM.getId(etherealPlanksItem);
                LOGGER.info("Item ID for ethereal_planks: {}", etherealId);
                var etherealPlanksEntry = itemRegistry.getEntry(etherealId);
                if (etherealPlanksEntry.isPresent()) {
                    boolean inPlanks = etherealPlanksEntry.get().isIn(planksTag);
                    boolean inNonFlam = etherealPlanksEntry.get().isIn(nonFlammable);
                    LOGGER.info("Tag check: in #minecraft:planks = {} | #minecraft:non_flammable_wood = {}",
                        inPlanks, inNonFlam);
                } else {
                    LOGGER.warn("Tag check: could not resolve registry entry for theendupdate:ethereal_planks");
                }
            } catch (Throwable t) {
                LOGGER.error("Error while logging recipes for namespace '{}'", MOD_ID, t);
            }
        });

        // Debug commands to introspect recipes at runtime
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("teu_list_recipes").executes(ctx -> {
                var server = ctx.getSource().getServer();
                var recipeManager = server.getRecipeManager();
                int count = 0;
                for (var entry : recipeManager.values()) {
                    RegistryKey<Recipe<?>> key = entry.id();
                    Identifier id = key.getValue();
                    if (MOD_ID.equals(id.getNamespace())) {
                        count++;
                        final String idString = id.toString();
                        ctx.getSource().sendFeedback(() -> Text.literal(idString), false);
                    }
                }
                final int finalCount = count;
                ctx.getSource().sendFeedback(() -> Text.literal("theendupdate recipe count: " + finalCount), false);
                return 1;
            }));

            dispatcher.register(literal("teu_has_recipe").then(net.minecraft.server.command.CommandManager.argument("id", net.minecraft.command.argument.IdentifierArgumentType.identifier()).executes(ctx -> {
                Identifier id = net.minecraft.command.argument.IdentifierArgumentType.getIdentifier(ctx, "id");
                var server = ctx.getSource().getServer();
                var recipeManager = server.getRecipeManager();
                RegistryKey<Recipe<?>> key = RegistryKey.of(RegistryKeys.RECIPE, id);
                var opt = recipeManager.get(key);
                boolean present = opt.isPresent();
                ctx.getSource().sendFeedback(() -> Text.literal("has " + id + ": " + present), false);
                return present ? 1 : 0;
            })));

            // Dump the contents of #minecraft:planks (items)
            dispatcher.register(literal("teu_dump_planks_tag").executes(ctx -> {
                var server = ctx.getSource().getServer();
                var itemRegistry2 = server.getRegistryManager().getOrThrow(RegistryKeys.ITEM);
                var planksTag = net.minecraft.registry.tag.ItemTags.PLANKS;
                int count2 = 0;
                for (var entry : itemRegistry2.iterateEntries(planksTag)) {
                    var key = entry.getKey();
                    if (key.isPresent()) {
                        count2++;
                        ctx.getSource().sendFeedback(() -> Text.literal(key.get().getValue().toString()), false);
                    }
                }
                final int finalCount2 = count2;
                ctx.getSource().sendFeedback(() -> Text.literal("#minecraft:planks size: " + finalCount2), false);
                return 1;
            }));
        });

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
    }
}


