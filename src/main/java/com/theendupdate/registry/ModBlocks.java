package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.BlockSetType;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block END_MIRE = registerBlock(
        "end_mire",
        key -> new com.theendupdate.block.EndMireBlock(
            AbstractBlock.Settings
                .copy(Blocks.END_STONE)
                .registryKey(key)
        )
    );

    public static final Block MOLD_BLOCK = registerBlock(
        "mold_block",
        key -> new com.theendupdate.block.MoldBlock(
            AbstractBlock.Settings
                .copy(Blocks.MOSS_BLOCK)
                .registryKey(key)
        )
    );

    public static final Block VOID_BLOOM = registerBlock(
        "void_bloom",
        key -> new com.theendupdate.block.VoidBloomBlock(
            AbstractBlock.Settings
                .copy(Blocks.POPPY)
                .offset(AbstractBlock.OffsetType.NONE)
                .nonOpaque()
                .registryKey(key)
        )
    );

    public static final Block VOID_SAP = registerBlockWithoutItem(
        "void_sap",
        key -> new com.theendupdate.block.VoidSapBlock(
            AbstractBlock.Settings
                .copy(Blocks.GLOW_LICHEN)
                .nonOpaque()
                .slipperiness(0.4F) // Honey block value for movement reduction
                .sounds(BlockSoundGroup.SLIME)
                .registryKey(key)
        )
    );

    public static final Block TENDRIL_SPROUT = registerBlock(
        "tendril_sprout",
        key -> new com.theendupdate.block.TendrilSproutBlock(
            AbstractBlock.Settings
                .copy(Blocks.WHEAT)
                .nonOpaque()
                .ticksRandomly()
                .sounds(BlockSoundGroup.GRASS)
                .registryKey(key)
        )
    );

    public static final Block TENDRIL_THREAD = registerBlock(
        "tendril_thread",
        key -> new com.theendupdate.block.TendrilThreadBlock(
            AbstractBlock.Settings
                .copy(Blocks.WHEAT)
                .nonOpaque()
                .ticksRandomly()
                .sounds(BlockSoundGroup.GRASS)
                .registryKey(key)
        )
    );

    public static final Block TENDRIL_CORE = registerBlock(
        "tendril_core",
        key -> new com.theendupdate.block.TendrilCoreBlock(
            AbstractBlock.Settings
                .copy(Blocks.WHEAT)
                .nonOpaque()
                .ticksRandomly()
                .sounds(BlockSoundGroup.GRASS)
                .strength(1.0F, 2.0F) // Slightly stronger than normal plants
                .registryKey(key)
        )
    );

    public static final Block ETHEREAL_SPOROCARP = registerBlock(
        "ethereal_sporocarp",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            AbstractBlock.Settings
                .copy(Blocks.END_STONE)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F) // Log-like durability
                .luminance(state -> 2) // Slight glow like End materials
                .registryKey(key)
        )
    );

    public static final Block ETHEREAL_PUSTULE = registerBlock(
        "ethereal_pustule",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_WOOD)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block ETHEREAL_PLANKS = registerBlock(
        "ethereal_planks",
        key -> new Block(
            AbstractBlock.Settings
                .copy(Blocks.OAK_PLANKS)
                .sounds(BlockSoundGroup.WOOD)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    // Plank variants
    public static final Block ETHEREAL_STAIRS = registerBlock(
        "ethereal_stairs",
        key -> new StairsBlock(
            ModBlocks.ETHEREAL_PLANKS.getDefaultState(),
            AbstractBlock.Settings.copy(Blocks.OAK_STAIRS).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_SLAB = registerBlock(
        "ethereal_slab",
        key -> new SlabBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_SLAB).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    // Fences and fence gates omitted for now; can add later if desired

    public static final Block ETHEREAL_BUTTON = registerBlock(
        "ethereal_button",
        key -> new ButtonBlock(
            BlockSetType.OAK,
            30,
            AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_PRESSURE_PLATE = registerBlock(
        "ethereal_pressure_plate",
        key -> new PressurePlateBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block MOLD_CRAWL = registerBlock(
        "mold_crawl",
        key -> new com.theendupdate.block.MoldcrawlBlock(
            AbstractBlock.Settings
                .copy(Blocks.TWISTING_VINES)
                .nonOpaque()
                .ticksRandomly()
                .noCollision()
                .registryKey(key)
        )
    );

    private static Block registerBlock(String name, java.util.function.Function<RegistryKey<Block>, Block> factory) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id);
        // Construct with registry key to satisfy settings that need an id during construction
        Block block = factory.apply(key);
        // Register the block
        Registry.register(Registries.BLOCK, id, block);
        // Register the block item so it appears in inventory and can be placed
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item.Settings itemSettings = new Item.Settings().registryKey(itemKey);
        Registry.register(Registries.ITEM, id, new BlockItem(block, itemSettings));
        // Add to a creative tab so it's visible in creative inventory
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(block));
        return block;
    }

    private static Block registerBlockWithoutItem(String name, java.util.function.Function<RegistryKey<Block>, Block> factory) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id);
        // Construct with registry key to satisfy settings that need an id during construction
        Block block = factory.apply(key);
        // Register the block only (item will be registered separately)
        Registry.register(Registries.BLOCK, id, block);
        return block;
    }

    public static void registerModBlocks() {
        // No-op, calling this ensures the class is loaded and static initializers run
    }
}


