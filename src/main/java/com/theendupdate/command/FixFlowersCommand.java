package com.theendupdate.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class FixFlowersCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("fixflowers")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(FixFlowersCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = source.getPlayer().getBlockPos();
        
        final int radius = 128;
        int fixedCount = 0;
        int scheduledCount = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -64; y <= 320; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (!world.isChunkLoaded(checkPos)) continue;
                    
                    BlockState state = world.getBlockState(checkPos);
                    if (state.getBlock() == com.theendupdate.registry.ModBlocks.ENDER_CHRYSANTHEMUM) {
                        // Reset to open state - no longer needed since we're using separate blocks
                        
                        // Schedule tick
                        world.scheduleBlockTick(checkPos, state.getBlock(), 1);
                        scheduledCount++;
                    }
                }
            }
        }
        
        final int fixed = fixedCount;
        final int scheduled = scheduledCount;
        source.sendFeedback(() -> Text.literal("Fixed " + fixed + " flowers and scheduled " + scheduled + " ticks within " + radius + " blocks"), true);
        return 1;
    }
}

