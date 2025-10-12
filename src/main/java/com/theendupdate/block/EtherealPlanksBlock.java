package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;

/**
 * Ethereal Planks Block with explicit piston behavior.
 * Ensures consistent pushability/pullability with pistons.
 */
public class EtherealPlanksBlock extends Block {
    public EtherealPlanksBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PistonBehavior getPistonBehavior(BlockState state) {
        // Explicitly set to NORMAL for consistent push/pull behavior (like all planks)
        return PistonBehavior.NORMAL;
    }
}

