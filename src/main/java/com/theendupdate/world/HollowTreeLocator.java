package com.theendupdate.world;

import com.theendupdate.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Thin wrapper around vanilla structure locate (same path as {@code /locate structure}).
 */
public final class HollowTreeLocator {
    private static final int SEARCH_RADIUS_CHUNKS = 512;

    private HollowTreeLocator() {}

    /** @return nearest hollow shadow tree structure position, or null if none */
    public static BlockPos locate(ServerLevel world, BlockPos origin) {
        if (!world.dimension().equals(Level.END)) {
            return null;
        }

        var registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var entry = registry.get(ModStructures.SHADOW_HOLLOW_TREE_KEY).orElse(null);
        if (entry == null) {
            return null;
        }

        var pair = world.getChunkSource().getGenerator().findNearestMapStructure(
            world,
            HolderSet.direct(entry),
            origin,
            SEARCH_RADIUS_CHUNKS,
            false
        );
        return pair == null ? null : pair.getFirst();
    }
}
