package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Moldcrawl - a horizontal vine-like plant that extends sideways.
 * Simplified implementation that mirrors twisting vines behavior, but along a horizontal direction.
 */
public class MoldcrawlBlock extends Block implements BonemealableBlock {
    public static final MapCodec<MoldcrawlBlock> CODEC = simpleCodec(MoldcrawlBlock::new);

    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_25; // 0..25, like twisting vines
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final BooleanProperty STUNTED = BooleanProperty.create("stunted");
    // when true (and TIP), the tip renders with the "vines" texture instead of the base one
    public static final BooleanProperty TIP_VINES = BooleanProperty.create("tip_vines");
    // natural growth soft cap per chain, 1-5. bonemeal ignores this
    public static final IntegerProperty NATURAL_CAP = IntegerProperty.create("natural_cap", 1, 5);

    // thin, non-colliding outline to look like a vine segment
    private static final VoxelShape THIN_X = Shapes.box(0.0, 0.25, 0.25, 1.0, 0.75, 0.75);
    private static final VoxelShape THIN_Z = Shapes.box(0.25, 0.25, 0.0, 0.75, 0.75, 1.0);

    public MoldcrawlBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(AGE, 0)
            .setValue(TIP, true)
            .setValue(STUNTED, false)
            .setValue(TIP_VINES, false)
            .setValue(NATURAL_CAP, 3)
        );
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE, TIP, STUNTED, TIP_VINES, NATURAL_CAP);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // mirrors vines: vanish when unsupported, but try reattaching the chain first
        if (!this.canSurvive(state, world, pos)) {
            BlockState reattached = tryReattachChain(state, world, pos);
			if (reattached == null) {
				boolean drop = world.getRandom().nextFloat() < 0.33f;
				world.destroyBlock(pos, drop);
				// schedule neighbors explicitly to guarantee horizontal cascade
				Direction f = state.getValue(FACING);
				BlockPos forwardPos = pos.relative(f);
				BlockPos backPos = pos.relative(f.getOpposite());
				if (world.getBlockState(forwardPos).is(this)) {
					world.scheduleTick(forwardPos, this, 1);
				}
				if (world.getBlockState(backPos).is(this)) {
					world.scheduleTick(backPos, this, 1);
				}
			}
            return;
        }
        // only the tip grows, unless stunted or fully matured
        if (state.getValue(TIP) && !state.getValue(STUNTED) && state.getValue(AGE) < 25) {
            // length bias: usually 1-3, 4 sporadic, 5 rare - never exceeds 5 naturally
            Direction f = state.getValue(FACING);
            BlockPos base = pos;
            while (world.getBlockState(base.relative(f.getOpposite())).is(this)) {
                base = base.relative(f.getOpposite());
            }
            BlockPos tip = base;
            int length = 1; // include base
            while (world.getBlockState(tip.relative(f)).is(this)) {
                tip = tip.relative(f);
                length++;
            }
            BlockState baseState = world.getBlockState(base);
            int naturalCap = baseState.getOptionalValue(NATURAL_CAP).orElse(3);
            if (length < naturalCap) {
                // ~20%/tick base growth gate
                if (random.nextInt(5) == 0) {
                    // rarity gates beyond length 3
                    boolean allowed = true;
                    if (length == 3 && naturalCap >= 4) {
                        allowed = random.nextInt(100) < 12; // ~12% from 3->4
                    } else if (length == 4 && naturalCap >= 5) {
                        allowed = random.nextInt(1000) < 38; // ~3.8% from 4->5
                    }
                    if (allowed) {
                        int maxSegments = 1 + random.nextInt(2); // burst of 1-2
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
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        return (dir.getAxis() == Direction.Axis.X) ? THIN_X : THIN_Z;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // no collision, like vines/twisting vines
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalDirection();
        // per-chain natural cap: 1/2/3 roughly equal (slight nudge to 3), small tails for 4 (~3%) and 5 (~1%)
        int roll = ctx.getLevel().getRandom().nextInt(10000); // basis points
        int cap;
        if (roll < 3300) cap = 1;             // ~33%
        else if (roll < 6600) cap = 2;        // ~33%
        else if (roll < 9700) cap = 3;        // ~31%
        else if (roll < 9700 + 300) cap = 4;  // ~3%
        else cap = 5;                          // ~1%
        return this.defaultBlockState().setValue(FACING, facing).setValue(NATURAL_CAP, cap);
    }

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
		// needs support from the back (opposite facing); can also stack on itself
		Direction back = state.getValue(FACING).getOpposite();
		BlockPos supportPos = pos.relative(back);
		BlockState support = world.getBlockState(supportPos);
		if (support.is(this)) return true;
		boolean solid = support.isFaceSturdy(world, supportPos, back.getOpposite());
		return solid;
	}

    // let bonemeal right-clicks fall through to the Fertilizable handler
    // no @Override - keeps this compatible across mapping signatures
    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        // shears stunt the tip, switching it to the vines texture
        if (stack.is(Items.SHEARS) && state.getValue(TIP) && !state.getValue(STUNTED)) {
            if (!world.isClientSide()) {
                world.setBlockAndUpdate(pos, state.setValue(STUNTED, true).setValue(TIP_VINES, true));
                stack.hurtAndBreak(1, player, hand);
                world.playSound(null, pos, net.minecraft.sounds.SoundEvents.GROWING_PLANT_CROP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // Mapping-safe override variant used by 1.21.8 that omits Hand param
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(Items.SHEARS) && state.getValue(TIP) && !state.getValue(STUNTED)) {
            if (!world.isClientSide()) {
                world.setBlockAndUpdate(pos, state.setValue(STUNTED, true).setValue(TIP_VINES, true));
                stack.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
                world.playSound(null, pos, net.minecraft.sounds.SoundEvents.GROWING_PLANT_CROP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        // works from any segment - walk to the tip and check the next space, even if stunted
        Direction dir = state.getValue(FACING);
        BlockPos tip = pos;
        while (world.getBlockState(tip.relative(dir)).is(this)) {
            tip = tip.relative(dir);
        }
        BlockPos next = tip.relative(dir);
        return world.getBlockState(next).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        // same condition as isValidBonemealTarget
        Direction dir = state.getValue(FACING);
        BlockPos tip = pos;
        while (world.getBlockState(tip.relative(dir)).is(this)) {
            tip = tip.relative(dir);
        }
        BlockPos next = tip.relative(dir);
        return world.getBlockState(next).isAir();
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        // 1-5 segments, similar to twisting vines burst growth, starting from the tip
        int segments = 1 + random.nextInt(5);
        tryGrowSegments(world, pos, state, segments, true);
    }

    private void tryGrowSegments(ServerLevel world, BlockPos origin, BlockState originState, int maxSegments, boolean fromBonemeal) {
        Direction dir = originState.getValue(FACING);
        BlockPos pos = origin;
        while (world.getBlockState(pos.relative(dir)).is(this)) {
            pos = pos.relative(dir);
        }
        int placed = 0;
        int age = originState.hasProperty(AGE) ? originState.getValue(AGE) : 0;
        while (placed < maxSegments && age <= 25) {
            BlockPos next = pos.relative(dir);
            if (!world.isEmptyBlock(next)) break;
            // old tip becomes a body segment now that a new tip is being placed
            BlockState current = world.getBlockState(pos);
            if (current.is(this)) {
                world.setBlockAndUpdate(pos, current.setValue(TIP, false).setValue(TIP_VINES, false));
            }
            age = Math.min(25, age + 1 + world.getRandom().nextInt(2)); // new tip's age, grows by 1-2
            boolean tipVines = false; // recomputed after growth for chain rules
            BlockState newTip = this.defaultBlockState()
                .setValue(FACING, dir)
                .setValue(AGE, age)
                .setValue(TIP, true)
                .setValue(STUNTED, false)
                .setValue(TIP_VINES, tipVines);
            world.setBlockAndUpdate(next, newTip);
            pos = next;
            placed++;
        }
        // bonemeal-grown tips stay stunted so natural growth doesn't continue them further
        BlockState finalState = world.getBlockState(pos);
        if (finalState.is(this)) {
            if (fromBonemeal) {
                world.setBlockAndUpdate(pos, finalState.setValue(STUNTED, true).setValue(TIP_VINES, true));
            }
        }
        updateChainTipFlags(world, origin);
    }

	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
		// don't replace with AIR here - that would pre-empt loot drops on player breaks
		if (!this.canSurvive(state, world, pos)) {
			// try reattaching immediately; if it fails, defer the actual break to scheduledTick
			// so it doesn't race with player break drops
			tryReattachChain(state, world, pos);
		}
		if (world instanceof net.minecraft.server.level.ServerLevel serverWorld) {
			serverWorld.scheduleTick(pos, this, 1);
		}

		// recompute on any neighbor change, since base-candidate conditions may have shifted
		if (world instanceof net.minecraft.world.level.Level w && !w.isClientSide()) {
			if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
				try {
					com.theendupdate.TheEndUpdate.LOGGER.info(
						"MoldCrawl neighborUpdate ENTER: pos={} neighborPos={} dir={} thisFacing={} isClient={} stateTip={} stateTipVines={}",
						pos.toShortString(), neighborPos.toShortString(), direction, state.getValue(FACING), false,
						state.getOptionalValue(TIP).orElse(false), state.getOptionalValue(TIP_VINES).orElse(false)
					);
					com.theendupdate.TheEndUpdate.LOGGER.info(
						"MoldCrawl neighborUpdate neighborBlock={}",
						net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(neighborState.getBlock())
					);
				} catch (Throwable ignored) {}
			}
			// fast path: forward neighbor changed, recompute this block's tip/vines flags right away
			try {
				Direction f = state.getValue(FACING);
				// walk both directions from this segment - robust even for long chains
				BlockPos left = pos;
				while (w.getBlockState(left.relative(f.getOpposite())).is(this)) {
					left = left.relative(f.getOpposite());
				}
				BlockPos tipPos = left;
				while (w.getBlockState(tipPos.relative(f)).is(this)) {
					tipPos = tipPos.relative(f);
				}
		BlockPos forwardPos = tipPos.relative(f);
				BlockState forwardState = w.getBlockState(forwardPos);
				boolean forwardIsAir = forwardState.isAir();
				boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
		boolean newTipVines = forwardIsAir || forwardHasFluid;
		// if this isn't actually the real tip (chain extended after the cached base), correct it
		if (!w.getBlockState(tipPos).getOptionalValue(TIP).orElse(false)) {
				// mark only the resolved tip as TIP, everything else as body, on this pass
				BlockPos scan = left;
				while (true) {
					BlockState cs = w.getBlockState(scan);
					if (!cs.is(this)) break;
					boolean isRealTip = scan.equals(tipPos);
					BlockState ns = cs.setValue(TIP, isRealTip).setValue(TIP_VINES, isRealTip && newTipVines);
					if (!ns.equals(cs)) {
						w.setBlock(scan, ns, Block.UPDATE_ALL);
					}
					if (scan.equals(tipPos)) break;
					scan = scan.relative(f);
				}
			}
				BlockState tipStateNow = w.getBlockState(tipPos);
				BlockState updatedTip = tipStateNow.setValue(TIP, true).setValue(TIP_VINES, newTipVines);
				if (!updatedTip.equals(tipStateNow)) {
					w.setBlock(tipPos, updatedTip, Block.UPDATE_ALL);
					w.updateNeighborsAt(tipPos, this);
				}
				// schedule a tick at the resolved tip too, to guarantee reevaluation next tick
				if (world instanceof net.minecraft.server.level.ServerLevel sw) {
					sw.scheduleTick(tipPos, this, 1);
				}
			} catch (Throwable ignored) {}

			updateChainTipFlags(w, pos);
			w.updateNeighborsAt(pos, this);
			// re-fetch from world so we don't return a stale 'state'
			BlockState after = w.getBlockState(pos);
			return after;
		}
		return state;
	}

    // survival tick: break when unsupported, matching vines' disappear-without-drops behavior
	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		if (this.canSurvive(state, world, pos)) {
			// recompute tip flags even when supported, to react to neighbor placements
			updateChainTipFlags(world, pos);
			return;
		}
		if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
			com.theendupdate.TheEndUpdate.LOGGER.info("MoldCrawl scheduledTick unsupported at {} facing {}", pos.toShortString(), state.getValue(FACING));
		}
		BlockState reattached = tryReattachChain(state, world, pos);
		if (reattached != null) {
			if (com.theendupdate.TheEndUpdate.DEBUG_MODE) {
				com.theendupdate.TheEndUpdate.LOGGER.info("MoldCrawl scheduledTick reattached at {} new facing {}", pos.toShortString(), reattached.getValue(FACING));
			}
			return;
		}
		// no reattachment possible - break with vines-style chance since loot table's random_chance doesn't run here
		boolean drop = world.getRandom().nextFloat() < 0.33f;
		world.destroyBlock(pos, drop);
		// schedule neighbors explicitly to guarantee horizontal cascade
		Direction f = state.getValue(FACING);
		BlockPos forwardPos = pos.relative(f);
		BlockPos backPos = pos.relative(f.getOpposite());
		if (world.getBlockState(forwardPos).is(this)) {
			world.scheduleTick(forwardPos, this, 1);
		}
		if (world.getBlockState(backPos).is(this)) {
			world.scheduleTick(backPos, this, 1);
		}
	}

    // intentionally no custom afterBreak - relies on loot table + scheduled ticks

    // intentionally no low-level neighborUpdate override - visuals go through getStateForNeighborUpdate

	private BlockState tryReattachChain(BlockState state, net.minecraft.world.level.LevelAccessor world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction back = facing.getOpposite();

        BlockPos base = pos;
        while (world.getBlockState(base.relative(back)).is(this)) {
            base = base.relative(back);
        }
        BlockPos tip = pos;
        while (world.getBlockState(tip.relative(facing)).is(this)) {
            tip = tip.relative(facing);
        }

		// can we flip and attach at the tip's forward side instead?
		BlockPos forwardPos = tip.relative(facing);
		BlockState forward = world.getBlockState(forwardPos);
		boolean forwardSupportBackFace = forward.isFaceSturdy(world, forwardPos, back);
		boolean forwardSupportFacingFace = forward.isFaceSturdy(world, forwardPos, facing);
		boolean forwardSupport = forwardSupportBackFace || forwardSupportFacingFace;
		if (!forwardSupport) {
			return null;
		}

		// flip the whole chain to face the opposite way; new tip ends up at the former base
		Direction newFacing = facing.getOpposite();
		BlockPos current = base;
        while (true) {
            BlockState currentState = world.getBlockState(current);
            int age = currentState.getOptionalValue(AGE).orElse(0);
            boolean isNewTip = current.equals(base);
            boolean tipVines = isNewTip && (currentState.getOptionalValue(STUNTED).orElse(false) || age >= 25);
            BlockState newState = this.defaultBlockState()
                .setValue(FACING, newFacing)
                .setValue(AGE, age)
                .setValue(TIP, isNewTip)
                .setValue(STUNTED, false)
                .setValue(TIP_VINES, tipVines);
            ((net.minecraft.world.level.Level)world).setBlock(current, newState, Block.UPDATE_ALL);
            if (current.equals(tip)) break;
            current = current.relative(facing);
        }

        updateChainTipFlags((net.minecraft.world.level.Level) world, pos);
        return ((net.minecraft.world.level.Level)world).getBlockState(pos);
    }

    private void updateChainTipFlags(net.minecraft.world.level.LevelAccessor world, BlockPos anyPos) {
        BlockState origin = world.getBlockState(anyPos);
        if (!origin.is(this)) return;
        Direction facing = origin.getValue(FACING);
        Direction back = facing.getOpposite();
        BlockPos base = anyPos;
        while (world.getBlockState(base.relative(back)).is(this)) {
            base = base.relative(back);
        }
        BlockPos tip = anyPos;
        while (world.getBlockState(tip.relative(facing)).is(this)) {
            tip = tip.relative(facing);
        }

        BlockPos cur = base;
        while (true) {
            BlockState st = world.getBlockState(cur);
            if (!st.is(this)) break;
            boolean isTip = cur.equals(tip);
            boolean tipVines = false;
            if (isTip) {
                // tip uses the vines texture only when forward is open (air) or fluid, base texture otherwise
                BlockPos forwardPos = cur.relative(facing);
                BlockState forwardState = world.getBlockState(forwardPos);
                boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
                boolean forwardIsAir = forwardState.isAir();
                tipVines = forwardIsAir || forwardHasFluid;
                
            }
            BlockState ns = st.setValue(TIP, isTip).setValue(TIP_VINES, tipVines);
                if (!ns.equals(st)) {
                    ((net.minecraft.world.level.Level)world).setBlock(cur, ns, Block.UPDATE_ALL);
                }
            if (cur.equals(tip)) break;
            cur = cur.relative(facing);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (!world.isClientSide()) {
            ((ServerLevel) world).scheduleTick(pos, this, 1);
        }
    }

    // low-level neighborUpdate signature differs across 1.21.x mappings; provide both variants
    // so we get immediate updates regardless of environment
    public void neighborUpdate(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos neighborPos, boolean moved) {
        handleNeighborChange(world, pos, state, neighborPos);
    }

    // older/alternate signature seen across mappings
    public void neighborUpdate(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos neighborPos) {
        handleNeighborChange(world, pos, state, neighborPos);
    }

	private void handleNeighborChange(Level world, BlockPos pos, BlockState state, BlockPos neighborPos) {
		if (world.isClientSide()) return;
		// resolve true base/tip, then apply the same immediate tip rule
		Direction f = state.getValue(FACING);
		BlockPos base = pos;
		while (world.getBlockState(base.relative(f.getOpposite())).is(this)) {
			base = base.relative(f.getOpposite());
		}
		BlockPos tipPos = base;
		while (world.getBlockState(tipPos.relative(f)).is(this)) {
			tipPos = tipPos.relative(f);
		}
		BlockPos forwardPos = tipPos.relative(f);
		BlockState forwardState = world.getBlockState(forwardPos);
		boolean forwardIsAir = forwardState.isAir();
		boolean forwardHasFluid = !forwardState.getFluidState().isEmpty();
		boolean newTipVines = forwardIsAir || forwardHasFluid;
		BlockState tipStateNow = world.getBlockState(tipPos);
		BlockState updatedTip = tipStateNow.setValue(TIP, true).setValue(TIP_VINES, newTipVines);
		if (!updatedTip.equals(tipStateNow)) {
			world.setBlock(tipPos, updatedTip, Block.UPDATE_ALL);
			world.updateNeighborsAt(tipPos, this);
		}
		updateChainTipFlags(world, pos);
		if (world instanceof ServerLevel sw) {
			sw.scheduleTick(pos, this, 1);
		}
	}

    // hook for global events to call when a nearby block changes
    public static void reactToExternalChange(Level world, BlockPos changedPos) {
        if (world.isClientSide()) return;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = changedPos.relative(d);
            BlockState st = world.getBlockState(neighbor);
            if (st.getBlock() instanceof MoldcrawlBlock mold) {
                mold.handleNeighborChange(world, neighbor, st, changedPos);
                if (world instanceof net.minecraft.server.level.ServerLevel sw) {
                    sw.scheduleTick(neighbor, mold, 1);
                }
            }
        }
    }
}


