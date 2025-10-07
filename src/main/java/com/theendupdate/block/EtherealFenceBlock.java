package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

public class EtherealFenceBlock extends FenceBlock {
    
    public EtherealFenceBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean canConnect(BlockState state, boolean neighborIsFullSquare, Direction dir) {
        Block block = state.getBlock();
        
        // Connect to all fences (including other custom fences)
        if (block instanceof FenceBlock) {
            return true;
        }
        
        // Use vanilla logic for other blocks
        return super.canConnect(state, neighborIsFullSquare, dir);
    }
}
