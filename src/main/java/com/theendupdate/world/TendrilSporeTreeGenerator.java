package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import com.theendupdate.block.MoldcrawlBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates the Tendril Spore tree structure
 * Creates organic formations of Ethereal Sporocarps (logs) without leaves
 */
public final class TendrilSporeTreeGenerator {
    private TendrilSporeTreeGenerator() {}

    public static boolean generateTree(ServerLevel world, BlockPos startPos, RandomSource random) {
        if (!hasEnoughSpace(world, startPos)) {
            return false;
        }

        // logs collected here first, mold crawl pass runs after so it never blocks log placement
        List<BlockPos> placedLogPositions = new ArrayList<>();

        int trunkHeight = 3 + random.nextInt(4); // 3-6 tall
        generateTrunk(world, startPos, trunkHeight, placedLogPositions);

        int branchCount = 2 + random.nextInt(3); // 2-4
        generateBranches(world, startPos, trunkHeight, branchCount, random, placedLogPositions);

        generateTendrils(world, startPos, trunkHeight, random, placedLogPositions);

        for (BlockPos logPos : placedLogPositions) {
            attemptMoldCrawlPlacements(world, logPos, world.getRandom());
        }

        return true;
    }

    private static boolean hasEnoughSpace(ServerLevel world, BlockPos startPos) {
        // 5x5x8 area around the tree position
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos checkPos = startPos.offset(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void generateTrunk(ServerLevel world, BlockPos startPos, int height, List<BlockPos> placedLogs) {
        for (int y = 0; y < height; y++) {
            BlockPos pos = startPos.above(y);
            world.setBlockAndUpdate(pos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y));
            placedLogs.add(pos);
        }
    }

    private static void generateBranches(ServerLevel world, BlockPos startPos, int trunkHeight, int branchCount, RandomSource random, List<BlockPos> placedLogs) {
        int startHeight = Math.max(1, trunkHeight / 2); // branches start around halfway up

        for (int i = 0; i < branchCount; i++) {
            int branchY = startHeight + random.nextInt(Math.max(1, trunkHeight - startHeight));

            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            Direction direction = directions[random.nextInt(directions.length)];

            int branchLength = 1 + random.nextInt(3); // 1-3 blocks

            generateSingleBranch(world, startPos.above(branchY), direction, branchLength, placedLogs);
        }
    }

    private static void generateSingleBranch(ServerLevel world, BlockPos startPos, Direction direction, int length, List<BlockPos> placedLogs) {
        for (int i = 1; i <= length; i++) {
            BlockPos branchPos = startPos.relative(direction, i);

            if (world.getBlockState(branchPos).isAir() || world.getBlockState(branchPos).canBeReplaced()) {
                Direction.Axis axis = direction.getAxis();
                world.setBlockAndUpdate(branchPos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                    .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, axis));
                placedLogs.add(branchPos);
            }
        }
    }

    private static void generateTendrils(ServerLevel world, BlockPos startPos, int trunkHeight, RandomSource random, List<BlockPos> placedLogs) {
        int tendrilCount = 1 + random.nextInt(3);

        for (int i = 0; i < tendrilCount; i++) {
            int tendrilY = Math.max(1, trunkHeight - 2) + random.nextInt(2);
            BlockPos tendrilStart = startPos.above(tendrilY);

            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            Direction direction = directions[random.nextInt(directions.length)];
            BlockPos tendrilPos = tendrilStart.relative(direction);

            if (world.getBlockState(tendrilPos).isAir() || world.getBlockState(tendrilPos).canBeReplaced()) {
                if (random.nextBoolean()) {
                    world.setBlockAndUpdate(tendrilPos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                        .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, direction.getAxis()));
                    placedLogs.add(tendrilPos);
                } else {
                    // reaches up or hangs down at random
                    BlockPos verticalPos = random.nextBoolean() ? tendrilPos.below() : tendrilPos.above();
                    if (world.getBlockState(verticalPos).isAir() || world.getBlockState(verticalPos).canBeReplaced()) {
                        world.setBlockAndUpdate(verticalPos, ModBlocks.ETHEREAL_SPOROCARP.defaultBlockState()
                            .setValue(com.theendupdate.block.EtherealSporocarpBlock.AXIS, Direction.Axis.Y));
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
    private static void attemptMoldCrawlPlacements(ServerLevel world, BlockPos logPos, RandomSource random) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}; // only horizontal facings are valid for Moldcrawl
        for (Direction outward : horizontalDirections) {
            BlockPos moldPos = logPos.relative(outward);
            if (!world.getBlockState(moldPos).isAir()) {
                continue;
            }
            if (random.nextFloat() >= 0.012f) { // ~1.2% per face
                continue;
            }
            BlockState moldState = ModBlocks.MOLD_CRAWL.defaultBlockState().setValue(MoldcrawlBlock.FACING, outward);
            if (moldState.canSurvive(world, moldPos)) {
                world.setBlock(moldPos, moldState, 3);
            }
        }
    }
}
