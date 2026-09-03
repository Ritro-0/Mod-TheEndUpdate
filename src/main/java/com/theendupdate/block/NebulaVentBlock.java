package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.block.entity.NebulaVentBlockEntity;
import com.theendupdate.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Nebula Vent Block - A short octagonal pad with a lower center well.
 * Features waterlogging support and emits particle-driven plumes via a block entity.
 */
public class NebulaVentBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<NebulaVentBlock> CODEC = simpleCodec(NebulaVentBlock::new);

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Matches the Blockbench model: 5-pixel rim around a 3-pixel center well.
    private static final VoxelShape SHAPE = Shapes.or(
        Shapes.box(3.0 / 16.0, 0.0, 0.0, 13.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0),
        Shapes.box(3.0 / 16.0, 0.0, 11.0 / 16.0, 13.0 / 16.0, 5.0 / 16.0, 1.0),
        Shapes.box(0.0, 0.0, 3.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0, 13.0 / 16.0),
        Shapes.box(11.0 / 16.0, 0.0, 3.0 / 16.0, 1.0, 5.0 / 16.0, 13.0 / 16.0),
        Shapes.box(0.0, 0.0, 0.0, 5.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0),
        Shapes.box(11.0 / 16.0, 0.0, 0.0, 1.0, 5.0 / 16.0, 5.0 / 16.0),
        Shapes.box(0.0, 0.0, 11.0 / 16.0, 5.0 / 16.0, 5.0 / 16.0, 1.0),
        Shapes.box(11.0 / 16.0, 0.0, 11.0 / 16.0, 1.0, 5.0 / 16.0, 1.0),
        Shapes.box(4.0 / 16.0, 0.0, 4.0 / 16.0, 12.0 / 16.0, 3.0 / 16.0, 12.0 / 16.0)
    );

    public NebulaVentBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(WATERLOGGED, false)
            .setValue(FACING, Direction.SOUTH));
    }

    @Override
    public MapCodec<NebulaVentBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        FluidState fluidState = ctx.getLevel().getFluidState(pos);
        Direction placementFacing = ctx.getHorizontalDirection();
        return this.defaultBlockState()
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
            .setValue(FACING, placementFacing);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess scheduledTickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }
        return super.updateShape(state, world, scheduledTickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NebulaVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.NEBULA_VENT) {
            return null;
        }
        return (w, p, s, blockEntity) -> NebulaVentBlockEntity.tick(w, p, s, (NebulaVentBlockEntity) blockEntity);
    }

    // not an override, just a helper like VoidSapBlock
    public boolean isTransparent(BlockState state, BlockGetter world, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }
}
