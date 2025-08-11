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

    /** 75% chance to place a void bloom adjacent to a fully mature chorus flower bud (not stem) */
    public static boolean tryGrow(World world, BlockPos chorusPos, Random random) {
        if (random.nextFloat() > 0.75f) return false;

        // Only target chorus flower blocks (the buds), not chorus plant blocks (the stems)
        BlockState state = world.getBlockState(chorusPos);
        if (!state.isOf(Blocks.CHORUS_FLOWER)) return false;
        
        // Check if the chorus flower is fully matured (age 5, purple color)
        int age = state.get(net.minecraft.state.property.Properties.AGE_5);
        if (age < 5) return false;

        // Try all 6 directions to find a valid spot
        Direction[] directions = Direction.values();
        for (int i = 0; i < 3; i++) { // Try up to 3 random directions
            Direction dir = directions[random.nextInt(directions.length)];
            BlockPos target = chorusPos.offset(dir);
            
            if (world.getBlockState(target).isAir()) {
                // Calculate the attachment direction (from void bloom back to chorus flower)
                Direction attachmentDirection = dir.getOpposite();
                
                // Create properly oriented void bloom state
                var voidBloomBlock = (com.theendupdate.block.VoidBloomBlock) ModBlocks.VOID_BLOOM;
                BlockState attachedState = voidBloomBlock.getAttachedState(attachmentDirection);
                
                com.theendupdate.TemplateMod.LOGGER.info("Placing Void Bloom at {} attached to chorus bud at {} (growth direction: {}, attachment face: {})", 
                    target, chorusPos, dir, attachmentDirection);
                    
                world.setBlockState(target, attachedState, 3);
                return true;
            }
        }
        
        return false;
    }
}


