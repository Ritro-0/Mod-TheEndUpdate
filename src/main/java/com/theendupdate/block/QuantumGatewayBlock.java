package com.theendupdate.block;

import net.minecraft.block.Block;

public class QuantumGatewayBlock extends Block {
    public QuantumGatewayBlock(Settings settings) {
        super(settings);
    }

    // Desired tint for the beacon beam when passing through this block (C26D84)
    public static final float[] BEAM_TINT = new float[] { 0.7608f, 0.4275f, 0.5176f };
}


