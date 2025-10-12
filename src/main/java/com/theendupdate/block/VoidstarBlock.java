package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;

/**
 * Voidstar Block with explicit piston behavior.
 * A durable metal-like block that can be pushed/pulled by pistons.
 */
public class VoidstarBlock extends Block {
    public VoidstarBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PistonBehavior getPistonBehavior(BlockState state) {
        // Explicitly set to NORMAL for consistent push/pull behavior (like iron/gold blocks)
        return PistonBehavior.NORMAL;
    }
}

