package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility to attempt spawning a Void Bloom on top/side/bottom of mature chorus plant/flower.
 * Call from a tick hook or command; for now, kept as a simple helper.
 */
public final class VoidBloomChorusGrowthFeature {
    private VoidBloomChorusGrowthFeature() {}

    /** 75% chance to place a void bloom adjacent to a fully mature chorus flower bud (not stem) */
    public static boolean tryGrow(Level world, BlockPos chorusPos, RandomSource random) {
        if (random.nextFloat() > 0.75f) return false;

        // only chorus flower buds, not chorus plant stems
        BlockState state = world.getBlockState(chorusPos);
        if (!state.is(Blocks.CHORUS_FLOWER)) return false;

        // fully matured (age 5, purple)
        int age = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_5);
        if (age < 5) return false;

        Direction[] directions = Direction.values();
        for (int i = 0; i < 3; i++) { // up to 3 random directions
            Direction dir = directions[random.nextInt(directions.length)];
            BlockPos target = chorusPos.relative(dir);

            if (world.getBlockState(target).isAir()) {
                Direction attachmentDirection = dir.getOpposite();
                var voidBloomBlock = (com.theendupdate.block.VoidBloomBlock) ModBlocks.VOID_BLOOM;
                BlockState attachedState = voidBloomBlock.getAttachedState(attachmentDirection);
                    
                world.setBlock(target, attachedState, 3);
                return true;
            }
        }
        
        return false;
    }
}


