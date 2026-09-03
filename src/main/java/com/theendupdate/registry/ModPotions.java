package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public final class ModPotions {
    public static final Holder<Potion> PHANTOM_WARD = register(
        "phantom_ward",
        new Potion(
            "phantom_ward",
            new MobEffectInstance(
                ModStatusEffects.PHANTOM_WARD,
                3600, // 3 min
                0
            )
        )
    );

    // "long" variant (redstone-extended, like vanilla) shares the same base potion name
    public static final Holder<Potion> LONG_PHANTOM_WARD = register(
        "long_phantom_ward",
        new Potion(
            "phantom_ward",
            new MobEffectInstance(
                ModStatusEffects.PHANTOM_WARD,
                9600, // 8 min
                0
            )
        )
    );

    private static Holder<Potion> register(String name, Potion potion) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModPotions() {}
}

