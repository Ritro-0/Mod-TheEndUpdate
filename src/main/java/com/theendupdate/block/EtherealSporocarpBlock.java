package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ethereal Sporocarp - the log-like block for the Tendril Spore tree.
 * Behaves like a rotatable pillar (log) with a slight glow.
 */
public class EtherealSporocarpBlock extends RotatedPillarBlock {
    public static final MapCodec<EtherealSporocarpBlock> CODEC = simpleCodec(EtherealSporocarpBlock::new);

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public EtherealSporocarpBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(AXIS, ctx.getClickedFace().getAxis());
    }
}


