package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.block.entity.NebulaVentBlockEntity;
import com.theendupdate.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Nebula Vent Block - A block with a 1x1x1 solid base and two tapering tubes forming lips extending upward.
 * Features waterlogging support and emits particle-driven plumes via a block entity.
 */
public class NebulaVentBlock extends BlockWithEntity implements Waterloggable {
    public static final MapCodec<NebulaVentBlock> CODEC = createCodec(NebulaVentBlock::new);
    
    // Waterlogging property
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;
    
    // Collision shape for the base when facing south - matches the visual model width (5.5 voxels wide = 0.34375 to 0.65625)
    // Base is 5.5 voxels wide (x=5.5 to 10.5), full depth (z=0 to 16), full height (y=0 to 16)
    private static final VoxelShape BASE_SHAPE_SOUTH = VoxelShapes.cuboid(
        0.34375, 0.0, 0.0,   // x=5.5/16, y=0, z=0
        0.65625, 1.0, 1.0     // x=10.5/16, y=1.0, z=1.0
    );
    
    // Outline shape includes both base and tubes (for visual rendering) when facing south
    // Combined outline shape (for visual selection highlight) when facing south
    private static final VoxelShape OUTLINE_SHAPE_SOUTH = BASE_SHAPE_SOUTH;
    // Raycast shape limited to the solid base so players can target the interior opening (per facing)
    private static final Map<Direction, VoxelShape> OUTLINE_SHAPES;
    private static final Map<Direction, VoxelShape> COLLISION_SHAPES;
    private static final Map<Direction, VoxelShape> RAYCAST_SHAPES;

    static {
        OUTLINE_SHAPES = new EnumMap<>(Direction.class);
        COLLISION_SHAPES = new EnumMap<>(Direction.class);
        RAYCAST_SHAPES = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.Type.HORIZONTAL) {
            VoxelShape outline = rotateSouthShapeTo(direction, OUTLINE_SHAPE_SOUTH);
            VoxelShape collision = rotateSouthShapeTo(direction, BASE_SHAPE_SOUTH);
            OUTLINE_SHAPES.put(direction, outline);
            COLLISION_SHAPES.put(direction, collision);
            RAYCAST_SHAPES.put(direction, collision);
        }
    }

    public NebulaVentBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
            .with(WATERLOGGED, false)
            .with(FACING, Direction.SOUTH));
    }

    @Override
    public MapCodec<NebulaVentBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        FluidState fluidState = ctx.getWorld().getFluidState(pos);
        Direction placementFacing = ctx.getHorizontalPlayerFacing();
        return this.getDefaultState()
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER)
            .with(FACING, placementFacing);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView scheduledTickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            scheduledTickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, scheduledTickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Visual outline includes both base and tube
        return OUTLINE_SHAPES.getOrDefault(state.get(FACING), OUTLINE_SHAPE_SOUTH);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        // Only the base should block cursor targeting so players can place blocks inside the lips
        return RAYCAST_SHAPES.getOrDefault(state.get(FACING), BASE_SHAPE_SOUTH);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Only the base is solid for collision - tube is walk-through for entities
        return COLLISION_SHAPES.getOrDefault(state.get(FACING), BASE_SHAPE_SOUTH);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NebulaVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.NEBULA_VENT) {
            return null;
        }
        return (w, p, s, blockEntity) -> NebulaVentBlockEntity.tick(w, p, s, (NebulaVentBlockEntity) blockEntity);
    }

    // Not an override - just a helper method like VoidSapBlock
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return !state.get(WATERLOGGED);
    }

    private static VoxelShape rotateSouthShapeTo(Direction target, VoxelShape southShape) {
        VoxelShape rotatedShape = southShape;
        int rotations = switch (target) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        for (int i = 0; i < rotations; i++) {
            rotatedShape = rotateShape90Y(rotatedShape);
        }
        return rotatedShape;
    }

    private static VoxelShape rotateShape90Y(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double[][] corners = {
                {minX, minZ},
                {minX, maxZ},
                {maxX, minZ},
                {maxX, maxZ}
            };
            double rotatedMinX = 1.0;
            double rotatedMinZ = 1.0;
            double rotatedMaxX = 0.0;
            double rotatedMaxZ = 0.0;

            for (double[] corner : corners) {
                double centeredX = corner[0] - 0.5;
                double centeredZ = corner[1] - 0.5;
                double rotatedX = -centeredZ;
                double rotatedZ = centeredX;
                double finalX = rotatedX + 0.5;
                double finalZ = rotatedZ + 0.5;
                rotatedMinX = Math.min(rotatedMinX, finalX);
                rotatedMinZ = Math.min(rotatedMinZ, finalZ);
                rotatedMaxX = Math.max(rotatedMaxX, finalX);
                rotatedMaxZ = Math.max(rotatedMaxZ, finalZ);
            }

            buffer[0] = VoxelShapes.union(buffer[0], VoxelShapes.cuboid(rotatedMinX, minY, rotatedMinZ, rotatedMaxX, maxY, rotatedMaxZ));
        });
        return buffer[0];
    }
}
