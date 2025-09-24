package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
// removed unused imports
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    
    private static final Identifier ETHEREAL_ORB_ID = Identifier.of(TemplateMod.MOD_ID, "ethereal_orb");
    private static final Identifier SHADOW_CREAKING_ID = Identifier.of(TemplateMod.MOD_ID, "shadow_creaking");
    private static final Identifier MINI_SHADOW_CREAKING_ID = Identifier.of(TemplateMod.MOD_ID, "mini_shadow_creaking");
    private static final Identifier TINY_SHADOW_CREAKING_ID = Identifier.of(TemplateMod.MOD_ID, "tiny_shadow_creaking");
    
    // The ethereal orb entity - a floating, glowing orb creature
    public static final EntityType<EtherealOrbEntity> ETHEREAL_ORB = Registry.register(
        Registries.ENTITY_TYPE,
        ETHEREAL_ORB_ID,
        EntityType.Builder.create(EtherealOrbEntity::new, SpawnGroup.AMBIENT)
            // 7×7 px by 11 px tall → 0.4375f × 0.6875f blocks
            .dimensions(0.4375f, 0.6875f)
            .eyeHeight(0.34375f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, ETHEREAL_ORB_ID))
    );

    // Shadow Creaking - hostile, weeping-angel-like
    public static final EntityType<ShadowCreakingEntity> SHADOW_CREAKING = Registry.register(
        Registries.ENTITY_TYPE,
        SHADOW_CREAKING_ID,
        EntityType.Builder.create(ShadowCreakingEntity::new, SpawnGroup.MONSTER)
            // Approximate vanilla Creaking size
            .dimensions(0.6f, 2.7f)
            .eyeHeight(2.2f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, SHADOW_CREAKING_ID))
    );

    // Mini Shadow Creaking - half-sized, faster, lower health
    public static final EntityType<MiniShadowCreakingEntity> MINI_SHADOW_CREAKING = Registry.register(
        Registries.ENTITY_TYPE,
        MINI_SHADOW_CREAKING_ID,
        EntityType.Builder.create(MiniShadowCreakingEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.3f, 1.35f)
            .eyeHeight(1.1f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, MINI_SHADOW_CREAKING_ID))
    );

    // Tiny Shadow Creaking - quarter of mini, extremely fast, silverfish stats
    public static final EntityType<TinyShadowCreakingEntity> TINY_SHADOW_CREAKING = Registry.register(
        Registries.ENTITY_TYPE,
        TINY_SHADOW_CREAKING_ID,
        EntityType.Builder.create(TinyShadowCreakingEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.15f, 0.675f)
            .eyeHeight(0.55f)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, TINY_SHADOW_CREAKING_ID))
    );
    
    public static void registerModEntities() {
        // cleaned debug log
        FabricDefaultAttributeRegistry.register(ETHEREAL_ORB, EtherealOrbEntity.createEtherealOrbAttributes());
        FabricDefaultAttributeRegistry.register(SHADOW_CREAKING, ShadowCreakingEntity.createShadowCreakingAttributes());
        FabricDefaultAttributeRegistry.register(MINI_SHADOW_CREAKING, MiniShadowCreakingEntity.createMiniAttributes());
        FabricDefaultAttributeRegistry.register(TINY_SHADOW_CREAKING, TinyShadowCreakingEntity.createTinyAttributes());
    }
}
