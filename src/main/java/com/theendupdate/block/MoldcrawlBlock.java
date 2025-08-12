package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Moldcrawl - a horizontal vine-like plant that extends sideways.
 * Simplified implementation that mirrors twisting vines behavior, but along a horizontal direction.
 */
public class MoldcrawlBlock extends Block implements Fertilizable {
    public static final MapCodec<MoldcrawlBlock> CODEC = createCodec(MoldcrawlBlock::new);

    // Orientation and growth state
    public static final Property<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty AGE = Properties.AGE_25; // 0..25 like twisting vines
    public static final BooleanProperty TIP = BooleanProperty.of("tip");
    public static final BooleanProperty STUNTED = BooleanProperty.of("stunted");
    // Derived flag: when true and TIP=true, the tip uses the "vines" texture
    public static final BooleanProperty TIP_VINES = BooleanProperty.of("tip_vines");

    // Thin, non-colliding outline to look like a vine segment.
    private static final VoxelShape THIN_X = VoxelShapes.cuboid(0.0, 0.25, 0.25, 1.0, 0.75, 0.75);
    private static final VoxelShape THIN_Z = VoxelShapes.cuboid(0.25, 0.25, 0.0, 0.75, 0.75, 1.0);

    public MoldcrawlBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(AGE, 0)
            .with(TIP, true)
            .with(STUNTED, false)
            .with(TIP_VINES, false)
        );
    }

    @Override
    public MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE, TIP, STUNTED, TIP_VINES);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Survival check first (mirror vines behavior of vanishing when unsupported)
        if (!this.canPlaceAt(state, world, pos)) {
            com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl randomTick unsupported at {} facing {}", pos.toShortString(), state.get(FACING));
            // Attempt chain-wide reattachment before breaking
            BlockState reattached = tryReattachChain(state, world, pos);
            if (reattached == null) {
                boolean drop = world.random.nextFloat() < 0.33f;
                world.breakBlock(pos, drop);
            }
            return;
        }
        // Only the tip grows, unless stunted or fully matured
        if (state.get(TIP) && !state.get(STUNTED) && state.get(AGE) < 25) {
            if (random.nextInt(5) == 0) {
                tryGrowSegments(world, pos, state, 1 + random.nextInt(2)); // 1-2 segments
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction dir = state.get(FACING);
        return (dir.getAxis() == Direction.Axis.X) ? THIN_X : THIN_Z;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // No collision like vines/twisting vines
        return VoxelShapes.empty();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // Require support from the back (opposite facing). Allow stacking on itself.
        Direction back = state.get(FACING).getOpposite();
        BlockPos supportPos = pos.offset(back);
        BlockState support = world.getBlockState(supportPos);
        if (support.isOf(this)) return true;
        boolean solid = support.isSideSolidFullSquare(world, supportPos, back.getOpposite());
        if (!solid) {
            try {
                com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl canPlaceAt=FALSE at {} facing {} (support at {} {} solid)",
                    pos.toShortString(), state.get(FACING), supportPos.toShortString(), solid);
            } catch (Throwable ignored) {}
        }
        return solid;
    }

    // Interaction: allow right-click with bonemeal to pass through to Fertilizable handler
    // Do not annotate with @Override to be compatible across mappings/signatures
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        // Shears stunt the tip, switching it to the "vines" texture
        if (stack.isOf(Items.SHEARS) && state.get(TIP)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(STUNTED, true).with(TIP_VINES, true));
                stack.damage(1, player, hand);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // Fertilizable impl (bonemeal)
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        if (!state.get(TIP) || state.get(STUNTED) || state.get(AGE) >= 25) return false;
        Direction dir = state.get(FACING);
        BlockPos cursor = pos.offset(dir);
        return world.getBlockState(cursor).isAir();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return isFertilizable(world, pos, state);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Grow 1-5 segments on bonemeal (similar to twisting vines burst growth)
        int segments = 1 + random.nextInt(5);
        tryGrowSegments(world, pos, state, segments);
    }

    private void tryGrowSegments(ServerWorld world, BlockPos origin, BlockState originState, int maxSegments) {
        Direction dir = originState.get(FACING);
        BlockPos pos = origin;
        // Find the current tip in the chain along FACING
        while (world.getBlockState(pos.offset(dir)).isOf(this)) {
            pos = pos.offset(dir);
        }
        // Place forward up to maxSegments into air
        int placed = 0;
        int age = originState.contains(AGE) ? originState.get(AGE) : 0;
        while (placed < maxSegments && age < 25) {
            BlockPos next = pos.offset(dir);
            if (!world.isAir(next)) break;
            // Current becomes body segment
            BlockState current = world.getBlockState(pos);
            if (current.isOf(this)) {
                world.setBlockState(pos, current.with(TIP, false).with(TIP_VINES, false));
            }
            // New tip grows by 1-2 age
            age = Math.min(25, age + 1 + world.random.nextInt(2));
            boolean tipVines = age >= 25; // matured tip -> vines texture
            BlockState newTip = this.getDefaultState()
                .with(FACING, dir)
                .with(AGE, age)
                .with(TIP, true)
                .with(STUNTED, false)
                .with(TIP_VINES, tipVines);
            world.setBlockState(next, newTip);
            pos = next;
            placed++;
        }
        // Recompute flags for final tip (account for neighbor being non-vine)
        BlockState finalState = world.getBlockState(pos);
        if (finalState.isOf(this)) {
            boolean isTip = !world.getBlockState(pos.offset(dir)).isOf(this);
            boolean tipVines = finalState.get(STUNTED) || finalState.get(AGE) >= 25;
            world.setBlockState(pos, finalState.with(TIP, isTip).with(TIP_VINES, tipVines));
        }
    }

    // Do not annotate with @Override to remain mapping-safe
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Do not replace with AIR here to avoid pre-empting loot drops on player breaks.
        // Attempt immediate reattachment (mutates world if successful).
        if (!this.canPlaceAt(state, world, pos)) {
            com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl neighborUpdate unsupported at {} facing {}", pos.toShortString(), state.get(FACING));
            BlockState reattached = tryReattachChain(state, world, pos);
            if (reattached != null) {
                com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl neighborUpdate reattached at {} new facing {}", pos.toShortString(), reattached.get(FACING));
            }
            // Even if reattach fails, defer actual breaking to scheduledTick to avoid racing with player drops.
        }
        // Schedule a survival check to handle horizontal reattachment/breaking reliably
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.scheduleBlockTick(pos, this, 1);
        }

        // Update tip flags if the forward neighbor changed
        Direction dir = state.get(FACING);
        if (direction == dir) {
            boolean isTip = !neighborState.isOf(this);
            boolean tipVines = isTip && (state.get(STUNTED) || state.get(AGE) >= 25);
            return state.with(TIP, isTip).with(TIP_VINES, tipVines);
        }

        return state;
    }

    // Survival tick: break when unsupported (matches vines behavior of disappearing without drops)
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (this.canPlaceAt(state, world, pos)) return;
        com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl scheduledTick unsupported at {} facing {}", pos.toShortString(), state.get(FACING));
        BlockState reattached = tryReattachChain(state, world, pos);
        if (reattached != null) {
            com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl scheduledTick reattached at {} new facing {}", pos.toShortString(), reattached.get(FACING));
            return;
        }
        // No reattachment possible; break with chance like vines (loot table's random_chance doesn't run here)
        boolean drop = world.random.nextFloat() < 0.33f;
        world.breakBlock(pos, drop);
    }

    private BlockState tryReattachChain(BlockState state, net.minecraft.world.WorldAccess world, BlockPos pos) {
        Direction facing = state.get(FACING);
        Direction back = facing.getOpposite();

        // Determine contiguous chain bounds along current facing
        BlockPos base = pos;
        while (world.getBlockState(base.offset(back)).isOf(this)) {
            base = base.offset(back);
        }
        BlockPos tip = pos;
        while (world.getBlockState(tip.offset(facing)).isOf(this)) {
            tip = tip.offset(facing);
        }

        // Is there support if we flip and attach at the current forward side of the tip?
        BlockPos forwardPos = tip.offset(facing);
        BlockState forward = world.getBlockState(forwardPos);
        boolean forwardSupportBackFace = forward.isSideSolidFullSquare(world, forwardPos, back);
        boolean forwardSupportFacingFace = forward.isSideSolidFullSquare(world, forwardPos, facing);
        com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl chain base {} tip {} forward {} supportBackFace={} supportFacingFace={} (back={}, facing={})",
            base.toShortString(), tip.toShortString(), forwardPos.toShortString(), forwardSupportBackFace, forwardSupportFacingFace, back, facing);
        boolean forwardSupport = forwardSupportBackFace || forwardSupportFacingFace;
        if (!forwardSupport) {
            return null;
        }

        // Flip entire chain to face opposite (newFacing), new tip is at former base
        Direction newFacing = facing.getOpposite();
        BlockPos current = base;
        while (true) {
            BlockState currentState = world.getBlockState(current);
            int age = currentState.getOrEmpty(AGE).orElse(0);
            boolean isNewTip = current.equals(base);
            boolean tipVines = isNewTip && (currentState.getOrEmpty(STUNTED).orElse(false) || age >= 25);
            BlockState newState = this.getDefaultState()
                .with(FACING, newFacing)
                .with(AGE, age)
                .with(TIP, isNewTip)
                .with(STUNTED, false)
                .with(TIP_VINES, tipVines);
            ((net.minecraft.world.World)world).setBlockState(current, newState, Block.NOTIFY_ALL);
            com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl flip set {} -> facing {} tip={} age={}", current.toShortString(), newFacing, isNewTip, age);
            if (current.equals(tip)) break;
            current = current.offset(facing);
        }

        // Return the new state at the original pos to satisfy the neighbor update path
        return ((net.minecraft.world.World)world).getBlockState(pos);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient) {
            ((ServerWorld) world).scheduleBlockTick(pos, this, 1);
        }
    }
}


