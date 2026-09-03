package com.theendupdate.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * Shadow Planks Block with explicit piston behavior.
 * Ensures consistent pushability/pullability with pistons.
 */
public class ShadowPlanksBlock extends Block {
    public ShadowPlanksBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        return PushReaction.NORMAL; // pushable/pullable, like all planks
    }
}

