package com.theendupdate.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
        if (support.isOf(Blocks.CHAIN)) {
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


