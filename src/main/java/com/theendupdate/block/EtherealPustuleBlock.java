package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Ethereal Pustule - six-sided "wood" variant of Ethereal Sporocarp.
 * Texture is the same on all six faces (like vanilla wood/hyphae),
 * but placement is directional via the AXIS property just like logs/wood.
 */
public class EtherealPustuleBlock extends PillarBlock {
    public static final MapCodec<EtherealPustuleBlock> CODEC = createCodec(EtherealPustuleBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    public EtherealPustuleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.Y));
    }

    @Override
    public MapCodec<? extends PillarBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
    }
}


