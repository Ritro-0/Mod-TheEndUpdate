package com.theendupdate.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import com.theendupdate.network.EnderChrysanthemumCloser;
import net.minecraft.server.world.ServerWorld;

public class TestChrysanthemumCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("testchrysanthemum")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                ServerWorld world = context.getSource().getWorld();
                BlockPos center = context.getSource().getPlayerOrThrow().getBlockPos();
                EnderChrysanthemumCloser.closeNearby(world, center, 32, 100);
                context.getSource().sendFeedback(() -> Text.literal("Triggered chrysanthemum close test at " + center), false);
                return 1;
            }));
    }
}

