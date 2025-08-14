package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class StellarithCrystalBlock extends Block {
    public StellarithCrystalBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void afterBreak(net.minecraft.world.World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient) {
            // Manual drop logic (mirror End Mire approach): Silk Touch -> drop self; else -> drop shard
            boolean hasSilk = false;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                hasSilk = ench != null && ench.toString().contains("minecraft:silk_touch");
            } catch (Throwable ignore) {}

            if (hasSilk) {
                Block.dropStack(world, pos, new ItemStack(this.asItem()));
            } else {
                Block.dropStack(world, pos, new ItemStack(com.theendupdate.registry.ModItems.VOIDSTAR_SHARD));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Do not call super to avoid default loot table path and prevent double drops
    }
}


