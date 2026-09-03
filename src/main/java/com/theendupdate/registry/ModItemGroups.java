package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Registers the custom creative mode tab for the End Update mod and fills it with all mod content.
 */
public final class ModItemGroups {
    public static final ResourceKey<CreativeModeTab> END_UPDATE_GROUP_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "end_update"));

    private ModItemGroups() {}

    public static void register() {
        CreativeModeTab group = CreativeModeTab.builder(null, -1)
            .title(Component.translatable("itemGroup." + TheEndUpdate.MOD_ID + ".end_update"))
            .icon(() -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "end_mire"))))
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, END_UPDATE_GROUP_KEY.identifier(), group);

        CreativeModeTabEvents.modifyOutputEvent(END_UPDATE_GROUP_KEY).register(output -> {
            output.accept(ModBlocks.END_MIRE);
            output.accept(ModBlocks.END_MURK);
            output.accept(ModBlocks.MOLD_BLOCK);
            output.accept(ModBlocks.ASH_STONE);
            output.accept(ModBlocks.ASH_STONE_STAIRS);
            output.accept(ModBlocks.ASH_STONE_SLAB);
            output.accept(ModBlocks.ASH_STONE_WALL);
            output.accept(ModBlocks.SMOOTH_ASH_STONE);
            output.accept(ModBlocks.SMOOTH_ASH_STONE_STAIRS);
            output.accept(ModBlocks.SMOOTH_ASH_STONE_SLAB);
            output.accept(ModBlocks.SMOOTH_ASH_STONE_WALL);
            
            output.accept(ModBlocks.VOID_BLOOM);
            output.accept(ModBlocks.ENDER_CHRYSANTHEMUM);
            output.accept(ModBlocks.CLOSED_ENDER_CHRYSANTHEMUM);
            output.accept(ModBlocks.VOID_SAP);
            output.accept(ModBlocks.TENDRIL_SPROUT);
            output.accept(ModBlocks.TENDRIL_THREAD);
            output.accept(ModBlocks.TENDRIL_CORE);
            
            output.accept(ModBlocks.ETHEREAL_SPOROCARP);
            output.accept(ModBlocks.ETHEREAL_PUSTULE);
            output.accept(ModBlocks.ETHEREAL_PLANKS);
            output.accept(ModBlocks.ETHEREAL_STAIRS);
            output.accept(ModBlocks.ETHEREAL_SLAB);
            output.accept(ModBlocks.ETHEREAL_DOOR);
            output.accept(ModBlocks.ETHEREAL_TRAPDOOR);
            output.accept(ModBlocks.ETHEREAL_FENCE);
            output.accept(ModBlocks.ETHEREAL_FENCE_GATE);
            output.accept(ModBlocks.ETHEREAL_BUTTON);
            output.accept(ModBlocks.ETHEREAL_PRESSURE_PLATE);
            
            // shelves, signs, hanging signs go after pressure plate, before bulb
            output.accept(ModBlocks.ETHEREAL_SHELF);
            if (ModBlocks.ETHEREAL_SIGN != null) {
                output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_sign")));
            }
            if (ModBlocks.ETHEREAL_HANGING_SIGN != null) {
                output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_hanging_sign")));
            }
            
            output.accept(ModBlocks.ETHEREAL_BULB);
            
            output.accept(ModBlocks.SHADOW_CRYPTOMYCOTA);
            output.accept(ModBlocks.SHADOW_UMBRACARP);
            output.accept(ModBlocks.STRIPPED_SHADOW_CRYPTOMYCOTA);
            output.accept(ModBlocks.STRIPPED_SHADOW_UMBRACARP);
            output.accept(ModBlocks.SHADOW_PLANKS);
            output.accept(ModBlocks.SHADOW_STAIRS);
            output.accept(ModBlocks.SHADOW_SLAB);
            output.accept(ModBlocks.SHADOW_DOOR);
            output.accept(ModBlocks.SHADOW_TRAPDOOR);
            output.accept(ModBlocks.SHADOW_FENCE);
            output.accept(ModBlocks.SHADOW_FENCE_GATE);
            output.accept(ModBlocks.SHADOW_BUTTON);
            output.accept(ModBlocks.SHADOW_PRESSURE_PLATE);
            
            // shelves, signs, hanging signs go after pressure plate, before shadow claw
            output.accept(ModBlocks.SHADOW_SHELF);
            if (ModBlocks.SHADOW_SIGN != null) {
                output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_sign")));
            }
            if (ModBlocks.SHADOW_HANGING_SIGN != null) {
                output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_hanging_sign")));
            }
            
            output.accept(ModBlocks.MEMBRANE_BLOCK);
            
            output.accept(ModBlocks.SHADOW_CLAW);
            
            output.accept(ModBlocks.MOLD_CRAWL);
            output.accept(ModBlocks.MOLD_SPORE);
            output.accept(ModBlocks.MOLD_SPORE_TUFT);
            output.accept(ModBlocks.MOLD_SPORE_SPROUT);
            
            output.accept(ModBlocks.STELLARITH_CRYSTAL);
            output.accept(ModBlocks.VOIDSTAR_BLOCK);
            output.accept(ModBlocks.ASTRAL_REMNANT);
            output.accept(ModBlocks.SPECTRAL_BLOCK);
            output.accept(ModBlocks.QUANTUM_GATEWAY);
            output.accept(ModBlocks.NEBULA_VENT_BLOCK);
            output.accept(ModBlocks.GRAVITITE_ORE);
            output.accept(ModBlocks.SHADOW_ALTAR);
            
            output.accept(ModItems.VOIDSTAR_SHARD);
            output.accept(ModItems.VOIDSTAR_NUGGET);
            output.accept(ModItems.VOIDSTAR_INGOT);
            output.accept(ModItems.SPECTRAL_DEBRIS);
            output.accept(ModItems.SPECTRAL_CLUSTER);
            output.accept(ModItems.TARDIGRADE_SHELL_BIT);
            output.accept(ModItems.TARDIGRADE_SHELL_BRICK);
            
            output.accept(ModItems.ENCHANTED_BOOK_COVER);
            output.accept(ModItems.ENCHANTED_PAGES);
            output.accept(ModItems.WOOD_CHIP);
            output.accept(ModItems.WOODEN_CONE);
            output.accept(ModItems.ICE_CREAM_CONE);
            output.accept(ModItems.STRAWBERRY_ICE_CREAM_CONE);
            
            output.accept(ModItems.GRAVITITE_ESSENCE);
            output.accept(ModItems.PURE_GRAVITITE);
            
            output.accept(ModItems.KING_PHANTOM_ESSENCE);
            
            output.accept(ModItems.ETHEREAL_ORB_SPAWN_EGG);
            output.accept(ModItems.KING_PHANTOM_SPAWN_EGG);
            output.accept(ModItems.SHADOW_CREAKING_SPAWN_EGG);
            output.accept(ModItems.MINI_SHADOW_CREAKING_SPAWN_EGG);
            output.accept(ModItems.TINY_SHADOW_CREAKING_SPAWN_EGG);
            output.accept(ModItems.VOID_TARDIGRADE_SPAWN_EGG);
            output.accept(ModItems.TETHERLING_SPAWN_EGG);
        });
    }
}


