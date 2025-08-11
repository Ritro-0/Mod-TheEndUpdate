package com.theendupdate.block;

import com.mojang.serialization.MapCodec;
import com.theendupdate.registry.ModBlocks;
import com.theendupdate.world.TendrilSporeTreeGenerator;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * Tendril Core - Final stage of the Tendril growth cycle
 * When fully mature, grows into a Tendril Spore tree
 */
public class TendrilCoreBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<TendrilCoreBlock> CODEC = createCodec(TendrilCoreBlock::new);
    
    // Growth stage (0-7, grows into tree at 7)
    public static final IntProperty AGE = Properties.AGE_7;
    
    // Whether growth has been stunted with shears
    public static final BooleanProperty STUNTED = BooleanProperty.of("stunted");
    
    // Shape for the large plant
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.1, 0.0, 0.1, 0.9, 1.0, 0.9);

    public TendrilCoreBlock(Settings settings) {
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
        // Same placement rules as other Tendril stages
        return floor.isOf(Blocks.END_STONE) || 
               floor.isOf(ModBlocks.END_MIRE) || 
               floor.isOf(ModBlocks.MOLD_BLOCK) ||
               floor.isIn(net.minecraft.registry.tag.BlockTags.DIRT) ||
               floor.isIn(net.minecraft.registry.tag.BlockTags.SAND);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return !state.get(STUNTED) && state.get(AGE) < 7;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(STUNTED)) {
            int age = state.get(AGE);
            if (age < 7) {
                // 15% chance to grow each tick (slowest growth stage)
                if (random.nextInt(7) == 0) {
                    if (age == 6) {
                        // Ready to become a tree! Generate Tendril Spore tree
                        generateTree(world, pos, random);
                    } else {
                        world.setBlockState(pos, state.with(AGE, age + 1));
                    }
                }
                // Rely solely on random ticks (no scheduled self-ticks)
            }
        }
    }

    // Remove scheduled ticks and initial scheduling; match vanilla plants

    private void generateTree(ServerWorld world, BlockPos pos, Random random) {
        com.theendupdate.TemplateMod.LOGGER.info("Tendril Core growing into Tendril Spore tree at " + pos);
        
        // Remove the core block first
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        
        // Generate the tree using our custom tree generator
        TendrilSporeTreeGenerator.generateTree(world, pos, random);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack heldItem = player.getStackInHand(player.getActiveHand());
        
        // Right-click with shears to stunt growth
        if (heldItem.isOf(Items.SHEARS)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(STUNTED, true));
                com.theendupdate.TemplateMod.LOGGER.info("Tendril Core growth stunted with shears at " + pos);
                
                // Damage the shears
                heldItem.damage(1, player, player.getActiveHand());
            }
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.PASS;
    }

    // Fertilizable implementation
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
                    // Bonemeal instantly makes it grow into a tree
                    generateTree(world, pos, random);
                } else {
                    world.setBlockState(pos, state.with(AGE, Math.min(7, age + random.nextInt(2) + 1)));
                }
            }
        }
    }
}
