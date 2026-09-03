package com.theendupdate.world;

import com.theendupdate.entity.TetherlingEntity;
import com.theendupdate.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Registers natural spawn behaviour for mod entities.
 */
public final class ModEntitySpawns {
    private static final int TARDIGRADE_WEIGHT = 4;
    private static final int TETHERLING_WEIGHT = 3;
    private static final int TARDIGRADE_MIN_GROUP = 1;
    private static final int TARDIGRADE_MAX_GROUP = 2;
    private static final int TETHERLING_MIN_GROUP = 1;
    private static final int TETHERLING_MAX_GROUP = 1;
    private static final int MIN_AIR_COLUMN = 5;
    private static final int MAX_AIR_COLUMN = 12;
    private static final int SURFACE_RADIUS = 8; // used by void tardigrade placement

    // Tetherling placement tuning (the “void midpoint” spawns)
    private static final int TETHERLING_NO_NEARBY_RADIUS = 300; // blocks (horizontal)
    private static final int TETHERLING_NO_NEARBY_RADIUS_SQ = TETHERLING_NO_NEARBY_RADIUS * TETHERLING_NO_NEARBY_RADIUS;
    private static final int TETHERLING_SURFACE_DELTA_MIN = 16; // spawnY - surfaceY
    private static final int TETHERLING_SURFACE_DELTA_MAX = 24;
    private static final int TETHERLING_SURFACE_NEAR_RADIUS = 20; // disallow island surfaces this close
    private static final int TETHERLING_SURFACE_SEARCH_RADIUS = 64; // must find an island surface in this range
    /** No natural tardigrade/tetherling spawns on/near the main End island. */
    private static final int MAIN_ISLAND_EXCLUSION_RADIUS = 350;
    private static final int MAIN_ISLAND_EXCLUSION_RADIUS_SQ =
        MAIN_ISLAND_EXCLUSION_RADIUS * MAIN_ISLAND_EXCLUSION_RADIUS;

    private ModEntitySpawns() {}

    public static void register() {
        SpawnPlacements.register(
            ModEntities.VOID_TARDIGRADE,
            SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ModEntitySpawns::canVoidTardigradeSpawn
        );

        BiomeModifications.addSpawn(
            BiomeSelectors.foundInTheEnd(),
            MobCategory.AMBIENT,
            ModEntities.VOID_TARDIGRADE,
            TARDIGRADE_WEIGHT,
            TARDIGRADE_MIN_GROUP,
            TARDIGRADE_MAX_GROUP
        );

        SpawnPlacements.register(
            ModEntities.TETHERLING,
            SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ModEntitySpawns::canVoidTetherlingSpawn
        );

        BiomeModifications.addSpawn(
            BiomeSelectors.foundInTheEnd(),
            MobCategory.AMBIENT,
            ModEntities.TETHERLING,
            TETHERLING_WEIGHT,
            TETHERLING_MIN_GROUP,
            TETHERLING_MAX_GROUP
        );
    }

    private static boolean canVoidTardigradeSpawn(
        EntityType<?> type,
        ServerLevelAccessor world,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!isEnd(world)) {
            return false;
        }

        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }

        if (isNearMainIsland(pos)) {
            return false;
        }

        if (!world.isEmptyBlock(pos) || !world.isEmptyBlock(pos.above()) || !world.getFluidState(pos).isEmpty()) {
            return false;
        }

        int bottomY = world.getMinY();
        if (pos.getY() <= bottomY + 16) {
            return false;
        }

        int airDepth = countAirBelow(world, pos);
        if (airDepth < MIN_AIR_COLUMN || airDepth > MAX_AIR_COLUMN) {
            return false;
        }

        return hasNearbyIslandSurface(world, pos);
    }

    private static boolean canVoidTetherlingSpawn(
        EntityType<?> type,
        ServerLevelAccessor world,
        EntitySpawnReason reason,
        BlockPos pos,
        RandomSource random
    ) {
        if (!isEnd(world)) {
            return false;
        }

        // non-natural spawns (spawn egg, commands) stay unblocked
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }

        if (isNearMainIsland(pos)) {
            return false;
        }

        // must be in the void: air column, no fluids
        if (!world.isEmptyBlock(pos) || !world.isEmptyBlock(pos.above()) || !world.getFluidState(pos).isEmpty()) {
            return false;
        }

        int bottomY = world.getMinY();
        if (pos.getY() <= bottomY + 16) {
            return false;
        }

        int airDepth = countAirBelow(world, pos);
        if (airDepth < MIN_AIR_COLUMN || airDepth > MAX_AIR_COLUMN) {
            return false;
        }

        // never let a second tetherling spawn close by
        if (hasNearbyTetherling(world, pos)) {
            return false;
        }

        // "middle between islands": no qualifying surface too close, but one must
        // exist at about (surfaceY + ~20) in a wider ring
        boolean hasNearSurface = hasIslandSurfaceAtDeltaWindow(
            world,
            pos,
            1,
            TETHERLING_SURFACE_NEAR_RADIUS,
            TETHERLING_SURFACE_DELTA_MIN,
            TETHERLING_SURFACE_DELTA_MAX
        );
        if (hasNearSurface) {
            return false;
        }

        int quadrantMask = getIslandSurfaceQuadrantMaskAtDeltaWindow(
            world,
            pos,
            TETHERLING_SURFACE_NEAR_RADIUS + 1,
            TETHERLING_SURFACE_SEARCH_RADIUS,
            TETHERLING_SURFACE_DELTA_MIN,
            TETHERLING_SURFACE_DELTA_MAX
        );
        return quadrantMask != 0 && Integer.bitCount(quadrantMask) >= 2;
    }

    private static boolean isEnd(ServerLevelAccessor world) {
        ResourceKey<Level> key = world.getLevel().dimension();
        return key.equals(Level.END);
    }

    private static boolean isNearMainIsland(BlockPos pos) {
        long x = pos.getX();
        long z = pos.getZ();
        return x * x + z * z < MAIN_ISLAND_EXCLUSION_RADIUS_SQ;
    }

    private static int countAirBelow(ServerLevelAccessor world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int depth = 0;
        for (int i = 0; i < MAX_AIR_COLUMN; i++) {
            cursor.move(Direction.DOWN);
            if (cursor.getY() <= world.getMinY()) {
                break;
            }
            if (!world.isEmptyBlock(cursor) || !world.getFluidState(cursor).isEmpty()) {
                break;
            }
            depth++;
        }
        return depth;
    }

    private static boolean hasNearbyIslandSurface(ServerLevelAccessor world, BlockPos pos) {
        int minRadiusSq = 9;   // 3 blocks
        int maxRadiusSq = SURFACE_RADIUS * SURFACE_RADIUS;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -SURFACE_RADIUS; dx <= SURFACE_RADIUS; dx++) {
            for (int dz = -SURFACE_RADIUS; dz <= SURFACE_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq < minRadiusSq || distSq > maxRadiusSq) continue;

                int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + dx, pos.getZ() + dz);
                if (surfaceY <= world.getMinY()) continue;

                if (!isWithinVerticalWindow(pos.getY(), surfaceY)) continue;

                mutable.set(pos.getX() + dx, surfaceY - 1, pos.getZ() + dz);
                BlockState state = world.getBlockState(mutable);
                if (state.isAir()) continue;
                if (!state.isRedstoneConductor(world, mutable)) continue;

                // confirm there is still void just outward from the surface
                mutable.set(pos.getX() + dx, surfaceY + 1, pos.getZ() + dz);
                if (world.isEmptyBlock(mutable)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasIslandSurfaceAtDeltaWindow(
        ServerLevelAccessor world,
        BlockPos pos,
        int minRadius,
        int maxRadius,
        int deltaMin,
        int deltaMax
    ) {
        int bottomY = world.getMinY();
        int minRadiusSq = minRadius * minRadius;
        int maxRadiusSq = maxRadius * maxRadius;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int spawnY = pos.getY();

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                if (dx == 0 && dz == 0) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq < minRadiusSq || distSq > maxRadiusSq) continue;

                int sampleX = pos.getX() + dx;
                int sampleZ = pos.getZ() + dz;

                int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, sampleX, sampleZ);
                if (surfaceY <= bottomY) continue;

                int delta = spawnY - surfaceY;
                if (delta < deltaMin || delta > deltaMax) continue;

                // confirm this sample is actually a surface into the void
                mutable.set(sampleX, surfaceY - 1, sampleZ);
                BlockState state = world.getBlockState(mutable);
                if (state.isAir()) continue;
                if (!state.isRedstoneConductor(world, mutable)) continue;

                // confirm there is still void just outward from the surface
                mutable.set(sampleX, surfaceY + 1, sampleZ);
                if (world.isEmptyBlock(mutable)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int getIslandSurfaceQuadrantMaskAtDeltaWindow(
        ServerLevelAccessor world,
        BlockPos pos,
        int minRadius,
        int maxRadius,
        int deltaMin,
        int deltaMax
    ) {
        int bottomY = world.getMinY();
        int minRadiusSq = minRadius * minRadius;
        int maxRadiusSq = maxRadius * maxRadius;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int spawnY = pos.getY();

        int mask = 0;
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                if (dx == 0 && dz == 0) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq < minRadiusSq || distSq > maxRadiusSq) continue;

                int sampleX = pos.getX() + dx;
                int sampleZ = pos.getZ() + dz;

                int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, sampleX, sampleZ);
                if (surfaceY <= bottomY) continue;

                int delta = spawnY - surfaceY;
                if (delta < deltaMin || delta > deltaMax) continue;

                mutable.set(sampleX, surfaceY - 1, sampleZ);
                BlockState state = world.getBlockState(mutable);
                if (state.isAir()) continue;
                if (!state.isRedstoneConductor(world, mutable)) continue;

                // confirm there is still void just outward from the surface
                mutable.set(sampleX, surfaceY + 1, sampleZ);
                if (!world.isEmptyBlock(mutable)) continue;

                // axis-aligned samples count toward one side, still contribute to direction diversity
                boolean east = dx >= 0;
                boolean west = dx < 0;
                boolean south = dz >= 0;
                boolean north = dz < 0;

                int bit;
                if (east && south) bit = 1;        // +x +z
                else if (west && south) bit = 2;   // -x +z
                else if (west && north) bit = 4;    // -x -z
                else if (east && north) bit = 8;    // +x -z
                else continue; // only possible when dx==0 and dz==0 (already skipped)

                mask |= bit;
                if (Integer.bitCount(mask) >= 2) {
                    return mask; // already satisfied
                }
            }
        }

        return mask;
    }

    private static boolean hasNearbyTetherling(ServerLevelAccessor world, BlockPos pos) {
        ServerLevel serverWorld = world.getLevel();
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;

        // keep the query box tight vertically, tetherlings live near their spawn altitude
        int verticalHalf = 64;
        int r = TETHERLING_NO_NEARBY_RADIUS;

        AABB box = new AABB(
            pos.getX() - r, pos.getY() - verticalHalf, pos.getZ() - r,
            pos.getX() + r + 1, pos.getY() + verticalHalf + 1, pos.getZ() + r + 1
        );

        for (TetherlingEntity tetherling : serverWorld.getEntitiesOfClass(
            TetherlingEntity.class,
            box,
            Entity::isAlive
        )) {
            if (tetherling == null || tetherling.isRemoved()) continue;
            double dx = tetherling.getX() - cx;
            double dz = tetherling.getZ() - cz;
            if (dx * dx + dz * dz <= TETHERLING_NO_NEARBY_RADIUS_SQ) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWithinVerticalWindow(int spawnY, int surfaceY) {
        int delta = spawnY - surfaceY;
        return delta >= 3 && delta <= 28;
    }
}

