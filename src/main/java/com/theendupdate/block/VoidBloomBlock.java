package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;

public class VoidBloomBlock extends PlantBlock {
    public static final Property<Direction> FACING = Properties.FACING;
    public static final MapCodec<VoidBloomBlock> CODEC = createCodec(VoidBloomBlock::new);

    public VoidBloomBlock(Settings settings) {
        super(settings
            .noCollision()
            .nonOpaque()
            .sounds(BlockSoundGroup.GRASS)
            .strength(0.0f)
            .ticksRandomly()
            .offset(Block.OffsetType.NONE)
        );
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    public MapCodec<? extends PlantBlock> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public Block.OffsetType getOffsetType() {
        return Block.OffsetType.NONE; // disable random XZ offset for consistent placement
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        // Allow normal ground support
        return floor.isSolidBlock(world, pos);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Valid if sitting on a solid block below
        BlockPos below = pos.down();
        if (world.getBlockState(below).isSolidBlock(world, below)) {
            return true;
        }
        // Also valid if attached adjacent to a chorus flower (bud)
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.offset(direction);
            BlockState adjacentState = world.getBlockState(adjacentPos);
            if (adjacentState.isOf(Blocks.CHORUS_FLOWER)) {
                return true;
            }
        }
        return false;
    }

    // Removed problematic override - block support will be handled by randomTick

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if still supported, break if not
        if (!this.canPlaceAt(state, world, pos)) {
            world.breakBlock(pos, true);
        }
    }
}


