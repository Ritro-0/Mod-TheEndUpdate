package com.theendupdate.world.structure;

import com.mojang.serialization.MapCodec;
import com.theendupdate.world.structure.piece.ShadowHollowTreePiece;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Places a forced-hollow Shadow tree with a Shadow Altar at its base.
 * Invalid/void sites return Optional.empty() so no ghost structure start is created.
 */
public class ShadowHollowTreeStructure extends Structure {
    public static final MapCodec<ShadowHollowTreeStructure> CODEC = simpleCodec(ShadowHollowTreeStructure::new);
    private static final int MIN_SURFACE_ABOVE_MIN_Y = 16;
    private static final int FLATNESS_TOLERANCE = 3;

    public ShadowHollowTreeStructure(Structure.StructureSettings config) {
        super(config);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int cx = chunkPos.getMiddleBlockX();
        int cz = chunkPos.getMiddleBlockZ();
        int minY = context.heightAccessor().getMinY();

        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
            cx,
            cz,
            Heightmap.Types.WORLD_SURFACE_WG,
            context.heightAccessor(),
            context.randomState()
        );

        // void, no real island surface here - don't create a structure start
        if (surfaceY <= minY + MIN_SURFACE_ABOVE_MIN_Y) {
            return Optional.empty();
        }

        // require a roughly flat patch so postProcess is unlikely to abort
        int supportingColumns = 0;
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                int sampleY = context.chunkGenerator().getFirstOccupiedHeight(
                    cx + dx,
                    cz + dz,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
                );
                if (sampleY > minY + MIN_SURFACE_ABOVE_MIN_Y
                    && Math.abs(sampleY - surfaceY) <= FLATNESS_TOLERANCE) {
                    supportingColumns++;
                }
            }
        }
        if (supportingColumns < 3) {
            return Optional.empty();
        }

        BlockPos pivot = new BlockPos(cx, surfaceY, cz);
        return Optional.of(new GenerationStub(pivot, collector -> {
            collector.addPiece(new ShadowHollowTreePiece(pivot));
        }));
    }

    @Override
    public StructureType<?> type() {
        return com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_TYPE;
    }
}
