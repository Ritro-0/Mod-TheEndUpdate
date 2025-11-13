package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent ETHEREAL_ORB_IDLE = register("entity.ethereal_orb.idle");
    public static final SoundEvent ETHEREAL_ORB_DEATH = register("entity.ethereal_orb.death");
    public static final SoundEvent ETHEREAL_ORB_LOSES_BULB = register("entity.ethereal_orb.loses_bulb");
    public static final SoundEvent ETHEREAL_ORB_TAMED = register("entity.ethereal_orb.tamed");
    public static final SoundEvent VOID_TARDIGRADE_IDLE = register("entity.void_tardigrade.idle");
    public static final SoundEvent VOID_TARDIGRADE_DEATH = register("entity.void_tardigrade.death");

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModSounds() {}
}


