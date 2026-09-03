package com.theendupdate.client.render;

import com.theendupdate.block.QuantumGatewayBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class QuantumGatewayRenderState extends BlockEntityRenderState {
    public float bob;
    public final float[] rotX = new float[QuantumGatewayBlockEntity.MOVING_SPARKLES];
    public final float[] rotY = new float[QuantumGatewayBlockEntity.MOVING_SPARKLES];
    public final float[] rotZ = new float[QuantumGatewayBlockEntity.MOVING_SPARKLES];
}
