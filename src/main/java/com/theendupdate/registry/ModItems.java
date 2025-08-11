package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item VOID_SAP = registerItem(
        "void_sap",
        key -> new net.minecraft.item.BlockItem(
            ModBlocks.VOID_SAP,
            new Item.Settings().registryKey(key)
        )
    );

    private static Item registerItem(String name, java.util.function.Function<RegistryKey<Item>, Item> factory) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item item = factory.apply(key);
        Registry.register(Registries.ITEM, id, item);
        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(item));
        return item;
    }

    public static void registerModItems() {
        // No-op, calling this ensures the class is loaded and static initializers run
    }
}
