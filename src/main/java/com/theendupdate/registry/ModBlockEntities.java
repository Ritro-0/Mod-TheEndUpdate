package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.block.QuantumGatewayBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    public static BlockEntityType<QuantumGatewayBlockEntity> QUANTUM_GATEWAY;

    public static void register() {
        QUANTUM_GATEWAY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(TemplateMod.MOD_ID, "quantum_gateway"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<QuantumGatewayBlockEntity>create(QuantumGatewayBlockEntity::new, ModBlocks.QUANTUM_GATEWAY).build()
        );
        
        // Shadow altar removed - now using standard Block instead of BlockWithEntity
    }
}


