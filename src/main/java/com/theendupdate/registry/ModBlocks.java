package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModBlocks {
    public static final BlockSetType ETHEREAL_BLOCK_SET_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
        .register(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal"));
    public static final WoodType ETHEREAL_WOOD_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(WoodType.OAK)
        .register(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal"), ETHEREAL_BLOCK_SET_TYPE);
    
    public static final BlockSetType SHADOW_BLOCK_SET_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
        .register(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow"));
    public static final WoodType SHADOW_WOOD_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(WoodType.OAK)
        .register(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow"), SHADOW_BLOCK_SET_TYPE);
    
    public static final Block END_MIRE = registerBlock(
        "end_mire",
        key -> new com.theendupdate.block.EndMireBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.END_STONE)
                .requiresCorrectToolForDrops()
                .strength(0.4F)
                .setId(key)
        )
    );

    public static final Block END_MURK = registerBlock(
        "end_murk",
        key -> new com.theendupdate.block.EndMurkBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.END_STONE)
                .requiresCorrectToolForDrops()
                .strength(0.4F)
                .setId(key)
        )
    );

    public static final Block MOLD_BLOCK = registerBlock(
        "mold_block",
        key -> new com.theendupdate.block.MoldBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.MOSS_BLOCK)
                .setId(key)
        )
    );

    public static final Block ASH_STONE = registerBlock(
        "ash_stone",
        key -> new Block(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.BASALT)
                .strength(0.625F, 2.1F) // half basalt's hardness/blast resistance (1.25, 4.2)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block ASH_STONE_STAIRS = registerBlock(
        "ash_stone_stairs",
        key -> new StairBlock(
            ModBlocks.ASH_STONE.defaultBlockState(),
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.BASALT)
                .strength(0.625F, 2.1F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block ASH_STONE_SLAB = registerBlock(
        "ash_stone_slab",
        key -> new SlabBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.BASALT)
                .strength(0.625F, 2.1F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block ASH_STONE_WALL = registerBlock(
        "ash_stone_wall",
        key -> new WallBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.COBBLESTONE_WALL)
                .strength(0.625F, 2.1F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block SMOOTH_ASH_STONE = registerBlock(
        "smooth_ash_stone",
        key -> new Block(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.SMOOTH_STONE)
                .strength(0.75F, 2.4F) // a bit tougher than ash stone, mirrors vanilla smooth stone
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block SMOOTH_ASH_STONE_STAIRS = registerBlock(
        "smooth_ash_stone_stairs",
        key -> new StairBlock(
            ModBlocks.SMOOTH_ASH_STONE.defaultBlockState(),
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.SMOOTH_STONE)
                .strength(0.75F, 2.4F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block SMOOTH_ASH_STONE_SLAB = registerBlock(
        "smooth_ash_stone_slab",
        key -> new SlabBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.SMOOTH_STONE)
                .strength(0.75F, 2.4F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block SMOOTH_ASH_STONE_WALL = registerBlock(
        "smooth_ash_stone_wall",
        key -> new WallBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.COBBLESTONE_WALL)
                .strength(0.75F, 2.4F)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block VOID_BLOOM = registerBlock(
        "void_bloom",
        key -> new com.theendupdate.block.VoidBloomBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.POPPY)
                .offsetType(BlockBehaviour.OffsetType.NONE)
                .noOcclusion()
                .setId(key)
        )
    );
    public static final Block POTTED_VOID_BLOOM = registerBlockNoItem(
        "potted_void_bloom",
        key -> new FlowerPotBlock(
            VOID_BLOOM,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block ENDER_CHRYSANTHEMUM = registerBlock(
        "ender_chrysanthemum",
        key -> new com.theendupdate.block.EnderChrysanthemumBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.POPPY)
                .offsetType(BlockBehaviour.OffsetType.NONE)
                .noOcclusion()
                .setId(key)
        )
    );
    public static final Block POTTED_ENDER_CHRYSANTHEMUM = registerBlockNoItem(
        "potted_ender_chrysanthemum",
        key -> new FlowerPotBlock(
            ENDER_CHRYSANTHEMUM,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block CLOSED_ENDER_CHRYSANTHEMUM = registerBlock(
        "closed_ender_chrysanthemum",
        key -> new com.theendupdate.block.ClosedEnderChrysanthemumBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.POPPY)
                .offsetType(BlockBehaviour.OffsetType.NONE)
                .noOcclusion()
                .setId(key)
        )
    );

    public static final Block POTTED_CLOSED_ENDER_CHRYSANTHEMUM = registerBlockNoItem(
        "potted_closed_ender_chrysanthemum",
        key -> new com.theendupdate.block.PottedClosedEnderChrysanthemumBlock(
            CLOSED_ENDER_CHRYSANTHEMUM,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block VOID_SAP = registerBlock(
        "void_sap",
        key -> new com.theendupdate.block.VoidSapBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.GLOW_LICHEN)
                .lightLevel(state -> 0)
                .noOcclusion()
                .friction(0.4F) // same as honey block, for the movement-slowing effect
                .sound(SoundType.SLIME_BLOCK)
                .setId(key)
        )
    );

    public static final Block TENDRIL_SPROUT = registerBlock(
        "tendril_sprout",
        key -> new com.theendupdate.block.TendrilSproutBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.WHEAT)
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.GRASS)
                .setId(key)
        )
    );
    public static final Block POTTED_TENDRIL_SPROUT = registerBlockNoItem(
        "potted_tendril_sprout",
        key -> new FlowerPotBlock(
            TENDRIL_SPROUT,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block TENDRIL_THREAD = registerBlock(
        "tendril_thread",
        key -> new com.theendupdate.block.TendrilThreadBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.WHEAT)
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.GRASS)
                .setId(key)
        )
    );
    public static final Block POTTED_TENDRIL_THREAD = registerBlockNoItem(
        "potted_tendril_thread",
        key -> new FlowerPotBlock(
            TENDRIL_THREAD,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block TENDRIL_CORE = registerBlock(
        "tendril_core",
        key -> new com.theendupdate.block.TendrilCoreBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.WHEAT)
                .noOcclusion()
                .randomTicks()
                .sound(SoundType.GRASS)
                .strength(1.0F, 2.0F) // a bit stronger than normal plants
                .setId(key)
        )
    );
    public static final Block POTTED_TENDRIL_CORE = registerBlockNoItem(
        "potted_tendril_core",
        key -> new FlowerPotBlock(
            TENDRIL_CORE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block ETHEREAL_SPOROCARP = registerBlock(
        "ethereal_sporocarp",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_LOG)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2) // faint glow, like other End materials
                .setId(key)
        )
    );

    public static final Block ETHEREAL_PUSTULE = registerBlock(
        "ethereal_pustule",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block ETHEREAL_PLANKS = registerBlock(
        "ethereal_planks",
        key -> new com.theendupdate.block.EtherealPlanksBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_PLANKS)
                .sound(SoundType.WOOD)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block ETHEREAL_STAIRS = registerBlock(
        "ethereal_stairs",
        key -> new StairBlock(
            ModBlocks.ETHEREAL_PLANKS.defaultBlockState(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block ETHEREAL_SLAB = registerBlock(
        "ethereal_slab",
        key -> new SlabBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block ETHEREAL_DOOR = registerBlock(
        "ethereal_door",
        key -> new DoorBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    public static final Block ETHEREAL_TRAPDOOR = registerBlock(
        "ethereal_trapdoor",
        key -> new TrapDoorBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    public static final Block ETHEREAL_FENCE = registerBlock(
        "ethereal_fence",
        key -> new FenceBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block ETHEREAL_FENCE_GATE = registerBlock(
        "ethereal_fence_gate",
        key -> new FenceGateBlock(
            ETHEREAL_WOOD_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block ETHEREAL_BUTTON = registerBlock(
        "ethereal_button",
        key -> new ButtonBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            30,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block ETHEREAL_PRESSURE_PLATE = registerBlock(
        "ethereal_pressure_plate",
        key -> new PressurePlateBlock(
            ETHEREAL_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    // declared before block entity registration needs them
    public static Block ETHEREAL_SIGN = null;
    public static Block ETHEREAL_WALL_SIGN = null;
    public static Block ETHEREAL_HANGING_SIGN = null;
    public static Block ETHEREAL_WALL_HANGING_SIGN = null;

    // luminous button crafted from the orb bulb; longer press, soul-lantern-bright
    public static final Block ETHEREAL_BULB = registerBlock(
        "ethereal_bulb",
        key -> new com.theendupdate.block.EtherealBulbButtonBlock(
            BlockSetType.STONE,
            60,
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 10)
                .noOcclusion()
                .setId(key)
        )
    );

    // shadow wood set clones the ethereal one
    public static final Block SHADOW_CRYPTOMYCOTA = registerBlock(
        "shadow_cryptomycota",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_LOG)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block SHADOW_UMBRACARP = registerBlock(
        "shadow_umbracarp",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block STRIPPED_SHADOW_CRYPTOMYCOTA = registerBlock(
        "stripped_shadow_cryptomycota",
        key -> new com.theendupdate.block.EtherealSporocarpBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_LOG)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block STRIPPED_SHADOW_UMBRACARP = registerBlock(
        "stripped_shadow_umbracarp",
        key -> new com.theendupdate.block.EtherealPustuleBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_WOOD)
                .sound(SoundType.WOOD)
                .strength(2.0F, 3.0F)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block SHADOW_PLANKS = registerBlock(
        "shadow_planks",
        key -> new com.theendupdate.block.ShadowPlanksBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_PLANKS)
                .sound(SoundType.WOOD)
                .lightLevel(state -> 2)
                .setId(key)
        )
    );

    public static final Block SHADOW_STAIRS = registerBlock(
        "shadow_stairs",
        key -> new StairBlock(
            ModBlocks.SHADOW_PLANKS.defaultBlockState(),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block SHADOW_SLAB = registerBlock(
        "shadow_slab",
        key -> new SlabBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block SHADOW_DOOR = registerBlock(
        "shadow_door",
        key -> new DoorBlock(
            SHADOW_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    public static final Block SHADOW_TRAPDOOR = registerBlock(
        "shadow_trapdoor",
        key -> new TrapDoorBlock(
            SHADOW_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    public static final Block SHADOW_FENCE = registerBlock(
        "shadow_fence",
        key -> new FenceBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block SHADOW_FENCE_GATE = registerBlock(
        "shadow_fence_gate",
        key -> new FenceGateBlock(
            SHADOW_WOOD_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block SHADOW_BUTTON = registerBlock(
        "shadow_button",
        key -> new ButtonBlock(
            SHADOW_BLOCK_SET_TYPE,
            30,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static final Block SHADOW_PRESSURE_PLATE = registerBlock(
        "shadow_pressure_plate",
        key -> new PressurePlateBlock(
            SHADOW_BLOCK_SET_TYPE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
        )
    );

    public static Block SHADOW_SIGN = null;
    public static Block SHADOW_WALL_SIGN = null;
    public static Block SHADOW_HANGING_SIGN = null;
    public static Block SHADOW_WALL_HANGING_SIGN = null;

    public static final Block SHADOW_CLAW = registerBlock(
        "shadow_claw",
        key -> new com.theendupdate.block.ShadowClawBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.OAK_SAPLING)
                .noOcclusion()
                .randomTicks()
                .setId(key)
        )
    );

    // no item; texture/model added in resources
    public static final Block POTTED_SHADOW_CLAW = registerBlockNoItem(
        "potted_shadow_claw",
        key -> new FlowerPotBlock(
            SHADOW_CLAW,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block MOLD_CRAWL = registerBlock(
        "mold_crawl",
        key -> new com.theendupdate.block.MoldcrawlBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.TWISTING_VINES)
                .noOcclusion()
                .randomTicks()
                .noCollision()
                .setId(key)
        )
    );

    public static final Block MOLD_SPORE = registerBlock(
        "mold_spore",
        key -> new com.theendupdate.block.MoldSporeBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.WARPED_ROOTS)
                .noOcclusion()
                .noCollision()
                .sound(SoundType.NETHER_WART)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(key)
        )
    );
    public static final Block POTTED_MOLD_SPORE = registerBlockNoItem(
        "potted_mold_spore",
        key -> new FlowerPotBlock(
            MOLD_SPORE,
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion().setId(key)
        )
    );

    public static final Block MOLD_SPORE_TUFT = registerBlock(
        "mold_spore_tuft",
        key -> new com.theendupdate.block.MoldSporeTuftBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.WARPED_ROOTS)
                .noOcclusion()
                .noCollision()
                .sound(SoundType.NETHER_WART)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(key)
        )
    );

    public static final Block MOLD_SPORE_SPROUT = registerBlock(
        "mold_spore_sprout",
        key -> new com.theendupdate.block.MoldSporeSproutBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.ROSE_BUSH)
                .noOcclusion()
                .noCollision()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(key)
        )
    );

    public static final Block STELLARITH_CRYSTAL = registerBlock(
        "stellarith_crystal",
        key -> new com.theendupdate.block.StellarithCrystalBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.AMETHYST_BLOCK)
                .setId(key)
        )
    );

    public static final Block VOIDSTAR_BLOCK = registerBlock(
        "voidstar_block",
        key -> new com.theendupdate.block.VoidstarBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.IRON_BLOCK)
                .strength(50.0F, 6.0F) // netherite-hard, but keeps iron's blast resistance
                .setId(key)
        )
    );

    public static final Block ASTRAL_REMNANT = registerBlock(
        "astral_remnant",
        key -> new com.theendupdate.block.AstralRemnantBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.AMETHYST_BLOCK)
                .strength(1.9F, 2.0F) // ~25% harder than amethyst
                .setId(key)
        )
    );

    public static final Block SPECTRAL_BLOCK = registerBlock(
        "spectral_block",
        key -> new com.theendupdate.block.SpectralBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.GLOWSTONE)
                .lightLevel(state -> 15)
                .setId(key)
        )
    );

    public static final Block QUANTUM_GATEWAY = registerBlock(
        "quantum_gateway",
        key -> new com.theendupdate.block.QuantumGatewayBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.GLASS) // non-opaque so beacon beams still pass through
                .setId(key)
        )
    );

    public static final Block GRAVITITE_ORE = registerBlockFireproofItem(
        "gravitite_ore",
        key -> new com.theendupdate.block.GravititeOreBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops()
                .setId(key)
        )
    );

    public static final Block SHADOW_ALTAR = registerBlock(
        "shadow_altar",
        key -> new com.theendupdate.block.ShadowAltarBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.SPAWNER) // spawner properties needed for correct transparency rendering
                .requiresCorrectToolForDrops()
                .strength(50.0F, 1200.0F)
                .sound(SoundType.WOOD) // wooden theme despite being nearly indestructible
                .noOcclusion()
                .setId(key)
        )
    );

    // vanilla ShelfBlock gives us item display for free
    public static final Block ETHEREAL_SHELF = registerBlock(
        "ethereal_shelf",
        key -> new net.minecraft.world.level.block.ShelfBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    public static final Block SHADOW_SHELF = registerBlock(
        "shadow_shelf",
        key -> new net.minecraft.world.level.block.ShelfBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.WOOD).lightLevel(state -> 2).noOcclusion().setId(key)
        )
    );

    // crafted from 9 phantom membranes
    public static final Block MEMBRANE_BLOCK = registerBlock(
        "membrane_block",
        key -> new com.theendupdate.block.MembraneBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.RESIN_BLOCK)
                .sound(SoundType.RESIN_BRICKS)
                .strength(0.8F, 0.8F)
                .setId(key)
        )
    );

    public static final Block NEBULA_VENT_BLOCK = registerBlock(
        "nebula_vent_block",
        key -> new com.theendupdate.block.NebulaVentBlock(
            BlockBehaviour.Properties
                .ofFullCopy(Blocks.SPONGE)
                .strength(0.6F, 0.6F)
                .noOcclusion()
                .isRedstoneConductor((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false)
                .isViewBlocking((state, world, pos) -> false)
                .setId(key)
        )
    );

    private static Block registerBlock(String name, java.util.function.Function<ResourceKey<Block>, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        Block block = factory.apply(key); // needs the key up front since some Properties settings require an id at construction
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemSettings = new Item.Properties().setId(itemKey);
        BlockItem item;
        boolean isPlantLike = (block instanceof VegetationBlock) || (block instanceof DoublePlantBlock);
        // delicate plants get adjacent-placing so they don't replace existing flowers/plants
        item = isPlantLike ? new com.theendupdate.item.AdjacentPlantBlockItem(block, itemSettings)
                           : new BlockItem(block, itemSettings);
        Registry.register(BuiltInRegistries.ITEM, id, item);
        // creative tab population happens in registerModBlocks() so ordering can be controlled manually
        return block;
    }

    private static Block registerBlockNoItem(String name, java.util.function.Function<ResourceKey<Block>, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        Block block = factory.apply(key);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        return block;
    }

    private static Block registerBlockFireproofItem(String name, java.util.function.Function<ResourceKey<Block>, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        Block block = factory.apply(key);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemSettings = new Item.Properties().setId(itemKey).fireResistant();
        BlockItem item;
        boolean isPlantLike = (block instanceof VegetationBlock) || (block instanceof DoublePlantBlock);
        item = isPlantLike ? new com.theendupdate.item.AdjacentPlantBlockItem(block, itemSettings)
                           : new BlockItem(block, itemSettings);
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return block;
    }

    private static void registerSignItem(String name, Block standingSign, Block wallSign) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemSettings = new Item.Properties().setId(itemKey);
        SignItem item = new SignItem(standingSign, wallSign, itemSettings);
        Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    private static void registerHangingSignItem(String name, Block hangingSign, Block wallHangingSign) {
        Identifier id = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties itemSettings = new Item.Properties().setId(itemKey);
        HangingSignItem item = new HangingSignItem(hangingSign, wallHangingSign, itemSettings);
        Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void registerModBlocks() {
        // calling this just forces the class to load so the static field initializers above run
        
        // vanilla sign block classes, not custom ones (matches how BOP does its signs)
        ETHEREAL_SIGN = registerBlockNoItem(
            "ethereal_sign",
            key -> new StandingSignBlock(
                ETHEREAL_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        ETHEREAL_WALL_SIGN = registerBlockNoItem(
            "ethereal_wall_sign",
            key -> new WallSignBlock(
                ETHEREAL_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        ETHEREAL_HANGING_SIGN = registerBlockNoItem(
            "ethereal_hanging_sign",
            key -> new CeilingHangingSignBlock(
                ETHEREAL_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        ETHEREAL_WALL_HANGING_SIGN = registerBlockNoItem(
            "ethereal_wall_hanging_sign",
            key -> new WallHangingSignBlock(
                ETHEREAL_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        SHADOW_SIGN = registerBlockNoItem(
            "shadow_sign",
            key -> new StandingSignBlock(
                SHADOW_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        SHADOW_WALL_SIGN = registerBlockNoItem(
            "shadow_wall_sign",
            key -> new WallSignBlock(
                SHADOW_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        SHADOW_HANGING_SIGN = registerBlockNoItem(
            "shadow_hanging_sign",
            key -> new CeilingHangingSignBlock(
                SHADOW_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        SHADOW_WALL_HANGING_SIGN = registerBlockNoItem(
            "shadow_wall_hanging_sign",
            key -> new WallHangingSignBlock(
                SHADOW_WOOD_TYPE,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN).sound(SoundType.WOOD).lightLevel(state -> 2).setId(key)
            )
        );
        
        registerSignItem("ethereal_sign", ETHEREAL_SIGN, ETHEREAL_WALL_SIGN);
        registerSignItem("shadow_sign", SHADOW_SIGN, SHADOW_WALL_SIGN);
        registerHangingSignItem("ethereal_hanging_sign", ETHEREAL_HANGING_SIGN, ETHEREAL_WALL_HANGING_SIGN);
        registerHangingSignItem("shadow_hanging_sign", SHADOW_HANGING_SIGN, SHADOW_WALL_HANGING_SIGN);
        
        populateVanillaCreativeTabs();
    }
    
    private static void populateVanillaCreativeTabs() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
            output.accept(ETHEREAL_SPOROCARP);
            output.accept(ETHEREAL_PUSTULE);
            output.accept(ETHEREAL_PLANKS);
            output.accept(ETHEREAL_STAIRS);
            output.accept(ETHEREAL_SLAB);
            output.accept(ETHEREAL_DOOR);
            output.accept(ETHEREAL_TRAPDOOR);
            output.accept(ETHEREAL_FENCE);
            output.accept(ETHEREAL_FENCE_GATE);
            output.accept(ETHEREAL_BUTTON);
            output.accept(ETHEREAL_PRESSURE_PLATE);
            // shelves, signs, hanging signs go after pressure plate, before bulb
            output.accept(ETHEREAL_SHELF);
            output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_sign")));
            output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_hanging_sign")));
            output.accept(ETHEREAL_BULB);
            
            output.accept(SHADOW_CRYPTOMYCOTA);
            output.accept(SHADOW_UMBRACARP);
            output.accept(STRIPPED_SHADOW_CRYPTOMYCOTA);
            output.accept(STRIPPED_SHADOW_UMBRACARP);
            output.accept(SHADOW_PLANKS);
            output.accept(SHADOW_STAIRS);
            output.accept(SHADOW_SLAB);
            output.accept(SHADOW_DOOR);
            output.accept(SHADOW_TRAPDOOR);
            output.accept(SHADOW_FENCE);
            output.accept(SHADOW_FENCE_GATE);
            output.accept(SHADOW_BUTTON);
            output.accept(SHADOW_PRESSURE_PLATE);
            // shelves, signs, hanging signs go after pressure plate, before shadow claw
            output.accept(SHADOW_SHELF);
            output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_sign")));
            output.accept(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_hanging_sign")));
            
            output.accept(ASH_STONE);
            output.accept(ASH_STONE_STAIRS);
            output.accept(ASH_STONE_SLAB);
            output.accept(ASH_STONE_WALL);
            output.accept(SMOOTH_ASH_STONE);
            output.accept(SMOOTH_ASH_STONE_STAIRS);
            output.accept(SMOOTH_ASH_STONE_SLAB);
            output.accept(SMOOTH_ASH_STONE_WALL);
            output.accept(END_MIRE);
            output.accept(END_MURK);
            output.accept(MOLD_BLOCK);
            output.accept(MEMBRANE_BLOCK);
            output.accept(STELLARITH_CRYSTAL);
            output.accept(VOIDSTAR_BLOCK);
            output.accept(ASTRAL_REMNANT);
            output.accept(SPECTRAL_BLOCK);
            output.accept(QUANTUM_GATEWAY);
            output.accept(SHADOW_ALTAR);
            output.accept(NEBULA_VENT_BLOCK);
        });
        
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
            output.accept(VOID_BLOOM);
            output.accept(ENDER_CHRYSANTHEMUM);
            output.accept(VOID_SAP);
            output.accept(TENDRIL_SPROUT);
            output.accept(TENDRIL_THREAD);
            output.accept(TENDRIL_CORE);
            output.accept(SHADOW_CLAW);
            output.accept(MOLD_CRAWL);
            output.accept(MOLD_SPORE);
            output.accept(MOLD_SPORE_TUFT);
            output.accept(MOLD_SPORE_SPROUT);
            output.accept(MOLD_BLOCK); // also appears in building blocks tab
            output.accept(GRAVITITE_ORE);
        });
    }
}


