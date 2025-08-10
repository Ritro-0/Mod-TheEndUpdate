package com.theendupdate.world;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Utility to attempt spawning a Void Bloom on top/side/bottom of mature chorus plant/flower.
 * Call from a tick hook or command; for now, kept as a simple helper.
 */
public final class VoidBloomChorusGrowthFeature {
    private VoidBloomChorusGrowthFeature() {}

    /** 75% chance to place a void bloom adjacent to a mature chorus flower/plant */
    public static boolean tryGrow(World world, BlockPos chorusPos, Random random) {
        if (random.nextFloat() > 0.75f) return false;

        // Check if the chorus is fully matured (chorus flower age 5) or a solid chorus plant block
        BlockState state = world.getBlockState(chorusPos);
        boolean mature = state.isOf(Blocks.CHORUS_PLANT) ||
            (state.isOf(Blocks.CHORUS_FLOWER) && state.get(net.minecraft.state.property.Properties.AGE_5) >= 5);
        if (!mature) return false;

        Direction dir = Direction.random(random);
        BlockPos target = chorusPos.offset(dir);
        if (!world.getBlockState(target).isAir()) return false;

        world.setBlockState(target, ModBlocks.VOID_BLOOM.getDefaultState(), 3);
        return true;
    }
}


