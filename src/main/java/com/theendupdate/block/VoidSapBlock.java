package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.theendupdate.registry.ModBlocks;

public class VoidSapBlock extends Block implements net.minecraft.block.Fertilizable {
    public static final MapCodec<VoidSapBlock> CODEC = createCodec(VoidSapBlock::new);
    
    // Properties for each face the void sap can be placed on
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    
    // VoxelShapes for each face (thin layer on each face)
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, 0.0625);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0);
    private static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(0.9375, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 0.0625, 1.0, 1.0);
    private static final VoxelShape UP_SHAPE = VoxelShapes.cuboid(0.0, 0.9375, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape DOWN_SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);

    public VoidSapBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
            .with(NORTH, false)
            .with(SOUTH, false)
            .with(EAST, false)
            .with(WEST, false)
            .with(UP, false)
            .with(DOWN, false));
        
    }
    
    @Override
    protected void onBlockAdded(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        
        // No longer need scheduled ticks - using EntityWallSlidingMixin for wall sliding
    }

    @Override
    public MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = VoxelShapes.empty();
        
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, NORTH_SHAPE);
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, SOUTH_SHAPE);
        if (state.get(EAST)) shape = VoxelShapes.union(shape, EAST_SHAPE);
        if (state.get(WEST)) shape = VoxelShapes.union(shape, WEST_SHAPE);
        if (state.get(UP)) shape = VoxelShapes.union(shape, UP_SHAPE);
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, DOWN_SHAPE);
        
        return shape.isEmpty() ? VoxelShapes.fullCube() : shape;
    }

    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context) {
        Direction side = context.getSide().getOpposite(); // Place on the clicked face
        BlockPos pos = context.getBlockPos();
        BlockState existingState = context.getWorld().getBlockState(pos);
        
        // If there's already void sap here, add to it
        if (existingState.isOf(this)) {
            return existingState.with(getPropertyForDirection(side), true);
        }
        
        // Check if we can place on this face
        if (!canPlaceOnFace(context.getWorld(), pos, side)) {
            return null; // Cannot place
        }
        
        // Otherwise create new void sap
        return this.getDefaultState().with(getPropertyForDirection(side), true);
    }

    @Override
    public boolean canReplace(BlockState state, net.minecraft.item.ItemPlacementContext context) {
        // Allow replacement if we're placing on a face that doesn't already have void sap
        if (context.getStack().getItem() == ModBlocks.VOID_SAP.asItem()) {
            Direction side = context.getSide().getOpposite();
            BooleanProperty property = getPropertyForDirection(side);
            return !state.get(property); // Can replace if this face is empty
        }
        return false;
    }

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Check if at least one face has a solid block to attach to
        for (Direction direction : Direction.values()) {
            if (state.get(getPropertyForDirection(direction))) {
                BlockPos attachedPos = pos.offset(direction);
                BlockState attachedState = world.getBlockState(attachedPos);
                if (!attachedState.isSideSolidFullSquare(world, attachedPos, direction.getOpposite())) {
                    return false;
                }
            }
        }
        return true;
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Remove faces that no longer have solid support
        BooleanProperty property = getPropertyForDirection(direction);
        if (state.get(property)) {
            if (!neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite())) {
                state = state.with(property, false);
            }
        }
        
        // If no faces remain, break the block
        if (!hasAnyFace(state)) {
            return Blocks.AIR.getDefaultState();
        }
        
        return state;
    }

    // CORRECT FERTILIZABLE INTERFACE - for bonemeal (like MoldBlock)
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return true; // Always allow bonemeal
    }

    @Override
    public boolean canGrow(World world, net.minecraft.util.math.random.Random random, BlockPos pos, BlockState state) {
        return true; // Always can grow
    }

    @Override
    public void grow(ServerWorld world, net.minecraft.util.math.random.Random random, BlockPos pos, BlockState state) {
        
        trySpread(state, world, pos, random);
    }

    

    // CORRECT BLOCK INTERACTION OVERRIDE - for right-click
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        ItemStack itemStack = player.getStackInHand(hand);
        
        // Add void sap to empty faces when right-clicking with void sap
        if (itemStack.getItem() == ModBlocks.VOID_SAP.asItem()) {
            Direction clickedFace = hit.getSide().getOpposite();
            BooleanProperty property = getPropertyForDirection(clickedFace);
            
            if (!state.get(property) && canPlaceOnFace(world, pos, clickedFace)) {
                if (!world.isClient) {
                    world.setBlockState(pos, state.with(property, true));
                    if (!player.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                }
                return ActionResult.SUCCESS;
            }
        }
        
        return ActionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Zero collision - entities pass through freely (for normal movement)
        return VoxelShapes.empty();
    }

    // Override velocity multiplier to slow down entities (like honey blocks)
    @Override
    public float getVelocityMultiplier() {
        // Honey blocks use 0.4, so half strength = 0.7
        
        return 0.7F;
    }

    // Override jump velocity multiplier to reduce jump height (like honey blocks)
    @Override
    public float getJumpVelocityMultiplier() {
        // Honey blocks use 0.5, so half strength = 0.75
        
        return 0.75F;
    }

    // Wall sliding will be handled by EntityWallSlidingMixin since onEntityCollision
    // doesn't work with blocks that have zero collision shape

    // Helper methods
    private boolean trySpread(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Try to spread to adjacent blocks
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.offset(direction);
            BlockState targetState = world.getBlockState(targetPos);
            
            if (targetState.isAir()) {
                // Check if we can place on any face of the target block
                for (Direction targetFace : Direction.values()) {
                    if (canPlaceOnFace(world, targetPos, targetFace)) {
                        BlockState newState = this.getDefaultState().with(getPropertyForDirection(targetFace), true);
                        world.setBlockState(targetPos, newState);
                        return true;
                    }
                }
            } else if (targetState.isOf(this)) {
                // Try to add to existing void sap
                for (Direction face : Direction.values()) {
                    BooleanProperty property = getPropertyForDirection(face);
                    if (!targetState.get(property) && canPlaceOnFace(world, targetPos, face)) {
                        world.setBlockState(targetPos, targetState.with(property, true));
                        return true;
                    }
                    // If the target block is already Void Sap and has all faces, try to spread to an adjacent block
                    if (hasAllFaces(targetState)) {
                        BlockPos adjacentPos = targetPos.offset(face);
                        if (world.getBlockState(adjacentPos).isAir() && canPlaceOnFace(world, adjacentPos, face.getOpposite())) {
                            BlockState newState = this.getDefaultState().with(getPropertyForDirection(face.getOpposite()), true);
                            world.setBlockState(adjacentPos, newState);
                            return true;
                        }
                    }
                }
            } else {
                // GLOW LICHEN BEHAVIOR: Try to spread onto the top of adjacent solid blocks
                if (direction.getAxis().isHorizontal()) {
                    BlockPos topPos = targetPos.up();
                    if (world.getBlockState(topPos).isAir() && canPlaceOnFace(world, topPos, Direction.DOWN)) {
                        BlockState newState = this.getDefaultState().with(getPropertyForDirection(Direction.DOWN), true);
                        world.setBlockState(topPos, newState);
                        return true;
                    }
                }
            }
        }
        
        // Try to add to current block if possible
        for (Direction face : Direction.values()) {
            BooleanProperty property = getPropertyForDirection(face);
            if (!state.get(property) && canPlaceOnFace(world, pos, face)) {
                world.setBlockState(pos, state.with(property, true));
                return true;
            }
        }
        
        return false;
    }

    private boolean canPlaceOnFace(WorldView world, BlockPos pos, Direction face) {
        BlockPos attachedPos = pos.offset(face);
        BlockState attachedState = world.getBlockState(attachedPos);
        return attachedState.isSideSolidFullSquare(world, attachedPos, face.getOpposite());
    }

    private boolean hasAnyFace(BlockState state) {
        return state.get(NORTH) || state.get(SOUTH) || state.get(EAST) || 
               state.get(WEST) || state.get(UP) || state.get(DOWN);
    }

    private boolean hasAllFaces(BlockState state) {
        return state.get(NORTH) && state.get(SOUTH) && state.get(EAST) && state.get(WEST) && state.get(UP) && state.get(DOWN);
    }

    private static BooleanProperty getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
}
