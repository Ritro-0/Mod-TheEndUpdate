package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent ETHEREAL_ORB_IDLE = register("entity.ethereal_orb.idle");
    public static final SoundEvent ETHEREAL_ORB_DEATH = register("entity.ethereal_orb.death");
    public static final SoundEvent ETHEREAL_ORB_LOSES_BULB = register("entity.ethereal_orb.loses_bulb");
    public static final SoundEvent ETHEREAL_ORB_TAMED = register("entity.ethereal_orb.tamed");
    public static final SoundEvent VOID_TARDIGRADE_IDLE = register("entity.void_tardigrade.idle");
    public static final SoundEvent VOID_TARDIGRADE_DEATH = register("entity.void_tardigrade.death");
    public static final SoundEvent TETHERLING_IDLE = register("entity.tetherling.idle");
    public static final SoundEvent TETHERLING_HURT = register("entity.tetherling.hurt");
    public static final SoundEvent TETHERLING_DEATH = register("entity.tetherling.death");
    public static final SoundEvent TETHERLING_GRAB = register("entity.tetherling.grab");
    public static final SoundEvent TETHERLING_YEET = register("entity.tetherling.yeet");
    public static final SoundEvent SHADOW_CREAKING_IDLE = register("entity.shadow_creaking.idle");
    public static final SoundEvent SHADOW_CREAKING_HURT = register("entity.shadow_creaking.hurt");
    public static final SoundEvent SHADOW_CREAKING_DEATH = register("entity.shadow_creaking.death");
    public static final SoundEvent SHADOW_CREAKING_ROAR = register("entity.shadow_creaking.roar");
    public static final SoundEvent SHADOW_CREAKING_SPAWN = register("entity.shadow_creaking.spawn");
    public static final SoundEvent SHADOW_ALTAR_LIT = register("block.shadow_altar.lit");

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {
        // Intentionally empty; class loading triggers static registration
    }

    private ModSounds() {}
}


