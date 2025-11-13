package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.block.Fertilizable;

public class MoldBlock extends Block implements Fertilizable {
    public MoldBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        // Always allow bonemeal usage
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        // Reduced spread compared to moss: do ~half the attempts in a small radius
        // Only place this block, no carpets/azalea/etc
        int attempts = 64; // moss typically attempts more; we intentionally reduce
        int radius = 2;    // small radius

        for (int i = 0; i < attempts; i++) {
            BlockPos target = pos.add(
                random.nextBetween(-radius, radius),
                random.nextBetween(-1, 1),
                random.nextBetween(-radius, radius)
            );

            BlockState targetState = world.getBlockState(target);

            // Skip invalid targets: air, fluids, plants, crops, bedrock, storage, redstone, command blocks
            if (world.isAir(target)) continue;
            if (!targetState.getFluidState().isEmpty()) continue;
            if (targetState.isIn(BlockTags.WOOL_CARPETS)
                || targetState.isIn(BlockTags.CANDLES)
                || targetState.isIn(BlockTags.DOORS)
                || targetState.isIn(BlockTags.TRAPDOORS)
                || targetState.isIn(BlockTags.STANDING_SIGNS)
                || targetState.isIn(BlockTags.WALL_SIGNS)
                || targetState.isIn(BlockTags.CEILING_HANGING_SIGNS)
                || targetState.isIn(BlockTags.WALL_HANGING_SIGNS)
                || targetState.isIn(BlockTags.BEDS)
                || targetState.isIn(BlockTags.RAILS)
                || targetState.isIn(BlockTags.CAULDRONS)
                || targetState.isIn(BlockTags.CAMPFIRES)
                || targetState.isIn(BlockTags.BANNERS)
                || targetState.isIn(BlockTags.FIRE)) {
                continue;
            }
            if (targetState.isIn(BlockTags.FLOWERS)) continue;
            if (targetState.isIn(BlockTags.CROPS)) continue;
            if (targetState.isIn(BlockTags.SAPLINGS)) continue;
            if (targetState.isIn(BlockTags.LEAVES)) continue;
            if (targetState.isOf(Blocks.BEDROCK)) continue;
            if (targetState.isOf(Blocks.REINFORCED_DEEPSLATE)) continue;
            
            // Skip doors, signs, hanging signs, and trapdoors
            // Custom mod doors and trapdoors
            if (targetState.isOf(ModBlocks.ETHEREAL_DOOR)) continue;
            if (targetState.isOf(ModBlocks.ETHEREAL_TRAPDOOR)) continue;
            if (targetState.isOf(ModBlocks.SHADOW_DOOR)) continue;
            if (targetState.isOf(ModBlocks.SHADOW_TRAPDOOR)) continue;
            
            // Skip short grass and snow layers
            if (targetState.isOf(Blocks.SHORT_GRASS)) continue;
            if (targetState.isOf(Blocks.SNOW)) continue;
            
            // Skip ladders, scaffolding, and TNT
            if (targetState.isIn(BlockTags.CLIMBABLE)) continue;
            if (targetState.isOf(Blocks.SCAFFOLDING)) continue;
            if (targetState.isOf(Blocks.TNT)) continue;
            
            // Protect specific plants that aren't covered by tags
            if (targetState.isOf(Blocks.CACTUS)) continue;
            if (targetState.isOf(Blocks.SUGAR_CANE)) continue;
            if (targetState.isOf(Blocks.BAMBOO)) continue;
            if (targetState.isOf(Blocks.BAMBOO_SAPLING)) continue;
            if (targetState.isOf(Blocks.CHORUS_PLANT)) continue;
            if (targetState.isOf(Blocks.CHORUS_FLOWER)) continue;
            if (targetState.isOf(Blocks.SWEET_BERRY_BUSH)) continue;
            if (targetState.isOf(Blocks.COCOA)) continue;
			// Vines (all)
			if (targetState.isOf(Blocks.CAVE_VINES)) continue;
			if (targetState.isOf(Blocks.CAVE_VINES_PLANT)) continue;
            if (targetState.isOf(Blocks.KELP)) continue;
            if (targetState.isOf(Blocks.KELP_PLANT)) continue;
            if (targetState.isOf(Blocks.SEAGRASS)) continue;
            if (targetState.isOf(Blocks.TALL_SEAGRASS)) continue;
            if (targetState.isOf(Blocks.VINE)) continue;
            if (targetState.isOf(Blocks.TWISTING_VINES)) continue;
            if (targetState.isOf(Blocks.TWISTING_VINES_PLANT)) continue;
            if (targetState.isOf(Blocks.WEEPING_VINES)) continue;
            if (targetState.isOf(Blocks.WEEPING_VINES_PLANT)) continue;
            if (targetState.isOf(Blocks.LILY_PAD)) continue;
            if (targetState.isOf(Blocks.FERN)) continue;
            if (targetState.isOf(Blocks.TALL_GRASS)) continue;
            if (targetState.isOf(Blocks.LARGE_FERN)) continue;
            if (targetState.isOf(Blocks.DEAD_BUSH)) continue;
            if (targetState.isOf(Blocks.AZALEA)) continue;
            if (targetState.isOf(Blocks.FLOWERING_AZALEA)) continue;
            if (targetState.isOf(Blocks.SPORE_BLOSSOM)) continue;
            if (targetState.isOf(Blocks.HANGING_ROOTS)) continue;
            if (targetState.isOf(Blocks.MOSS_BLOCK)) continue;
            if (targetState.isOf(Blocks.MOSS_CARPET)) continue;
            if (targetState.isOf(Blocks.GLOW_LICHEN)) continue;
            if (targetState.isOf(Blocks.DRIED_KELP_BLOCK)) continue;
			// Dripleafs and pale moss
			if (targetState.isOf(Blocks.SMALL_DRIPLEAF)) continue;
			if (targetState.isOf(Blocks.BIG_DRIPLEAF)) continue;
			if (targetState.isOf(Blocks.PALE_HANGING_MOSS)) continue;
			if (targetState.isOf(Blocks.PALE_MOSS_CARPET)) continue;
			// Crimson/Warped flora
			if (targetState.isOf(Blocks.CRIMSON_FUNGUS)) continue;
			if (targetState.isOf(Blocks.WARPED_FUNGUS)) continue;
			if (targetState.isOf(Blocks.CRIMSON_ROOTS)) continue;
			if (targetState.isOf(Blocks.WARPED_ROOTS)) continue;
			if (targetState.isOf(Blocks.NETHER_SPROUTS)) continue;
			// Double-tall flowers and sniffer flowers
			if (targetState.isOf(Blocks.SUNFLOWER)) continue;
			if (targetState.isOf(Blocks.LILAC)) continue;
			if (targetState.isOf(Blocks.ROSE_BUSH)) continue;
			if (targetState.isOf(Blocks.PEONY)) continue;
			if (targetState.isOf(Blocks.TORCHFLOWER)) continue;
			if (targetState.isOf(Blocks.PITCHER_PLANT)) continue;
			// Sea pickles and coral fans
			if (targetState.isOf(Blocks.SEA_PICKLE)) continue;
			if (targetState.isOf(Blocks.TUBE_CORAL_FAN)) continue;
			if (targetState.isOf(Blocks.BRAIN_CORAL_FAN)) continue;
			if (targetState.isOf(Blocks.BUBBLE_CORAL_FAN)) continue;
			if (targetState.isOf(Blocks.FIRE_CORAL_FAN)) continue;
			if (targetState.isOf(Blocks.HORN_CORAL_FAN)) continue;
			if (targetState.isOf(Blocks.TUBE_CORAL_WALL_FAN)) continue;
			if (targetState.isOf(Blocks.BRAIN_CORAL_WALL_FAN)) continue;
			if (targetState.isOf(Blocks.BUBBLE_CORAL_WALL_FAN)) continue;
			if (targetState.isOf(Blocks.FIRE_CORAL_WALL_FAN)) continue;
			if (targetState.isOf(Blocks.HORN_CORAL_WALL_FAN)) continue;
			// Carpets (explicit list to avoid tag mapping issues)
            // Protect our custom blocks
            if (targetState.isOf(ModBlocks.QUANTUM_GATEWAY)) continue;
            if (targetState.isOf(ModBlocks.SHADOW_ALTAR)) continue;
            
            // Protect our custom plants
            if (targetState.isOf(ModBlocks.VOID_BLOOM)) continue;
            if (targetState.isOf(ModBlocks.ENDER_CHRYSANTHEMUM)) continue;
            if (targetState.isOf(ModBlocks.VOID_SAP)) continue;
            if (targetState.isOf(ModBlocks.TENDRIL_SPROUT)) continue;
            if (targetState.isOf(ModBlocks.TENDRIL_THREAD)) continue;
            if (targetState.isOf(ModBlocks.TENDRIL_CORE)) continue;
            if (targetState.isOf(ModBlocks.SHADOW_CLAW)) continue;
            if (targetState.isOf(ModBlocks.MOLD_CRAWL)) continue;
            if (targetState.isOf(ModBlocks.MOLD_SPORE)) continue;
            if (targetState.isOf(ModBlocks.MOLD_SPORE_TUFT)) continue;
            if (targetState.isOf(ModBlocks.MOLD_SPORE_SPROUT)) continue;
            if (targetState.isOf(ModBlocks.POTTED_VOID_BLOOM)) continue;
            if (targetState.isOf(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) continue;
            if (targetState.isOf(ModBlocks.POTTED_TENDRIL_SPROUT)) continue;
            if (targetState.isOf(ModBlocks.POTTED_TENDRIL_THREAD)) continue;
            if (targetState.isOf(ModBlocks.POTTED_TENDRIL_CORE)) continue;
            if (targetState.isOf(ModBlocks.POTTED_SHADOW_CLAW)) continue;
            if (targetState.isOf(ModBlocks.POTTED_MOLD_SPORE)) continue;
            
            // Protect storage blocks
            if (targetState.isOf(Blocks.CHEST)) continue;
            if (targetState.isOf(Blocks.TRAPPED_CHEST)) continue;
            if (targetState.isOf(Blocks.ENDER_CHEST)) continue;
            if (targetState.isOf(Blocks.BARREL)) continue;
			if (targetState.isOf(Blocks.LECTERN)) continue;
			if (targetState.isOf(Blocks.CHISELED_BOOKSHELF)) continue;
			if (targetState.isOf(Blocks.BOOKSHELF)) continue;
			if (targetState.isOf(Blocks.DECORATED_POT)) continue;
			
			// Custom mod shelves
			if (targetState.isOf(ModBlocks.ETHEREAL_SHELF)) continue;
			if (targetState.isOf(ModBlocks.SHADOW_SHELF)) continue;
			if (targetState.isOf(Blocks.BEEHIVE)) continue;
			if (targetState.isOf(Blocks.BEE_NEST)) continue;
            
            // Protect all shulker box variants (including colored ones)
            if (targetState.isOf(Blocks.SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.WHITE_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.ORANGE_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.MAGENTA_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.LIGHT_BLUE_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.YELLOW_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.LIME_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.PINK_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.GRAY_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.LIGHT_GRAY_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.CYAN_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.PURPLE_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.BLUE_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.BROWN_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.GREEN_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.RED_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.BLACK_SHULKER_BOX)) continue;
            if (targetState.isOf(Blocks.DISPENSER)) continue;
            if (targetState.isOf(Blocks.DROPPER)) continue;
            if (targetState.isOf(Blocks.HOPPER)) continue;
            if (targetState.isOf(Blocks.FURNACE)) continue;
            if (targetState.isOf(Blocks.BLAST_FURNACE)) continue;
            if (targetState.isOf(Blocks.SMOKER)) continue;
            if (targetState.isOf(Blocks.BREWING_STAND)) continue;
            if (targetState.isOf(Blocks.ENCHANTING_TABLE)) continue;
            if (targetState.isOf(Blocks.ANVIL)) continue;
            if (targetState.isOf(Blocks.CHIPPED_ANVIL)) continue;
            if (targetState.isOf(Blocks.DAMAGED_ANVIL)) continue;
            
			// Protect redstone components and related
            if (targetState.isOf(Blocks.LIGHTNING_ROD)) continue;
			// Copper bulbs and all weathered/oxidized/waxed stages
			if (targetState.isOf(Blocks.COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.EXPOSED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.WEATHERED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.OXIDIZED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.WAXED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.WAXED_EXPOSED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.WAXED_WEATHERED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.WAXED_OXIDIZED_COPPER_BULB)) continue;
			if (targetState.isOf(Blocks.PISTON)) continue;
			if (targetState.isOf(Blocks.STICKY_PISTON)) continue;
			if (targetState.isOf(Blocks.OBSERVER)) continue;
			if (targetState.isOf(Blocks.DAYLIGHT_DETECTOR)) continue;
			if (targetState.isOf(Blocks.TARGET)) continue;
			if (targetState.isOf(Blocks.NOTE_BLOCK)) continue;
			if (targetState.isOf(Blocks.JUKEBOX)) continue;
			if (targetState.isOf(Blocks.LEVER)) continue;
			if (targetState.isOf(Blocks.STONE_BUTTON)) continue;
			if (targetState.isOf(Blocks.POLISHED_BLACKSTONE_BUTTON)) continue;
			if (targetState.isOf(Blocks.OAK_BUTTON)) continue;
			if (targetState.isOf(Blocks.SPRUCE_BUTTON)) continue;
			if (targetState.isOf(Blocks.BIRCH_BUTTON)) continue;
			if (targetState.isOf(Blocks.JUNGLE_BUTTON)) continue;
			if (targetState.isOf(Blocks.ACACIA_BUTTON)) continue;
			if (targetState.isOf(Blocks.DARK_OAK_BUTTON)) continue;
			if (targetState.isOf(Blocks.MANGROVE_BUTTON)) continue;
			if (targetState.isOf(Blocks.CHERRY_BUTTON)) continue;
			if (targetState.isOf(Blocks.BAMBOO_BUTTON)) continue;
			if (targetState.isOf(Blocks.CRIMSON_BUTTON)) continue;
			if (targetState.isOf(Blocks.WARPED_BUTTON)) continue;
			if (targetState.isOf(Blocks.STONE_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.OAK_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.SPRUCE_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.BIRCH_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.JUNGLE_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.ACACIA_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.DARK_OAK_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.MANGROVE_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.CHERRY_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.BAMBOO_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.CRIMSON_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.WARPED_PRESSURE_PLATE)) continue;
			if (targetState.isOf(Blocks.TRIPWIRE_HOOK)) continue;
			if (targetState.isOf(Blocks.TRIPWIRE)) continue;
			// Rails are handled via tag above; keep explicit for safety
			if (targetState.isOf(Blocks.RAIL)) continue;
			if (targetState.isOf(Blocks.POWERED_RAIL)) continue;
			if (targetState.isOf(Blocks.DETECTOR_RAIL)) continue;
			if (targetState.isOf(Blocks.ACTIVATOR_RAIL)) continue;
			// Lighting and decor commonly preserved
            if (targetState.isOf(Blocks.BELL)) continue;
			// Skulls and heads (all variants)
			if (targetState.isOf(Blocks.SKELETON_SKULL)) continue;
			if (targetState.isOf(Blocks.WITHER_SKELETON_SKULL)) continue;
			if (targetState.isOf(Blocks.ZOMBIE_HEAD)) continue;
			if (targetState.isOf(Blocks.CREEPER_HEAD)) continue;
			if (targetState.isOf(Blocks.DRAGON_HEAD)) continue;
			if (targetState.isOf(Blocks.PIGLIN_HEAD)) continue;
			if (targetState.isOf(Blocks.PLAYER_HEAD)) continue;
			if (targetState.isOf(Blocks.SKELETON_WALL_SKULL)) continue;
			if (targetState.isOf(Blocks.WITHER_SKELETON_WALL_SKULL)) continue;
			if (targetState.isOf(Blocks.ZOMBIE_WALL_HEAD)) continue;
			if (targetState.isOf(Blocks.CREEPER_WALL_HEAD)) continue;
			if (targetState.isOf(Blocks.DRAGON_WALL_HEAD)) continue;
			if (targetState.isOf(Blocks.PIGLIN_WALL_HEAD)) continue;
			if (targetState.isOf(Blocks.PLAYER_WALL_HEAD)) continue;
			// Slime/honey used for contraptions
			if (targetState.isOf(Blocks.SLIME_BLOCK)) continue;
			if (targetState.isOf(Blocks.HONEY_BLOCK)) continue;
			
			// Cauldrons (all variants)
			// Creaking heart (Pale Garden)
			if (targetState.isOf(Blocks.CREAKING_HEART)) continue;
			
			// Check by registry ID for blocks not yet in Blocks class (1.21.9+ blocks)
			Identifier blockId = Registries.BLOCK.getId(targetState.getBlock());
			String blockIdString = blockId.toString();
			
			// Dried ghast decorative block
			if (blockIdString.equals("minecraft:dried_ghast")) continue;
			
			// Iron chains and copper chains (all oxidation stages)
			if (blockIdString.contains("chain") && !blockIdString.contains("command")) continue;
			
			// Copper chests (all oxidation stages)
			if (blockIdString.contains("copper_chest")) continue;
			
			// Copper golem statues (all oxidation stages)
			if (blockIdString.contains("copper_golem_statue")) continue;
            
            // Protect command exclusive blocks
            if (targetState.isOf(Blocks.COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.CHAIN_COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.REPEATING_COMMAND_BLOCK)) continue;
            if (targetState.isOf(Blocks.STRUCTURE_BLOCK)) continue;
            if (targetState.isOf(Blocks.JIGSAW)) continue;
            if (targetState.isOf(Blocks.STRUCTURE_VOID)) continue;
            
            // Protect other important blocks
            if (targetState.isOf(Blocks.SPAWNER)) continue;
            if (targetState.isOf(Blocks.BEACON)) continue;
            if (targetState.isOf(Blocks.END_PORTAL_FRAME)) continue;
            if (targetState.isOf(Blocks.END_PORTAL)) continue;
            if (targetState.isOf(Blocks.NETHER_PORTAL)) continue;
            if (targetState.isOf(Blocks.END_GATEWAY)) continue;
			if (targetState.isOf(Blocks.LODESTONE)) continue;
			if (targetState.isOf(Blocks.RESPAWN_ANCHOR)) continue;
			if (targetState.isOf(Blocks.HEAVY_CORE)) continue;
			if (targetState.isOf(Blocks.TRIAL_SPAWNER)) continue;
			if (targetState.isOf(Blocks.VAULT)) continue;
			if (targetState.isOf(Blocks.CONDUIT)) continue;
			if (targetState.isOf(Blocks.POINTED_DRIPSTONE)) continue;
			if (targetState.isOf(Blocks.CRAFTER)) continue;
			// Fire should never be overwritten
			if (targetState.isOf(Blocks.FIRE)) continue;
			if (targetState.isOf(Blocks.SOUL_FIRE)) continue;
			// Sponges
			if (targetState.isOf(Blocks.SPONGE)) continue;
			if (targetState.isOf(Blocks.WET_SPONGE)) continue;
			// Sculk family and sensors
			if (targetState.isOf(Blocks.SCULK)) continue;
			if (targetState.isOf(Blocks.SCULK_VEIN)) continue;
			if (targetState.isOf(Blocks.SCULK_SENSOR)) continue;
			if (targetState.isOf(Blocks.CALIBRATED_SCULK_SENSOR)) continue;
			if (targetState.isOf(Blocks.SCULK_SHRIEKER)) continue;
			if (targetState.isOf(Blocks.SCULK_CATALYST)) continue;
			// Fragile/rare
			if (targetState.isOf(Blocks.BUDDING_AMETHYST)) continue;
			if (targetState.isOf(Blocks.AMETHYST_CLUSTER)) continue;
			if (targetState.isOf(Blocks.LARGE_AMETHYST_BUD)) continue;
			if (targetState.isOf(Blocks.MEDIUM_AMETHYST_BUD)) continue;
			if (targetState.isOf(Blocks.SMALL_AMETHYST_BUD)) continue;
			if (targetState.isOf(Blocks.TURTLE_EGG)) continue;
			if (targetState.isOf(Blocks.SNIFFER_EGG)) continue;
            
            // Check if block has colored name (rarity indicator)
            // This would require checking the block's item form, but for now we'll protect specific rare blocks
            if (targetState.isOf(Blocks.DRAGON_EGG)) continue;
            if (targetState.isOf(Blocks.ANCIENT_DEBRIS)) continue;
            if (targetState.isOf(Blocks.NETHERITE_BLOCK)) continue;

            world.setBlockState(target, state, Block.NOTIFY_ALL);
        }
    }
}


