package com.theendupdate.registry;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModDamageTypes {
    public static final RegistryKey<DamageType> SHADOW_CREAKING_BEAM = RegistryKey.of(
        RegistryKeys.DAMAGE_TYPE,
        Identifier.of("theendupdate", "shadow_creaking_beam")
    );
}

