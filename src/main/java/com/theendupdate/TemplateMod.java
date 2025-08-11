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
        });
    }
}


