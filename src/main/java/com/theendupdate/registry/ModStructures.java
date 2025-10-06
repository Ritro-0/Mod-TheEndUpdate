package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.world.structure.ShadowHollowTreeStructure;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.gen.structure.Structure;

public final class ModStructures {
	public static final StructureType<ShadowHollowTreeStructure> SHADOW_HOLLOW_TREE_TYPE = () -> ShadowHollowTreeStructure.CODEC;
    public static final StructurePieceType SHADOW_HOLLOW_TREE_PIECE = Registry.register(
        Registries.STRUCTURE_PIECE,
        Identifier.of(TemplateMod.MOD_ID, "shadow_hollow_tree_piece"),
        new StructurePieceType() {
            @Override
            public StructurePiece load(StructureContext context, NbtCompound nbt) {
                return new com.theendupdate.world.structure.piece.ShadowHollowTreePiece(context, nbt);
            }
        }
    );

	public static final RegistryKey<Structure> SHADOW_HOLLOW_TREE_KEY = RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(TemplateMod.MOD_ID, "shadow_hollow_tree"));

	public static void register() {
		Registry.register(Registries.STRUCTURE_TYPE, Identifier.of(TemplateMod.MOD_ID, "shadow_hollow_tree"), SHADOW_HOLLOW_TREE_TYPE);
	}
}


