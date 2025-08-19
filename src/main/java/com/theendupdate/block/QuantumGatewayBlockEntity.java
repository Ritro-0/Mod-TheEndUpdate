package com.theendupdate.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import com.theendupdate.screen.GatewayScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.math.BlockPos;

public class QuantumGatewayBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public final SimpleInventory inventory = new SimpleInventory(3);

    public QuantumGatewayBlockEntity(BlockPos pos, BlockState state) {
        super(com.theendupdate.registry.ModBlockEntities.QUANTUM_GATEWAY, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.repair");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GatewayScreenHandler(syncId, playerInventory, this.inventory, this.getPos());
    }
}


