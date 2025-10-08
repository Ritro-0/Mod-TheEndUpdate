package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
            .icon(() -> new ItemStack(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "voidstar_block"))))
            .build();
        Registry.register(Registries.ITEM_GROUP, END_UPDATE_GROUP_KEY.getValue(), group);

        // Populate the group with all mod blocks and items by deferring to existing registries
        ItemGroupEvents.modifyEntriesEvent(END_UPDATE_GROUP_KEY).register(entries -> {
            // Add all registered blocks that have items
            Registries.BLOCK.stream()
                .filter(block -> Registries.BLOCK.getId(block) != null && TemplateMod.MOD_ID.equals(Registries.BLOCK.getId(block).getNamespace()))
                .map(block -> block.asItem())
                .filter(item -> item != Items.AIR)
                .forEach(entries::add);

            // Add all standalone items
            Registries.ITEM.stream()
                .filter(item -> {
                    Identifier id = Registries.ITEM.getId(item);
                    return id != null && TemplateMod.MOD_ID.equals(id.getNamespace());
                })
                .forEach(entries::add);
        });
    }
}


