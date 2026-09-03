package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PottedClosedEnderChrysanthemumBlock extends FlowerPotBlock {
    public PottedClosedEnderChrysanthemumBlock(Block content, net.minecraft.world.level.block.state.BlockBehaviour.Properties settings) {
        super(content, settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            com.theendupdate.network.EnderChrysanthemumCloser.addClosedPositionManually(serverWorld, pos);
            if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
                com.theendupdate.TheEndUpdate.LOGGER.info("[EndUpdate] Potted closed chrysanthemum at {} - will open post-flash", pos);
            }
        }
    }
}

