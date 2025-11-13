package com.theendupdate.world;

import com.theendupdate.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocation;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Registers natural spawn behaviour for mod entities.
 */
public final class ModEntitySpawns {
    private static final int TARDIGRADE_WEIGHT = 4;
    private static final int TARDIGRADE_MIN_GROUP = 1;
    private static final int TARDIGRADE_MAX_GROUP = 2;
    private static final int MIN_AIR_COLUMN = 5;
    private static final int MAX_AIR_COLUMN = 12;
    private static final int SURFACE_RADIUS = 8;

    private ModEntitySpawns() {}

    public static void register() {
        SpawnRestriction.register(
            ModEntities.VOID_TARDIGRADE,
            SpawnLocationTypes.UNRESTRICTED,
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
            ModEntitySpawns::canVoidTardigradeSpawn
        );

        BiomeModifications.addSpawn(
            BiomeSelectors.foundInTheEnd(),
            SpawnGroup.AMBIENT,
            ModEntities.VOID_TARDIGRADE,
            TARDIGRADE_WEIGHT,
            TARDIGRADE_MIN_GROUP,
            TARDIGRADE_MAX_GROUP
        );
    }

    private static boolean canVoidTardigradeSpawn(
        EntityType<?> type,
        ServerWorldAccess world,
        SpawnReason reason,
        BlockPos pos,
        Random random
    ) {
        if (!isEnd(world)) {
            return false;
        }

        if (reason != SpawnReason.NATURAL && reason != SpawnReason.CHUNK_GENERATION) {
            return true;
        }

        if (!world.isAir(pos) || !world.isAir(pos.up()) || !world.getFluidState(pos).isEmpty()) {
            return false;
        }

        int bottomY = world.getBottomY();
        if (pos.getY() <= bottomY + 16) {
            return false;
        }

        int airDepth = countAirBelow(world, pos);
        if (airDepth < MIN_AIR_COLUMN || airDepth > MAX_AIR_COLUMN) {
            return false;
        }

        return hasNearbyIslandSurface(world, pos);
    }

    private static boolean isEnd(ServerWorldAccess world) {
        RegistryKey<World> key = world.toServerWorld().getRegistryKey();
        return key.equals(World.END);
    }

    private static int countAirBelow(ServerWorldAccess world, BlockPos pos) {
        BlockPos.Mutable cursor = pos.mutableCopy();
        int depth = 0;
        for (int i = 0; i < MAX_AIR_COLUMN; i++) {
            cursor.move(Direction.DOWN);
            if (cursor.getY() <= world.getBottomY()) {
                break;
            }
            if (!world.isAir(cursor) || !world.getFluidState(cursor).isEmpty()) {
                break;
            }
            depth++;
        }
        return depth;
    }

    private static boolean hasNearbyIslandSurface(ServerWorldAccess world, BlockPos pos) {
        int minRadiusSq = 9;   // 3 blocks
        int maxRadiusSq = SURFACE_RADIUS * SURFACE_RADIUS;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int dx = -SURFACE_RADIUS; dx <= SURFACE_RADIUS; dx++) {
            for (int dz = -SURFACE_RADIUS; dz <= SURFACE_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq < minRadiusSq || distSq > maxRadiusSq) continue;

                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX() + dx, pos.getZ() + dz);
                if (surfaceY <= world.getBottomY()) continue;

                if (!isWithinVerticalWindow(pos.getY(), surfaceY)) continue;

                mutable.set(pos.getX() + dx, surfaceY - 1, pos.getZ() + dz);
                BlockState state = world.getBlockState(mutable);
                if (state.isAir()) continue;
                if (!state.isSolidBlock(world, mutable)) continue;

                // confirm there is still void just outward from the surface
                mutable.set(pos.getX() + dx, surfaceY + 1, pos.getZ() + dz);
                if (world.isAir(mutable)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWithinVerticalWindow(int spawnY, int surfaceY) {
        int delta = spawnY - surfaceY;
        return delta >= 3 && delta <= 28;
    }
}

