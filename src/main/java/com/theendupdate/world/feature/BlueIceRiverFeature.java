package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Generates surface blue-ice rivers that cross End islands and continue as hanging
 * ice beyond island edges.
 *
 * - Width: 5–8 blocks
 * - Bias: higher density near biome edges, but not exclusive
 * - Span: paths are generated per supercell and typically run edge-to-edge
 * - Hanging: when a river meets an island edge, continue outward/down as frozen "drips"
 */
public class BlueIceRiverFeature extends Feature<DefaultFeatureConfig> {
	private static final int MAIN_ISLAND_EXCLUSION_RADIUS = 800;
	private static final int SUPERCELL_SIZE = 128; // generate long paths per 128x128 area
	private static final int PATH_CELLS_SPAN = 3;   // allow paths to run across 3 cells for coast-to-coast reach
	private static final int RIVERS_PER_CELL_BASE = 0; // base count per supercell
	private static final float RIVERS_PER_CELL_EXTRA_CHANCE = 0.18f; // ~18% chance to spawn one river in this cell

	public BlueIceRiverFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		int cx = origin.getX() + 8;
		int cz = origin.getZ() + 8;
		if (Math.sqrt((double) (cx * cx) + (double) (cz * cz)) < MAIN_ISLAND_EXCLUSION_RADIUS) {
			return false;
		}

		ChunkPos chunkPos = new ChunkPos(origin);
		int chunkMinX = chunkPos.getStartX();
		int chunkMinZ = chunkPos.getStartZ();
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		// Determine supercell for deterministic cross-chunk paths
		int cellX = Math.floorDiv(chunkMinX + 8, SUPERCELL_SIZE);
		int cellZ = Math.floorDiv(chunkMinZ + 8, SUPERCELL_SIZE);
		int cellMinX = cellX * SUPERCELL_SIZE;
		int cellMinZ = cellZ * SUPERCELL_SIZE;
		int cellMaxX = cellMinX + SUPERCELL_SIZE - 1;
		int cellMaxZ = cellMinZ + SUPERCELL_SIZE - 1;

		long cellSeed = mix64(cellX * 0x9E3779B97F4A7C15L ^ (cellZ * 0xC2B2AE3D27D4EB4FL));
		java.util.Random cellRand = new java.util.Random(cellSeed);

		int riversInCell = RIVERS_PER_CELL_BASE + (cellRand.nextFloat() < RIVERS_PER_CELL_EXTRA_CHANCE ? 1 : 0);
		boolean placedAny = false;

		for (int r = 0; r < riversInCell; r++) {
			PathSpec path = choosePathAcrossCell(cellRand, cellMinX, cellMinZ, cellMaxX, cellMaxZ);
			if (path == null) continue;

			// March along the path; each chunk places only positions within its bounds
			double x = path.startX;
			double z = path.startZ;
			double dirX = path.dirX;
			double dirZ = path.dirZ;
			// Within-chunk continuity hold to promote coast-to-coast paths
			final int ISLAND_HOLD_STEPS = 26;
			int islandStreak = 0;

			// Allow across multiple cells
			int allowedMinX = cellMinX - SUPERCELL_SIZE;
			int allowedMaxX = cellMaxX + SUPERCELL_SIZE;
			int allowedMinZ = cellMinZ - SUPERCELL_SIZE;
			int allowedMaxZ = cellMaxZ + SUPERCELL_SIZE;

			int steps = SUPERCELL_SIZE * PATH_CELLS_SPAN * 2; // spacious upper bound

			for (int step = 0; step < steps; step++) {
				int xi = (int) Math.round(x);
				int zi = (int) Math.round(z);

				if (xi >= chunkMinX && xi <= chunkMaxX && zi >= chunkMinZ && zi <= chunkMaxZ) {
					// Only query heights within the current chunk region to avoid cross-chunk loads
					BlockPos topHere = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(xi, 0, zi)).down();
					boolean onIsland = topHere.getY() > world.getBottomY() && isEndIslandSurface(world.getBlockState(topHere));
					if (onIsland) {
						islandStreak = ISLAND_HOLD_STEPS;
					} else if (islandStreak > 0) {
						islandStreak--;
					}

					int width = 5 + (int) hashToRange(xi, zi, 0, 3); // 5..8
					boolean force = islandStreak > 0;
					placedAny |= placeRiverStripe(world, xi, zi, width, force);
				}

				// Slight jitter to prevent straight lines, keeps deterministic using hashed offsets
				if ((step & 7) == 0) {
					long h = mix64((long) xi * 31_557L ^ (long) zi * 7_021L ^ cellSeed);
					double jitter = ((h & 0xFFL) / 255.0 - 0.5) * 0.6; // -0.3..0.3
					double rot = jitter * Math.PI * 0.25; // rotate up to ~45deg small
					double ndx = dirX * Math.cos(rot) - dirZ * Math.sin(rot);
					double ndz = dirX * Math.sin(rot) + dirZ * Math.cos(rot);
					double len = Math.max(0.0001, Math.hypot(ndx, ndz));
					dirX = ndx / len;
					dirZ = ndz / len;
				}

				x += dirX;
				z += dirZ;

				// Stop after leaving the allowed multi-cell corridor
				if (xi < allowedMinX - 2 || xi > allowedMaxX + 2 || zi < allowedMinZ - 2 || zi > allowedMaxZ + 2) {
					break;
				}
			}
		}

		return placedAny;
	}

	private static class PathSpec {
		final double startX, startZ, dirX, dirZ;
		PathSpec(double startX, double startZ, double dirX, double dirZ) {
			this.startX = startX; this.startZ = startZ; this.dirX = dirX; this.dirZ = dirZ;
		}
	}

	private static PathSpec choosePathAcrossCell(java.util.Random rand, int minX, int minZ, int maxX, int maxZ) {
		// Pick a random entry edge and aim toward the opposite edge with a small angle variation
		int edge = rand.nextInt(4); // 0:N,1:S,2:W,3:E
		double startX, startZ, dirX, dirZ;
		double angleJitter = (rand.nextDouble() - 0.5) * Math.toRadians(18);
		switch (edge) {
			case 0: // North -> heading +Z
				startX = minX + 8 + rand.nextInt(Math.max(1, (maxX - minX) - 16));
				startZ = minZ;
				dirX = Math.sin(angleJitter);
				dirZ = Math.cos(angleJitter);
				break;
			case 1: // South -> heading -Z
				startX = minX + 8 + rand.nextInt(Math.max(1, (maxX - minX) - 16));
				startZ = maxZ;
				dirX = Math.sin(-angleJitter);
				dirZ = -Math.cos(angleJitter);
				break;
			case 2: // West -> heading +X
				startX = minX;
				startZ = minZ + 8 + rand.nextInt(Math.max(1, (maxZ - minZ) - 16));
				dirX = Math.cos(angleJitter);
				dirZ = Math.sin(angleJitter);
				break;
			default: // East -> heading -X
				startX = maxX;
				startZ = minZ + 8 + rand.nextInt(Math.max(1, (maxZ - minZ) - 16));
				dirX = -Math.cos(angleJitter);
				dirZ = Math.sin(angleJitter);
				break;
		}
		double len = Math.max(0.0001, Math.hypot(dirX, dirZ));
		return new PathSpec(startX, startZ, dirX / len, dirZ / len);
	}

	private boolean placeRiverStripe(StructureWorldAccess world, int centerX, int centerZ, int width, boolean force) {
		int bottomY = world.getBottomY();
		int placed = 0;

		// Determine chunk bounds from the center to ensure we never query outside this chunk region
		int chunkMinX = Math.floorDiv(centerX, 16) * 16;
		int chunkMinZ = Math.floorDiv(centerZ, 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		// Determine bias for biome edges
		BlockPos surfaceCenter = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, new BlockPos(centerX, 0, centerZ)).down();
		if (surfaceCenter.getY() <= bottomY) return false;
		if (!isEndIslandSurface(world.getBlockState(surfaceCenter))) return false;

		boolean nearEdge = isBiomeEdge(world, surfaceCenter);
		if (!force && !nearEdge) {
			// Mostly favor edges: skip non-edge stripes some of the time
			long h = mix64(((long) centerX << 32) ^ (long) centerZ);
			// Reduce skipping to improve continuity (approx 19% skip)
			if ((h & 0xFF) < 48) {
				return false;
			}
		}

		// Build an orthogonal basis using the path's predominant direction estimated from local gradients
		Direction mainDir = guessDownhillDirection(world, surfaceCenter);
		Direction orth = (mainDir.getAxis() == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;

		int half = width / 2;
		for (int o = -half; o <= half; o++) {
			BlockPos col = surfaceCenter.offset(orth, o);
			if (col.getX() < chunkMinX || col.getX() > chunkMaxX || col.getZ() < chunkMinZ || col.getZ() > chunkMaxZ) continue;
			BlockPos top = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, col).down();
			if (top.getY() <= bottomY) continue;
			BlockState topState = world.getBlockState(top);

			// Avoid End Cities heuristically (limited to within-chunk to avoid cross-chunk queries)
			if ((o & 3) == 0 && isNearEndCityBlocks(world, top, 12)) continue;

			if (isEndIslandSurface(topState)) {
				// Carve/replace top with blue ice
				world.setBlockState(top, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
				placed++;

				// Forward/back smoothing to avoid visible striping: paint small brush in path direction
				BlockPos fwdXZ = top.offset(mainDir);
				if (fwdXZ.getX() >= chunkMinX && fwdXZ.getX() <= chunkMaxX && fwdXZ.getZ() >= chunkMinZ && fwdXZ.getZ() <= chunkMaxZ) {
					BlockPos fwd = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, fwdXZ).down();
					if (isEndIslandSurface(world.getBlockState(fwd))) {
						world.setBlockState(fwd, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
				BlockPos fwd2XZ = top.offset(mainDir, 2);
				if (fwd2XZ.getX() >= chunkMinX && fwd2XZ.getX() <= chunkMaxX && fwd2XZ.getZ() >= chunkMinZ && fwd2XZ.getZ() <= chunkMaxZ) {
					BlockPos fwd2 = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, fwd2XZ).down();
					if ((Math.abs(o) <= half - 1) && isEndIslandSurface(world.getBlockState(fwd2))) {
						world.setBlockState(fwd2, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}

				// If this column borders the void or a large drop, create a hanging drip
				Direction outward = mostOpenOutward(world, top);
				if (outward != null) {
					makeHangingShelf(world, top, outward, 2 + (int) hashToRange(top.getX(), top.getZ(), 0, 2),
						3 + (int) hashToRange(top.getZ(), top.getX(), 0, 4));
				}

				// If this is a floating platform, create pillar(s) connecting to lower river/surface below
				BlockPos underside = findUndersideBelowTop(world, top);
				if (underside != null) {
					BlockPos lowerSurface = findNextLowerSurfaceBelow(world, underside, 56);
					if (lowerSurface != null) {
						int drop = underside.getY() - lowerSurface.getY();
						if (drop >= 6) {
							placeIcePillar(world, underside.down(), lowerSurface.up());
							// Seed a bit of river continuation on the lower surface for visual connection
							if (isEndIslandSurface(world.getBlockState(lowerSurface))) {
								world.setBlockState(lowerSurface, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
								// widen by one orth step for cohesion
								BlockPos ls1 = lowerSurface.offset(orth);
								BlockPos ls2 = lowerSurface.offset(orth.getOpposite());
								if (isEndIslandSurface(world.getBlockState(ls1))) {
									world.setBlockState(ls1, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
								}
								if (isEndIslandSurface(world.getBlockState(ls2))) {
									world.setBlockState(ls2, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
								}
							}
						}
					}
				}
			}
		}

		return placed > 0;
	}

	private static BlockPos findUndersideBelowTop(StructureWorldAccess world, BlockPos topSurface) {
		int bottomY = world.getBottomY();
		for (int y = topSurface.getY(); y > bottomY + 5; y--) {
			BlockPos p = new BlockPos(topSurface.getX(), y, topSurface.getZ());
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && world.getBlockState(p.down()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static BlockPos findNextLowerSurfaceBelow(StructureWorldAccess world, BlockPos startBelow, int maxDepth) {
		int bottomY = world.getBottomY();
		int minY = Math.max(bottomY + 5, startBelow.getY() - maxDepth);
		for (int y = startBelow.getY() - 2; y >= minY; y--) {
			BlockPos p = new BlockPos(startBelow.getX(), y, startBelow.getZ());
			if (isEndIslandSurface(world.getBlockState(p)) && world.getBlockState(p.up()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static void placeIcePillar(StructureWorldAccess world, BlockPos fromExclusive, BlockPos toInclusive) {
		// Clamp to this chunk only
		int chunkMinX = Math.floorDiv(fromExclusive.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(fromExclusive.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		int minY = Math.min(fromExclusive.getY(), toInclusive.getY());
		for (int y = fromExclusive.getY(); y >= minY && y >= world.getBottomY() + 1; y--) {
			BlockPos p = new BlockPos(fromExclusive.getX(), y, fromExclusive.getZ());
			if (p.getX() < chunkMinX || p.getX() > chunkMaxX || p.getZ() < chunkMinZ || p.getZ() > chunkMaxZ) {
				break;
			}
			if (!world.getBlockState(p).isAir()) {
				// stop when we hit solid to avoid tunneling through islands
				break;
			}
			world.setBlockState(p, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
			if (y <= toInclusive.getY()) {
				break;
			}
		}
	}

	private static void makeHangingShelf(StructureWorldAccess world, BlockPos edgeTop, Direction outward, int outExtent, int downExtent) {
		// Build a small shelf and drips just outside and below the island edge
		int chunkMinX = Math.floorDiv(edgeTop.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(edgeTop.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		BlockPos cursor = edgeTop.offset(outward);
		for (int out = 0; out < outExtent; out++) {
			if (cursor.getX() < chunkMinX || cursor.getX() > chunkMaxX || cursor.getZ() < chunkMinZ || cursor.getZ() > chunkMaxZ) {
				break;
			}
			BlockPos shelfPos = cursor.down();
			if (world.getBlockState(shelfPos).isAir()) {
				world.setBlockState(shelfPos, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
			// Small stalactite-like drips
			int drip = Math.max(1, downExtent - out);
			BlockPos d = shelfPos.down();
			for (int i = 0; i < drip; i++) {
				if (d.getX() < chunkMinX || d.getX() > chunkMaxX || d.getZ() < chunkMinZ || d.getZ() > chunkMaxZ) break;
				if (!world.getBlockState(d).isAir()) break;
				world.setBlockState(d, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
				d = d.down();
			}
			cursor = cursor.offset(outward);
		}
	}

	private static Direction mostOpenOutward(StructureWorldAccess world, BlockPos top) {
		// Look for side with greatest immediate drop to the void/air to decide outward direction
		int chunkMinX = Math.floorDiv(top.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(top.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		int yTop = top.getY();
		int bestDrop = 3;
		Direction best = null;
		for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			BlockPos n = top.offset(d);
			if (n.getX() < chunkMinX || n.getX() > chunkMaxX || n.getZ() < chunkMinZ || n.getZ() > chunkMaxZ) continue;
			BlockPos nTop = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, n).down();
			int drop = yTop - nTop.getY();
			if (nTop.getY() <= world.getBottomY()) drop = 999; // void immediately
			if (drop > bestDrop) {
				bestDrop = drop; best = d;
			}
		}
		return best;
	}

	private static boolean isEndIslandSurface(BlockState state) {
		return state.isOf(Blocks.END_STONE)
			|| state.isOf(ModBlocks.END_MIRE)
			|| state.isOf(ModBlocks.MOLD_BLOCK)
			|| state.isOf(Blocks.OBSIDIAN);
	}

	private static boolean isBiomeEdge(StructureWorldAccess world, BlockPos surface) {
		RegistryEntry<Biome> here = world.getBiome(surface);
		return !here.equals(world.getBiome(surface.east()))
			|| !here.equals(world.getBiome(surface.west()))
			|| !here.equals(world.getBiome(surface.north()))
			|| !here.equals(world.getBiome(surface.south()));
	}

	private static Direction guessDownhillDirection(StructureWorldAccess world, BlockPos pos) {
		// Guard neighbor samples to this chunk
		int chunkMinX = Math.floorDiv(pos.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(pos.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;
		int yCenter = pos.getY();

		int e = (pos.east().getX() >= chunkMinX && pos.east().getX() <= chunkMaxX && pos.east().getZ() >= chunkMinZ && pos.east().getZ() <= chunkMaxZ)
			? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pos.east()).getY() : yCenter;
		int w = (pos.west().getX() >= chunkMinX && pos.west().getX() <= chunkMaxX && pos.west().getZ() >= chunkMinZ && pos.west().getZ() <= chunkMaxZ)
			? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pos.west()).getY() : yCenter;
		int n = (pos.north().getX() >= chunkMinX && pos.north().getX() <= chunkMaxX && pos.north().getZ() >= chunkMinZ && pos.north().getZ() <= chunkMaxZ)
			? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pos.north()).getY() : yCenter;
		int s = (pos.south().getX() >= chunkMinX && pos.south().getX() <= chunkMaxX && pos.south().getZ() >= chunkMinZ && pos.south().getZ() <= chunkMaxZ)
			? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, pos.south()).getY() : yCenter;

		int dx = (w - e);
		int dz = (n - s);
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		} else {
			return dz > 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}

	private static boolean isNearEndCityBlocks(StructureWorldAccess world, BlockPos center, int radius) {
		// Clamp samples within this chunk to avoid cross-chunk block queries
		int chunkMinX = Math.floorDiv(center.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(center.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		Random r = world.getRandom();
		for (int i = 0; i < 12; i++) {
			int dx = Math.max(chunkMinX, Math.min(chunkMaxX, center.getX() + r.nextBetween(-radius, radius)));
			int dz = Math.max(chunkMinZ, Math.min(chunkMaxZ, center.getZ() + r.nextBetween(-radius, radius)));
			int y = Math.max(world.getBottomY() + 8, Math.min(center.getY(), world.getBottomY() + world.getHeight() - 8));
			for (int dy = -8; dy <= 8; dy += 4) {
				BlockState s = world.getBlockState(new BlockPos(dx, y + dy, dz));
				if (isEndCityBlock(s)) return true;
			}
		}
		return false;
	}

	private static boolean isEndCityBlock(BlockState s) {
		Block b = s.getBlock();
		return b == Blocks.PURPUR_BLOCK
			|| b == Blocks.PURPUR_PILLAR
			|| b == Blocks.PURPUR_STAIRS
			|| b == Blocks.PURPUR_SLAB
			|| b == Blocks.END_STONE_BRICKS
			|| b == Blocks.END_ROD
			|| b == Blocks.CHORUS_FLOWER
			|| b == Blocks.CHORUS_PLANT;
	}

	private static long mix64(long x) {
		x ^= (x >>> 33);
		x *= 0xff51afd7ed558ccdL;
		x ^= (x >>> 33);
		x *= 0xc4ceb9fe1a85ec53L;
		x ^= (x >>> 33);
		return x;
	}

	private static long hash2(long x, long z) {
		long h = x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL;
		return mix64(h);
	}

	private static double hashToRange(int x, int z, int min, int maxInclusive) {
		long h = hash2(x, z);
		int span = Math.max(1, (maxInclusive - min + 1));
		return min + (int) ((h >>> 32) % span);
	}
}



