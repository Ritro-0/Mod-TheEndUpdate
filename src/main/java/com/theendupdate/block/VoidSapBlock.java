package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.TheEndUpdate;

public class VoidSapBlock extends Block implements net.minecraft.world.level.block.BonemealableBlock {
    public static final MapCodec<VoidSapBlock> CODEC = simpleCodec(VoidSapBlock::new);
    
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.0625);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0.9375, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape WEST_SHAPE = Shapes.box(0.0, 0.0, 0.0, 0.0625, 1.0, 1.0);
    private static final VoxelShape UP_SHAPE = Shapes.box(0.0, 0.9375, 0.0, 1.0, 1.0, 1.0);
    private static final VoxelShape DOWN_SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);

    public VoidSapBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false));
        
    }
    
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        // wall sliding is handled by EntityWallSlidingMixin, no scheduled ticks needed here
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction side = context.getClickedFace().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockState existingState = context.getLevel().getBlockState(pos);
        
        if (existingState.is(this)) {
            return existingState.setValue(getPropertyForDirection(side), true);
        }
        
        if (!canPlaceOnFace(context.getLevel(), pos, side)) {
            return null; // can't place
        }
        
        return this.defaultBlockState().setValue(getPropertyForDirection(side), true);
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        if (context.getItemInHand().getItem() == ModBlocks.VOID_SAP.asItem()) {
            Direction side = context.getClickedFace().getOpposite();
            BooleanProperty property = getPropertyForDirection(side);
            return !state.getValue(property);
        }
        return false;
    }

    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (state.getValue(getPropertyForDirection(direction))) {
                BlockPos attachedPos = pos.relative(direction);
                BlockState attachedState = world.getBlockState(attachedPos);
                if (!attachedState.isFaceSturdy(world, attachedPos, direction.getOpposite())) {
                    return false;
                }
            }
        }
        return true;
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty property = getPropertyForDirection(direction);
        if (state.getValue(property)) {
            if (!neighborState.isFaceSturdy(world, neighborPos, direction.getOpposite())) {
                state = state.setValue(property, false);
            }
        }
        
        if (!hasAnyFace(state)) {
            return Blocks.AIR.defaultBlockState();
        }
        
        return state;
    }

    // bonemeal, mirrors MoldBlock
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, net.minecraft.util.RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, net.minecraft.util.RandomSource random, BlockPos pos, BlockState state) {
        trySpread(state, world, pos, random);
    }

    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (itemStack.getItem() == ModBlocks.VOID_SAP.asItem()) {
            Direction clickedFace = hit.getDirection().getOpposite();
            BooleanProperty property = getPropertyForDirection(clickedFace);
            
            if (!state.getValue(property) && canPlaceOnFace(world, pos, clickedFace)) {
                if (!world.isClientSide()) {
                    world.setBlockAndUpdate(pos, state.setValue(property, true));
                    if (!player.getAbilities().instabuild) {
                        itemStack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // zero collision so entities pass through freely
        return Shapes.empty();
    }

    @Override
    public float getSpeedFactor() {
        return 0.7F; // honey blocks use 0.4, this is half-strength of that
    }

    @Override
    public float getJumpFactor() {
        return 0.75F; // honey blocks use 0.5, this is half-strength of that
    }

    // wall sliding is handled by EntityWallSlidingMixin - onEntityCollision doesn't
    // fire for blocks with a zero collision shape

    private boolean trySpread(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int maxSpreadDistance = TheEndUpdate.VOID_SAP_SPREAD_RADIUS; // hardcoded until a GameRules API is available
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = world.getBlockState(targetPos);
            if (maxSpreadDistance > 0 && pos.distManhattan(targetPos) > maxSpreadDistance) {
                continue;
            }
            
            if (targetState.isAir()) {
                for (Direction targetFace : Direction.values()) {
                    if (canPlaceOnFace(world, targetPos, targetFace)) {
                        if (maxSpreadDistance > 0 && pos.distManhattan(targetPos) > maxSpreadDistance) continue;
                        BlockState newState = this.defaultBlockState().setValue(getPropertyForDirection(targetFace), true);
                        world.setBlockAndUpdate(targetPos, newState);
                        return true;
                    }
                }
            } else if (targetState.is(this)) {
                for (Direction face : Direction.values()) {
                    BooleanProperty property = getPropertyForDirection(face);
                    if (!targetState.getValue(property) && canPlaceOnFace(world, targetPos, face)) {
                        if (maxSpreadDistance > 0 && pos.distManhattan(targetPos) > maxSpreadDistance) continue;
                        world.setBlockAndUpdate(targetPos, targetState.setValue(property, true));
                        return true;
                    }
                    // fully covered already - try pushing onto the next block over instead
                    if (hasAllFaces(targetState)) {
                        BlockPos adjacentPos = targetPos.relative(face);
                        if (maxSpreadDistance > 0 && pos.distManhattan(adjacentPos) > maxSpreadDistance) continue;
                        if (world.getBlockState(adjacentPos).isAir() && canPlaceOnFace(world, adjacentPos, face.getOpposite())) {
                            BlockState newState = this.defaultBlockState().setValue(getPropertyForDirection(face.getOpposite()), true);
                            world.setBlockAndUpdate(adjacentPos, newState);
                            return true;
                        }
                    }
                }
            } else {
                // glow lichen style: spread onto the top of adjacent solid blocks
                if (direction.getAxis().isHorizontal()) {
                    BlockPos topPos = targetPos.above();
                    if (world.getBlockState(topPos).isAir() && canPlaceOnFace(world, topPos, Direction.DOWN)) {
                        BlockState newState = this.defaultBlockState().setValue(getPropertyForDirection(Direction.DOWN), true);
                        world.setBlockAndUpdate(topPos, newState);
                        return true;
                    }
                }
            }
        }
        
        for (Direction face : Direction.values()) {
            BooleanProperty property = getPropertyForDirection(face);
            if (!state.getValue(property) && canPlaceOnFace(world, pos, face)) {
                if (maxSpreadDistance > 0 && pos.distManhattan(pos.relative(face)) > maxSpreadDistance) continue;
                world.setBlockAndUpdate(pos, state.setValue(property, true));
                return true;
            }
        }
        
        return false;
    }

    private boolean canPlaceOnFace(LevelReader world, BlockPos pos, Direction face) {
        BlockPos attachedPos = pos.relative(face);
        BlockState attachedState = world.getBlockState(attachedPos);
        return attachedState.isFaceSturdy(world, attachedPos, face.getOpposite());
    }

    private boolean hasAnyFace(BlockState state) {
        return state.getValue(NORTH) || state.getValue(SOUTH) || state.getValue(EAST) || 
               state.getValue(WEST) || state.getValue(UP) || state.getValue(DOWN);
    }

    private boolean hasAllFaces(BlockState state) {
        return state.getValue(NORTH) && state.getValue(SOUTH) && state.getValue(EAST) && state.getValue(WEST) && state.getValue(UP) && state.getValue(DOWN);
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
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0f; // Full brightness for transparent blocks
    }

    public boolean isTransparent(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }
}
