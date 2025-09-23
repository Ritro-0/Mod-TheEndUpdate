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

	// Flow styling
	private static final int MEANDER_GRID = 24; // blocks between meander control points
	private static final double MEANDER_STRENGTH = 0.18; // radians of turn per sample
	private static final int WIDTH_NOISE_GRID = 32; // coherence of width field

	private static int coherentWidthAt(int x, int z) {
		// Bilinear noise on a grid for stable width across chunks
		int gx = Math.floorDiv(x, WIDTH_NOISE_GRID);
		int gz = Math.floorDiv(z, WIDTH_NOISE_GRID);
		int bx = gx * WIDTH_NOISE_GRID;
		int bz = gz * WIDTH_NOISE_GRID;
		double fx = (x - bx) / (double) WIDTH_NOISE_GRID;
		double fz = (z - bz) / (double) WIDTH_NOISE_GRID;
		double n00 = ((mix64((((long) gx) << 32) ^ (long) gz) & 0xFFFF) / 65535.0);
		double n10 = ((mix64((((long) (gx + 1)) << 32) ^ (long) gz) & 0xFFFF) / 65535.0);
		double n01 = ((mix64((((long) gx) << 32) ^ (long) (gz + 1)) & 0xFFFF) / 65535.0);
		double n11 = ((mix64((((long) (gx + 1)) << 32) ^ (long) (gz + 1)) & 0xFFFF) / 65535.0);
		double nx0 = n00 * (1.0 - fx) + n10 * fx;
		double nx1 = n01 * (1.0 - fx) + n11 * fx;
		double n = nx0 * (1.0 - fz) + nx1 * fz; // 0..1
		int base = 7; // widened baseline
		int range = 3; // +0..3 → 7..10
		return base + (int) Math.round(n * range);
	}

	public BlueIceRiverFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		// Rivers are global; no mask suppression

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
			// Remember the intended cross-cell heading to discourage coastal hugging
			final double goalDX = path.dirX;
			final double goalDZ = path.dirZ;
			// Within-chunk continuity hold to promote coast-to-coast paths
			final int ISLAND_HOLD_STEPS = 26;
			int islandStreak = 0;
			// Smooth width across steps to avoid stuttery/thin segments
			double smoothWidth = 8.0; // start near median

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

                    // Widen rivers by ~2 blocks on average: 7..10
					// Deterministic width field to match across chunk edges
					int targetWidth = coherentWidthAt(xi, zi);
					smoothWidth = smoothWidth * 0.75 + targetWidth * 0.25; // mild easing, still stable
					int width = Math.max(5, (int) Math.round(smoothWidth));
					boolean force = islandStreak > 0;
					placedAny |= placeRiverStripe(world, xi, zi, width, force);
				}

				// Slight jitter/meander stays deterministic and local; keep it but do not query across chunks
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

				// Add low-frequency meandering to reduce straight segments
				if ((step % MEANDER_GRID) == 0) {
					int mx = Math.floorDiv(xi, MEANDER_GRID);
					int mz = Math.floorDiv(zi, MEANDER_GRID);
					long mh = mix64((((long) mx) << 32) ^ (long) mz ^ cellSeed);
					double a = (((mh >>> 16) & 0x3FFL) / 1023.0) * 2.0 - 1.0; // [-1,1]
					double rot2 = a * MEANDER_STRENGTH;
					double mdx = dirX * Math.cos(rot2) - dirZ * Math.sin(rot2);
					double mdz = dirX * Math.sin(rot2) + dirZ * Math.cos(rot2);
					double mlen = Math.max(0.0001, Math.hypot(mdx, mdz));
					dirX = mdx / mlen;
					dirZ = mdz / mlen;
				}

                // Slope preference with coastal avoidance: steer along slopes but resist void edges
                if ((step & 3) == 0) { // light-weight sampling every 4 steps
                    // Only sample when the current position is within this chunk
                    if (xi >= chunkMinX && xi <= chunkMaxX && zi >= chunkMinZ && zi <= chunkMaxZ) {
                        int cMinX = chunkMinX, cMaxX = chunkMaxX, cMinZ = chunkMinZ, cMaxZ = chunkMaxZ;
                        BlockPos p = new BlockPos(xi, 0, zi);
                        BlockPos eP = p.east();
                        BlockPos wP = p.west();
                        BlockPos nP = p.north();
                        BlockPos sP = p.south();
                        int yCenter = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, p).getY();
                        int yE = (eP.getX() >= cMinX && eP.getX() <= cMaxX && eP.getZ() >= cMinZ && eP.getZ() <= cMaxZ)
                            ? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, eP).getY() : yCenter;
                        int yW = (wP.getX() >= cMinX && wP.getX() <= cMaxX && wP.getZ() >= cMinZ && wP.getZ() <= cMaxZ)
                            ? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, wP).getY() : yCenter;
                        int yN = (nP.getX() >= cMinX && nP.getX() <= cMaxX && nP.getZ() >= cMinZ && nP.getZ() <= cMaxZ)
                            ? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, nP).getY() : yCenter;
                        int yS = (sP.getX() >= cMinX && sP.getX() <= cMaxX && sP.getZ() >= cMinZ && sP.getZ() <= cMaxZ)
                            ? world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, sP).getY() : yCenter;

                        // Gradient components: positive means downhill toward that axis direction
                        double gx = (double) (yW - yE); // downhill toward east if positive
                        double gz = (double) (yN - yS); // downhill toward south if positive
                        double gLen = Math.hypot(gx, gz);

                        if (gLen > 0.001) {
                            // Blend current direction with normalized gradient (favoring slopes up or down equally)
                            double ngx = gx / gLen;
                            double ngz = gz / gLen;
                            // Detect proximity to a cliff/void edge by local drop severity
                            int maxDrop = Math.max(Math.max(yCenter - yE, yCenter - yW), Math.max(yCenter - yN, yCenter - yS));
                            double coast = Math.max(0.0, Math.min(1.0, (maxDrop - 5) / 10.0)); // 0 when gentle, ->1 near sheer drop

                            // Compute an inland push opposite steepest descent to avoid hugging coasts
                            double inlandX = -ngx;
                            double inlandZ = -ngz;

                            // Encourage staying aligned with the original cross-cell heading near coasts
                            double goalLen = Math.max(0.0001, Math.hypot(goalDX, goalDZ));
                            double gdx = goalDX / goalLen;
                            double gdz = goalDZ / goalLen;

                            // Base weights
                            double wSlope = 0.16; // follow slope a bit
                            double wGoal = 0.18 + 0.22 * coast; // stronger cross-island alignment near coasts
                            double wInland = 0.00 + 0.30 * coast; // push inland when edge is severe

                            double rem = Math.max(0.0, 1.0 - (wSlope + wGoal + wInland));
                            double bdx = rem * dirX + wSlope * ngx + wGoal * gdx + wInland * inlandX;
                            double bdz = rem * dirZ + wSlope * ngz + wGoal * gdz + wInland * inlandZ;
                            double blen = Math.max(0.0001, Math.hypot(bdx, bdz));
                            dirX = bdx / blen;
                            dirZ = bdz / blen;
                        }
                    }
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
		boolean nearChunkBorder = ((centerX & 15) <= 1) || ((centerX & 15) >= 14) || ((centerZ & 15) <= 1) || ((centerZ & 15) >= 14);
		if (!force && !nearEdge && !nearChunkBorder) {
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
		int radius = Math.max(1, half);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				BlockPos sample = surfaceCenter.add(dx, 0, dz);
				if (sample.getX() < chunkMinX || sample.getX() > chunkMaxX || sample.getZ() < chunkMinZ || sample.getZ() > chunkMaxZ) continue;
				BlockPos top = world.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, sample).down();
				if (top.getY() <= bottomY) continue;
				BlockState topState = world.getBlockState(top);

				// Avoid End Cities heuristically (limited to within-chunk to avoid cross-chunk queries)
					if (((dx ^ dz) & 3) == 0 && isNearEndCityBlocks(world, top, 12)) continue;
					// Avoid overwriting large feature blocks like our huge trees' logs when adjacent
					// Keep within-chunk only: peek 1 block up for wood-like blocks
					if ((dx * dx + dz * dz) <= 1) {
						BlockPos up1 = top.up();
						BlockState up1s = world.getBlockState(up1);
						if (up1s.getBlock() == net.minecraft.block.Blocks.OAK_LOG || up1s.getBlock() == net.minecraft.block.Blocks.SPRUCE_LOG
							|| up1s.getBlock() == net.minecraft.block.Blocks.DARK_OAK_LOG) {
							continue;
						}
					}

				if (isEndIslandSurface(topState)) {
					world.setBlockState(top, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
					placed++;

					// Forward/back smoothing: only for inner disk to keep cost modest
					if (dx * dx + dz * dz <= Math.max(1, radius - 1) * Math.max(1, radius - 1)) {
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
							if (isEndIslandSurface(world.getBlockState(fwd2))) {
								world.setBlockState(fwd2, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
							}
						}
					}

					// Hanging shelf scaled by width, and lightly paint adjacent vertical faces to read as a river wall
					Direction outward = mostOpenOutward(world, top);
					if (outward != null) {
						int shelfOut = Math.min(6, 2 + (width / 4) + (int) hashToRange(top.getX(), top.getZ(), 0, 1));
						int dripDown = Math.min(10, 3 + (width / 2) + (int) hashToRange(top.getZ(), top.getX(), 0, 2));
						makeHangingShelf(world, top, outward, shelfOut, dripDown);

					// Quick wall coating: 2-3 blocks down along the outward face (slightly more aggressive)
					for (int dy = 1; dy <= Math.min(3, 1 + (width + 2) / 5); dy++) {
							BlockPos downFace = top.offset(outward).down(dy);
							if (downFace.getX() >= chunkMinX && downFace.getX() <= chunkMaxX && downFace.getZ() >= chunkMinZ && downFace.getZ() <= chunkMaxZ) {
								BlockState s = world.getBlockState(downFace);
								if (s.isAir()) {
									world.setBlockState(downFace, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
								}
							}
						}
					}

					// Pillar continuation if floating
					BlockPos underside = findUndersideBelowTop(world, top);
					if (underside != null) {
						BlockPos lowerSurface = findNextLowerSurfaceBelow(world, underside, 56);
						if (lowerSurface != null) {
							int drop = underside.getY() - lowerSurface.getY();
							if (drop >= 6) {
								placeIcePillar(world, underside.down(), lowerSurface.up(), Math.max(1, width / 4));
								if (isEndIslandSurface(world.getBlockState(lowerSurface))) {
									world.setBlockState(lowerSurface, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
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

private static void placeIcePillar(StructureWorldAccess world, BlockPos fromExclusive, BlockPos toInclusive, int radius) {
		// Clamp to this chunk only
		int chunkMinX = Math.floorDiv(fromExclusive.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(fromExclusive.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		int minY = Math.min(fromExclusive.getY(), toInclusive.getY());
    for (int y = fromExclusive.getY(); y >= minY && y >= world.getBottomY() + 1; y--) {
        // Draw a small solid disk per level
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockPos p = new BlockPos(fromExclusive.getX() + dx, y, fromExclusive.getZ() + dz);
                if (p.getX() < chunkMinX || p.getX() > chunkMaxX || p.getZ() < chunkMinZ || p.getZ() > chunkMaxZ) {
                    continue;
                }
                if (!world.getBlockState(p).isAir()) {
                    continue;
                }
                world.setBlockState(p, Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
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



