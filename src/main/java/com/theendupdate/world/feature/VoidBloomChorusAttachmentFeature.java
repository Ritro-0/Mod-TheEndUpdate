package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.block.VoidBloomBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * After vanilla places chorus plants/flowers, scan the chunk and attach Void Blooms
 * to mature chorus flowers using the same rules we use during growth from manual placement.
 */
public class VoidBloomChorusAttachmentFeature extends Feature<DefaultFeatureConfig> {
    public VoidBloomChorusAttachmentFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();

        // Suppress chorus-related attachments inside Shadowlands region
        int chunkX = context.getOrigin().getX() >> 4;
        int chunkZ = context.getOrigin().getZ() >> 4;
        if (com.theendupdate.world.ShadowlandsRegion.isInRegion(chunkX, chunkZ)) {
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(context.getOrigin());
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // Iterate each column and search vertical range for chorus flowers
        int bottomY = world.getBottomY();
        int topYExclusive = world.getBottomY() + world.getHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                for (int y = bottomY; y < topYExclusive; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isOf(Blocks.CHORUS_FLOWER)) {
                        continue;
                    }

                    // Only attach to fully mature buds (AGE_5 == 5), matching growth rule
                    int age = state.get(Properties.AGE_5);
                    if (age < 5) {
                        continue;
                    }

                    // 75% chance to try placing a bloom, same as growth helper
                    if (random.nextFloat() > 0.75f) {
                        continue;
                    }

                    // Try up to three random directions to place an attached bloom
                    Direction[] directions = Direction.values();
                    for (int attempt = 0; attempt < 3; attempt++) {
                        Direction dir = directions[random.nextInt(directions.length)];
                        BlockPos target = pos.offset(dir);
                        if (!world.getBlockState(target).isAir()) {
                            continue;
                        }

                        Direction attachmentDirection = dir.getOpposite();
                        BlockState attachedState = ((VoidBloomBlock) ModBlocks.VOID_BLOOM)
                            .getAttachedState(attachmentDirection);

                        // Respect canPlaceAt and server-side placement rules
                        if (attachedState.canPlaceAt(world, target)) {
                            world.setBlockState(target, attachedState, Block.NOTIFY_LISTENERS);
                            break; // placed for this flower; move on
                        }
                    }
                }
            }
        }

        return true;
    }
}


