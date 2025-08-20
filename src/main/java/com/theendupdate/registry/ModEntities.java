package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.EtherealOrbEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    
    private static final Identifier ETHEREAL_ORB_ID = Identifier.of(TemplateMod.MOD_ID, "ethereal_orb");
    
    // The ethereal orb entity - a floating, glowing orb creature
    public static final EntityType<EtherealOrbEntity> ETHEREAL_ORB = Registry.register(
        Registries.ENTITY_TYPE,
        ETHEREAL_ORB_ID,
        EntityType.Builder.create(EtherealOrbEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.8f, 0.8f) // Small orb-like size
            .eyeHeight(0.4f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, ETHEREAL_ORB_ID))
    );
    
    public static void registerModEntities() {
        // cleaned debug log
        FabricDefaultAttributeRegistry.register(ETHEREAL_ORB, EtherealOrbEntity.createEtherealOrbAttributes());
    }
}
