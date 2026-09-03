package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
public class VoidBloomBlock extends net.minecraft.world.level.block.VegetationBlock {
    public static final MapCodec<VoidBloomBlock> CODEC = simpleCodec(VoidBloomBlock::new);
    // direction TO the chorus bud this bloom is attached to (not from it)
    public static final Property<Direction> ATTACHMENT_FACE = BlockStateProperties.FACING;

    // Dimensions: 12x15 px
    private static final double WIDTH = 12.0 / 16.0;      // 0.75
    private static final double HALF = WIDTH / 2.0;       // 0.375
    private static final double HEIGHT = 15.0 / 16.0;     // 0.9375
    private static final double MIN = 0.5 - HALF;         // 0.125
    private static final double MAX = 0.5 + HALF;         // 0.875

    private static final VoxelShape SHAPE_DOWN = Shapes.box(MIN, 0.0, MIN, MAX, HEIGHT, MAX);
    private static final VoxelShape SHAPE_UP = Shapes.box(MIN, 1.0 - HEIGHT, MIN, MAX, 1.0, MAX);
    private static final VoxelShape SHAPE_NORTH = Shapes.box(MIN, 0.5 - HALF, 0.0, MAX, 0.5 + HALF, HEIGHT);
    private static final VoxelShape SHAPE_SOUTH = Shapes.box(MIN, 0.5 - HALF, 1.0 - HEIGHT, MAX, 0.5 + HALF, 1.0);
    private static final VoxelShape SHAPE_WEST = Shapes.box(0.0, 0.5 - HALF, MIN, HEIGHT, 0.5 + HALF, MAX);
    private static final VoxelShape SHAPE_EAST = Shapes.box(1.0 - HEIGHT, 0.5 - HALF, MIN, 1.0, 0.5 + HALF, MAX);

    public VoidBloomBlock(Properties settings) {
        super(settings);
        // defaults to sitting on top of something (attached downward)
        this.registerDefaultState(this.stateDefinition.any().setValue(ATTACHMENT_FACE, Direction.DOWN));
    }

    @Override
    public MapCodec<? extends net.minecraft.world.level.block.VegetationBlock> codec() { 
        return CODEC; 
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ATTACHMENT_FACE);
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction face = state.getValue(ATTACHMENT_FACE);
        return switch (face) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // no collision, like vanilla flowers
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction attachmentFace = state.getValue(ATTACHMENT_FACE);
        
        BlockPos attachedPos = pos.relative(attachmentFace);
        BlockState attachedState = world.getBlockState(attachedPos);
        
        if (attachedState.is(Blocks.CHORUS_FLOWER)) {
            return true;
        }
        
        // attaches TO attachmentFace, so the sturdy check needs the opposite face
        Direction oppositeFace = attachmentFace.getOpposite();
        boolean canPlace = attachedState.isFaceSturdy(world, attachedPos, oppositeFace);
        
        return canPlace;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        
        // prefer attaching to an adjacent chorus flower if there is one nearby
        BlockPos clickedPos = context.getClickedPos();
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = clickedPos.relative(direction);
            if (context.getLevel().getBlockState(adjacentPos).is(Blocks.CHORUS_FLOWER)) {
                return this.getAttachedState(direction.getOpposite());
            }
        }
        
        // otherwise attach to the clicked face - e.g. clicking UP means attaching downward
        Direction attachmentDirection = clickedFace.getOpposite();
        return this.getAttachedState(attachmentDirection);
    }

    public BlockState getAttachedState(Direction chorusDirection) {
        // ATTACHMENT_FACE is the direction FROM the bloom TO the chorus flower
        return this.defaultBlockState().setValue(ATTACHMENT_FACE, chorusDirection);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!this.canSurvive(state, world, pos)) {
            world.destroyBlock(pos, true);
        }
    }
}


