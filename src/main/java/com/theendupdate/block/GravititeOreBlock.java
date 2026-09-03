package com.theendupdate.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * Gravitite Ore Block with explicit piston behavior.
 * A blast-resistant ore block that can be pushed/pulled by pistons.
 */
public class GravititeOreBlock extends Block {
    public GravititeOreBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    // Mapping-safe: omit @Override for cross-version compatibility
    public PushReaction getPistonBehavior(BlockState state) {
        return PushReaction.NORMAL; // pushable/pullable, like other ores
    }
}

