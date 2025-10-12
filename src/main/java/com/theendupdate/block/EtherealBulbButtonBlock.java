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
    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        // Return a full cube shape on the attachment face so walls see it as solid
        var face = state.get(FACE);
        if (face.toString().equals("FLOOR")) {
            // For floor-mounted, return a shape that fills the bottom so walls detect it
            return VoxelShapes.cuboid(0, 0, 0, 1, 0.001, 1);
        }
        return super.getSidesShape(state, world, pos);
    }

    @Override
    protected VoxelShape getCullingShape(BlockState state) {
        // Return a full cube for culling purposes on the attachment face
        var face = state.get(FACE);
        if (face.toString().equals("FLOOR")) {
            return VoxelShapes.fullCube();
        }
        return super.getCullingShape(state);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var face = state.get(FACE); // BlockFace in this mappings
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
        // Note: Blocks.CHAIN constant may have changed in 1.21.10, using alternative check
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

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        // Notify the block we're attached to so walls/fences can update their connections
        if (!world.isClient()) {
            notifyAttachedBlock(state, world, pos);
        }
    }

    // Mapping-safe: omit @Override and use broader signature
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // Notify the block we were attached to so walls/fences can update their connections
        if (!state.isOf(newState.getBlock()) && !world.isClient()) {
            // Pass the new state (which is at our position after removal) for proper neighbor updates
            notifyAttachedBlockRemoved(state, world, pos, newState);
        }
        if (world instanceof ServerWorld sw) {
            super.onStateReplaced(state, sw, pos, moved);
        }
    }

    // 1.21.8 superclass override variant
    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
    }

    private void notifyAttachedBlock(BlockState state, World world, BlockPos pos) {
        var face = state.get(FACE);
        Direction facing = state.get(FACING);
        BlockPos attachedPos;
        
        switch (face) {
            case FLOOR -> attachedPos = pos.down();
            case CEILING -> attachedPos = pos.up();
            default -> attachedPos = pos.offset(facing.getOpposite());
        }
        
        // Get the attached block's state
        BlockState attachedState = world.getBlockState(attachedPos);
        
        // Re-setting the block state forces visual updates on client
        world.setBlockState(attachedPos, attachedState, Block.NOTIFY_ALL);
        
        // Update neighbors to ensure the wall recalculates
        world.updateNeighbors(attachedPos, attachedState.getBlock());
    }

    private void notifyAttachedBlockRemoved(BlockState oldState, World world, BlockPos pos, BlockState replacementState) {
        var face = oldState.get(FACE);
        Direction facing = oldState.get(FACING);
        BlockPos attachedPos;
        
        switch (face) {
            case FLOOR -> attachedPos = pos.down();
            case CEILING -> attachedPos = pos.up();
            default -> attachedPos = pos.offset(facing.getOpposite());
        }
        
        // Get the attached block's state and force it to recalculate by re-setting it
        BlockState attachedState = world.getBlockState(attachedPos);
        
        // Re-setting the block state forces walls/fences to recalculate their connections
        world.setBlockState(attachedPos, attachedState, Block.NOTIFY_ALL);
    }
}


