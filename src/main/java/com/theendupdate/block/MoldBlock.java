package com.theendupdate.block;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MoldBlock extends Block implements BonemealableBlock {
    public MoldBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        // reduced spread vs moss - moss attempts more, we intentionally do less in a smaller radius, and only place this block
        int attempts = 64;
        int radius = 2;

        for (int i = 0; i < attempts; i++) {
            BlockPos target = pos.offset(
                random.nextIntBetweenInclusive(-radius, radius),
                random.nextIntBetweenInclusive(-1, 1),
                random.nextIntBetweenInclusive(-radius, radius)
            );

            BlockState targetState = world.getBlockState(target);

            // Skip invalid targets: air, fluids, plants, crops, bedrock, storage, redstone, command blocks
            if (world.isEmptyBlock(target)) continue;
            if (!targetState.getFluidState().isEmpty()) continue;
            if (targetState.is(BlockTags.WOOL_CARPETS)
                || targetState.is(BlockTags.CANDLES)
                || targetState.is(BlockTags.DOORS)
                || targetState.is(BlockTags.TRAPDOORS)
                || targetState.is(BlockTags.STANDING_SIGNS)
                || targetState.is(BlockTags.WALL_SIGNS)
                || targetState.is(BlockTags.CEILING_HANGING_SIGNS)
                || targetState.is(BlockTags.WALL_HANGING_SIGNS)
                || targetState.is(BlockTags.BEDS)
                || targetState.is(BlockTags.RAILS)
                || targetState.is(BlockTags.CAULDRONS)
                || targetState.is(BlockTags.CAMPFIRES)
                || targetState.is(BlockTags.BANNERS)
                || targetState.is(BlockTags.FIRE)) {
                continue;
            }
            if (targetState.is(BlockTags.FLOWERS)) continue;
            if (targetState.is(BlockTags.CROPS)) continue;
            if (targetState.getBlock() instanceof SaplingBlock) continue;
            if (targetState.is(BlockTags.LEAVES)) continue;
            if (targetState.is(Blocks.BEDROCK)) continue;
            if (targetState.is(Blocks.REINFORCED_DEEPSLATE)) continue;
            
            // custom mod doors and trapdoors
            if (targetState.is(ModBlocks.ETHEREAL_DOOR)) continue;
            if (targetState.is(ModBlocks.ETHEREAL_TRAPDOOR)) continue;
            if (targetState.is(ModBlocks.SHADOW_DOOR)) continue;
            if (targetState.is(ModBlocks.SHADOW_TRAPDOOR)) continue;
            
            // Skip short grass and snow layers
            if (targetState.is(Blocks.SHORT_GRASS)) continue;
            if (targetState.is(Blocks.SNOW)) continue;
            
            // Skip ladders, scaffolding, and TNT
            if (targetState.is(BlockTags.CLIMBABLE)) continue;
            if (targetState.is(Blocks.SCAFFOLDING)) continue;
            if (targetState.is(Blocks.TNT)) continue;
            
            // Protect specific plants that aren't covered by tags
            if (targetState.is(Blocks.CACTUS)) continue;
            if (targetState.is(Blocks.SUGAR_CANE)) continue;
            if (targetState.is(Blocks.BAMBOO)) continue;
            if (targetState.is(Blocks.BAMBOO_SAPLING)) continue;
            if (targetState.is(Blocks.CHORUS_PLANT)) continue;
            if (targetState.is(Blocks.CHORUS_FLOWER)) continue;
            if (targetState.is(Blocks.SWEET_BERRY_BUSH)) continue;
            if (targetState.is(Blocks.COCOA)) continue;
			// Vines (all)
			if (targetState.is(Blocks.CAVE_VINES)) continue;
			if (targetState.is(Blocks.CAVE_VINES_PLANT)) continue;
            if (targetState.is(Blocks.KELP)) continue;
            if (targetState.is(Blocks.KELP_PLANT)) continue;
            if (targetState.is(Blocks.SEAGRASS)) continue;
            if (targetState.is(Blocks.TALL_SEAGRASS)) continue;
            if (targetState.is(Blocks.VINE)) continue;
            if (targetState.is(Blocks.TWISTING_VINES)) continue;
            if (targetState.is(Blocks.TWISTING_VINES_PLANT)) continue;
            if (targetState.is(Blocks.WEEPING_VINES)) continue;
            if (targetState.is(Blocks.WEEPING_VINES_PLANT)) continue;
            if (targetState.is(Blocks.LILY_PAD)) continue;
            if (targetState.is(Blocks.FERN)) continue;
            if (targetState.is(Blocks.TALL_GRASS)) continue;
            if (targetState.is(Blocks.LARGE_FERN)) continue;
            if (targetState.is(Blocks.DEAD_BUSH)) continue;
            if (targetState.is(Blocks.AZALEA)) continue;
            if (targetState.is(Blocks.FLOWERING_AZALEA)) continue;
            if (targetState.is(Blocks.SPORE_BLOSSOM)) continue;
            if (targetState.is(Blocks.HANGING_ROOTS)) continue;
            if (targetState.is(Blocks.MOSS_BLOCK)) continue;
            if (targetState.is(Blocks.MOSS_CARPET)) continue;
            if (targetState.is(Blocks.GLOW_LICHEN)) continue;
            if (targetState.is(Blocks.DRIED_KELP_BLOCK)) continue;
			// Dripleafs and pale moss
			if (targetState.is(Blocks.SMALL_DRIPLEAF)) continue;
			if (targetState.is(Blocks.BIG_DRIPLEAF)) continue;
			if (targetState.is(Blocks.PALE_HANGING_MOSS)) continue;
			if (targetState.is(Blocks.PALE_MOSS_CARPET)) continue;
			// Crimson/Warped flora
			if (targetState.is(Blocks.CRIMSON_FUNGUS)) continue;
			if (targetState.is(Blocks.WARPED_FUNGUS)) continue;
			if (targetState.is(Blocks.CRIMSON_ROOTS)) continue;
			if (targetState.is(Blocks.WARPED_ROOTS)) continue;
			if (targetState.is(Blocks.NETHER_SPROUTS)) continue;
			// Double-tall flowers and sniffer flowers
			if (targetState.is(Blocks.SUNFLOWER)) continue;
			if (targetState.is(Blocks.LILAC)) continue;
			if (targetState.is(Blocks.ROSE_BUSH)) continue;
			if (targetState.is(Blocks.PEONY)) continue;
			if (targetState.is(Blocks.TORCHFLOWER)) continue;
			if (targetState.is(Blocks.PITCHER_PLANT)) continue;
			// Sea pickles and coral fans
			if (targetState.is(Blocks.SEA_PICKLE)) continue;
			if (targetState.is(Blocks.TUBE_CORAL_FAN)) continue;
			if (targetState.is(Blocks.BRAIN_CORAL_FAN)) continue;
			if (targetState.is(Blocks.BUBBLE_CORAL_FAN)) continue;
			if (targetState.is(Blocks.FIRE_CORAL_FAN)) continue;
			if (targetState.is(Blocks.HORN_CORAL_FAN)) continue;
			if (targetState.is(Blocks.TUBE_CORAL_WALL_FAN)) continue;
			if (targetState.is(Blocks.BRAIN_CORAL_WALL_FAN)) continue;
			if (targetState.is(Blocks.BUBBLE_CORAL_WALL_FAN)) continue;
			if (targetState.is(Blocks.FIRE_CORAL_WALL_FAN)) continue;
			if (targetState.is(Blocks.HORN_CORAL_WALL_FAN)) continue;
            // Protect our custom blocks
            if (targetState.is(ModBlocks.QUANTUM_GATEWAY)) continue;
            if (targetState.is(ModBlocks.SHADOW_ALTAR)) continue;
            
            // Protect our custom plants
            if (targetState.is(ModBlocks.VOID_BLOOM)) continue;
            if (targetState.is(ModBlocks.ENDER_CHRYSANTHEMUM)) continue;
            if (targetState.is(ModBlocks.VOID_SAP)) continue;
            if (targetState.is(ModBlocks.TENDRIL_SPROUT)) continue;
            if (targetState.is(ModBlocks.TENDRIL_THREAD)) continue;
            if (targetState.is(ModBlocks.TENDRIL_CORE)) continue;
            if (targetState.is(ModBlocks.SHADOW_CLAW)) continue;
            if (targetState.is(ModBlocks.MOLD_CRAWL)) continue;
            if (targetState.is(ModBlocks.MOLD_SPORE)) continue;
            if (targetState.is(ModBlocks.MOLD_SPORE_TUFT)) continue;
            if (targetState.is(ModBlocks.MOLD_SPORE_SPROUT)) continue;
            if (targetState.is(ModBlocks.POTTED_VOID_BLOOM)) continue;
            if (targetState.is(ModBlocks.POTTED_ENDER_CHRYSANTHEMUM)) continue;
            if (targetState.is(ModBlocks.POTTED_TENDRIL_SPROUT)) continue;
            if (targetState.is(ModBlocks.POTTED_TENDRIL_THREAD)) continue;
            if (targetState.is(ModBlocks.POTTED_TENDRIL_CORE)) continue;
            if (targetState.is(ModBlocks.POTTED_SHADOW_CLAW)) continue;
            if (targetState.is(ModBlocks.POTTED_MOLD_SPORE)) continue;
            
            // Protect storage blocks
            if (targetState.is(Blocks.CHEST)) continue;
            if (targetState.is(Blocks.TRAPPED_CHEST)) continue;
            if (targetState.is(Blocks.ENDER_CHEST)) continue;
            if (targetState.is(Blocks.BARREL)) continue;
			if (targetState.is(Blocks.LECTERN)) continue;
			if (targetState.is(Blocks.CHISELED_BOOKSHELF)) continue;
			if (targetState.is(Blocks.BOOKSHELF)) continue;
			if (targetState.is(Blocks.DECORATED_POT)) continue;
			
			// Custom mod shelves
			if (targetState.is(ModBlocks.ETHEREAL_SHELF)) continue;
			if (targetState.is(ModBlocks.SHADOW_SHELF)) continue;
			if (targetState.is(Blocks.BEEHIVE)) continue;
			if (targetState.is(Blocks.BEE_NEST)) continue;
            
            // Protect all shulker box variants (including colored ones)
            if (targetState.is(BlockTags.SHULKER_BOXES)) continue;
            if (targetState.is(Blocks.DISPENSER)) continue;
            if (targetState.is(Blocks.DROPPER)) continue;
            if (targetState.is(Blocks.HOPPER)) continue;
            if (targetState.is(Blocks.FURNACE)) continue;
            if (targetState.is(Blocks.BLAST_FURNACE)) continue;
            if (targetState.is(Blocks.SMOKER)) continue;
            if (targetState.is(Blocks.BREWING_STAND)) continue;
            if (targetState.is(Blocks.ENCHANTING_TABLE)) continue;
            if (targetState.is(Blocks.ANVIL)) continue;
            if (targetState.is(Blocks.CHIPPED_ANVIL)) continue;
            if (targetState.is(Blocks.DAMAGED_ANVIL)) continue;
            
			// Protect redstone components and related
            if (targetState.is(BlockTags.LIGHTNING_RODS)) continue;
			// Copper bulbs and all weathered/oxidized/waxed stages
			if (Blocks.COPPER_BULB.asList().stream().anyMatch(targetState::is)) continue;
			if (targetState.is(Blocks.PISTON)) continue;
			if (targetState.is(Blocks.STICKY_PISTON)) continue;
			if (targetState.is(Blocks.OBSERVER)) continue;
			if (targetState.is(Blocks.DAYLIGHT_DETECTOR)) continue;
			if (targetState.is(Blocks.TARGET)) continue;
			if (targetState.is(Blocks.NOTE_BLOCK)) continue;
			if (targetState.is(Blocks.JUKEBOX)) continue;
			if (targetState.is(Blocks.LEVER)) continue;
			if (targetState.is(Blocks.STONE_BUTTON)) continue;
			if (targetState.is(Blocks.POLISHED_BLACKSTONE_BUTTON)) continue;
			if (targetState.is(Blocks.OAK_BUTTON)) continue;
			if (targetState.is(Blocks.SPRUCE_BUTTON)) continue;
			if (targetState.is(Blocks.BIRCH_BUTTON)) continue;
			if (targetState.is(Blocks.JUNGLE_BUTTON)) continue;
			if (targetState.is(Blocks.ACACIA_BUTTON)) continue;
			if (targetState.is(Blocks.DARK_OAK_BUTTON)) continue;
			if (targetState.is(Blocks.MANGROVE_BUTTON)) continue;
			if (targetState.is(Blocks.CHERRY_BUTTON)) continue;
			if (targetState.is(Blocks.BAMBOO_BUTTON)) continue;
			if (targetState.is(Blocks.CRIMSON_BUTTON)) continue;
			if (targetState.is(Blocks.WARPED_BUTTON)) continue;
			if (targetState.is(Blocks.STONE_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.OAK_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.SPRUCE_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.BIRCH_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.JUNGLE_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.ACACIA_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.DARK_OAK_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.MANGROVE_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.CHERRY_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.BAMBOO_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.CRIMSON_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.WARPED_PRESSURE_PLATE)) continue;
			if (targetState.is(Blocks.TRIPWIRE_HOOK)) continue;
			if (targetState.is(Blocks.TRIPWIRE)) continue;
			// Rails are handled via tag above; keep explicit for safety
			if (targetState.is(Blocks.RAIL)) continue;
			if (targetState.is(Blocks.POWERED_RAIL)) continue;
			if (targetState.is(Blocks.DETECTOR_RAIL)) continue;
			if (targetState.is(Blocks.ACTIVATOR_RAIL)) continue;
			// Lighting and decor commonly preserved
            if (targetState.is(Blocks.BELL)) continue;
			// Skulls and heads (all variants)
			if (targetState.is(Blocks.SKELETON_SKULL)) continue;
			if (targetState.is(Blocks.WITHER_SKELETON_SKULL)) continue;
			if (targetState.is(Blocks.ZOMBIE_HEAD)) continue;
			if (targetState.is(Blocks.CREEPER_HEAD)) continue;
			if (targetState.is(Blocks.DRAGON_HEAD)) continue;
			if (targetState.is(Blocks.PIGLIN_HEAD)) continue;
			if (targetState.is(Blocks.PLAYER_HEAD)) continue;
			if (targetState.is(Blocks.SKELETON_WALL_SKULL)) continue;
			if (targetState.is(Blocks.WITHER_SKELETON_WALL_SKULL)) continue;
			if (targetState.is(Blocks.ZOMBIE_WALL_HEAD)) continue;
			if (targetState.is(Blocks.CREEPER_WALL_HEAD)) continue;
			if (targetState.is(Blocks.DRAGON_WALL_HEAD)) continue;
			if (targetState.is(Blocks.PIGLIN_WALL_HEAD)) continue;
			if (targetState.is(Blocks.PLAYER_WALL_HEAD)) continue;
			// Slime/honey used for contraptions
			if (targetState.is(Blocks.SLIME_BLOCK)) continue;
			if (targetState.is(Blocks.HONEY_BLOCK)) continue;
			
			// Creaking heart (Pale Garden)
			if (targetState.is(Blocks.CREAKING_HEART)) continue;
			
			// Check by registry ID for blocks not yet in Blocks class (1.21.9+ blocks)
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());
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
            if (targetState.is(Blocks.COMMAND_BLOCK)) continue;
            if (targetState.is(Blocks.CHAIN_COMMAND_BLOCK)) continue;
            if (targetState.is(Blocks.REPEATING_COMMAND_BLOCK)) continue;
            if (targetState.is(Blocks.STRUCTURE_BLOCK)) continue;
            if (targetState.is(Blocks.JIGSAW)) continue;
            if (targetState.is(Blocks.STRUCTURE_VOID)) continue;
            
            // Protect other important blocks
            if (targetState.is(Blocks.SPAWNER)) continue;
            if (targetState.is(Blocks.BEACON)) continue;
            if (targetState.is(Blocks.END_PORTAL_FRAME)) continue;
            if (targetState.is(Blocks.END_PORTAL)) continue;
            if (targetState.is(Blocks.NETHER_PORTAL)) continue;
            if (targetState.is(Blocks.END_GATEWAY)) continue;
			if (targetState.is(Blocks.LODESTONE)) continue;
			if (targetState.is(Blocks.RESPAWN_ANCHOR)) continue;
			if (targetState.is(Blocks.HEAVY_CORE)) continue;
			if (targetState.is(Blocks.TRIAL_SPAWNER)) continue;
			if (targetState.is(Blocks.VAULT)) continue;
			if (targetState.is(Blocks.CONDUIT)) continue;
			if (targetState.is(Blocks.POINTED_DRIPSTONE)) continue;
			if (targetState.is(Blocks.CRAFTER)) continue;
			// Fire should never be overwritten
			if (targetState.is(Blocks.FIRE)) continue;
			if (targetState.is(Blocks.SOUL_FIRE)) continue;
			// Sponges
			if (targetState.is(Blocks.SPONGE)) continue;
			if (targetState.is(Blocks.WET_SPONGE)) continue;
			// Sculk family and sensors
			if (targetState.is(Blocks.SCULK)) continue;
			if (targetState.is(Blocks.SCULK_VEIN)) continue;
			if (targetState.is(Blocks.SCULK_SENSOR)) continue;
			if (targetState.is(Blocks.CALIBRATED_SCULK_SENSOR)) continue;
			if (targetState.is(Blocks.SCULK_SHRIEKER)) continue;
			if (targetState.is(Blocks.SCULK_CATALYST)) continue;
			// Fragile/rare
			if (targetState.is(Blocks.BUDDING_AMETHYST)) continue;
			if (targetState.is(Blocks.AMETHYST_CLUSTER)) continue;
			if (targetState.is(Blocks.LARGE_AMETHYST_BUD)) continue;
			if (targetState.is(Blocks.MEDIUM_AMETHYST_BUD)) continue;
			if (targetState.is(Blocks.SMALL_AMETHYST_BUD)) continue;
			if (targetState.is(Blocks.TURTLE_EGG)) continue;
			if (targetState.is(Blocks.SNIFFER_EGG)) continue;
            
            // ideally we'd check rarity via the item form, for now just an explicit list of rare blocks
            if (targetState.is(Blocks.DRAGON_EGG)) continue;
            if (targetState.is(Blocks.ANCIENT_DEBRIS)) continue;
            if (targetState.is(Blocks.NETHERITE_BLOCK)) continue;

            world.setBlock(target, state, Block.UPDATE_ALL);
        }
    }
}


