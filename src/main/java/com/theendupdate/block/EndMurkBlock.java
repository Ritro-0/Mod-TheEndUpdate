package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;

public class EndMurkBlock extends Block implements Fertilizable {
    public EndMurkBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // Always allow bonemeal interaction
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // First: try to spread to adjacent End Stone like Nylium -> Netherrack
        boolean spread = trySpreadToEndStone(world, pos, random);
        if (spread) {
            return;
        }
        // Otherwise: generate Shadow Claws around (Shadowlands vegetation only)
        generateShadowClaws(world, pos, random);
    }

    private boolean trySpreadToEndStone(ServerWorld world, BlockPos origin, Random random) {
        boolean convertedAny = false;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos targetPos = origin.offset(direction);
            BlockState targetState = world.getBlockState(targetPos);
            if (targetState.isOf(Blocks.END_STONE)) {
                world.setBlockState(targetPos, this.getDefaultState(), Block.NOTIFY_ALL);
                convertedAny = true;
            }
        }
        return convertedAny;
    }

    private void generateShadowClaws(ServerWorld world, BlockPos origin, Random random) {
        int attempts = 24; // denser than spores
        for (int i = 0; i < attempts; i++) {
            BlockPos target = origin.add(random.nextBetween(-3, 3), random.nextBetween(-1, 1), random.nextBetween(-3, 3));
            BlockPos above = target.up();
            if (!world.getBlockState(target).isOf(this)) continue;
            if (!world.isAir(above)) continue;
            int variant = random.nextBetween(0, 3);
            BlockState claw = ModBlocks.SHADOW_CLAW.getDefaultState().with(ShadowClawBlock.VARIANT, variant);
            if (claw.canPlaceAt(world, above)) {
                world.setBlockState(above, claw, Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity, ItemStack tool) {
        // Manually handle drops to mirror nylium: Silk Touch -> End Murk; else -> End Stone
        if (!world.isClient) {
            int silk = 0;
            try {
                ItemEnchantmentsComponent ench = tool.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    silk = ench.toString().contains("minecraft:silk_touch") ? 1 : 0;
                }
            } catch (Throwable t) {}
            if (silk > 0) {
                Block.dropStack(world, pos, new ItemStack(this.asItem()));
            } else {
                Block.dropStack(world, pos, new ItemStack(Blocks.END_STONE.asItem()));
            }
            ((ServerWorld) world).emitGameEvent(player, net.minecraft.world.event.GameEvent.BLOCK_DESTROY, pos);
        }
        // Intentionally do not call super.afterBreak to prevent default loot table drops (avoids double-drops)
    }
}


