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
		// Only override during item rendering/predicate evaluation when a specific stack is being processed
		ItemStack current = GatewayCompassContext.get();
		if (!GatewayCompassContext.isTaggedGatewayCompass(current)) return;

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
		cir.setReturnValue(Optional.of(GlobalPos.create(world.getRegistryKey(), new BlockPos(x, y, z))));
	}
}


