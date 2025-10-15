package com.theendupdate.mixin.client;

import com.theendupdate.accessor.BeaconRenderStateQuantumAccessor;
import com.theendupdate.block.QuantumGatewayBlock;
import net.minecraft.client.render.block.entity.state.BeaconBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BeaconBlockEntityRenderState.class)
public abstract class BeaconBlockEntityRenderStateMixin implements BeaconRenderStateQuantumAccessor {
    
    @Unique
    private boolean theendupdate$hasQuantumGateway = false;
    
    @Unique
    private boolean theendupdate$isRedstonePowered = false;
    
    @Unique
    private float[] theendupdate$beamTint = QuantumGatewayBlock.BEAM_TINT.clone();
    
    @Unique
    private int theendupdate$beaconY = 0;
    
    @Unique
    private int theendupdate$topY = 256;
    
    @Override
    public boolean theendupdate$hasQuantumGateway() {
        return this.theendupdate$hasQuantumGateway;
    }
    
    @Override
    public void theendupdate$setHasQuantumGateway(boolean has) {
        this.theendupdate$hasQuantumGateway = has;
    }
    
    @Override
    public boolean theendupdate$isRedstonePowered() {
        return this.theendupdate$isRedstonePowered;
    }
    
    @Override
    public void theendupdate$setRedstonePowered(boolean powered) {
        this.theendupdate$isRedstonePowered = powered;
    }
    
    @Override
    public float[] theendupdate$getBeamTint() {
        return this.theendupdate$beamTint;
    }
    
    @Override
    public void theendupdate$setBeamTint(float[] tint) {
        this.theendupdate$beamTint = tint;
    }
    
    @Override
    public int theendupdate$getBeaconY() {
        return this.theendupdate$beaconY;
    }
    
    @Override
    public void theendupdate$setBeaconY(int y) {
        this.theendupdate$beaconY = y;
    }
    
    @Override
    public int theendupdate$getTopY() {
        return this.theendupdate$topY;
    }
    
    @Override
    public void theendupdate$setTopY(int y) {
        this.theendupdate$topY = y;
    }
}

