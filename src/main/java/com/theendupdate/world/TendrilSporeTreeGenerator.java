package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Generates the Tendril Spore tree structure
 * Creates organic formations of Ethereal Sporocarps (logs) without leaves
 */
public final class TendrilSporeTreeGenerator {
    private TendrilSporeTreeGenerator() {}

    public static void generateTree(ServerWorld world, BlockPos startPos, Random random) {
        com.theendupdate.TemplateMod.LOGGER.info("Generating Tendril Spore tree at " + startPos);
        
        // Check if there's enough space (basic 5x5x8 area check)
        if (!hasEnoughSpace(world, startPos)) {
            com.theendupdate.TemplateMod.LOGGER.info("Not enough space for Tendril Spore tree at " + startPos);
            // If not enough space, place the core back
            world.setBlockState(startPos, ModBlocks.TENDRIL_CORE.getDefaultState());
            return;
        }
        
        // Generate the main trunk (3-6 blocks tall)
        int trunkHeight = 3 + random.nextInt(4);
        generateTrunk(world, startPos, trunkHeight);
        
        // Generate side branches (2-4 branches)
        int branchCount = 2 + random.nextInt(3);
        generateBranches(world, startPos, trunkHeight, branchCount, random);
        
        // Generate some decorative tendrils
        generateTendrils(world, startPos, trunkHeight, random);
        
        com.theendupdate.TemplateMod.LOGGER.info("Tendril Spore tree generation completed at " + startPos);
    }

    private static boolean hasEnoughSpace(ServerWorld world, BlockPos startPos) {
        // Check a 5x5x8 area around the tree position
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos checkPos = startPos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    // Must be air or replaceable
                    if (!state.isAir() && !state.isReplaceable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void generateTrunk(ServerWorld world, BlockPos startPos, int height) {
        // Generate main vertical trunk
        for (int y = 0; y < height; y++) {
            BlockPos pos = startPos.up(y);
            world.setBlockState(pos, ModBlocks.ETHEREAL_SPOROCARPS.getDefaultState()
                .with(com.theendupdate.block.EtherealSporocarpsBlock.AXIS, Direction.Axis.Y));
        }
    }

    private static void generateBranches(ServerWorld world, BlockPos startPos, int trunkHeight, int branchCount, Random random) {
        // Generate branches starting from about halfway up the trunk
        int startHeight = Math.max(1, trunkHeight / 2);
        
        for (int i = 0; i < branchCount; i++) {
            // Random height for this branch
            int branchY = startHeight + random.nextInt(Math.max(1, trunkHeight - startHeight));
            
            // Random direction (North, South, East, West)
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            Direction direction = directions[random.nextInt(directions.length)];
            
            // Random branch length (1-3 blocks)
            int branchLength = 1 + random.nextInt(3);
            
            generateSingleBranch(world, startPos.up(branchY), direction, branchLength);
        }
    }

    private static void generateSingleBranch(ServerWorld world, BlockPos startPos, Direction direction, int length) {
        for (int i = 1; i <= length; i++) {
            BlockPos branchPos = startPos.offset(direction, i);
            
            // Check if the position is valid
            if (world.getBlockState(branchPos).isAir() || world.getBlockState(branchPos).isReplaceable()) {
                // Set the log with horizontal axis
                Direction.Axis axis = direction.getAxis();
                world.setBlockState(branchPos, ModBlocks.ETHEREAL_SPOROCARPS.getDefaultState()
                    .with(com.theendupdate.block.EtherealSporocarpsBlock.AXIS, axis));
            }
        }
    }

    private static void generateTendrils(ServerWorld world, BlockPos startPos, int trunkHeight, Random random) {
        // Generate some decorative hanging or reaching tendrils
        int tendrilCount = 1 + random.nextInt(3);
        
        for (int i = 0; i < tendrilCount; i++) {
            // Pick a random position near the top of the tree
            int tendrilY = Math.max(1, trunkHeight - 2) + random.nextInt(2);
            BlockPos tendrilStart = startPos.up(tendrilY);
            
            // Random direction for the tendril
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            Direction direction = directions[random.nextInt(directions.length)];
            BlockPos tendrilPos = tendrilStart.offset(direction);
            
            // Check if we can place a tendril block
            if (world.getBlockState(tendrilPos).isAir() || world.getBlockState(tendrilPos).isReplaceable()) {
                // 50% chance for horizontal or vertical tendril
                if (random.nextBoolean()) {
                    // Horizontal tendril
                    world.setBlockState(tendrilPos, ModBlocks.ETHEREAL_SPOROCARPS.getDefaultState()
                        .with(com.theendupdate.block.EtherealSporocarpsBlock.AXIS, direction.getAxis()));
                } else {
                    // Vertical tendril (hanging down or reaching up)
                    BlockPos verticalPos = random.nextBoolean() ? tendrilPos.down() : tendrilPos.up();
                    if (world.getBlockState(verticalPos).isAir() || world.getBlockState(verticalPos).isReplaceable()) {
                        world.setBlockState(verticalPos, ModBlocks.ETHEREAL_SPOROCARPS.getDefaultState()
                            .with(com.theendupdate.block.EtherealSporocarpsBlock.AXIS, Direction.Axis.Y));
                    }
                }
            }
        }
    }
}
