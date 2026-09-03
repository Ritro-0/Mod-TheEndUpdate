package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
// Food components are handled in custom item class for 1.21.8
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Item VOIDSTAR_SHARD = registerItem(
        "voidstar_shard",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item VOIDSTAR_NUGGET = registerItem(
        "voidstar_nugget",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item VOIDSTAR_INGOT = registerItem(
        "voidstar_ingot",
        key -> new Item(new Item.Properties().setId(key))
    );
    
    public static final Item SPECTRAL_DEBRIS = registerItem(
        "spectral_debris",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item SPECTRAL_CLUSTER = registerItem(
        "spectral_cluster",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item TARDIGRADE_SHELL_BIT = registerItem(
        "tardigrade_shell_bit",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item TARDIGRADE_SHELL_BRICK = registerItem(
        "tardigrade_shell_brick",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item ENCHANTED_BOOK_COVER = registerItem(
        "enchanted_book_cover",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item ENCHANTED_PAGES = registerItem(
        "enchanted_pages",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item WOOD_CHIP = registerItem(
        "wood_chip",
        key -> new Item(new Item.Properties().setId(key))
    );

    public static final Item WOODEN_CONE = registerItem(
        "wooden_cone",
        key -> new com.theendupdate.item.WoodenConeItem(new Item.Properties().setId(key))
    );

    public static final Item ICE_CREAM_CONE = registerItem(
        "ice_cream_cone",
        key -> new com.theendupdate.item.IceCreamConeItem(
            new Item.Properties()
                .setId(key)
                .food(
                    new FoodProperties.Builder()
                        .nutrition(1)
                        .saturationModifier(0.2f)
                        .alwaysEdible()
                        .build()
                )
                .component(
                    DataComponents.FOOD,
                    new FoodProperties.Builder()
                        .nutrition(1)
                        .saturationModifier(0.2f)
                        .alwaysEdible()
                        .build()
                )
        ),
        CreativeModeTabs.FOOD_AND_DRINKS
    );

    public static final Item STRAWBERRY_ICE_CREAM_CONE = registerItem(
        "strawberry_ice_cream_cone",
        key -> new com.theendupdate.item.StrawberryIceCreamConeItem(
            new Item.Properties()
                .setId(key)
                .food(
                    new FoodProperties.Builder()
                        .nutrition(1)
                        .saturationModifier(0.2f)
                        .alwaysEdible()
                        .build()
                )
                .component(
                    DataComponents.FOOD,
                    new FoodProperties.Builder()
                        .nutrition(1)
                        .saturationModifier(0.2f)
                        .alwaysEdible()
                        .build()
                )
        ),
        CreativeModeTabs.FOOD_AND_DRINKS
    );

    public static final Item GRAVITITE_ESSENCE = registerItem(
        "gravitite_essence",
        key -> new Item(new Item.Properties().setId(key).fireResistant())
    );

    public static final Item PURE_GRAVITITE = registerItem(
        "pure_gravitite",
        key -> new Item(new Item.Properties().setId(key).fireResistant())
    );

    // used for brewing the Phantom Ward potion
    public static final Item KING_PHANTOM_ESSENCE = registerItem(
        "king_phantom_essence",
        key -> new Item(new Item.Properties().setId(key))
    );
    
    public static final Item ETHEREAL_ORB_SPAWN_EGG = registerItem(
        "ethereal_orb_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.ETHEREAL_ORB,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item KING_PHANTOM_SPAWN_EGG = registerItem(
        "king_phantom_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.KING_PHANTOM,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "shadow_creaking_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.SHADOW_CREAKING,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item MINI_SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "mini_shadow_creaking_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.MINI_SHADOW_CREAKING,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item TINY_SHADOW_CREAKING_SPAWN_EGG = registerItem(
        "tiny_shadow_creaking_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.TINY_SHADOW_CREAKING,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item VOID_TARDIGRADE_SPAWN_EGG = registerItem(
        "void_tardigrade_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.VOID_TARDIGRADE,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    public static final Item TETHERLING_SPAWN_EGG = registerItem(
        "tetherling_spawn_egg",
        key -> new com.theendupdate.item.CustomSpawnEggItem(
            ModEntities.TETHERLING,
            new Item.Properties().setId(key)
        ),
        net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS
    );

    // Shadow Hunter's Map is a regular filled_map with special NBT, no custom item needed
    // Closed Ender Chrysanthemum places a permanently-closed flower, registered as a normal block item in ModBlocks

    @SafeVarargs
    private static Item registerItem(String name, java.util.function.Function<ResourceKey<Item>, Item> factory, net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab>... groups) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item item = factory.apply(key);
        Registry.register(BuiltInRegistries.ITEM, id, item);
        // default to INGREDIENTS tab if none specified
        if (groups == null || groups.length == 0) {
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(item));
        } else {
            for (net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> group : groups) {
                CreativeModeTabEvents.modifyOutputEvent(group).register(output -> output.accept(item));
            }
        }
        return item;
    }

    public static void registerModItems() {
        // no-op; just forces the class to load so static field initializers run
    }
}
