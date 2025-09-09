package com.theendupdate.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
// no StructureWorldAccess usage in current debug commands
// no Text usage

public final class DebugCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("endupdate_debug_spike").executes(DebugCommands::spawnSpikeAtPlayer));
        // Shadowlands locate command removed
    }

    private static int spawnSpikeAtPlayer(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return 0;
        final ServerWorld world = src.getWorld();
        BlockPos pos = player.getBlockPos();
        // Random reserved for future debug helpers
        try {
            // Debug helper removed; no-op to avoid compile errors in newer mappings
            src.sendFeedback(() -> net.minecraft.text.Text.literal("[EndUpdate] Debug spike attempt at " + pos), false);
        } catch (Throwable t) {
            src.sendError(net.minecraft.text.Text.literal("[EndUpdate] Debug spike failed: " + t.getMessage()));
        }
        return 1;
    }

    private DebugCommands() {}
}


