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
 * Ethereal Sporocarp - the log-like block for the Tendril Spore tree.
 * Behaves like a rotatable pillar (log) with a slight glow.
 */
public class EtherealSporocarpBlock extends PillarBlock {
    public static final MapCodec<EtherealSporocarpBlock> CODEC = createCodec(EtherealSporocarpBlock::new);

    // Axis property for rotation (like regular logs)
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    public EtherealSporocarpBlock(Settings settings) {
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
        // Set axis based on clicked face (like logs)
        return this.getDefaultState().with(AXIS, ctx.getSide().getAxis());
    }
}


