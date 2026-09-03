package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.effect.PhantomWardStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public final class ModStatusEffects {
    // Phantom Ward - repels phantoms (but not King Phantoms)
    public static final Holder<MobEffect> PHANTOM_WARD = register(
        "phantom_ward",
        new PhantomWardStatusEffect()
    );

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, effect);
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModStatusEffects() {}
}

