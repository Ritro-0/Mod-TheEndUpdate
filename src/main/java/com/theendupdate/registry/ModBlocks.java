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
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.ShelfBlock;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SignItem;
import net.minecraft.item.HangingSignItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    // Register custom WoodTypes and BlockSetTypes using Fabric API
    public static final BlockSetType ETHEREAL_BLOCK_SET_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
        .register(Identifier.of(TemplateMod.MOD_ID, "ethereal"));
    public static final WoodType ETHEREAL_WOOD_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(WoodType.OAK)
        .register(Identifier.of(TemplateMod.MOD_ID, "ethereal"), ETHEREAL_BLOCK_SET_TYPE);
    
    public static final BlockSetType SHADOW_BLOCK_SET_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
        .register(Identifier.of(TemplateMod.MOD_ID, "shadow"));
    public static final WoodType SHADOW_WOOD_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(WoodType.OAK)
        .register(Identifier.of(TemplateMod.MOD_ID, "shadow"), SHADOW_BLOCK_SET_TYPE);
    
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
    public static final Block POTTED_VOID_BLOOM = registerBlockNoItem(
        "potted_void_bloom",
        key -> new FlowerPotBlock(
            VOID_BLOOM,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
    public static final Block POTTED_ENDER_CHRYSANTHEMUM = registerBlockNoItem(
        "potted_ender_chrysanthemum",
        key -> new FlowerPotBlock(
            ENDER_CHRYSANTHEMUM,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
        )
    );

    public static final Block VOID_SAP = registerBlock(
        "void_sap",
        key -> new com.theendupdate.block.VoidSapBlock(
            AbstractBlock.Settings
                .copy(Blocks.GLOW_LICHEN)
                .luminance(state -> 0) // Remove light emission
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
    public static final Block POTTED_TENDRIL_SPROUT = registerBlockNoItem(
        "potted_tendril_sprout",
        key -> new FlowerPotBlock(
            TENDRIL_SPROUT,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
    public static final Block POTTED_TENDRIL_THREAD = registerBlockNoItem(
        "potted_tendril_thread",
        key -> new FlowerPotBlock(
            TENDRIL_THREAD,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
    public static final Block POTTED_TENDRIL_CORE = registerBlockNoItem(
        "potted_tendril_core",
        key -> new FlowerPotBlock(
            TENDRIL_CORE,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
        key -> new com.theendupdate.block.EtherealPlanksBlock(
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
            ETHEREAL_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_DOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block ETHEREAL_TRAPDOOR = registerBlock(
        "ethereal_trapdoor",
        key -> new TrapdoorBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block ETHEREAL_FENCE = registerBlock(
        "ethereal_fence",
        key -> new FenceBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_FENCE_GATE = registerBlock(
        "ethereal_fence_gate",
        key -> new FenceGateBlock(
            ETHEREAL_WOOD_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_BUTTON = registerBlock(
        "ethereal_button",
        key -> new ButtonBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            30,
            AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block ETHEREAL_PRESSURE_PLATE = registerBlock(
        "ethereal_pressure_plate",
        key -> new PressurePlateBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    // Ethereal signs - must be declared before block entity registration
    public static Block ETHEREAL_SIGN = null;
    public static Block ETHEREAL_WALL_SIGN = null;
    public static Block ETHEREAL_HANGING_SIGN = null;
    public static Block ETHEREAL_WALL_HANGING_SIGN = null;

    // Ethereal Bulb - luminous button crafted from orb bulb, longer press, bright like soul lantern (~10)
    public static final Block ETHEREAL_BULB = registerBlock(
        "ethereal_bulb",
        key -> new com.theendupdate.block.EtherealBulbButtonBlock(
            BlockSetType.STONE,
            60,
            AbstractBlock.Settings.copy(Blocks.STONE_BUTTON)
                .sounds(BlockSoundGroup.AMETHYST_BLOCK)
                .luminance(state -> 10)
                .nonOpaque()
                .registryKey(key)
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

    public static final Block SHADOW_UMBRACARP = registerBlock(
        "shadow_umbracarp",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_WOOD)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block STRIPPED_SHADOW_CRYPTOMYCOTA = registerBlock(
        "stripped_shadow_cryptomycota",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            AbstractBlock.Settings
                .copy(Blocks.OAK_LOG)
                .sounds(BlockSoundGroup.WOOD)
                .strength(2.0F, 3.0F)
                .luminance(state -> 2)
                .registryKey(key)
        )
    );

    public static final Block STRIPPED_SHADOW_UMBRACARP = registerBlock(
        "stripped_shadow_umbracarp",
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
        key -> new com.theendupdate.block.ShadowPlanksBlock(
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
            SHADOW_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_DOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block SHADOW_TRAPDOOR = registerBlock(
        "shadow_trapdoor",
        key -> new TrapdoorBlock(
            SHADOW_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block SHADOW_FENCE = registerBlock(
        "shadow_fence",
        key -> new FenceBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_FENCE_GATE = registerBlock(
        "shadow_fence_gate",
        key -> new FenceGateBlock(
            SHADOW_WOOD_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_BUTTON = registerBlock(
        "shadow_button",
        key -> new ButtonBlock(
            SHADOW_BLOCK_SET_TYPE,
            30,
            AbstractBlock.Settings.copy(Blocks.OAK_BUTTON).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    public static final Block SHADOW_PRESSURE_PLATE = registerBlock(
        "shadow_pressure_plate",
        key -> new PressurePlateBlock(
            SHADOW_BLOCK_SET_TYPE,
            AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
        )
    );

    // Shadow signs - must be declared before block entity registration
    public static Block SHADOW_SIGN = null;
    public static Block SHADOW_WALL_SIGN = null;
    public static Block SHADOW_HANGING_SIGN = null;
    public static Block SHADOW_WALL_HANGING_SIGN = null;

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

    // Potted variant (no item). Texture/model added in resources.
    public static final Block POTTED_SHADOW_CLAW = registerBlockNoItem(
        "potted_shadow_claw",
        key -> new FlowerPotBlock(
            SHADOW_CLAW,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
    public static final Block POTTED_MOLD_SPORE = registerBlockNoItem(
        "potted_mold_spore",
        key -> new FlowerPotBlock(
            MOLD_SPORE,
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION).nonOpaque().registryKey(key)
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
        key -> new com.theendupdate.block.VoidstarBlock(
            AbstractBlock.Settings
                .copy(Blocks.IRON_BLOCK)
                .strength(50.0F, 6.0F) // Match Netherite hardness; keep iron-like blast resistance
                .registryKey(key)
        )
    );

    public static final Block ASTRAL_REMNANT = registerBlock(
        "astral_remnant",
        key -> new com.theendupdate.block.AstralRemnantBlock(
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
        key -> new com.theendupdate.block.GravititeOreBlock(
            AbstractBlock.Settings
                .copy(Blocks.NETHERITE_BLOCK) // match netherite mining speed/requirements and blast resistance
                .requiresTool()
                .registryKey(key)
        )
    );

    // Shadow Altar - functional block with activation, particles, and cooldown
    public static final Block SHADOW_ALTAR = registerBlock(
        "shadow_altar",
        key -> new com.theendupdate.block.ShadowAltarBlock(
            AbstractBlock.Settings
                .copy(Blocks.SPAWNER) // spawner properties for proper transparency rendering
                .requiresTool()
                .strength(50.0F, 1200.0F)
                .sounds(BlockSoundGroup.WOOD) // wooden-themed, but extremely tough
                .nonOpaque()
                .registryKey(key)
        )
    );

    // Shelves - using vanilla ShelfBlock class for item display functionality
    public static final Block ETHEREAL_SHELF = registerBlock(
        "ethereal_shelf",
        key -> new net.minecraft.block.ShelfBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    public static final Block SHADOW_SHELF = registerBlock(
        "shadow_shelf",
        key -> new net.minecraft.block.ShelfBlock(
            AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).nonOpaque().registryKey(key)
        )
    );

    // Membrane Block - crafted from 9 phantom membranes
    public static final Block MEMBRANE_BLOCK = registerBlock(
        "membrane_block",
        key -> new com.theendupdate.block.MembraneBlock(
            AbstractBlock.Settings
                .copy(Blocks.RESIN_BLOCK)
                .sounds(BlockSoundGroup.RESIN_BRICKS)
                .strength(0.8F, 0.8F)
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
        // NOTE: Creative tab population moved to registerModBlocks() for manual ordering
        return block;
    }

    // Registers a block without creating a BlockItem (for potted plants, etc.)
    private static Block registerBlockNoItem(String name, java.util.function.Function<RegistryKey<Block>, Block> factory) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id);
        Block block = factory.apply(key);
        Registry.register(Registries.BLOCK, id, block);
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
        // NOTE: Creative tab population moved to registerModBlocks() for manual ordering
        return block;
    }

    // Register sign items (signs need special SignItem)
    private static void registerSignItem(String name, Block standingSign, Block wallSign) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item.Settings itemSettings = new Item.Settings().registryKey(itemKey);
        SignItem item = new SignItem(standingSign, wallSign, itemSettings);
        Registry.register(Registries.ITEM, id, item);
        // NOTE: Creative tab population moved to registerModBlocks() for manual ordering
    }

    // Register hanging sign items (hanging signs need special HangingSignItem)
    private static void registerHangingSignItem(String name, Block hangingSign, Block wallHangingSign) {
        Identifier id = Identifier.of(TemplateMod.MOD_ID, name);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item.Settings itemSettings = new Item.Settings().registryKey(itemKey);
        HangingSignItem item = new HangingSignItem(hangingSign, wallHangingSign, itemSettings);
        Registry.register(Registries.ITEM, id, item);
        // NOTE: Creative tab population moved to registerModBlocks() for manual ordering
    }

    public static void registerModBlocks() {
        // No-op, calling this ensures the class is loaded and static initializers run
        
        // Register sign blocks using VANILLA sign block classes - just like BOP does!
        ETHEREAL_SIGN = registerBlockNoItem(
            "ethereal_sign",
            key -> new SignBlock(
                ETHEREAL_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        
        ETHEREAL_WALL_SIGN = registerBlockNoItem(
            "ethereal_wall_sign",
            key -> new WallSignBlock(
                ETHEREAL_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        
        ETHEREAL_HANGING_SIGN = registerBlockNoItem(
            "ethereal_hanging_sign",
            key -> new HangingSignBlock(
                ETHEREAL_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_HANGING_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        net.minecraft.block.entity.BlockEntityType.HANGING_SIGN.addSupportedBlock(ETHEREAL_HANGING_SIGN);
        
        ETHEREAL_WALL_HANGING_SIGN = registerBlockNoItem(
            "ethereal_wall_hanging_sign",
            key -> new WallHangingSignBlock(
                ETHEREAL_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_WALL_HANGING_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        net.minecraft.block.entity.BlockEntityType.HANGING_SIGN.addSupportedBlock(ETHEREAL_WALL_HANGING_SIGN);
        
        SHADOW_SIGN = registerBlockNoItem(
            "shadow_sign",
            key -> new SignBlock(
                SHADOW_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        
        SHADOW_WALL_SIGN = registerBlockNoItem(
            "shadow_wall_sign",
            key -> new WallSignBlock(
                SHADOW_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        
        SHADOW_HANGING_SIGN = registerBlockNoItem(
            "shadow_hanging_sign",
            key -> new HangingSignBlock(
                SHADOW_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_HANGING_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        net.minecraft.block.entity.BlockEntityType.HANGING_SIGN.addSupportedBlock(SHADOW_HANGING_SIGN);
        
        SHADOW_WALL_HANGING_SIGN = registerBlockNoItem(
            "shadow_wall_hanging_sign",
            key -> new WallHangingSignBlock(
                SHADOW_WOOD_TYPE,
                AbstractBlock.Settings.copy(Blocks.OAK_WALL_HANGING_SIGN).sounds(BlockSoundGroup.WOOD).luminance(state -> 2).registryKey(key)
            )
        );
        net.minecraft.block.entity.BlockEntityType.HANGING_SIGN.addSupportedBlock(SHADOW_WALL_HANGING_SIGN);
        
        // Register sign items after block initialization
        registerSignItem("ethereal_sign", ETHEREAL_SIGN, ETHEREAL_WALL_SIGN);
        registerSignItem("shadow_sign", SHADOW_SIGN, SHADOW_WALL_SIGN);
        registerHangingSignItem("ethereal_hanging_sign", ETHEREAL_HANGING_SIGN, ETHEREAL_WALL_HANGING_SIGN);
        registerHangingSignItem("shadow_hanging_sign", SHADOW_HANGING_SIGN, SHADOW_WALL_HANGING_SIGN);
        
        // Manually populate vanilla creative tabs with proper ordering
        populateVanillaCreativeTabs();
    }
    
    private static void populateVanillaCreativeTabs() {
        // BUILDING_BLOCKS tab - manually ordered
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            // Ethereal wood set
            entries.add(ETHEREAL_SPOROCARP);
            entries.add(ETHEREAL_PUSTULE);
            entries.add(ETHEREAL_PLANKS);
            entries.add(ETHEREAL_STAIRS);
            entries.add(ETHEREAL_SLAB);
            entries.add(ETHEREAL_DOOR);
            entries.add(ETHEREAL_TRAPDOOR);
            entries.add(ETHEREAL_FENCE);
            entries.add(ETHEREAL_FENCE_GATE);
            entries.add(ETHEREAL_BUTTON);
            entries.add(ETHEREAL_PRESSURE_PLATE);
            // Ethereal shelves, signs, hanging signs (after pressure plate, before bulb)
            entries.add(ETHEREAL_SHELF);
            entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "ethereal_sign")));
            entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "ethereal_hanging_sign")));
            entries.add(ETHEREAL_BULB);
            
            // Shadow wood set
            entries.add(SHADOW_CRYPTOMYCOTA);
            entries.add(SHADOW_UMBRACARP);
            entries.add(STRIPPED_SHADOW_CRYPTOMYCOTA);
            entries.add(STRIPPED_SHADOW_UMBRACARP);
            entries.add(SHADOW_PLANKS);
            entries.add(SHADOW_STAIRS);
            entries.add(SHADOW_SLAB);
            entries.add(SHADOW_DOOR);
            entries.add(SHADOW_TRAPDOOR);
            entries.add(SHADOW_FENCE);
            entries.add(SHADOW_FENCE_GATE);
            entries.add(SHADOW_BUTTON);
            entries.add(SHADOW_PRESSURE_PLATE);
            // Shadow shelves, signs, hanging signs (after pressure plate, before shadow claw)
            entries.add(SHADOW_SHELF);
            entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "shadow_sign")));
            entries.add(Registries.ITEM.get(Identifier.of(TemplateMod.MOD_ID, "shadow_hanging_sign")));
            
            // Other building blocks
            entries.add(END_MIRE);
            entries.add(END_MURK);
            entries.add(MOLD_BLOCK);
            entries.add(MEMBRANE_BLOCK);
            entries.add(STELLARITH_CRYSTAL);
            entries.add(VOIDSTAR_BLOCK);
            entries.add(ASTRAL_REMNANT);
            entries.add(SPECTRAL_BLOCK);
            entries.add(QUANTUM_GATEWAY);
            entries.add(SHADOW_ALTAR);
        });
        
        // NATURAL tab - plants and natural items
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.add(VOID_BLOOM);
            entries.add(ENDER_CHRYSANTHEMUM);
            entries.add(VOID_SAP);
            entries.add(TENDRIL_SPROUT);
            entries.add(TENDRIL_THREAD);
            entries.add(TENDRIL_CORE);
            entries.add(SHADOW_CLAW);
            entries.add(MOLD_CRAWL);
            entries.add(MOLD_SPORE);
            entries.add(MOLD_SPORE_TUFT);
            entries.add(MOLD_SPORE_SPROUT);
            entries.add(MOLD_BLOCK); // Also appears in building blocks
            entries.add(GRAVITITE_ORE);
        });
    }
}


