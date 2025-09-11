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
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;

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

    public static final Block END_MURK = registerBlock(
        "end_murk",
        key -> new com.theendupdate.block.EndMurkBlock(
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

    public static final Block ENDER_CHRYSANTHEMUM = registerBlock(
        "ender_chrysanthemum",
        key -> new com.theendupdate.block.EnderChrysanthemumBlock(
            AbstractBlock.Settings
                .copy(Blocks.POPPY)
                .offset(AbstractBlock.OffsetType.NONE)
                .nonOpaque()
                .registryKey(key)
        )
    );

    public static final Block VOID_SAP = registerBlock(
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
                .copy(Blocks.OAK_LOG)
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

    // Wood set extensions
    public static final Block ETHEREAL_DOOR = registerBlock(
        "ethereal_door",
        key -> new DoorBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_DOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block ETHEREAL_TRAPDOOR = registerBlock(
        "ethereal_trapdoor",
        key -> new TrapdoorBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block ETHEREAL_FENCE = registerBlock(
        "ethereal_fence",
        key -> new net.minecraft.block.FenceBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_FENCE_GATE = registerBlock(
        "ethereal_fence_gate",
        key -> new FenceGateBlock(
            WoodType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

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

    // Shadow Cryptomycota wood set (clone of Ethereal set)
    public static final Block SHADOW_CRYPTOMYCOTA = registerBlock(
        "shadow_cryptomycota",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_LOG)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block UMBRACARP = registerBlock(
        "umbracarp",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_WOOD)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block SHADOW_PLANKS = registerBlock(
        "shadow_planks",
        key -> new Block(
            AbstractBlock.Settings
                .copy(Blocks.OAK_PLANKS)
                .sounds(BlockSoundGroup.WOOD)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block SHADOW_STAIRS = registerBlock(
        "shadow_stairs",
        key -> new StairsBlock(
            ModBlocks.SHADOW_PLANKS.getDefaultState(),
            AbstractBlock.Settings.copy(Blocks.OAK_STAIRS).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_SLAB = registerBlock(
        "shadow_slab",
        key -> new SlabBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_SLAB).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_DOOR = registerBlock(
        "shadow_door",
        key -> new DoorBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_DOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block SHADOW_TRAPDOOR = registerBlock(
        "shadow_trapdoor",
        key -> new TrapdoorBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block SHADOW_FENCE = registerBlock(
        "shadow_fence",
        key -> new net.minecraft.block.FenceBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_FENCE_GATE = registerBlock(
        "shadow_fence_gate",
        key -> new FenceGateBlock(
            WoodType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_BUTTON = registerBlock(
        "shadow_button",
        key -> new ButtonBlock(
            BlockSetType.OAK,
            30,
            AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_PRESSURE_PLATE = registerBlock(
        "shadow_pressure_plate",
        key -> new PressurePlateBlock(
            BlockSetType.OAK,
            AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    // Shadow Claw sapling
    public static final Block SHADOW_CLAW = registerBlock(
        "shadow_claw",
        key -> new com.theendupdate.block.ShadowClawBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_SAPLING)
                .nonOpaque()
                .ticksRandomly()
                .registryKey(key)
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

    public static final Block MOLD_SPORE = registerBlock(
        "mold_spore",
        key -> new com.theendupdate.block.MoldSporeBlock(
            AbstractBlock.Settings
                .copy(Blocks.WARPED_ROOTS)
                .nonOpaque()
                .noCollision()
                .sounds(BlockSoundGroup.NETHER_WART)
                .offset(AbstractBlock.OffsetType.XZ)
                .registryKey(key)
        )
    );

    public static final Block MOLD_SPORE_TUFT = registerBlock(
        "mold_spore_tuft",
        key -> new com.theendupdate.block.MoldSporeTuftBlock(
            AbstractBlock.Settings
                .copy(Blocks.WARPED_ROOTS)
                .nonOpaque()
                .noCollision()
                .sounds(BlockSoundGroup.NETHER_WART)
                .offset(AbstractBlock.OffsetType.XZ)
                .registryKey(key)
        )
    );

    public static final Block MOLD_SPORE_SPROUT = registerBlock(
        "mold_spore_sprout",
        key -> new com.theendupdate.block.MoldSporeSproutBlock(
            AbstractBlock.Settings
                .copy(Blocks.ROSE_BUSH)
                .nonOpaque()
                .noCollision()
                .sounds(BlockSoundGroup.GRASS)
                .offset(AbstractBlock.OffsetType.XZ)
                .registryKey(key)
        )
    );

    // Crystals and metals
    public static final Block STELLARITH_CRYSTAL = registerBlock(
        "stellarith_crystal",
        key -> new com.theendupdate.block.StellarithCrystalBlock(
            AbstractBlock.Settings
                .copy(Blocks.AMETHYST_BLOCK)
                .registryKey(key)
        )
    );

    public static final Block VOIDSTAR_BLOCK = registerBlock(
        "voidstar_block",
        key -> new Block(
            AbstractBlock.Settings
                .copy(Blocks.IRON_BLOCK)
                .strength(50.0F, 6.0F) // Match Netherite hardness; keep iron-like blast resistance
                .registryKey(key)
        )
    );

    public static final Block ASTRAL_REMNANT = registerBlock(
        "astral_remnant",
        key -> new Block(
            AbstractBlock.Settings
                .copy(Blocks.AMETHYST_BLOCK)
                .strength(1.9F, 2.0F) // ~25% harder than amethyst, slightly more resistant
                .registryKey(key)
        )
    );

    // Spectral Block - brightest block with extended effective radius
    public static final Block SPECTRAL_BLOCK = registerBlock(
        "spectral_block",
        key -> new com.theendupdate.block.SpectralBlock(
            AbstractBlock.Settings
                .copy(Blocks.GLOWSTONE)
                .luminance(state -> 15)
                .registryKey(key)
        )
    );

    // Beacon-friendly transparent block
    public static final Block QUANTUM_GATEWAY = registerBlock(
        "quantum_gateway",
        key -> new com.theendupdate.block.QuantumGatewayBlock(
            AbstractBlock.Settings
                .copy(Blocks.GLASS) // Non-opaque like glass so beacon beams pass through
                .registryKey(key)
        )
    );

    // Gravitite Ore: blast-proof like Netherite block; fireproof block item
    public static final Block GRAVITITE_ORE = registerBlockFireproofItem(
        "gravitite_ore",
        key -> new Block(
            AbstractBlock.Settings
                .copy(Blocks.NETHERITE_BLOCK) // match netherite mining speed/requirements and blast resistance
                .requiresTool()
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
        BlockItem item;
        boolean isPlantLike = (block instanceof PlantBlock) || (block instanceof TallPlantBlock);
        // Use adjacent-placing behavior for delicate plants so they don't replace flowers/plants
        item = isPlantLike ? new com.theendupdate.item.AdjacentPlantBlockItem(block, itemSettings)
                           : new BlockItem(block, itemSettings);
        Registry.register(Registries.ITEM, id, item);
        // Add to a creative tab so it's visible in creative inventory
        if (isPlantLike) {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> entries.add(block));
        } else {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(block));
        }
        return block;
    }

    // Variant that registers a fireproof BlockItem for blast/burn-proof item form
    private static Block registerBlockFireproofItem(String name, java.util.function.Function<RegistryKey<Block>, Block> factory) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id);
        Block block = factory.apply(key);
        Registry.register(Registries.BLOCK, id, block);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item.Settings itemSettings = new Item.Settings().registryKey(itemKey).fireproof();
        BlockItem item;
        boolean isPlantLike = (block instanceof PlantBlock) || (block instanceof TallPlantBlock);
        item = isPlantLike ? new com.theendupdate.item.AdjacentPlantBlockItem(block, itemSettings)
                           : new BlockItem(block, itemSettings);
        Registry.register(Registries.ITEM, id, item);
        if (isPlantLike) {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> entries.add(block));
        } else if (name.endsWith("_ore")) {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> entries.add(block));
        } else {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(block));
        }
        return block;
    }

    public static void registerModBlocks() {
        // No-op, calling this ensures the class is loaded and static initializers run
    }
}


