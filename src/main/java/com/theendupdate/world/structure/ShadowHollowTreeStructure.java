package com.theendupdate.world.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

/**
 * Simple structure wrapper that places a forced-hollow Shadow tree with a Shadow Altar at its base.
 */
public class ShadowHollowTreeStructure extends Structure {
    public static final MapCodec<ShadowHollowTreeStructure> CODEC = createCodec(ShadowHollowTreeStructure::new);

    public ShadowHollowTreeStructure(Structure.Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        int cx = chunkPos.getStartX() + 8;
        int cz = chunkPos.getStartZ() + 8;
        BlockPos pivot = new BlockPos(cx, 0, cz);
        return Optional.of(new StructurePosition(pivot, collector -> {
            collector.addPiece(new com.theendupdate.world.structure.piece.ShadowHollowTreePiece(pivot));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_TYPE;
    }
}


