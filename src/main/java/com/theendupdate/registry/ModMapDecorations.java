package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModMapDecorations {
    public static final MapDecorationType SHADOW_HUNTER = Registry.register(
        Registries.MAP_DECORATION_TYPE,
        Identifier.of(TemplateMod.MOD_ID, "shadow_hunter"),
        new MapDecorationType(
            Identifier.of(TemplateMod.MOD_ID, "map/decorations/shadow_hunter"),
            false,
            9,
            false,
            false
        )
    );

    public static void register() {
        // Registration happens automatically when the field is accessed
        TemplateMod.LOGGER.info("Registered map decoration type: shadow_hunter");
    }
}
