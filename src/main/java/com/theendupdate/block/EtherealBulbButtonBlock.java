package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ethereal bulb button that can attach to tops/bottoms of thin blocks like fences, gates, walls, bars,
 * and either end of a chain. Extends ButtonBlock but relaxes support checks accordingly.
 */
public class EtherealBulbButtonBlock extends ButtonBlock {

    public EtherealBulbButtonBlock(BlockSetType type, int pressTicks, Properties settings) {
        super(type, pressTicks, settings);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var face = state.getValue(FACE);
        if (face.toString().equals("FLOOR")) {
            return Shapes.box(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875);
        }
        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (!world.isClientSide() && state.getValue(FACE).toString().equals("FLOOR")) {
            BlockPos belowPos = pos.below();
            BlockState belowState = world.getBlockState(belowPos);
            world.setBlock(belowPos, belowState, Block.UPDATE_ALL);
        }
    }

    public void onStateReplaced(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !world.isClientSide() && state.getValue(FACE).toString().equals("FLOOR")) {
            BlockPos belowPos = pos.below();
            BlockState belowState = world.getBlockState(belowPos);
            world.setBlock(belowPos, belowState, Block.UPDATE_ALL);
        }
        if (world instanceof ServerLevel sw) {
            super.affectNeighborsAfterRemoval(state, sw, pos, moved);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var face = state.getValue(FACE);
        Direction facing = state.getValue(FACING);
        BlockPos supportPos;
        Direction supportSide;
        switch (face) {
            case FLOOR -> {
                supportPos = pos.below();
                supportSide = Direction.UP;
            }
            case CEILING -> {
                supportPos = pos.above();
                supportSide = Direction.DOWN;
            }
            default -> { // WALL
                supportPos = pos.relative(facing.getOpposite());
                supportSide = facing;
            }
        }
        BlockState support = world.getBlockState(supportPos);
        if (support.isAir()) return false;

        if (isAllowedThinSupport(face, support, supportSide)) return true;
        
        // Allow on end rod tips: rod facing must point toward button
        if (support.is(Blocks.END_ROD)) {
            Direction rodFacing = support.getValue(BlockStateProperties.FACING);
            if (face.toString().equals("FLOOR")) return rodFacing == Direction.UP;
            if (face.toString().equals("CEILING")) return rodFacing == Direction.DOWN;
            return rodFacing == supportSide;
        }

        // Allow on lightning rod tips (all oxidation stages)
        if (isLightningRod(support)) {
            Direction rodFacing = support.getValue(BlockStateProperties.FACING);
            if (face.toString().equals("FLOOR")) return rodFacing == Direction.UP;
            if (face.toString().equals("CEILING")) return rodFacing == Direction.DOWN;
            return false;
        }

        return super.canSurvive(state, world, pos);
    }

    private boolean isAllowedThinSupport(Object face, BlockState support, Direction supportSide) {
        // Chain ends: allow if axis matches the attachment direction axis
        if (support.hasProperty(BlockStateProperties.AXIS) && (support.getBlock().getDescriptionId().contains("chain"))) {
            var axis = support.getValue(BlockStateProperties.AXIS);
            // Floor/Ceiling -> vertical chain only
            if (face.toString().equals("FLOOR") || face.toString().equals("CEILING")) {
                return axis == Direction.Axis.Y;
            }
            // Wall: horizontal attachment; require chain axis to match facing axis
            return axis == supportSide.getAxis();
        }

        // Tops/bottoms of fences, gates, walls, bars
        boolean isThin = support.getBlock() instanceof FenceBlock
            || support.getBlock() instanceof FenceGateBlock
            || support.getBlock() instanceof WallBlock
            || support.getBlock() instanceof IronBarsBlock
            || support.is(Blocks.IRON_BARS);

        if (!isThin) return false;

        // top/bottom only, not the sides
        return face.toString().equals("FLOOR") || face.toString().equals("CEILING");
    }

    private boolean isLightningRod(BlockState state) {
        return state.is(BlockTags.LIGHTNING_RODS);
    }
}
