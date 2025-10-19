package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.effect.PhantomWardStatusEffect;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModStatusEffects {
    // Phantom Ward - repels phantoms (but not King Phantoms)
    public static final RegistryEntry<StatusEffect> PHANTOM_WARD = register(
        "phantom_ward",
        new PhantomWardStatusEffect()
    );

    private static RegistryEntry<StatusEffect> register(String name, StatusEffect effect) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        return Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModStatusEffects() {}
}

