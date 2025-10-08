package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityGetDeathPosMixin {

	@Inject(method = "getLastDeathPos", at = @At("HEAD"), cancellable = true)
	private void theendupdate$overrideDeathPosOnlyForTaggedStack(CallbackInfoReturnable<Optional<GlobalPos>> cir) {
		ItemStack current = GatewayCompassContext.get();
		
		// Handle quantum gateway compasses
		if (GatewayCompassContext.isTaggedGatewayCompass(current)) {
			PlayerEntity self = (PlayerEntity)(Object)this;
			World world = self.getWorld();
			if (!(world instanceof ClientWorld)) return;

			NbtComponent custom = current.get(DataComponentTypes.CUSTOM_DATA);
			if (custom == null) return;
			var tag = custom.copyNbt();
			String dim = tag.getString("gd").orElse("");
			String worldDim = world.getRegistryKey().getValue().toString();
			if (!worldDim.equals(dim)) return;

			int x = tag.getInt("gx").orElse(0);
			int y = tag.getInt("gy").orElse(0);
			int z = tag.getInt("gz").orElse(0);
			GlobalPos gp = GlobalPos.create(world.getRegistryKey(), new BlockPos(x, y, z));
			cir.setReturnValue(Optional.of(gp));
			cir.cancel();
			return;
		}
		
		// Handle Shadow Hunter's Tracker
		if (GatewayCompassContext.isShadowHuntersTracker(current)) {
			PlayerEntity self = (PlayerEntity)(Object)this;
			World world = self.getWorld();
			if (!(world instanceof ClientWorld)) return;

			NbtComponent custom = current.get(DataComponentTypes.CUSTOM_DATA);
			if (custom == null) return;
			var tag = custom.copyNbt();
			
			// Check if we have hollow tree coordinates
			if (!(tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z"))) {
				// No coordinates bound yet - try to find nearby altar or let it point randomly
				BlockPos playerPos = self.getBlockPos();
				BlockPos nearbyAltar = findNearbyAltar(world, playerPos, playerPos);
				if (nearbyAltar != null) {
					// Found a nearby altar - point to it
					GlobalPos gp = GlobalPos.create(world.getRegistryKey(), nearbyAltar);
					cir.setReturnValue(Optional.of(gp));
					cir.cancel();
					return;
				}
				// No nearby altar found - let compass point randomly until bound to structure
				return;
			}
			
			String dim = tag.getString("world_dimension").orElse("");
			String worldDim = world.getRegistryKey().getValue().toString();
			if (!worldDim.equals(dim)) return;

			int x = tag.getInt("hollow_tree_x").orElse(0);
			int y = tag.getInt("hollow_tree_y").orElse(0);
			int z = tag.getInt("hollow_tree_z").orElse(0);
			BlockPos structurePos = new BlockPos(x, y, z);
			
			// Check if we should point to the altar or structure based on precise_mode
			boolean preciseMode = tag.contains("precise_mode") && tag.getBoolean("precise_mode").orElse(false);
			
			BlockPos targetPos;
			if (preciseMode && tag.contains("altar_x") && tag.contains("altar_y") && tag.contains("altar_z")) {
				// Point to the precise altar location
				int altarX = tag.getInt("altar_x").orElse(x);
				int altarY = tag.getInt("altar_y").orElse(y);
				int altarZ = tag.getInt("altar_z").orElse(z);
				targetPos = new BlockPos(altarX, altarY, altarZ);
			} else {
				// Point to the structure location
				targetPos = structurePos;
			}
			
			GlobalPos gp = GlobalPos.create(world.getRegistryKey(), targetPos);
			cir.setReturnValue(Optional.of(gp));
			cir.cancel();
		}
	}
	
	private BlockPos findNearbyAltar(World world, BlockPos center, BlockPos playerPos) {
		// Search in a 16 block radius around the structure position
		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -16; dz <= 16; dz++) {
				for (int dy = -16; dy <= 16; dy++) {
					BlockPos checkPos = center.add(dx, dy, dz);
					try {
						if (world.getBlockState(checkPos).isOf(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
							return checkPos;
						}
					} catch (Exception e) {
						// Skip if chunk not loaded
					}
				}
			}
		}
		return null;
	}
}


