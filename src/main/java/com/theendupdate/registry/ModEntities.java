package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.entity.EtherealOrbEntity;
import com.theendupdate.entity.EyesEntity;
import com.theendupdate.entity.KingPhantomEntity;
import com.theendupdate.entity.MiniShadowCreakingEntity;
import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.entity.TetherlingEntity;
import com.theendupdate.entity.TinyShadowCreakingEntity;
import com.theendupdate.entity.VoidTardigradeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

	private static final Identifier ETHEREAL_ORB_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "ethereal_orb");
	private static final Identifier KING_PHANTOM_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "king_phantom");
	private static final Identifier SHADOW_CREAKING_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "shadow_creaking");
	private static final Identifier MINI_SHADOW_CREAKING_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "mini_shadow_creaking");
	private static final Identifier TINY_SHADOW_CREAKING_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "tiny_shadow_creaking");
	private static final Identifier VOID_TARDIGRADE_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "void_tardigrade");
	private static final Identifier TETHERLING_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "tetherling");
	private static final Identifier EYES_ID = Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "eyes");

	public static final EntityType<EtherealOrbEntity> ETHEREAL_ORB = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ETHEREAL_ORB_ID,
		EntityType.Builder.of(EtherealOrbEntity::new, MobCategory.AMBIENT)
			.sized(0.4375f, 0.6875f)
			.eyeHeight(0.34375f)
			.clientTrackingRange(64)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, ETHEREAL_ORB_ID))
	);

	public static final EntityType<KingPhantomEntity> KING_PHANTOM = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		KING_PHANTOM_ID,
		EntityType.Builder.of(KingPhantomEntity::new, MobCategory.MONSTER)
			.sized(3.6f, 2.0f)
			.eyeHeight(1.0f)
			.clientTrackingRange(80)
			.updateInterval(3)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, KING_PHANTOM_ID))
	);

	public static final EntityType<ShadowCreakingEntity> SHADOW_CREAKING = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SHADOW_CREAKING_ID,
		EntityType.Builder.of(ShadowCreakingEntity::new, MobCategory.MONSTER)
			.sized(1.6f, 5.7f)
			.eyeHeight(4.6f)
			.clientTrackingRange(64)
			.updateInterval(2)
			.noLootTable()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, SHADOW_CREAKING_ID))
	);

	public static final EntityType<MiniShadowCreakingEntity> MINI_SHADOW_CREAKING = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MINI_SHADOW_CREAKING_ID,
		EntityType.Builder.of(MiniShadowCreakingEntity::new, MobCategory.MONSTER)
			.sized(0.8f, 2.85f)
			.eyeHeight(2.3f)
			.clientTrackingRange(64)
			.updateInterval(2)
			.noLootTable()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, MINI_SHADOW_CREAKING_ID))
	);

	public static final EntityType<TinyShadowCreakingEntity> TINY_SHADOW_CREAKING = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		TINY_SHADOW_CREAKING_ID,
		EntityType.Builder.of(TinyShadowCreakingEntity::new, MobCategory.MONSTER)
			.sized(0.4f, 1.425f)
			.eyeHeight(1.15f)
			.clientTrackingRange(64)
			.updateInterval(2)
			.noLootTable()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, TINY_SHADOW_CREAKING_ID))
	);

	public static final EntityType<VoidTardigradeEntity> VOID_TARDIGRADE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		VOID_TARDIGRADE_ID,
		EntityType.Builder.of(VoidTardigradeEntity::new, MobCategory.AMBIENT)
			.sized(0.8f, 0.6f)
			.eyeHeight(0.4f)
			.clientTrackingRange(48)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, VOID_TARDIGRADE_ID))
	);

	public static final EntityType<TetherlingEntity> TETHERLING = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		TETHERLING_ID,
		EntityType.Builder.of(TetherlingEntity::new, MobCategory.AMBIENT)
			.sized(1.375f, 1.25f)
			.eyeHeight(0.92f)
			.clientTrackingRange(56)
			.updateInterval(2)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, TETHERLING_ID))
	);

	public static final EntityType<EyesEntity> EYES = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		EYES_ID,
		EntityType.Builder.of(EyesEntity::new, MobCategory.MISC)
			.sized(1.0f, 1.0f)
			.eyeHeight(0.5f)
			.clientTrackingRange(128)
			.updateInterval(1)
			.noSave()
			.noLootTable()
			.fireImmune()
			.build(ResourceKey.create(Registries.ENTITY_TYPE, EYES_ID))
	);

	public static void registerModEntities() {
		FabricDefaultAttributeRegistry.register(ETHEREAL_ORB, EtherealOrbEntity.createEtherealOrbAttributes());
		FabricDefaultAttributeRegistry.register(KING_PHANTOM, KingPhantomEntity.createKingPhantomAttributes());
		FabricDefaultAttributeRegistry.register(SHADOW_CREAKING, ShadowCreakingEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(MINI_SHADOW_CREAKING, MiniShadowCreakingEntity.createMiniAttributes());
		FabricDefaultAttributeRegistry.register(TINY_SHADOW_CREAKING, TinyShadowCreakingEntity.createTinyAttributes());
		FabricDefaultAttributeRegistry.register(VOID_TARDIGRADE, VoidTardigradeEntity.createVoidTardigradeAttributes());
		FabricDefaultAttributeRegistry.register(TETHERLING, TetherlingEntity.createTetherlingAttributes());
		FabricDefaultAttributeRegistry.register(EYES, EyesEntity.createAttributes());
	}
}
