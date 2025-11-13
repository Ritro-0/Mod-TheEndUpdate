package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.block.NebulaVentBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Carves crater-shaped bowls into End island surfaces, lines them with tuff, and nests a nebula vent at the center.
 *
 * - Radius varies 5–15 blocks depending on available terrain.
 * - Walls and floors are coated with tuff to highlight the crater silhouette.
 * - Always places exactly one nebula vent block at the crater floor center.
 */
public class NebulaCraterFeature extends Feature<DefaultFeatureConfig> {
	private static final int MIN_RADIUS = 5;
	private static final int MAX_RADIUS = 15;
	private static final int CHUNK_GRID = 6; // 6 chunks ≈ 96 blocks spacing
	private static final float CELL_CRATER_CHANCE = 0.55f; // at most one crater per cell, ~55% spawn rate
	private static final int MAIN_ISLAND_EXCLUSION_RADIUS = 400;
	private static final int MAX_SAMPLE_HEIGHT_VARIATION = 18;

	public NebulaCraterFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();

		ChunkPos chunkPos = new ChunkPos(origin);
		int chunkMinX = chunkPos.getStartX();
		int chunkMinZ = chunkPos.getStartZ();
		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;

		if (!isCellAnchor(chunkX, chunkZ)) {
			return false;
		}

		if (!shouldSpawnInCell(chunkX, chunkZ)) {
			return false;
		}

		int centerX = chunkMinX + random.nextInt(16);
		int centerZ = chunkMinZ + random.nextInt(16);

		if (isNearMainIsland(centerX, centerZ)) {
			return false;
		}

		BlockPos surface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(centerX, 0, centerZ)).down();
		if (surface.getY() <= world.getBottomY() + 10) {
			return false;
		}

		BlockState surfaceState = world.getBlockState(surface);
		if (!isEndTerrain(surfaceState)) {
			return false;
		}
		if (!world.isAir(surface.up())) {
			return false;
		}

		int radius = random.nextBetween(MIN_RADIUS, MAX_RADIUS);
		if (!hasConsistentSurface(world, surface, radius)) {
			return false;
		}

		if (hasOverheadObstruction(world, surface, radius)) {
			return false;
		}

		return carveCrater(world, surface, radius, random);
	}

	private static boolean carveCrater(StructureWorldAccess world, BlockPos centerSurface, int radius, Random random) {
		int centerX = centerSurface.getX();
		int centerY = centerSurface.getY();
		int centerZ = centerSurface.getZ();

		int bottomY = world.getBottomY() + 1;
		int maxDepth = Math.max(4, (int) Math.round(radius * 0.65));

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		boolean modifiedAny = false;
		int lowestFloor = Integer.MAX_VALUE;
		Integer centerFloorY = null;

		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double dist = Math.sqrt((double) (dx * dx + dz * dz));
				if (dist > radius + 1.2) {
					continue;
				}

				int columnX = centerX + dx;
				int columnZ = centerZ + dz;
				BlockPos sampleColumn = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(columnX, 0, columnZ)).down();
				if (sampleColumn.getY() <= bottomY + 4) {
					continue;
				}

				BlockState topState = world.getBlockState(sampleColumn);
				boolean coreArea = dist <= radius * 0.75;
				if (!isEndTerrain(topState) && coreArea) {
					return false; // abort if interior lacks end terrain to carve
				}

				double normalized = Math.min(1.0, dist / radius);
				double depthFactor = 1.0 - normalized * normalized;
				if (depthFactor <= 0.0) {
					continue;
				}

				int depthHere = Math.max(2, (int) Math.round(maxDepth * depthFactor));
				int floorY = Math.max(bottomY + 2, centerY - depthHere);
				if (sampleColumn.getY() - floorY < 2) {
					continue;
				}

				// Clear space above the floor
				for (int y = sampleColumn.getY(); y > floorY; y--) {
					mutable.set(columnX, y, columnZ);
					BlockState state = world.getBlockState(mutable);
					if (!isCarvable(state)) {
						break;
					}
					if (!state.isAir()) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
						modifiedAny = true;
					}
				}

				// Place floor ash stone layer
				mutable.set(columnX, floorY, columnZ);
				BlockState floorState = world.getBlockState(mutable);
				if (isCarvable(floorState) || floorState.isAir()) {
					world.setBlockState(mutable, ModBlocks.ASH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
					modifiedAny = true;
				}

				// Thicken the bowl slightly
				if (depthHere >= 4) {
					for (int extra = 1; extra <= 2; extra++) {
						int depthCutoff = depthHere - extra * 2;
						if (depthCutoff <= 0) break;
						int fillY = floorY - extra;
						if (fillY <= bottomY) break;
						mutable.set(columnX, fillY, columnZ);
						BlockState fillState = world.getBlockState(mutable);
						if (isCarvable(fillState)) {
							world.setBlockState(mutable, ModBlocks.ASH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
						}
					}
				}

				if (dx == 0 && dz == 0) {
					centerFloorY = floorY;
				}

				if (floorY < lowestFloor) {
					lowestFloor = floorY;
				}
			}
		}

		if (!modifiedAny || centerFloorY == null) {
			return false;
		}

		// Paint crater walls with ash stone to get a cohesive look
		BlockPos.Mutable wallCursor = new BlockPos.Mutable();
		int wallMinY = Math.max(lowestFloor - 2, bottomY + 1);
		int wallMaxY = centerY + 3;
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double dist = Math.sqrt((double) (dx * dx + dz * dz));
				if (dist > radius + 1.4) continue;
				int x = centerX + dx;
				int z = centerZ + dz;
				for (int y = wallMaxY; y >= wallMinY; y--) {
					wallCursor.set(x, y, z);
					BlockState current = world.getBlockState(wallCursor);
					if (!isEndTerrain(current)) continue;
					if (!adjacentToAir(world, wallCursor)) continue;
					world.setBlockState(wallCursor, ModBlocks.ASH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}

		// Place nebula vent on the crater floor
		BlockPos ventPos = new BlockPos(centerX, centerFloorY + 1, centerZ);
		BlockState belowVent = world.getBlockState(ventPos.down());
		if (!belowVent.isOf(ModBlocks.ASH_STONE)) {
			world.setBlockState(ventPos.down(), ModBlocks.ASH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		BlockState ventState = ModBlocks.NEBULA_VENT_BLOCK.getDefaultState()
			.with(NebulaVentBlock.FACING, Direction.Type.HORIZONTAL.random(random));
		world.setBlockState(ventPos, ventState, Block.NOTIFY_LISTENERS);

		// Ensure immediate rim has a thicker ash stone band for readability
		BlockPos.Mutable rimCursor = new BlockPos.Mutable();
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double dist = Math.sqrt((double) (dx * dx + dz * dz));
				if (dist < radius - 1.5 || dist > radius + 1.8) continue;
				int rimX = centerX + dx;
				int rimZ = centerZ + dz;
				int rimY = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(rimX, 0, rimZ)).getY();
				for (int y = rimY; y >= rimY - 2 && y > bottomY; y--) {
					rimCursor.set(rimX, y, rimZ);
					BlockState rimState = world.getBlockState(rimCursor);
					if (!isEndTerrain(rimState)) continue;
					world.setBlockState(rimCursor, ModBlocks.ASH_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}

		removeUnsupportedShadowClaws(world, centerSurface, radius + 2);
		removeFloatingMoldSpores(world, centerSurface, radius + 2);
		return true;
	}

	private static boolean hasConsistentSurface(StructureWorldAccess world, BlockPos centerSurface, int radius) {
		int centerY = centerSurface.getY();
		int minY = centerY;
		int maxY = centerY;
		int solidSamples = 0;
		int neededSamples = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int angle = 0; angle < 360; angle += 45) {
			double rad = Math.toRadians(angle);
			int dx = (int) Math.round(Math.cos(rad) * radius);
			int dz = (int) Math.round(Math.sin(rad) * radius);
			int sampleX = centerSurface.getX() + dx;
			int sampleZ = centerSurface.getZ() + dz;
			BlockPos sampleSurface = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(sampleX, 0, sampleZ)).down();
			if (sampleSurface.getY() <= world.getBottomY() + 6) {
				return false;
			}
			BlockState state = world.getBlockState(sampleSurface);
			if (isEndTerrain(state)) {
				solidSamples++;
			}
			neededSamples++;
			minY = Math.min(minY, sampleSurface.getY());
			maxY = Math.max(maxY, sampleSurface.getY());

			// Check for end city/purpur presence to avoid carving into structures
			for (int dy = 0; dy <= 12; dy += 4) {
				mutable.set(sampleX, sampleSurface.getY() + dy, sampleZ);
				if (isProtectedStructureBlock(world.getBlockState(mutable))) {
					return false;
				}
			}
		}

		if (maxY - minY > MAX_SAMPLE_HEIGHT_VARIATION) {
			return false;
		}

		return solidSamples >= (int) Math.ceil(neededSamples * 0.6);
	}

	private static boolean hasOverheadObstruction(StructureWorldAccess world, BlockPos centerSurface, int radius) {
		int centerY = centerSurface.getY();
		int maxDepth = Math.max(4, (int) Math.round(radius * 0.65));
		int maxCheckHeight = centerY + maxDepth + 8;

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockPos.Mutable surfacePos = new BlockPos.Mutable();

		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > radius + 1.2) {
					continue;
				}

				int columnX = centerSurface.getX() + dx;
				int columnZ = centerSurface.getZ() + dz;

				surfacePos.set(columnX, 0, columnZ);
				int columnTopY = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, surfacePos).getY();
				int ceilingY = Math.min(Math.max(columnTopY, centerY + 1), maxCheckHeight);

				for (int y = centerY + 1; y <= ceilingY; y++) {
					mutable.set(columnX, y, columnZ);
					BlockState state = world.getBlockState(mutable);
					if (state.isAir()) {
						continue;
					}
					if (!isCarvable(state)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean isNearMainIsland(int x, int z) {
		double distance = Math.sqrt((double) (x * x) + (double) (z * z));
		return distance < MAIN_ISLAND_EXCLUSION_RADIUS;
	}

	private static boolean isEndTerrain(BlockState state) {
		if (state.isAir()) return false;
		Block block = state.getBlock();
		return block == Blocks.END_STONE
			|| block == Blocks.OBSIDIAN
			|| block == ModBlocks.ASH_STONE
			|| block == ModBlocks.END_MIRE
			|| block == ModBlocks.MOLD_BLOCK
			|| block == ModBlocks.END_MURK;
	}

	private static boolean isCarvable(BlockState state) {
		if (state.isAir()) return true;
		Block block = state.getBlock();
		return block == Blocks.END_STONE
			|| block == Blocks.OBSIDIAN
			|| block == ModBlocks.ASH_STONE
			|| block == ModBlocks.END_MIRE
			|| block == ModBlocks.MOLD_BLOCK
			|| block == ModBlocks.END_MURK
			|| state.isReplaceable();
	}

	private static boolean adjacentToAir(StructureWorldAccess world, BlockPos.Mutable pos) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = pos.offset(direction);
			if (world.getBlockState(neighbor).isAir()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isProtectedStructureBlock(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.END_STONE_BRICKS
			|| block == Blocks.PURPUR_BLOCK
			|| block == Blocks.PURPUR_PILLAR
			|| block == Blocks.PURPUR_STAIRS
			|| block == Blocks.PURPUR_SLAB
			|| block == Blocks.CHEST
			|| block == Blocks.SHULKER_BOX;
	}

	private static void removeUnsupportedShadowClaws(StructureWorldAccess world, BlockPos centerSurface, int radius) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int startY = centerSurface.getY() + 6;
		int minY = Math.max(world.getBottomY() + 1, centerSurface.getY() - radius - 6);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				int x = centerSurface.getX() + dx;
				int z = centerSurface.getZ() + dz;
				for (int y = startY; y >= minY; y--) {
					mutable.set(x, y, z);
					BlockState state = world.getBlockState(mutable);
					if (state.isOf(ModBlocks.SHADOW_CLAW) && world.getBlockState(mutable.down()).isAir()) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
			}
		}
	}

	private static void removeFloatingMoldSpores(StructureWorldAccess world, BlockPos centerSurface, int radius) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int startY = centerSurface.getY() + 6;
		int minY = Math.max(world.getBottomY() + 1, centerSurface.getY() - radius - 6);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				int x = centerSurface.getX() + dx;
				int z = centerSurface.getZ() + dz;
				for (int y = startY; y >= minY; y--) {
					mutable.set(x, y, z);
					BlockState state = world.getBlockState(mutable);
					
					// Check if this is a mold spore block
					if (state.isOf(ModBlocks.MOLD_SPORE) || state.isOf(ModBlocks.MOLD_SPORE_TUFT)) {
						// Simple blocks: check if ground below is invalid
						BlockPos below = mutable.down();
						BlockState belowState = world.getBlockState(below);
						if (belowState.isAir() || !belowState.isSolid()) {
							world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
						}
					} else if (state.isOf(ModBlocks.MOLD_SPORE_SPROUT)) {
						// Tall plants: check if there's valid ground support
						BlockPos below = mutable.down();
						BlockState belowState = world.getBlockState(below);
						
						// If below is also a sprout (upper half), check one more block down
						if (belowState.isOf(ModBlocks.MOLD_SPORE_SPROUT)) {
							BlockPos groundPos = below.down();
							BlockState groundState = world.getBlockState(groundPos);
							if (groundState.isAir() || !groundState.isSolid()) {
								// Remove both halves
								world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
								world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
							}
						} else {
							// This is likely the lower half - check if ground below is invalid
							if (belowState.isAir() || !belowState.isSolid()) {
								// Remove both halves
								world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
								BlockPos above = mutable.up();
								BlockState aboveState = world.getBlockState(above);
								if (aboveState.isOf(ModBlocks.MOLD_SPORE_SPROUT)) {
									world.setBlockState(above, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
								}
							}
						}
					}
				}
			}
		}
	}

	private static boolean isCellAnchor(int chunkX, int chunkZ) {
		int cellX = Math.floorDiv(chunkX, CHUNK_GRID);
		int cellZ = Math.floorDiv(chunkZ, CHUNK_GRID);
		long cellSeed = mix64(((long) cellX << 32) ^ (long) cellZ ^ 0x52A4F2D7B19A37F3L);
		int offsetX = (int) Long.remainderUnsigned(mix64(cellSeed ^ 0x9E3779B97F4A7C15L), CHUNK_GRID);
		int offsetZ = (int) Long.remainderUnsigned(mix64(cellSeed ^ 0xC2B2AE3D27D4EB4FL), CHUNK_GRID);
		int anchorX = cellX * CHUNK_GRID + offsetX;
		int anchorZ = cellZ * CHUNK_GRID + offsetZ;
		return chunkX == anchorX && chunkZ == anchorZ;
	}

	private static boolean shouldSpawnInCell(int chunkX, int chunkZ) {
		int cellX = Math.floorDiv(chunkX, CHUNK_GRID);
		int cellZ = Math.floorDiv(chunkZ, CHUNK_GRID);
		long cellSeed = mix64(((long) cellX << 32) ^ (long) cellZ ^ 0x7FF8B2C1D223A4C5L);
		java.util.Random cellRand = new java.util.Random(cellSeed);
		return cellRand.nextFloat() < CELL_CRATER_CHANCE;
	}

	private static long mix64(long x) {
		x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
		x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
		return x ^ (x >>> 31);
	}
}

