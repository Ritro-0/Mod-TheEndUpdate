package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tendril Sprout - First stage of the Tendril growth cycle
 * Can be placed on end stone, end mire, mold, etc.
 * Grows into Tendril Thread when ready
 */
public class TendrilSproutBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<TendrilSproutBlock> CODEC = simpleCodec(TendrilSproutBlock::new);
    
    // 0-7, grows into next stage at 7
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    
    // stunted via shears
    public static final BooleanProperty STUNTED = BooleanProperty.create("stunted");
    
    // small plant, 4x4 by 14px
    private static final VoxelShape SHAPE = Shapes.box(0.375, 0.0, 0.375, 0.625, 0.875, 0.625);

    public TendrilSproutBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(AGE, 0)
            .setValue(STUNTED, false));
    }

    @Override
    public MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, STUNTED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return Block.isFaceFull(floor.getCollisionShape(world, pos), net.minecraft.core.Direction.UP);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(STUNTED) && state.getValue(AGE) < 7;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!state.getValue(STUNTED)) {
            int age = state.getValue(AGE);
            if (age < 7) {
                // 25% chance to grow each tick (similar to crops)
                if (random.nextInt(4) == 0) {
                    if (age == 6) {
                        // grown up, becomes Tendril Thread
                        world.setBlockAndUpdate(pos, ModBlocks.TENDRIL_THREAD.defaultBlockState());
                    } else {
                        world.setBlockAndUpdate(pos, state.setValue(AGE, age + 1));
                    }
                }
                // random ticks only, no scheduled self-ticks
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
        
        if (heldItem.is(Items.SHEARS) && !state.getValue(STUNTED)) {
            if (!world.isClientSide()) {
                world.setBlockAndUpdate(pos, state.setValue(STUNTED, true));
                heldItem.hurtAndBreak(1, player, player.getUsedItemHand());
                world.playSound(null, pos, net.minecraft.sounds.SoundEvents.GROWING_PLANT_CROP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return !state.getValue(STUNTED) && state.getValue(AGE) < 7;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return !state.getValue(STUNTED) && state.getValue(AGE) < 7;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        if (!state.getValue(STUNTED)) {
            int age = state.getValue(AGE);
            if (age < 7) {
                // if this pushes age >= 6, transition immediately instead of overshooting past 7
                int increment = 1 + random.nextInt(2); // +1 or +2
                int newAge = Math.min(7, age + increment);
                if (age == 6 || newAge >= 6) {
                    world.setBlockAndUpdate(pos, ModBlocks.TENDRIL_THREAD.defaultBlockState());
                } else {
                    world.setBlockAndUpdate(pos, state.setValue(AGE, newAge));
                }
            }
        }
    }
}
