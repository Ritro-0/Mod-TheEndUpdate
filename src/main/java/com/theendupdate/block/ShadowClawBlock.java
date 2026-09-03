package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.world.ShadowClawTreeGenerator;
import com.theendupdate.registry.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shadow Claw - a sapling-like plant that grows a massive shadow tree.
 */
public class ShadowClawBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<ShadowClawBlock> CODEC = simpleCodec(ShadowClawBlock::new);

    // Variant (0..3) selects one of four textures/models on placement
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    private static final VoxelShape SHAPE = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.8, 0.75);

    public ShadowClawBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // deterministic seed from time+pos so client/server agree on the variant - avoids visual stuttering
        long worldTime = ctx.getLevel().getGameTime();
        long seed = (worldTime + ctx.getClickedPos().asLong()) * 25214903917L + 11L;
        int variant = (int) (Long.rotateRight(seed, 16) & 3L);
        return this.defaultBlockState().setValue(VARIANT, variant);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        // variant already set deterministically in getStateForPlacement, nothing extra needed here
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        // any solid top face, same as vanilla saplings
        return Block.isFaceFull(floor.getCollisionShape(world, pos), net.minecraft.core.Direction.UP);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // slow growth, needs a full 3x3 cluster to trigger
        if (random.nextInt(30) == 0) {
            BlockPos anchor = findClusterAnchor(world, pos);
            if (anchor != null) {
                BlockPos center = anchor.offset(1, 0, 1);
                // snapshot exact states (preserves variants) so we can roll back on failure
                Map<BlockPos, BlockState> snapshot = new HashMap<>(9);
                for (int dx = 0; dx < 3; dx++) {
                    for (int dz = 0; dz < 3; dz++) {
                        BlockPos p = anchor.offset(dx, 0, dz);
                        snapshot.put(p, world.getBlockState(p));
                    }
                }
                clearCluster(world, anchor);
                ShadowClawTreeGenerator.generate(world, center, random);
                // treat it as a success if any of the first few trunk blocks got placed
                boolean success = false;
                for (int y = 0; y <= 2 && !success; y++) {
                    if (world.getBlockState(center.above(y)).is(ModBlocks.SHADOW_CRYPTOMYCOTA)) success = true;
                }
                if (!success) {
                    for (Map.Entry<BlockPos, BlockState> e : snapshot.entrySet()) {
                        world.setBlock(e.getKey(), e.getValue(), 3);
                    }
                }
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        // needs the 3x3 cluster, plus a chance gate so bonemeal isn't guaranteed (vanilla-like)
        return findClusterAnchor(world, pos) != null && random.nextFloat() < 0.45f;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        // needs a 3x3 of saplings, unlike dark oak's 2x2
        BlockPos anchor = findClusterAnchor(world, pos);
        if (anchor == null) {
            return;
        }
        BlockPos center = anchor.offset(1, 0, 1);
        Map<BlockPos, BlockState> snapshot = new HashMap<>(9);
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = anchor.offset(dx, 0, dz);
                snapshot.put(p, world.getBlockState(p));
            }
        }
        BlockPos trunkBase = center;
        clearCluster(world, anchor);
        ShadowClawTreeGenerator.generate(world, trunkBase, random);
        boolean success = false;
        for (int y = 0; y <= 2 && !success; y++) {
            if (world.getBlockState(trunkBase.above(y)).is(ModBlocks.SHADOW_CRYPTOMYCOTA)) success = true;
        }
        if (!success) {
            // exact rollback, so variants don't get re-rolled
            for (Map.Entry<BlockPos, BlockState> e : snapshot.entrySet()) {
                world.setBlock(e.getKey(), e.getValue(), 3);
            }
        }
    }

    private BlockPos findClusterAnchor(LevelReader world, BlockPos pos) {
        // find a 3x3 of SHADOW_CLAW containing pos, return the NW corner
        for (int ox = -2; ox <= 0; ox++) {
            for (int oz = -2; oz <= 0; oz++) {
                BlockPos nw = pos.offset(ox, 0, oz);
                if (containsPos(nw, pos) && isFull3x3(world, nw)) {
                    return nw;
                }
            }
        }
        return null;
    }

    private boolean containsPos(BlockPos nw, BlockPos p) {
        return p.getX() >= nw.getX() && p.getX() <= nw.getX() + 2 && p.getZ() >= nw.getZ() && p.getZ() <= nw.getZ() + 2;
    }

    private boolean isFull3x3(LevelReader world, BlockPos nw) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos check = nw.offset(dx, 0, dz);
                if (!world.getBlockState(check).is(ModBlocks.SHADOW_CLAW)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void clearCluster(ServerLevel world, BlockPos nw) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = nw.offset(dx, 0, dz);
                world.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}


