package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.world.structure.ShadowHollowTreeStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModStructures {
	public static final StructureType<ShadowHollowTreeStructure> SHADOW_HOLLOW_TREE_TYPE = () -> ShadowHollowTreeStructure.CODEC;
    public static final StructurePieceType SHADOW_HOLLOW_TREE_PIECE = Registry.register(
        BuiltInRegistries.STRUCTURE_PIECE,
        Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_hollow_tree_piece"),
        new StructurePieceType() {
            @Override
            public StructurePiece load(StructurePieceSerializationContext context, CompoundTag nbt) {
                return new com.theendupdate.world.structure.piece.ShadowHollowTreePiece(context, nbt);
            }
        }
    );

	public static final ResourceKey<Structure> SHADOW_HOLLOW_TREE_KEY = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_hollow_tree"));

	public static void register() {
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_hollow_tree"), SHADOW_HOLLOW_TREE_TYPE);
	}
}


