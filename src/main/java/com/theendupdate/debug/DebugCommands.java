package com.theendupdate.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.theendupdate.world.feature.EndCrystalSpikeFeature;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

public final class DebugCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("endupdate_debug_spike").executes(DebugCommands::spawnSpikeAtPlayer));
    }

    private static int spawnSpikeAtPlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return 0;
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        Random random = world.getRandom();
        try {
            // Reuse the feature's public generate via a synthetic FeatureContext substitute is complex; call through a helper
            EndCrystalSpikeFeature.debugPlaceOne(world, pos, random);
            src.sendFeedback(() -> net.minecraft.text.Text.literal("[EndUpdate] Debug spike attempt at " + pos), false);
        } catch (Throwable t) {
            src.sendError(net.minecraft.text.Text.literal("[EndUpdate] Debug spike failed: " + t.getMessage()));
        }
        return 1;
    }

    private DebugCommands() {}
}


