package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import com.theendupdate.block.MoldcrawlBlock;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Tendril Spore tree structure
 * Creates organic formations of Ethereal Sporocarps (logs) without leaves
 */
public final class TendrilSporeTreeGenerator {
    private TendrilSporeTreeGenerator() {}

    public static boolean generateTree(ServerWorld world, BlockPos startPos, Random random) {
        
        
        // Check if there's enough space (basic 5x5x8 area check)
        if (!hasEnoughSpace(world, startPos)) {
            return false;
        }
        
        // Collect positions of all placed sporocarps first; mold crawl is placed after logs are finalized
        List<BlockPos> placedLogPositions = new ArrayList<>();

        // Generate the main trunk (3-6 blocks tall)
        int trunkHeight = 3 + random.nextInt(4);
        generateTrunk(world, startPos, trunkHeight, placedLogPositions);
        
        // Generate side branches (2-4 branches)
        int branchCount = 2 + random.nextInt(3);
        generateBranches(world, startPos, trunkHeight, branchCount, random, placedLogPositions);
        
        // Generate some decorative tendrils
        generateTendrils(world, startPos, trunkHeight, random, placedLogPositions);

        // After all logs are placed, do a mold crawl pass so it never blocks log placement
        for (BlockPos logPos : placedLogPositions) {
            attemptMoldCrawlPlacements(world, logPos, world.random);
        }
        
        
        return true;
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

    private static void generateTrunk(ServerWorld world, BlockPos startPos, int height, List<BlockPos> placedLogs) {
        // Generate main vertical trunk
        for (int y = 0; y < height; y++) {
            BlockPos pos = startPos.up(y);
            world.setBlockState(pos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y));
            placedLogs.add(pos);
        }
    }

    private static void generateBranches(ServerWorld world, BlockPos startPos, int trunkHeight, int branchCount, Random random, List<BlockPos> placedLogs) {
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
            
            generateSingleBranch(world, startPos.up(branchY), direction, branchLength, placedLogs);
        }
    }

    private static void generateSingleBranch(ServerWorld world, BlockPos startPos, Direction direction, int length, List<BlockPos> placedLogs) {
        for (int i = 1; i <= length; i++) {
            BlockPos branchPos = startPos.offset(direction, i);
            
            // Check if the position is valid
            if (world.getBlockState(branchPos).isAir() || world.getBlockState(branchPos).isReplaceable()) {
                // Set the log with horizontal axis
                Direction.Axis axis = direction.getAxis();
                world.setBlockState(branchPos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                    .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, axis));
                placedLogs.add(branchPos);
            }
        }
    }

    private static void generateTendrils(ServerWorld world, BlockPos startPos, int trunkHeight, Random random, List<BlockPos> placedLogs) {
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
                    world.setBlockState(tendrilPos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                        .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, direction.getAxis()));
                    placedLogs.add(tendrilPos);
                } else {
                    // Vertical tendril (hanging down or reaching up)
                    BlockPos verticalPos = random.nextBoolean() ? tendrilPos.down() : tendrilPos.up();
                    if (world.getBlockState(verticalPos).isAir() || world.getBlockState(verticalPos).isReplaceable()) {
                        world.setBlockState(verticalPos, ModBlocks.ETHEREAL_SPOROCARP.getDefaultState()
                            .with(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y));
                        placedLogs.add(verticalPos);
                    }
                }
            }
        }
    }

    /**
     * For each horizontal face adjacent to the given log position, roll ~1.2% to place a mold crawl facing outward
     * if the adjacent block is air and the mold crawl can attach back to the log face.
     */
    private static void attemptMoldCrawlPlacements(ServerWorld world, BlockPos logPos, Random random) {
        // Only horizontal facings are valid for Moldcrawl
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction outward : horizontalDirections) {
            BlockPos moldPos = logPos.offset(outward);
            // Adjacent space must be empty to place the initial mold crawl segment
            if (!world.getBlockState(moldPos).isAir()) {
                continue;
            }
            // Chance: ~1.2% per valid block face
            if (random.nextFloat() >= 0.012f) {
                continue;
            }
            BlockState moldState = ModBlocks.MOLD_CRAWL.getDefaultState().with(MoldcrawlBlock.FACING, outward);
            // Ensure placement is valid (requires support on back side which is the log)
            if (moldState.canPlaceAt(world, moldPos)) {
                world.setBlockState(moldPos, moldState, 3);
            }
        }
    }
}
