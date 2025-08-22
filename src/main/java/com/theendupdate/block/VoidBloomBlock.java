package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.WorldView;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;
import net.minecraft.world.BlockView;
public class VoidBloomBlock extends net.minecraft.block.PlantBlock {
    public static final MapCodec<VoidBloomBlock> CODEC = createCodec(VoidBloomBlock::new);
    // Track which direction the void bloom is attached to (the direction TO the chorus bud)
    public static final Property<Direction> ATTACHMENT_FACE = Properties.FACING;

    // Dimensions: 12x15 px
    private static final double WIDTH = 12.0 / 16.0;      // 0.75
    private static final double HALF = WIDTH / 2.0;       // 0.375
    private static final double HEIGHT = 15.0 / 16.0;     // 0.9375
    private static final double MIN = 0.5 - HALF;         // 0.125
    private static final double MAX = 0.5 + HALF;         // 0.875

    private static final VoxelShape SHAPE_DOWN = VoxelShapes.cuboid(MIN, 0.0, MIN, MAX, HEIGHT, MAX);
    private static final VoxelShape SHAPE_UP = VoxelShapes.cuboid(MIN, 1.0 - HEIGHT, MIN, MAX, 1.0, MAX);
    private static final VoxelShape SHAPE_NORTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 0.0, MAX, 0.5 + HALF, HEIGHT);
    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.cuboid(MIN, 0.5 - HALF, 1.0 - HEIGHT, MAX, 0.5 + HALF, 1.0);
    private static final VoxelShape SHAPE_WEST = VoxelShapes.cuboid(0.0, 0.5 - HALF, MIN, HEIGHT, 0.5 + HALF, MAX);
    private static final VoxelShape SHAPE_EAST = VoxelShapes.cuboid(1.0 - HEIGHT, 0.5 - HALF, MIN, 1.0, 0.5 + HALF, MAX);

    public VoidBloomBlock(Settings settings) {
        super(settings);
        // Default attachment is downward (sitting on top of something)
        this.setDefaultState(this.stateManager.getDefaultState().with(ATTACHMENT_FACE, Direction.DOWN));
    }

    @Override
    public MapCodec<? extends net.minecraft.block.PlantBlock> getCodec() { 
        return CODEC; 
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ATTACHMENT_FACE);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, net.minecraft.world.BlockView world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction face = state.get(ATTACHMENT_FACE);
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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // No collision like vanilla flowers
        return VoxelShapes.empty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction attachmentFace = state.get(ATTACHMENT_FACE);
        
        // Check if there's a chorus flower in the attachment direction
        BlockPos attachedPos = pos.offset(attachmentFace);
        BlockState attachedState = world.getBlockState(attachedPos);
        
        if (attachedState.isOf(Blocks.CHORUS_FLOWER)) {
            return true;
        }
        
        // Check if the attached block has a solid face for the void bloom to attach to
        // The void bloom attaches TO the attachmentFace direction, so we check the opposite face
        Direction oppositeFace = attachmentFace.getOpposite();
        boolean canPlace = attachedState.isSideSolidFullSquare(world, attachedPos, oppositeFace);
        
        return canPlace;
    }

    /**
     * Override to handle proper facing when placing the block
     */
    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        // Get the face that was clicked
        Direction clickedFace = context.getSide();
        
        // If placing against a chorus flower, attach to that face
        BlockPos clickedPos = context.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = clickedPos.offset(direction);
            if (context.getWorld().getBlockState(adjacentPos).isOf(Blocks.CHORUS_FLOWER)) {
                // Attach to the chorus flower face
                return this.getAttachedState(direction.getOpposite());
            }
        }
        
        // For any other block, attach to the clicked face
        // The attachment face is the direction FROM the void bloom TO the clicked surface
        // So if we clicked the UP face, the void bloom should attach downward (opposite direction)
        Direction attachmentDirection = clickedFace.getOpposite();
        return this.getAttachedState(attachmentDirection);
    }

    /**
     * Helper method to create a void bloom state attached to a specific chorus flower
     */
    public BlockState getAttachedState(Direction chorusDirection) {
        // The attachment face is the direction FROM the void bloom TO the chorus flower
        return this.getDefaultState().with(ATTACHMENT_FACE, chorusDirection);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if still supported, break if not
        if (!this.canPlaceAt(state, world, pos)) {
            world.breakBlock(pos, true);
        }
    }
}


