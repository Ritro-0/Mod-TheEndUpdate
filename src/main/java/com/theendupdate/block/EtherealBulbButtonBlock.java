package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Ethereal bulb button that can attach to tops/bottoms of thin blocks like fences, gates, walls, bars,
 * and either end of a chain. Extends ButtonBlock but relaxes support checks accordingly.
 */
public class EtherealBulbButtonBlock extends ButtonBlock {

    public EtherealBulbButtonBlock(BlockSetType type, int pressTicks, Settings settings) {
        super(type, pressTicks, settings);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        var face = state.get(FACE);
        if (face.toString().equals("FLOOR")) {
            return VoxelShapes.cuboid(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875);
        }
        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient() && state.get(FACE).toString().equals("FLOOR")) {
            BlockPos belowPos = pos.down();
            BlockState belowState = world.getBlockState(belowPos);
            world.setBlockState(belowPos, belowState, Block.NOTIFY_ALL);
        }
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient() && state.get(FACE).toString().equals("FLOOR")) {
            BlockPos belowPos = pos.down();
            BlockState belowState = world.getBlockState(belowPos);
            world.setBlockState(belowPos, belowState, Block.NOTIFY_ALL);
        }
        if (world instanceof ServerWorld sw) {
            super.onStateReplaced(state, sw, pos, moved);
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var face = state.get(FACE);
        Direction facing = state.get(FACING);
        BlockPos supportPos;
        Direction supportSide;
        switch (face) {
            case FLOOR -> {
                supportPos = pos.down();
                supportSide = Direction.UP;
            }
            case CEILING -> {
                supportPos = pos.up();
                supportSide = Direction.DOWN;
            }
            default -> { // WALL
                supportPos = pos.offset(facing.getOpposite());
                supportSide = facing;
            }
        }
        BlockState support = world.getBlockState(supportPos);
        if (support.isAir()) return false;

        if (isAllowedThinSupport(face, support, supportSide)) return true;
        // Allow on end rod tips: rod facing must point toward button
        if (support.isOf(Blocks.END_ROD)) {
            Direction rodFacing = support.get(Properties.FACING);
            if (face.toString().equals("FLOOR")) return rodFacing == Direction.UP;
            if (face.toString().equals("CEILING")) return rodFacing == Direction.DOWN;
            return rodFacing == supportSide;
        }

        return super.canPlaceAt(state, world, pos);
    }

    private boolean isAllowedThinSupport(Object face, BlockState support, Direction supportSide) {
        // Chain ends: allow if axis matches the attachment direction axis
        if (support.contains(Properties.AXIS) && (support.getBlock().getTranslationKey().contains("chain"))) {
            var axis = support.get(Properties.AXIS);
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
            || support.getBlock() instanceof PaneBlock
            || support.isOf(Blocks.IRON_BARS);

        if (!isThin) return false;

        // Only allow on top or bottom (floor/ceiling), not on the sides of these blocks
        return face.toString().equals("FLOOR") || face.toString().equals("CEILING");
    }
}
