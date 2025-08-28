package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.world.ShadowClawTreeGenerator;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/**
 * Shadow Claw - a sapling-like plant that grows a massive shadow tree.
 */
public class ShadowClawBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<ShadowClawBlock> CODEC = createCodec(ShadowClawBlock::new);

    // Variant (0..3) selects one of four textures/models on placement
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, 3);

    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.25, 0.0, 0.25, 0.75, 0.8, 0.75);

    public ShadowClawBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(VARIANT, 0));
    }

    @Override
    public MapCodec<? extends PlantBlock> getCodec() {
        return CODEC;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        int variant = ctx.getWorld().getRandom().nextInt(4);
        return this.getDefaultState().with(VARIANT, variant);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        // Allow placement on any block with a solid top face (vanilla sapling behavior analogue)
        return Block.isFaceFullSquare(floor.getCollisionShape(world, pos), net.minecraft.util.math.Direction.UP);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Very slow natural growth and only if a valid 3x3 cluster exists
        if (random.nextInt(30) == 0) {
            BlockPos anchor = findClusterAnchor(world, pos);
            if (anchor != null) {
                BlockPos center = anchor.add(1, 0, 1);
                clearCluster(world, anchor);
                ShadowClawTreeGenerator.generate(world, center, random);
            }
        }
    }

    // Fertilizable (bonemeal) behavior
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Require a 3x3 of Shadow Claw saplings similar to dark oak's 2x2
        BlockPos anchor = findClusterAnchor(world, pos);
        if (anchor == null) {
            return; // no 3x3 cluster → bonemeal has no effect
        }
        BlockPos center = anchor.add(1, 0, 1);
        clearCluster(world, anchor);
        ShadowClawTreeGenerator.generate(world, center, random);
    }

    private BlockPos findClusterAnchor(WorldView world, BlockPos pos) {
        // Search for a 3x3 of SHADOW_CLAW with this pos inside; return NW corner if found
        for (int ox = -2; ox <= 0; ox++) {
            for (int oz = -2; oz <= 0; oz++) {
                BlockPos nw = pos.add(ox, 0, oz);
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

    private boolean isFull3x3(WorldView world, BlockPos nw) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos check = nw.add(dx, 0, dz);
                if (!world.getBlockState(check).isOf(ModBlocks.SHADOW_CLAW)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void clearCluster(ServerWorld world, BlockPos nw) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = nw.add(dx, 0, dz);
                world.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
        }
    }
}


