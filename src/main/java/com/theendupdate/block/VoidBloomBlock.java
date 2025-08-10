package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;

public class VoidBloomBlock extends Block {
    public static final Property<Direction> FACING = Properties.FACING;
    public static final MapCodec<VoidBloomBlock> CODEC = createCodec(VoidBloomBlock::new);

    public VoidBloomBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.UP));
    }

    @Override
    public MapCodec<? extends Block> getCodec() { return CODEC; }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, net.minecraft.world.BlockView world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if still supported, break if not
        if (!this.canPlaceAt(state, world, pos)) {
            world.breakBlock(pos, true);
        }
    }
}


