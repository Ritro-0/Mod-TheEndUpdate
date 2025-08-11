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
 * Ethereal Sporocarps - The "log" blocks that make up the Tendril Spore tree
 * These are rotatable like regular logs but have unique properties
 */
public class EtherealSporocarpsBlock extends PillarBlock {
    public static final MapCodec<EtherealSporocarpsBlock> CODEC = createCodec(EtherealSporocarpsBlock::new);
    
    // Axis property for rotation (like regular logs)
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    public EtherealSporocarpsBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.Y));
        com.theendupdate.TemplateMod.LOGGER.info("EtherealSporocarpsBlock initialized!");
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

    // These logs are special - they have a slight glow and unique properties
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, net.minecraft.world.BlockView world, net.minecraft.util.math.BlockPos pos) {
        return 0.8f; // Slightly bright like End materials
    }
}
