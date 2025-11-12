package com.theendupdate.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PottedClosedEnderChrysanthemumBlock extends FlowerPotBlock {
    public PottedClosedEnderChrysanthemumBlock(Block content, net.minecraft.block.AbstractBlock.Settings settings) {
        super(content, settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable net.minecraft.entity.LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            com.theendupdate.network.EnderChrysanthemumCloser.addClosedPositionManually(serverWorld, pos);
            if (com.theendupdate.TemplateMod.DEBUG_MODE) {
                com.theendupdate.TemplateMod.LOGGER.info("[EndUpdate] Potted closed chrysanthemum at {} - will open post-flash", pos);
            }
        }
    }
}

