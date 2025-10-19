package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModPotions {
    // Base Phantom Ward potion (3 minutes, like vanilla potions)
    public static final RegistryEntry<Potion> PHANTOM_WARD = register(
        "phantom_ward",
        new Potion(
            null, // No base potion name
            new StatusEffectInstance(
                ModStatusEffects.PHANTOM_WARD,
                3600, // 3 minutes (3 * 60 * 20 ticks)
                0
            )
        )
    );

    // Long Phantom Ward potion (8 minutes with redstone, like vanilla)
    public static final RegistryEntry<Potion> LONG_PHANTOM_WARD = register(
        "long_phantom_ward",
        new Potion(
            "phantom_ward", // Base potion name for this variant
            new StatusEffectInstance(
                ModStatusEffects.PHANTOM_WARD,
                9600, // 8 minutes (8 * 60 * 20 ticks)
                0
            )
        )
    );

    private static RegistryEntry<Potion> register(String name, Potion potion) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        return Registry.registerReference(Registries.POTION, id, potion);
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModPotions() {}
}

