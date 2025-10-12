package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;

/**
 * Gravitite Ore Block with explicit piston behavior.
 * A blast-resistant ore block that can be pushed/pulled by pistons.
 */
public class GravititeOreBlock extends Block {
    public GravititeOreBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PistonBehavior getPistonBehavior(BlockState state) {
        // Explicitly set to NORMAL for consistent push/pull behavior (like other ores)
        return PistonBehavior.NORMAL;
    }
}

