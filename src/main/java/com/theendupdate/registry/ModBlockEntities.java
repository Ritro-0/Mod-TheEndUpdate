package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.block.QuantumGatewayBlockEntity;
import com.theendupdate.block.ShadowAltarBlockEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import java.lang.reflect.Field;

public final class ModBlockEntities {
    public static BlockEntityType<QuantumGatewayBlockEntity> QUANTUM_GATEWAY;
    public static BlockEntityType<ShadowAltarBlockEntity> SHADOW_ALTAR;
    public static BlockEntityType<SignBlockEntity> ETHEREAL_SIGN;
    public static BlockEntityType<SignBlockEntity> SHADOW_SIGN;
    public static BlockEntityType<HangingSignBlockEntity> ETHEREAL_HANGING_SIGN;
    public static BlockEntityType<HangingSignBlockEntity> SHADOW_HANGING_SIGN;
    public static BlockEntityType<ShelfBlockEntity> ETHEREAL_SHELF;
    public static BlockEntityType<ShelfBlockEntity> SHADOW_SHELF;

    public static void register() {
        QUANTUM_GATEWAY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(TemplateMod.MOD_ID, "quantum_gateway"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<QuantumGatewayBlockEntity>create(QuantumGatewayBlockEntity::new, ModBlocks.QUANTUM_GATEWAY).build()
        );
        SHADOW_ALTAR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(TemplateMod.MOD_ID, "shadow_altar"),
            net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<ShadowAltarBlockEntity>create(ShadowAltarBlockEntity::new, ModBlocks.SHADOW_ALTAR).build()
        );
    }
    
    // This will be called AFTER ModBlocks.registerModBlocks() to add our blocks to VANILLA block entity types
    public static void registerSignBlockEntities() {
        // Find the 'blocks' field by its type (Set<Block>) instead of by name
        // This works in both dev and production without needing to know the exact field name
        try {
            Field blocksField = null;
            for (Field field : BlockEntityType.class.getDeclaredFields()) {
                // Look for a Set field that's not static and not final
                // The blocks field is an instance field that we can modify
                if (java.util.Set.class.isAssignableFrom(field.getType()) &&
                    !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    // Verify it contains blocks by checking if we can get it from a known BlockEntityType
                    try {
                        Object value = field.get(BlockEntityType.SIGN);
                        if (value instanceof java.util.Set) {
                            blocksField = field;
                            break;
                        }
                    } catch (Exception ignored) {
                        // Not the right field, keep looking
                    }
                }
            }
            
            if (blocksField == null) {
                throw new RuntimeException("Could not find blocks field in BlockEntityType");
            }
            
            TemplateMod.LOGGER.info("Found blocks field: {}", blocksField.getName());
            
            // The blocks Set is immutable, so we need to create a new mutable Set with our blocks added
            @SuppressWarnings("unchecked")
            java.util.Set<net.minecraft.block.Block> originalSignBlocks = 
                (java.util.Set<net.minecraft.block.Block>) blocksField.get(BlockEntityType.SIGN);
            java.util.Set<net.minecraft.block.Block> newSignBlocks = new java.util.HashSet<>(originalSignBlocks);
            newSignBlocks.add(ModBlocks.ETHEREAL_SIGN);
            newSignBlocks.add(ModBlocks.ETHEREAL_WALL_SIGN);
            newSignBlocks.add(ModBlocks.SHADOW_SIGN);
            newSignBlocks.add(ModBlocks.SHADOW_WALL_SIGN);
            blocksField.set(BlockEntityType.SIGN, newSignBlocks);
            
            @SuppressWarnings("unchecked")
            java.util.Set<net.minecraft.block.Block> originalHangingSignBlocks = 
                (java.util.Set<net.minecraft.block.Block>) blocksField.get(BlockEntityType.HANGING_SIGN);
            java.util.Set<net.minecraft.block.Block> newHangingSignBlocks = new java.util.HashSet<>(originalHangingSignBlocks);
            newHangingSignBlocks.add(ModBlocks.ETHEREAL_HANGING_SIGN);
            newHangingSignBlocks.add(ModBlocks.ETHEREAL_WALL_HANGING_SIGN);
            newHangingSignBlocks.add(ModBlocks.SHADOW_HANGING_SIGN);
            newHangingSignBlocks.add(ModBlocks.SHADOW_WALL_HANGING_SIGN);
            blocksField.set(BlockEntityType.HANGING_SIGN, newHangingSignBlocks);
            
            @SuppressWarnings("unchecked")
            java.util.Set<net.minecraft.block.Block> originalShelfBlocks = 
                (java.util.Set<net.minecraft.block.Block>) blocksField.get(BlockEntityType.SHELF);
            java.util.Set<net.minecraft.block.Block> newShelfBlocks = new java.util.HashSet<>(originalShelfBlocks);
            newShelfBlocks.add(ModBlocks.ETHEREAL_SHELF);
            newShelfBlocks.add(ModBlocks.SHADOW_SHELF);
            blocksField.set(BlockEntityType.SHELF, newShelfBlocks);
            
            TemplateMod.LOGGER.info("Successfully added custom sign and shelf blocks to vanilla block entity types");
        } catch (Exception e) {
            throw new RuntimeException("Failed to add custom sign and shelf blocks to vanilla block entity types", e);
        }
        
        // Store references to vanilla types for convenience (used by renderers on client)
        ETHEREAL_SIGN = BlockEntityType.SIGN;
        SHADOW_SIGN = BlockEntityType.SIGN;
        ETHEREAL_HANGING_SIGN = BlockEntityType.HANGING_SIGN;
        SHADOW_HANGING_SIGN = BlockEntityType.HANGING_SIGN;
        ETHEREAL_SHELF = BlockEntityType.SHELF;
        SHADOW_SHELF = BlockEntityType.SHELF;
    }
}

