package com.theendupdate.world.feature;

import com.mojang.serialization.Codec;
import com.theendupdate.block.VoidBloomBlock;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * After vanilla places chorus plants/flowers, scan the chunk and attach Void Blooms
 * to mature chorus flowers using the same rules we use during growth from manual placement.
 */
public class VoidBloomChorusAttachmentFeature extends Feature<NoneFeatureConfiguration> {
    public VoidBloomChorusAttachmentFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource random = context.random();

        // suppress chorus attachments inside Shadowlands
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        if (com.theendupdate.world.ShadowlandsRegion.isInRegion(chunkX, chunkZ)) {
            return false;
        }

        ChunkPos chunkPos = ChunkPos.containing(context.origin());
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        int bottomY = world.getMinY();
        int topYExclusive = world.getMinY() + world.getHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                for (int y = bottomY; y < topYExclusive; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (!state.is(Blocks.CHORUS_FLOWER)) {
                        continue;
                    }

                    // only fully mature buds (AGE_5 == 5), matches growth rule
                    int age = state.getValue(BlockStateProperties.AGE_5);
                    if (age < 5) {
                        continue;
                    }

                    // 75% chance, same as growth helper
                    if (random.nextFloat() > 0.75f) {
                        continue;
                    }

                    Direction[] directions = Direction.values();
                    for (int attempt = 0; attempt < 3; attempt++) {
                        Direction dir = directions[random.nextInt(directions.length)];
                        BlockPos target = pos.relative(dir);
                        if (!world.getBlockState(target).isAir()) {
                            continue;
                        }

                        Direction attachmentDirection = dir.getOpposite();
                        BlockState attachedState = ((VoidBloomBlock) ModBlocks.VOID_BLOOM)
                            .getAttachedState(attachmentDirection);

                        if (attachedState.canSurvive(world, target)) {
                            world.setBlock(target, attachedState, Block.UPDATE_CLIENTS);
                            break; // one bloom per flower
                        }
                    }
                }
            }
        }

        return true;
    }
}


