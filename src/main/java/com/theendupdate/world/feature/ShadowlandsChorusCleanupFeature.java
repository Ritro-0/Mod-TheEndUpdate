package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Removes vanilla chorus plants/flowers inside the Shadowlands region after
 * vegetation placement. Runs at TOP_LAYER_MODIFICATION to ensure cleanup.
 */
public class ShadowlandsChorusCleanupFeature extends Feature<NoneFeatureConfiguration> {
    public ShadowlandsChorusCleanupFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        // only act within Shadowlands mask
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        if (!com.theendupdate.world.ShadowlandsRegion.isInRegion(chunkX, chunkZ)) return false;

        ChunkPos cp = ChunkPos.containing(origin);
        int startX = cp.getMinBlockX();
        int startZ = cp.getMinBlockZ();
        int bottomY = world.getMinY();
        int topY = world.getMinY() + world.getHeight();
        boolean any = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                for (int y = bottomY; y < topY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    var s = world.getBlockState(p);
                    if (s.is(Blocks.CHORUS_PLANT) || s.is(Blocks.CHORUS_FLOWER)) {
                        world.setBlock(p, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                        any = true;
                    }
                }
            }
        }

        return any;
    }
}


