package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Registers the custom creative mode tab for the End Update mod and fills it with all mod content.
 */
public final class ModItemGroups {
    public static final RegistryKey<ItemGroup> END_UPDATE_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(TemplateMod.MOD_ID, "end_update"));

    private ModItemGroups() {}

    public static void register() {
        // Create item group with icon and title
        ItemGroup group = ItemGroup.create(null, -1)
            .displayName(Text.translatable("itemGroup." + TemplateMod.MOD_ID + ".end_update"))
            .icon(() -> new ItemStack(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "end_mire"))))
            .build();
        Registry.register(Registries.ITEM_GROUP, END_UPDATE_GROUP_KEY.getValue(), group);

        // Populate the group with manually ordered items
        ItemGroupEvents.modifyEntriesEvent(END_UPDATE_GROUP_KEY).register(entries -> {
            // Terrain blocks
            entries.add(ModBlocks.END_MIRE);
            entries.add(ModBlocks.END_MURK);
            entries.add(ModBlocks.MOLD_BLOCK);
            
            // Plants
            entries.add(ModBlocks.VOID_BLOOM);
            entries.add(ModBlocks.ENDER_CHRYSANTHEMUM);
            entries.add(ModBlocks.VOID_SAP);
            entries.add(ModBlocks.TENDRIL_SPROUT);
            entries.add(ModBlocks.TENDRIL_THREAD);
            entries.add(ModBlocks.TENDRIL_CORE);
            
            // Ethereal wood set
            entries.add(ModBlocks.ETHEREAL_SPOROCARP);
            entries.add(ModBlocks.ETHEREAL_PUSTULE);
            entries.add(ModBlocks.ETHEREAL_PLANKS);
            entries.add(ModBlocks.ETHEREAL_STAIRS);
            entries.add(ModBlocks.ETHEREAL_SLAB);
            entries.add(ModBlocks.ETHEREAL_DOOR);
            entries.add(ModBlocks.ETHEREAL_TRAPDOOR);
            entries.add(ModBlocks.ETHEREAL_FENCE);
            entries.add(ModBlocks.ETHEREAL_FENCE_GATE);
            entries.add(ModBlocks.ETHEREAL_BUTTON);
            entries.add(ModBlocks.ETHEREAL_PRESSURE_PLATE);
            
            // Ethereal shelves, signs, hanging signs (after pressure plate, before bulb)
            entries.add(ModBlocks.ETHEREAL_SHELF);
            if (ModBlocks.ETHEREAL_SIGN != null) {
                entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "ethereal_sign")));
            }
            if (ModBlocks.ETHEREAL_HANGING_SIGN != null) {
                entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "ethereal_hanging_sign")));
            }
            
            entries.add(ModBlocks.ETHEREAL_BULB);
            
            // Shadow wood set
            entries.add(ModBlocks.SHADOW_CRYPTOMYCOTA);
            entries.add(ModBlocks.SHADOW_UMBRACARP);
            entries.add(ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA);
            entries.add(ModBlocks.STRIPPED_SHADOW_UMBRACARP);
            entries.add(ModBlocks.SHADOW_PLANKS);
            entries.add(ModBlocks.SHADOW_STAIRS);
            entries.add(ModBlocks.SHADOW_SLAB);
            entries.add(ModBlocks.SHADOW_DOOR);
            entries.add(ModBlocks.SHADOW_TRAPDOOR);
            entries.add(ModBlocks.SHADOW_FENCE);
            entries.add(ModBlocks.SHADOW_FENCE_GATE);
            entries.add(ModBlocks.SHADOW_BUTTON);
            entries.add(ModBlocks.SHADOW_PRESSURE_PLATE);
            
            // Shadow shelves, signs, hanging signs (after pressure plate, before shadow claw)
            entries.add(ModBlocks.SHADOW_SHELF);
            if (ModBlocks.SHADOW_SIGN != null) {
                entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "shadow_sign")));
            }
            if (ModBlocks.SHADOW_HANGING_SIGN != null) {
                entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "shadow_hanging_sign")));
            }
            
            entries.add(ModBlocks.SHADOW_CLAW);
            
            // Mold variants
            entries.add(ModBlocks.MOLD_CRAWL);
            entries.add(ModBlocks.MOLD_SPORE);
            entries.add(ModBlocks.MOLD_SPORE_TUFT);
            entries.add(ModBlocks.MOLD_SPORE_SPROUT);
            
            // Crystals and special blocks
            entries.add(ModBlocks.STELLARITH_CRYSTAL);
            entries.add(ModBlocks.VOIDSTAR_BLOCK);
            entries.add(ModBlocks.ASTRAL_REMNANT);
            entries.add(ModBlocks.SPECTRAL_BLOCK);
            entries.add(ModBlocks.QUANTUM_GATEWAY);
            entries.add(ModBlocks.GRAVITITE_ORE);
            entries.add(ModBlocks.SHADOW_ALTAR);
            
            // Items - materials
            entries.add(ModItems.VOIDSTAR_SHARD);
            entries.add(ModItems.VOIDSTAR_NUGGET);
            entries.add(ModItems.VOIDSTAR_INGOT);
            entries.add(ModItems.SPECTRAL_DEBRIS);
            entries.add(ModItems.SPECTRAL_CLUSTER);
            
            // Items - quest items
            entries.add(ModItems.ENCHANTED_BOOK_COVER);
            entries.add(ModItems.ENCHANTED_PAGES);
            entries.add(ModItems.WOOD_CHIP);
            entries.add(ModItems.WOODEN_CONE);
            entries.add(ModItems.ICE_CREAM_CONE);
            entries.add(ModItems.STRAWBERRY_ICE_CREAM_CONE);
            
            // Items - Gravitite materials
            entries.add(ModItems.GRAVITITE_ESSENCE);
            entries.add(ModItems.PURE_GRAVITITE);
            
            // Spawn eggs
            entries.add(ModItems.ETHEREAL_ORB_SPAWN_EGG);
            entries.add(ModItems.SHADOW_CREAKING_SPAWN_EGG);
            entries.add(ModItems.MINI_SHADOW_CREAKING_SPAWN_EGG);
            entries.add(ModItems.TINY_SHADOW_CREAKING_SPAWN_EGG);
        });
    }
}


