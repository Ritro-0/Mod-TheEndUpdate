package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModItems {
    // Metals and materials
    public static final Item VOIDSTAR_SHARD = registerItem(
        "voidstar_shard",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item VOIDSTAR_NUGGET = registerItem(
        "voidstar_nugget",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item VOIDSTAR_INGOT = registerItem(
        "voidstar_ingot",
        key -> new Item(new Item.Settings().registryKey(key))
    );
    
    // New drop from charged Ethereal Orb
    public static final Item SPECTRAL_DEBRIS = registerItem(
        "spectral_debris",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item SPECTRAL_CLUSTER = registerItem(
        "spectral_cluster",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    // Quest items and components
    public static final Item ENCHANTED_BOOK_COVER = registerItem(
        "enchanted_book_cover",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item ENCHANTED_PAGES = registerItem(
        "enchanted_pages",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item WOOD_CHIP = registerItem(
        "wood_chip",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    public static final Item WOODEN_CONE = registerItem(
        "wooden_cone",
        key -> new com.theendupdate.item.WoodenConeItem(new Item.Settings().registryKey(key))
    );

    public static final Item ICE_CREAM_CONE = registerItem(
        "ice_cream_cone",
        key -> new Item(new Item.Settings().registryKey(key))
    );

    // Gravitite materials
    public static final Item GRAVITITE_ESSENCE = registerItem(
        "gravitite_essence",
        key -> new Item(new Item.Settings().registryKey(key).fireproof())
    );

    public static final Item PURE_GRAVITITE = registerItem(
        "pure_gravitite",
        key -> new Item(new Item.Settings().registryKey(key).fireproof())
    );

    // Removed custom bound recovery compass; vanilla recovery compass will be used
    
    // Spawn eggs
    public static final Item ETHEREAL_ORB_SPAWN_EGG = registerItem(
        "ethereal_orb_spawn_egg",
        key -> new SpawnEggItem(
            com.theendupdate.registry.ModEntities.ETHEREAL_ORB,
            new Item.Settings().registryKey(key)
        ),
        net.minecraft.item.ItemGroups.SPAWN_EGGS
    );

    public static final Item SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "shadow_creaking_spawn_egg",
        key -> new SpawnEggItem(
            com.theendupdate.registry.ModEntities.SHADOW_CREAKING,
            new Item.Settings().registryKey(key)
        ),
        net.minecraft.item.ItemGroups.SPAWN_EGGS
    );

    public static final Item MINI_SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "mini_shadow_creaking_spawn_egg",
        key -> new SpawnEggItem(
            com.theendupdate.registry.ModEntities.MINI_SHADOW_CREAKING,
            new Item.Settings().registryKey(key)
        ),
        net.minecraft.item.ItemGroups.SPAWN_EGGS
    );

    public static final Item TINY_SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "tiny_shadow_creaking_spawn_egg",
        key -> new SpawnEggItem(
            com.theendupdate.registry.ModEntities.TINY_SHADOW_CREAKING,
            new Item.Settings().registryKey(key)
        ),
        net.minecraft.item.ItemGroups.SPAWN_EGGS
    );

    @SafeVarargs
    private static Item registerItem(String name, java.util.function.Function<RegistryKey<Item>, Item> factory, net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup>... groups) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item item = factory.apply(key);
        Registry.register(Registries.ITEM, id, item);
        // Add to creative tabs: default to INGREDIENTS if none provided
        if (groups == null || groups.length == 0) {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(item));
        } else {
            for (net.minecraft.registry.RegistryKey<net.minecraft.item.ItemGroup> group : groups) {
                ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
            }
        }
        return item;
    }

    public static void registerModItems() {
        // No-op, calling this ensures the class is loaded and static initializers run
    }
}
