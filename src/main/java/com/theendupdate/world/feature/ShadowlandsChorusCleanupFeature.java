package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Removes vanilla chorus plants/flowers inside the Shadowlands region after
 * vegetation placement. Runs at TOP_LAYER_MODIFICATION to ensure cleanup.
 */
public class ShadowlandsChorusCleanupFeature extends Feature<DefaultFeatureConfig> {
    public ShadowlandsChorusCleanupFeature(Codec<DefaultFeatureConfig> codec) { super(codec); }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        // Only act within Shadowlands mask
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        if (!com.theendupdate.world.ShadowlandsRegion.isInRegion(chunkX, chunkZ)) return false;

        ChunkPos cp = new ChunkPos(origin);
        int startX = cp.getStartX();
        int startZ = cp.getStartZ();
        int bottomY = world.getBottomY();
        int topY = world.getBottomY() + world.getHeight();
        boolean any = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                for (int y = bottomY; y < topY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    var s = world.getBlockState(p);
                    if (s.isOf(Blocks.CHORUS_PLANT) || s.isOf(Blocks.CHORUS_FLOWER)) {
                        world.setBlockState(p, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
                        any = true;
                    }
                }
            }
        }

        return any;
    }
}


