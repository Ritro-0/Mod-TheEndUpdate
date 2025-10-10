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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
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
    // Natural growth soft cap per chain (1..5). Bonemeal ignores this cap.
    public static final IntProperty NATURAL_CAP = IntProperty.of("natural_cap", 1, 5);

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
            .with(NATURAL_CAP, 3)
        );
    }

    @Override
    public MapCodec<? extends Block> getCodec() {
        return CODEC;
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE, TIP, STUNTED, TIP_VINES, NATURAL_CAP);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Survival check first (mirror vines behavior of vanishing when unsupported)
        if (!this.canPlaceAt(state, world, pos)) {
            // Attempt chain-wide reattachment before breaking
            BlockState reattached = tryReattachChain(state, world, pos);
			if (reattached == null) {
				boolean drop = world.random.nextFloat() < 0.33f;
				world.breakBlock(pos, drop);
				// Explicitly schedule neighbors to guarantee horizontal cascade
				Direction f = state.get(FACING);
				BlockPos forwardPos = pos.offset(f);
				BlockPos backPos = pos.offset(f.getOpposite());
				if (world.getBlockState(forwardPos).isOf(this)) {
					world.scheduleBlockTick(forwardPos, this, 1);
				}
				if (world.getBlockState(backPos).isOf(this)) {
					world.scheduleBlockTick(backPos, this, 1);
				}
			}
            return;
        }
        // Only the tip grows, unless stunted or fully matured
        if (state.get(TIP) && !state.get(STUNTED) && state.get(AGE) < 25) {
            // Natural length bias: usually 1-3, 4 sporadic, 5 rare; never exceed 5 naturally
            Direction f = state.get(FACING);
            // Find base
            BlockPos base = pos;
            while (world.getBlockState(base.offset(f.getOpposite())).isOf(this)) {
                base = base.offset(f.getOpposite());
            }
            // Find tip and compute current length
            BlockPos tip = base;
            int length = 1; // include base
            while (world.getBlockState(tip.offset(f)).isOf(this)) {
                tip = tip.offset(f);
                length++;
            }
            // Hard cap by chain's NATURAL_CAP (bonemeal ignores this cap)
            BlockState baseState = world.getBlockState(base);
            int naturalCap = baseState.getOrEmpty(NATURAL_CAP).orElse(3);
            if (length < naturalCap) {
                // Base natural growth gate (approx 20% per tick)
                if (random.nextInt(5) == 0) {
                    // Additional rarity gates beyond length 3
                    boolean allowed = true;
                    if (length == 3 && naturalCap >= 4) {
                        // From 3 to 4: about 12% of those that reach 3 (conditional)
                        allowed = random.nextInt(100) < 12;
                    } else if (length == 4 && naturalCap >= 5) {
                        // From 4 to 5: about 3.8% of those at 4 (conditional)
                        allowed = random.nextInt(1000) < 38; // 3.8%
                    }
                    if (allowed) {
                        int maxSegments = 1 + random.nextInt(2); // natural burst 1-2
                        int remainingCap = naturalCap - length;
                        if (maxSegments > remainingCap) maxSegments = remainingCap;
                        if (maxSegments > 0) {
                            tryGrowSegments(world, pos, state, maxSegments, false);
                        }
                    }
                }
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
        // Draw a per-chain natural cap according to target weights:
        // Baseline caps at 1,2,3 are roughly equal with a slight nudge to 3 overall.
        // Then small tails for 4 (~3%) and 5 (~1%).
        int roll = ctx.getWorld().random.nextInt(10000); // basis points
        int cap;
        if (roll < 3300) cap = 1;             // ~33%
        else if (roll < 6600) cap = 2;        // ~33%
        else if (roll < 9700) cap = 3;        // ~31%
        else if (roll < 9700 + 300) cap = 4;  // ~3%
        else cap = 5;                          // ~1%
        return this.getDefaultState().with(FACING, facing).with(NATURAL_CAP, cap);
    }

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		// Require support from the back (opposite facing). Allow stacking on itself.
		Direction back = state.get(FACING).getOpposite();
		BlockPos supportPos = pos.offset(back);
		BlockState support = world.getBlockState(supportPos);
		if (support.isOf(this)) return true;
		boolean solid = support.isSideSolidFullSquare(world, supportPos, back.getOpposite());
		return solid;
	}

    // Interaction: allow right-click with bonemeal to pass through to Fertilizable handler
    // Do not annotate with @Override to be compatible across mappings/signatures
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        // Shears stunt the tip, switching it to the "vines" texture
        if (stack.isOf(Items.SHEARS) && state.get(TIP)) {
            if (!world.isClient()) {
                world.setBlockState(pos, state.with(STUNTED, true).with(TIP_VINES, true));
                stack.damage(1, player, hand);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // Mapping-safe override variant used by 1.21.8 that omits Hand param
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack stack = player.getMainHandStack();
        if (stack.isOf(Items.SHEARS) && state.get(TIP)) {
            if (!world.isClient()) {
                world.setBlockState(pos, state.with(STUNTED, true).with(TIP_VINES, true));
                stack.damage(1, player, Hand.MAIN_HAND);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    // Fertilizable impl (bonemeal)
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        // Allow bonemeal from any segment. Find the chain tip in current facing and check next space.
        Direction dir = state.get(FACING);
        BlockPos tip = pos;
        while (world.getBlockState(tip.offset(dir)).isOf(this)) {
            tip = tip.offset(dir);
        }
        // Stunted tips still allow bonemeal-driven extension (like matured tips)
        BlockPos next = tip.offset(dir);
        return world.getBlockState(next).isAir();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        // Same condition as isFertilizable
        Direction dir = state.get(FACING);
        BlockPos tip = pos;
        while (world.getBlockState(tip.offset(dir)).isOf(this)) {
            tip = tip.offset(dir);
        }
        // Stunted tips still allow bonemeal-driven extension
        BlockPos next = tip.offset(dir);
        return world.getBlockState(next).isAir();
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Grow 1-5 segments on bonemeal (similar to twisting vines burst growth). Start growth from the tip.
        int segments = 1 + random.nextInt(5);
        tryGrowSegments(world, pos, state, segments, true);
    }

    private void tryGrowSegments(ServerWorld world, BlockPos origin, BlockState originState, int maxSegments, boolean fromBonemeal) {
        Direction dir = originState.get(FACING);
        BlockPos pos = origin;
        // Find the current tip in the chain along FACING
        while (world.getBlockState(pos.offset(dir)).isOf(this)) {
            pos = pos.offset(dir);
        }
        // Place forward up to maxSegments into air
        int placed = 0;
        int age = originState.contains(AGE) ? originState.get(AGE) : 0;
        while (placed < maxSegments && age <= 25) {
            BlockPos next = pos.offset(dir);
            if (!world.isAir(next)) break;
            // Current becomes body segment
            BlockState current = world.getBlockState(pos);
            if (current.isOf(this)) {
                world.setBlockState(pos, current.with(TIP, false).with(TIP_VINES, false));
            }
            // New tip grows by 1-2 age
            age = Math.min(25, age + 1 + world.random.nextInt(2));
            boolean tipVines = false; // recomputed after growth for chain rules
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
        // If bonemeal initiated growth, keep the new tip stunted so natural growth doesn't continue.
        BlockState finalState = world.getBlockState(pos);
        if (finalState.isOf(this)) {
            if (fromBonemeal) {
                world.setBlockState(pos, finalState.with(STUNTED, true).with(TIP_VINES, true));
            }
        }
        // Recompute flags for the whole chain based on stunted/matured and base-candidate rule
        updateChainTipFlags(world, origin);
    }

	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		// Do not replace with AIR here to avoid pre-empting loot drops on player breaks.
		// Attempt immediate reattachment (mutates world if successful).
		if (!this.canPlaceAt(state, world, pos)) {
			tryReattachChain(state, world, pos);
			// Even if reattach fails, defer actual breaking to scheduledTick to avoid racing with player drops.
		}
		// Schedule a survival check to handle horizontal reattachment/breaking reliably
		if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
			serverWorld.scheduleBlockTick(pos, this, 1);
		}

		// Recompute chain tip flags on ANY neighbor change to reflect new base-candidate conditions
		if (world instanceof net.minecraft.world.World w && !w.isClient()) {
			try {
				com.theendupdate.TemplateMod.LOGGER.info(
					"MoldCrawl neighborUpdate ENTER: pos={} neighborPos={} dir={} thisFacing={} isClient={} stateTip={} stateTipVines={}",
					pos.toShortString(), neighborPos.toShortString(), direction, state.get(FACING), false,
					state.getOrEmpty(TIP).orElse(false), state.getOrEmpty(TIP_VINES).orElse(false)
				);
				com.theendupdate.TemplateMod.LOGGER.info(
					"MoldCrawl neighborUpdate neighborBlock={}",
					net.minecraft.registry.Registries.BLOCK.getId(neighborState.getBlock())
				);
			} catch (Throwable ignored) {}
			// Fast-path: if the forward neighbor of this block changed, immediately recompute this block's tip/vines flags
			try {
				Direction f = state.get(FACING);
				// Always recompute the real chain tip from this segment both directions (robust against long chains)
				BlockPos left = pos;
				while (w.getBlockState(left.offset(f.getOpposite())).isOf(this)) {
					left = left.offset(f.getOpposite());
				}
				BlockPos tipPos = left;
				while (w.getBlockState(tipPos.offset(f)).isOf(this)) {
					tipPos = tipPos.offset(f);
				}
		BlockPos forwardPos = tipPos.offset(f);
				BlockState forwardState = w.getBlockState(forwardPos);
				boolean forwardIsAir = forwardState.isAir();
				boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
		boolean newTipVines = forwardIsAir || forwardHasFluid;
		// If we're not looking at the real tip (e.g., if the chain extended after cached base), correct it
		if (!w.getBlockState(tipPos).getOrEmpty(TIP).orElse(false)) {
				// Mark only the resolved tip as TIP, all others as body on this pass
				BlockPos scan = left;
				while (true) {
					BlockState cs = w.getBlockState(scan);
					if (!cs.isOf(this)) break;
					boolean isRealTip = scan.equals(tipPos);
					BlockState ns = cs.with(TIP, isRealTip).with(TIP_VINES, isRealTip && newTipVines);
					if (!ns.equals(cs)) {
						w.setBlockState(scan, ns, Block.NOTIFY_ALL);
					}
					if (scan.equals(tipPos)) break;
					scan = scan.offset(f);
				}
			}
				BlockState tipStateNow = w.getBlockState(tipPos);
				BlockState updatedTip = tipStateNow.with(TIP, true).with(TIP_VINES, newTipVines);
				if (!updatedTip.equals(tipStateNow)) {
					w.setBlockState(tipPos, updatedTip, Block.NOTIFY_ALL);
					w.updateNeighbors(tipPos, this);
				} else {
					// no change
				}
				// Also schedule a survival/visual tick at the resolved tip to guarantee reevaluation next tick
				if (world instanceof net.minecraft.server.world.ServerWorld sw) {
					sw.scheduleBlockTick(tipPos, this, 1);
				}
			} catch (Throwable ignored) {}

			updateChainTipFlags(w, pos);
			w.updateNeighbors(pos, this);
			// Return the freshly updated state from world (ensures we don't return a stale 'state')
			BlockState after = w.getBlockState(pos);
			return after;
		}
		return state;
	}

    // Survival tick: break when unsupported (matches vines behavior of disappearing without drops)
	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (this.canPlaceAt(state, world, pos)) {
			// Even when supported, recompute tip flags to react to neighbor placements (base-candidate texture rule)
			updateChainTipFlags(world, pos);
			return;
		}
		com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl scheduledTick unsupported at {} facing {}", pos.toShortString(), state.get(FACING));
		BlockState reattached = tryReattachChain(state, world, pos);
		if (reattached != null) {
			com.theendupdate.TemplateMod.LOGGER.info("MoldCrawl scheduledTick reattached at {} new facing {}", pos.toShortString(), reattached.get(FACING));
			return;
		}
		// No reattachment possible; break with chance like vines (loot table's random_chance doesn't run here)
		boolean drop = world.random.nextFloat() < 0.33f;
		world.breakBlock(pos, drop);
		// Explicitly schedule neighbors to guarantee horizontal cascade
		Direction f = state.get(FACING);
		BlockPos forwardPos = pos.offset(f);
		BlockPos backPos = pos.offset(f.getOpposite());
		if (world.getBlockState(forwardPos).isOf(this)) {
			world.scheduleBlockTick(forwardPos, this, 1);
		}
		if (world.getBlockState(backPos).isOf(this)) {
			world.scheduleBlockTick(backPos, this, 1);
		}
	}

	// Removed custom natural drop helper; drop rate now handled via breakBlock(pos, drop)

    // Intentionally no custom afterBreak; rely on loot table and scheduled ticks like the previous build

    // Intentionally no low-level neighborUpdate override; we handle visuals via getStateForNeighborUpdate

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
            if (current.equals(tip)) break;
            current = current.offset(facing);
        }

        // Recompute flags for flipped chain, then return the state at pos
        updateChainTipFlags((net.minecraft.world.World) world, pos);
        return ((net.minecraft.world.World)world).getBlockState(pos);
    }

    private void updateChainTipFlags(net.minecraft.world.WorldAccess world, BlockPos anyPos) {
        BlockState origin = world.getBlockState(anyPos);
        if (!origin.isOf(this)) return;
        Direction facing = origin.get(FACING);
        Direction back = facing.getOpposite();
        // Find chain bounds
        BlockPos base = anyPos;
        while (world.getBlockState(base.offset(back)).isOf(this)) {
            base = base.offset(back);
        }
        BlockPos tip = anyPos;
        while (world.getBlockState(tip.offset(facing)).isOf(this)) {
            tip = tip.offset(facing);
        }
        
        // Walk the chain, set TIP/TIP_VINES
        BlockPos cur = base;
        while (true) {
            BlockState st = world.getBlockState(cur);
            if (!st.isOf(this)) break;
            boolean isTip = cur.equals(tip);
            boolean tipVines = false;
            if (isTip) {
                // New rule: tip uses vines texture only when forward is open (air) or contains fluid; otherwise base texture.
                BlockPos forwardPos = cur.offset(facing);
                BlockState forwardState = world.getBlockState(forwardPos);
                boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
                boolean forwardIsAir = forwardState.isAir();
                tipVines = forwardIsAir || forwardHasFluid;
                
            }
            BlockState ns = st.with(TIP, isTip).with(TIP_VINES, tipVines);
                if (!ns.equals(st)) {
                    ((net.minecraft.world.World)world).setBlockState(cur, ns, Block.NOTIFY_ALL);
                }
            if (cur.equals(tip)) break;
            cur = cur.offset(facing);
        }
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            ((ServerWorld) world).scheduleBlockTick(pos, this, 1);
        }
    }

	// Removed alternate onStateReplaced signature to match working version

    // Low-level neighborUpdate overrides differ across mappings in 1.21.x; rely on getStateForNeighborUpdate instead.
    // Add mapping-safe neighborUpdate variants to ensure we see immediate updates across environments.
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos neighborPos, boolean moved) {
        
        handleNeighborChange(world, pos, state, neighborPos);
    }

    // Older/alternate signature seen across mappings
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos neighborPos) {
        
        handleNeighborChange(world, pos, state, neighborPos);
    }

	private void handleNeighborChange(World world, BlockPos pos, BlockState state, BlockPos neighborPos) {
		if (world.isClient()) return;
		// Resolve true base and tip, then apply the same immediate tip rule as before
		Direction f = state.get(FACING);
		BlockPos base = pos;
		while (world.getBlockState(base.offset(f.getOpposite())).isOf(this)) {
			base = base.offset(f.getOpposite());
		}
		BlockPos tipPos = base;
		while (world.getBlockState(tipPos.offset(f)).isOf(this)) {
			tipPos = tipPos.offset(f);
		}
		BlockPos forwardPos = tipPos.offset(f);
		BlockState forwardState = world.getBlockState(forwardPos);
		boolean forwardIsAir = forwardState.isAir();
		boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
		boolean newTipVines = forwardIsAir || forwardHasFluid;
		BlockState tipStateNow = world.getBlockState(tipPos);
		BlockState updatedTip = tipStateNow.with(TIP, true).with(TIP_VINES, newTipVines);
		if (!updatedTip.equals(tipStateNow)) {
			world.setBlockState(tipPos, updatedTip, Block.NOTIFY_ALL);
			world.updateNeighbors(tipPos, this);
		}
		// Also run chain-wide flags for consistency
		updateChainTipFlags(world, pos);
		if (world instanceof ServerWorld sw) {
			sw.scheduleBlockTick(pos, this, 1);
		}
	}

    // External hook: called by global events when any nearby block changes
    public static void reactToExternalChange(World world, BlockPos changedPos) {
        if (world.isClient()) return;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = changedPos.offset(d);
            BlockState st = world.getBlockState(neighbor);
            if (st.getBlock() instanceof MoldcrawlBlock mold) {
                
                mold.handleNeighborChange(world, neighbor, st, changedPos);
                if (world instanceof net.minecraft.server.world.ServerWorld sw) {
                    sw.scheduleBlockTick(neighbor, mold, 1);
                }
            }
        }
    }
}


