package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates surface blue-ice rivers that cross End islands and continue as hanging
 * ice beyond island edges.
 *
 * - Width: 5–8 blocks
 * - Bias: higher density near biome edges, but not exclusive
 * - Span: paths are generated per supercell and typically run edge-to-edge
 * - Hanging: when a river meets an island edge, continue outward/down as frozen "drips"
 */
public class BlueIceRiverFeature extends Feature<NoneFeatureConfiguration> {
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
		// bilinear noise so width stays stable across chunk borders
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

	public BlueIceRiverFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel world = context.level();
		BlockPos origin = context.origin();

		// rivers are global, no mask suppression

		int cx = origin.getX() + 8;
		int cz = origin.getZ() + 8;
		if (Math.sqrt((double) (cx * cx) + (double) (cz * cz)) < MAIN_ISLAND_EXCLUSION_RADIUS) {
			return false;
		}

		ChunkPos chunkPos = ChunkPos.containing(origin);
		int chunkMinX = chunkPos.getMinBlockX();
		int chunkMinZ = chunkPos.getMinBlockZ();
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		com.theendupdate.world.OuterEndLayout.Continent continent =
			com.theendupdate.world.OuterEndLayout.shadowContinentAt(cx, cz);
		if (continent != null) {
			return placeShadowlandsCoastToCoast(world, continent, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ);
		}

		// supercell coords, kept deterministic so paths agree across chunks
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

			// march the path; each chunk only places positions inside its own bounds
			double x = path.startX;
			double z = path.startZ;
			double dirX = path.dirX;
			double dirZ = path.dirZ;
			// original heading, used later to resist coastal hugging
			final double goalDX = path.dirX;
			final double goalDZ = path.dirZ;
			// holds direction steady while crossing an island for coast-to-coast paths
			final int ISLAND_HOLD_STEPS = 26;
			int islandStreak = 0;
			double smoothWidth = 8.0; // start near median, eased below to avoid stuttery segments

			int allowedMinX = cellMinX - SUPERCELL_SIZE;
			int allowedMaxX = cellMaxX + SUPERCELL_SIZE;
			int allowedMinZ = cellMinZ - SUPERCELL_SIZE;
			int allowedMaxZ = cellMaxZ + SUPERCELL_SIZE;

			int steps = SUPERCELL_SIZE * PATH_CELLS_SPAN * 2; // spacious upper bound

			for (int step = 0; step < steps; step++) {
				int xi = (int) Math.round(x);
				int zi = (int) Math.round(z);

				// deterministic every step so adjacent chunks agree exactly
				int targetWidth = coherentWidthAt(xi, zi);
				smoothWidth = smoothWidth * 0.75 + targetWidth * 0.25; // mild easing, still stable
				int width = Math.max(5, (int) Math.round(smoothWidth));

				if (xi >= chunkMinX && xi <= chunkMaxX && zi >= chunkMinZ && zi <= chunkMaxZ) {
					// stay within current chunk to avoid cross-chunk loads
					BlockPos topHere = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(xi, 0, zi)).below();
					boolean onIsland = topHere.getY() > world.getMinY() && isEndIslandSurface(world.getBlockState(topHere));
					if (onIsland) {
						islandStreak = ISLAND_HOLD_STEPS;
					} else if (islandStreak > 0) {
						islandStreak--;
					}

					boolean force = islandStreak > 0;
					placedAny |= placeRiverStripe(world, xi, zi, width, force);
				}

				// jitter stays deterministic/local, no cross-chunk queries
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

				// low-frequency meander to break up straight segments
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

                // steer along slopes but resist void edges (coastal avoidance)
                if ((step & 3) == 0) { // sample every 4 steps, light-weight
                    if (xi >= chunkMinX && xi <= chunkMaxX && zi >= chunkMinZ && zi <= chunkMaxZ) {
                        int cMinX = chunkMinX, cMaxX = chunkMaxX, cMinZ = chunkMinZ, cMaxZ = chunkMaxZ;
                        BlockPos p = new BlockPos(xi, 0, zi);
                        BlockPos eP = p.east();
                        BlockPos wP = p.west();
                        BlockPos nP = p.north();
                        BlockPos sP = p.south();
                        int yCenter = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, p).getY();
                        int yE = (eP.getX() >= cMinX && eP.getX() <= cMaxX && eP.getZ() >= cMinZ && eP.getZ() <= cMaxZ)
                            ? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, eP).getY() : yCenter;
                        int yW = (wP.getX() >= cMinX && wP.getX() <= cMaxX && wP.getZ() >= cMinZ && wP.getZ() <= cMaxZ)
                            ? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, wP).getY() : yCenter;
                        int yN = (nP.getX() >= cMinX && nP.getX() <= cMaxX && nP.getZ() >= cMinZ && nP.getZ() <= cMaxZ)
                            ? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, nP).getY() : yCenter;
                        int yS = (sP.getX() >= cMinX && sP.getX() <= cMaxX && sP.getZ() >= cMinZ && sP.getZ() <= cMaxZ)
                            ? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, sP).getY() : yCenter;

                        // positive = downhill toward that axis direction
                        double gx = (double) (yW - yE); // downhill toward east if positive
                        double gz = (double) (yN - yS); // downhill toward south if positive
                        double gLen = Math.hypot(gx, gz);

                        if (gLen > 0.001) {
                            double ngx = gx / gLen;
                            double ngz = gz / gLen;
                            // proximity to a cliff/void edge, from local drop severity
                            int maxDrop = Math.max(Math.max(yCenter - yE, yCenter - yW), Math.max(yCenter - yN, yCenter - yS));
                            double coast = Math.max(0.0, Math.min(1.0, (maxDrop - 5) / 10.0)); // 0 gentle, ->1 near sheer drop

                            // push inland, opposite steepest descent, to avoid hugging coasts
                            double inlandX = -ngx;
                            double inlandZ = -ngz;

                            // stay aligned with original cross-cell heading near coasts
                            double goalLen = Math.max(0.0001, Math.hypot(goalDX, goalDZ));
                            double gdx = goalDX / goalLen;
                            double gdz = goalDZ / goalLen;

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

				// stop once outside the allowed multi-cell corridor
				if (xi < allowedMinX - 2 || xi > allowedMaxX + 2 || zi < allowedMinZ - 2 || zi > allowedMaxZ + 2) {
					break;
				}
			}
		}

		return placedAny;
	}

	private boolean placeShadowlandsCoastToCoast(
		WorldGenLevel world,
		com.theendupdate.world.OuterEndLayout.Continent continent,
		int chunkMinX,
		int chunkMinZ,
		int chunkMaxX,
		int chunkMaxZ
	) {
		long seed = mix64((long) continent.centerX() * 0x9E3779B97F4A7C15L ^ (long) continent.centerZ() * 0xC2B2AE3D27D4EB4FL);
		int riverCount = 2 + (int) ((seed >>> 11) % 3L);
		boolean placedAny = false;

		for (int r = 0; r < riverCount; r++) {
			double baseAngle = (Math.PI * r) / (double) riverCount;
			double jitter = (((seed >>> (16 + r * 5)) & 0x3FFL) / 1023.0 - 0.5) * 0.55;
			double angle = baseAngle + jitter;
			double[] start = continent.coastPoint(angle);
			double[] end = continent.coastPoint(angle + Math.PI);
			double dx = end[0] - start[0];
			double dz = end[1] - start[1];
			double length = Math.hypot(dx, dz);
			if (length < 48.0) {
				continue;
			}
			double inv = 1.0 / length;
			double dirX = dx * inv;
			double dirZ = dz * inv;
			double nx = -dirZ;
			double nz = dirX;
			double phase = ((seed >>> (8 + r)) & 0xFF) / 255.0 * Math.PI * 2.0;

			int steps = (int) Math.ceil(length);
			for (int step = 0; step <= steps; step++) {
				double t = step / (double) steps;
				double envelope = Math.sin(t * Math.PI);
				double meander = envelope * (
					Math.sin(t * Math.PI * 2.6 + phase) * 22.0
					+ Math.sin(t * Math.PI * 6.1 + phase * 0.4) * 9.0
				);
				int xi = (int) Math.round(start[0] + dirX * step + nx * meander);
				int zi = (int) Math.round(start[1] + dirZ * step + nz * meander);
				if (xi < chunkMinX || xi > chunkMaxX || zi < chunkMinZ || zi > chunkMaxZ) {
					continue;
				}
				int width = Math.max(5, coherentWidthAt(xi, zi));
				placedAny |= placeRiverStripe(world, xi, zi, width, true);
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
		// random entry edge, aim toward the opposite edge with a small angle variation
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

	private boolean placeRiverStripe(WorldGenLevel world, int centerX, int centerZ, int width, boolean force) {
		int bottomY = world.getMinY();
		int placed = 0;

		// chunk bounds derived from center so we never query outside this chunk
		int chunkMinX = Math.floorDiv(centerX, 16) * 16;
		int chunkMinZ = Math.floorDiv(centerZ, 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		BlockPos surfaceCenter = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(centerX, 0, centerZ)).below();
		if (surfaceCenter.getY() <= bottomY) return false;
		if (!isEndIslandSurface(world.getBlockState(surfaceCenter))) return false;

		int half = width / 2;
		int radius = Math.max(1, half);
		boolean nearEdge = isBiomeEdge(world, surfaceCenter);
		// don't randomly skip stripes that span a chunk border, width-aware
		boolean nearChunkBorder = ((centerX & 15) < radius) || ((centerX & 15) > (15 - radius))
			|| ((centerZ & 15) < radius) || ((centerZ & 15) > (15 - radius));
		if (!force && !nearEdge && !nearChunkBorder) {
			// favor biome edges, skip non-edge stripes ~19% of the time for continuity
			long h = mix64(((long) centerX << 32) ^ (long) centerZ);
			if ((h & 0xFF) < 48) {
				return false;
			}
		}

		// orthogonal basis from predominant flow direction, estimated from local gradients
		Direction mainDir = guessDownhillDirection(world, surfaceCenter);
		Direction orth = (mainDir.getAxis() == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				BlockPos sample = surfaceCenter.offset(dx, 0, dz);
				if (sample.getX() < chunkMinX || sample.getX() > chunkMaxX || sample.getZ() < chunkMinZ || sample.getZ() > chunkMaxZ) continue;
				BlockPos top = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, sample).below();
				if (top.getY() <= bottomY) continue;
				BlockState topState = world.getBlockState(top);

				// avoid End Cities heuristically, within-chunk only
					if (((dx ^ dz) & 3) == 0 && isNearEndCityBlocks(world, top, 12)) continue;
					// don't overwrite huge tree logs adjacent to the stripe
					if ((dx * dx + dz * dz) <= 1) {
						BlockPos up1 = top.above();
						BlockState up1s = world.getBlockState(up1);
						if (up1s.getBlock() == net.minecraft.world.level.block.Blocks.OAK_LOG || up1s.getBlock() == net.minecraft.world.level.block.Blocks.SPRUCE_LOG
							|| up1s.getBlock() == net.minecraft.world.level.block.Blocks.DARK_OAK_LOG) {
							continue;
						}
					}

				if (isEndIslandSurface(topState)) {
					world.setBlock(top, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
					placed++;

					// uniform base thickness, then a curved "boat" shape below it
					int widthAxisX = orth.getStepX();
					int widthAxisZ = orth.getStepZ();
					int off = dx * widthAxisX + dz * widthAxisZ; // perpendicular offset from center
					double u = Math.min(1.0, Math.abs(off) / (double) Math.max(1, radius));
					int baseDepth = 3; // extend straight down at least 3 blocks everywhere under the river
					int extraCenter = Math.max(2, Math.min(6, (width + 2) / 3)); // additional depth scales with width
					int extra = Math.max(0, (int) Math.floor(extraCenter * (1.0 - u * u))); // parabolic, 0 at edges
					int totalDepth = baseDepth + extra;
					for (int dy = 1; dy <= totalDepth; dy++) {
						BlockPos b = top.below(dy);
						if (b.getX() < chunkMinX || b.getX() > chunkMaxX || b.getZ() < chunkMinZ || b.getZ() > chunkMaxZ) continue;
						BlockState bs = world.getBlockState(b);
						if (bs.isAir() || isEndIslandSurface(bs) || bs.canBeReplaced()) {
							world.setBlock(b, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}

					// forward/back smoothing, inner disk only to keep cost modest
					if (dx * dx + dz * dz <= Math.max(1, radius - 1) * Math.max(1, radius - 1)) {
						BlockPos fwdXZ = top.relative(mainDir);
						if (fwdXZ.getX() >= chunkMinX && fwdXZ.getX() <= chunkMaxX && fwdXZ.getZ() >= chunkMinZ && fwdXZ.getZ() <= chunkMaxZ) {
							BlockPos fwd = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, fwdXZ).below();
							if (isEndIslandSurface(world.getBlockState(fwd))) {
								world.setBlock(fwd, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
							}
						}
						BlockPos fwd2XZ = top.relative(mainDir, 2);
						if (fwd2XZ.getX() >= chunkMinX && fwd2XZ.getX() <= chunkMaxX && fwd2XZ.getZ() >= chunkMinZ && fwd2XZ.getZ() <= chunkMaxZ) {
							BlockPos fwd2 = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, fwd2XZ).below();
							if (isEndIslandSurface(world.getBlockState(fwd2))) {
								world.setBlock(fwd2, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
							}
						}
					}

					// hanging shelf scaled by width, plus paint adjacent faces to read as a river wall
					Direction outward = mostOpenOutward(world, top);
					if (outward != null) {
						int shelfOut = Math.min(6, 2 + (width / 4) + (int) hashToRange(top.getX(), top.getZ(), 0, 1));
						int dripDown = Math.min(10, 3 + (width / 2) + (int) hashToRange(top.getZ(), top.getX(), 0, 2));
						makeHangingShelf(world, top, outward, shelfOut, dripDown);

					for (int dy = 1; dy <= Math.min(3, 1 + (width + 2) / 5); dy++) {
							BlockPos downFace = top.relative(outward).below(dy);
							if (downFace.getX() >= chunkMinX && downFace.getX() <= chunkMaxX && downFace.getZ() >= chunkMinZ && downFace.getZ() <= chunkMaxZ) {
								BlockState s = world.getBlockState(downFace);
								if (s.isAir()) {
									world.setBlock(downFace, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
								}
							}
						}
					}

					// continue as a pillar if the river ends up floating
					BlockPos underside = findUndersideBelowTop(world, top);
					if (underside != null) {
						BlockPos lowerSurface = findNextLowerSurfaceBelow(world, underside, 56);
						if (lowerSurface != null) {
							int drop = underside.getY() - lowerSurface.getY();
							if (drop >= 6) {
								placeIcePillar(world, underside.below(), lowerSurface.above(), Math.max(1, width / 4));
								if (isEndIslandSurface(world.getBlockState(lowerSurface))) {
									world.setBlock(lowerSurface, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
									BlockPos ls1 = lowerSurface.relative(orth);
									BlockPos ls2 = lowerSurface.relative(orth.getOpposite());
									if (isEndIslandSurface(world.getBlockState(ls1))) {
										world.setBlock(ls1, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
									}
									if (isEndIslandSurface(world.getBlockState(ls2))) {
										world.setBlock(ls2, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
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

	private static BlockPos findUndersideBelowTop(WorldGenLevel world, BlockPos topSurface) {
		int bottomY = world.getMinY();
		for (int y = topSurface.getY(); y > bottomY + 5; y--) {
			BlockPos p = new BlockPos(topSurface.getX(), y, topSurface.getZ());
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && world.getBlockState(p.below()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static BlockPos findNextLowerSurfaceBelow(WorldGenLevel world, BlockPos startBelow, int maxDepth) {
		int bottomY = world.getMinY();
		int minY = Math.max(bottomY + 5, startBelow.getY() - maxDepth);
		for (int y = startBelow.getY() - 2; y >= minY; y--) {
			BlockPos p = new BlockPos(startBelow.getX(), y, startBelow.getZ());
			if (isEndIslandSurface(world.getBlockState(p)) && world.getBlockState(p.above()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static void placeIcePillar(WorldGenLevel world, BlockPos fromExclusive, BlockPos toInclusive, int radius) {
		// clamp to this chunk only
		int chunkMinX = Math.floorDiv(fromExclusive.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(fromExclusive.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		int minY = Math.min(fromExclusive.getY(), toInclusive.getY());
    for (int y = fromExclusive.getY(); y >= minY && y >= world.getMinY() + 1; y--) {
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
                world.setBlock(p, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        if (y <= toInclusive.getY()) {
            break;
        }
    }
	}

	private static void makeHangingShelf(WorldGenLevel world, BlockPos edgeTop, Direction outward, int outExtent, int downExtent) {
		// small shelf and drips just outside/below the island edge
		int chunkMinX = Math.floorDiv(edgeTop.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(edgeTop.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		BlockPos cursor = edgeTop.relative(outward);
		for (int out = 0; out < outExtent; out++) {
			if (cursor.getX() < chunkMinX || cursor.getX() > chunkMaxX || cursor.getZ() < chunkMinZ || cursor.getZ() > chunkMaxZ) {
				break;
			}
			BlockPos shelfPos = cursor.below();
			if (world.getBlockState(shelfPos).isAir()) {
				world.setBlock(shelfPos, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			// stalactite-like drips
			int drip = Math.max(1, downExtent - out);
			BlockPos d = shelfPos.below();
			for (int i = 0; i < drip; i++) {
				if (d.getX() < chunkMinX || d.getX() > chunkMaxX || d.getZ() < chunkMinZ || d.getZ() > chunkMaxZ) break;
				if (!world.getBlockState(d).isAir()) break;
				world.setBlock(d, Blocks.BLUE_ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
				d = d.below();
			}
			cursor = cursor.relative(outward);
		}
	}

	private static Direction mostOpenOutward(WorldGenLevel world, BlockPos top) {
		// side with the greatest immediate drop is the outward direction
		int chunkMinX = Math.floorDiv(top.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(top.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		int yTop = top.getY();
		int bestDrop = 3;
		Direction best = null;
		for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			BlockPos n = top.relative(d);
			if (n.getX() < chunkMinX || n.getX() > chunkMaxX || n.getZ() < chunkMinZ || n.getZ() > chunkMaxZ) continue;
			BlockPos nTop = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, n).below();
			int drop = yTop - nTop.getY();
			if (nTop.getY() <= world.getMinY()) drop = 999; // void immediately
			if (drop > bestDrop) {
				bestDrop = drop; best = d;
			}
		}
		return best;
	}

	private static boolean isEndIslandSurface(BlockState state) {
		return state.is(Blocks.END_STONE)
			|| state.is(ModBlocks.END_MIRE)
			|| state.is(ModBlocks.END_MURK)
			|| state.is(ModBlocks.MOLD_BLOCK)
			|| state.is(Blocks.OBSIDIAN);
	}

	private static boolean isBiomeEdge(WorldGenLevel world, BlockPos surface) {
		Holder<Biome> here = world.getBiome(surface);
		return !here.equals(world.getBiome(surface.east()))
			|| !here.equals(world.getBiome(surface.west()))
			|| !here.equals(world.getBiome(surface.north()))
			|| !here.equals(world.getBiome(surface.south()));
	}

	private static Direction guessDownhillDirection(WorldGenLevel world, BlockPos pos) {
		// guard neighbor samples to this chunk
		int chunkMinX = Math.floorDiv(pos.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(pos.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;
		int yCenter = pos.getY();

		int e = (pos.east().getX() >= chunkMinX && pos.east().getX() <= chunkMaxX && pos.east().getZ() >= chunkMinZ && pos.east().getZ() <= chunkMaxZ)
			? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos.east()).getY() : yCenter;
		int w = (pos.west().getX() >= chunkMinX && pos.west().getX() <= chunkMaxX && pos.west().getZ() >= chunkMinZ && pos.west().getZ() <= chunkMaxZ)
			? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos.west()).getY() : yCenter;
		int n = (pos.north().getX() >= chunkMinX && pos.north().getX() <= chunkMaxX && pos.north().getZ() >= chunkMinZ && pos.north().getZ() <= chunkMaxZ)
			? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos.north()).getY() : yCenter;
		int s = (pos.south().getX() >= chunkMinX && pos.south().getX() <= chunkMaxX && pos.south().getZ() >= chunkMinZ && pos.south().getZ() <= chunkMaxZ)
			? world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos.south()).getY() : yCenter;

		int dx = (w - e);
		int dz = (n - s);
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		} else {
			return dz > 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}

	private static boolean isNearEndCityBlocks(WorldGenLevel world, BlockPos center, int radius) {
		// clamp samples within this chunk to avoid cross-chunk queries
		int chunkMinX = Math.floorDiv(center.getX(), 16) * 16;
		int chunkMinZ = Math.floorDiv(center.getZ(), 16) * 16;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		RandomSource r = world.getRandom();
		for (int i = 0; i < 12; i++) {
			int dx = Math.max(chunkMinX, Math.min(chunkMaxX, center.getX() + r.nextIntBetweenInclusive(-radius, radius)));
			int dz = Math.max(chunkMinZ, Math.min(chunkMaxZ, center.getZ() + r.nextIntBetweenInclusive(-radius, radius)));
			int y = Math.max(world.getMinY() + 8, Math.min(center.getY(), world.getMinY() + world.getHeight() - 8));
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



