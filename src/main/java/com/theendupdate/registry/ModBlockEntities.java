package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.block.QuantumGatewayBlockEntity;
import com.theendupdate.block.ShadowAltarBlockEntity;
import com.theendupdate.block.entity.NebulaVentBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public final class ModBlockEntities {
    public static BlockEntityType<NebulaVentBlockEntity> NEBULA_VENT;
    public static BlockEntityType<QuantumGatewayBlockEntity> QUANTUM_GATEWAY;
    public static BlockEntityType<ShadowAltarBlockEntity> SHADOW_ALTAR;

    public static void register() {
        NEBULA_VENT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "nebula_vent"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<NebulaVentBlockEntity>create(NebulaVentBlockEntity::new, ModBlocks.NEBULA_VENT_BLOCK).build()
        );
        QUANTUM_GATEWAY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "quantum_gateway"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<QuantumGatewayBlockEntity>create(QuantumGatewayBlockEntity::new, ModBlocks.QUANTUM_GATEWAY).build()
        );
        SHADOW_ALTAR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_altar"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<ShadowAltarBlockEntity>create(ShadowAltarBlockEntity::new, ModBlocks.SHADOW_ALTAR).build()
        );
    }

    // Called after ModBlocks.registerModBlocks() so the sign/shelf blocks exist.
    public static void registerSignBlockEntities() {
        BlockEntityTypes.SIGN.addValidBlock(ModBlocks.ETHEREAL_SIGN);
        BlockEntityTypes.SIGN.addValidBlock(ModBlocks.ETHEREAL_WALL_SIGN);
        BlockEntityTypes.SIGN.addValidBlock(ModBlocks.SHADOW_SIGN);
        BlockEntityTypes.SIGN.addValidBlock(ModBlocks.SHADOW_WALL_SIGN);

        BlockEntityTypes.HANGING_SIGN.addValidBlock(ModBlocks.ETHEREAL_HANGING_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(ModBlocks.ETHEREAL_WALL_HANGING_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(ModBlocks.SHADOW_HANGING_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(ModBlocks.SHADOW_WALL_HANGING_SIGN);

        BlockEntityTypes.SHELF.addValidBlock(ModBlocks.ETHEREAL_SHELF);
        BlockEntityTypes.SHELF.addValidBlock(ModBlocks.SHADOW_SHELF);
    }
}
