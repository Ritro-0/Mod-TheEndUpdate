package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Tendril Sprout - First stage of the Tendril growth cycle
 * Can be placed on end stone, end mire, mold, etc.
 * Grows into Tendril Thread when ready
 */
public class TendrilSproutBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<TendrilSproutBlock> CODEC = createCodec(TendrilSproutBlock::new);
    
    // Growth stage (0-7, grows into next stage at 7)
    public static final IntProperty AGE = Properties.AGE_7;
    
    // Whether growth has been stunted with shears
    public static final BooleanProperty STUNTED = BooleanProperty.of("stunted");
    
    // Shape for the small plant
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.25, 0.0, 0.25, 0.75, 0.4, 0.75);

    public TendrilSproutBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
            .with(AGE, 0)
            .with(STUNTED, false));
    }

    @Override
    public MapCodec<? extends PlantBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE, STUNTED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        // Allow placement on any block with a solid top face
        return Block.isFaceFullSquare(floor.getCollisionShape(world, pos), net.minecraft.util.math.Direction.UP);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        // Only tick if not stunted and not fully grown
        return !state.get(STUNTED) && state.get(AGE) < 7;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(STUNTED)) {
            int age = state.get(AGE);
            if (age < 7) {
                // 25% chance to grow each tick (similar to crops)
                if (random.nextInt(4) == 0) {
                    if (age == 6) {
                        // Ready to become Tendril Thread
                        world.setBlockState(pos, ModBlocks.TENDRIL_THREAD.getDefaultState());
                        com.theendupdate.TemplateMod.LOGGER.info("Tendril Sprout grew into Tendril Thread at " + pos);
                    } else {
                        world.setBlockState(pos, state.with(AGE, age + 1));
                    }
                }
                // Rely solely on random ticks (no scheduled self-ticks)
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack heldItem = player.getStackInHand(player.getActiveHand());
        
        // Right-click with shears to stunt growth
        if (heldItem.isOf(Items.SHEARS)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(STUNTED, true));
                com.theendupdate.TemplateMod.LOGGER.info("Tendril Sprout growth stunted with shears at " + pos);
                
                // Damage the shears
                heldItem.damage(1, player, player.getActiveHand());
            }
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }

    // Fertilizable implementation (bonemeal can speed growth)
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return !state.get(STUNTED) && state.get(AGE) < 7;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return !state.get(STUNTED) && state.get(AGE) < 7;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if (!state.get(STUNTED)) {
            int age = state.get(AGE);
            if (age < 7) {
                if (age == 6) {
                    // Bonemeal makes it grow into next stage immediately
                    world.setBlockState(pos, ModBlocks.TENDRIL_THREAD.getDefaultState());
                } else {
                    world.setBlockState(pos, state.with(AGE, Math.min(7, age + random.nextInt(2) + 1)));
                }
            }
        }
    }
}
