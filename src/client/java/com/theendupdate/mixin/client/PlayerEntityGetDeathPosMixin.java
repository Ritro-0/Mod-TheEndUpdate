package com.theendupdate.mixin.client;

import com.theendupdate.client.GatewayCompassContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(Player.class)
public abstract class PlayerEntityGetDeathPosMixin {

    // recovery compasses read last death via getLastDeathLocation (26.1.x), not getLastDeathPos
    @Inject(method = "getLastDeathLocation", at = @At("HEAD"), cancellable = true)
    private void theendupdate$overrideDeathLocationForGatewayCompass(CallbackInfoReturnable<Optional<GlobalPos>> cir) {
        ItemStack current = GatewayCompassContext.get();

        if (GatewayCompassContext.isTaggedGatewayCompass(current)) {
            Player self = (Player) (Object) this;
            Level world = self.level();
            if (!(world instanceof ClientLevel clientWorld)) return;

            CustomData custom = current.get(DataComponents.CUSTOM_DATA);
            if (custom == null) return;
            var tag = custom.copyTag();
            String dim = tag.getString("gd").orElse("");
            String worldDim = clientWorld.dimension().identifier().toString();
            if (!worldDim.equals(dim)) return;

            int x = tag.getInt("gx").orElse(0);
            int y = tag.getInt("gy").orElse(0);
            int z = tag.getInt("gz").orElse(0);
            GlobalPos gp = GlobalPos.of(clientWorld.dimension(), new BlockPos(x, y, z));
            cir.setReturnValue(Optional.of(gp));
            cir.cancel();
            return;
        }

        if (GatewayCompassContext.isShadowHuntersTracker(current)) {
            Player self = (Player) (Object) this;
            Level world = self.level();
            if (!(world instanceof ClientLevel clientWorld)) return;

            CustomData custom = current.get(DataComponents.CUSTOM_DATA);
            if (custom == null) return;
            var tag = custom.copyTag();

            if (!(tag.contains("hollow_tree_x") && tag.contains("hollow_tree_y") && tag.contains("hollow_tree_z"))) {
                BlockPos playerPos = self.blockPosition();
                BlockPos nearbyAltar = findNearbyAltar(world, playerPos, playerPos);
                if (nearbyAltar != null) {
                    GlobalPos gp = GlobalPos.of(clientWorld.dimension(), nearbyAltar);
                    cir.setReturnValue(Optional.of(gp));
                    cir.cancel();
                    return;
                }
                return;
            }

            // only restrict by stored dimension when present, older/unbound tags may omit it
            if (tag.contains("world_dimension")) {
                String dim = tag.getString("world_dimension").orElse("");
                String worldDim = clientWorld.dimension().identifier().toString();
                if (!dim.isEmpty() && !worldDim.equals(dim)) {
                    return;
                }
            }

            int x = tag.getInt("hollow_tree_x").orElse(0);
            int y = tag.getInt("hollow_tree_y").orElse(0);
            int z = tag.getInt("hollow_tree_z").orElse(0);
            BlockPos structurePos = new BlockPos(x, y, z);

            boolean preciseMode = tag.contains("precise_mode") && tag.getBoolean("precise_mode").orElse(false);

            BlockPos targetPos;
            if (preciseMode && tag.contains("altar_x") && tag.contains("altar_y") && tag.contains("altar_z")) {
                int altarX = tag.getInt("altar_x").orElse(x);
                int altarY = tag.getInt("altar_y").orElse(y);
                int altarZ = tag.getInt("altar_z").orElse(z);
                targetPos = new BlockPos(altarX, altarY, altarZ);
            } else {
                targetPos = structurePos;
            }

            GlobalPos gp = GlobalPos.of(clientWorld.dimension(), targetPos);
            cir.setReturnValue(Optional.of(gp));
            cir.cancel();
        }
    }

    private BlockPos findNearbyAltar(Level world, BlockPos center, BlockPos playerPos) {
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                for (int dy = -16; dy <= 16; dy++) {
                    BlockPos checkPos = center.offset(dx, dy, dz);
                    try {
                        if (world.getBlockState(checkPos).is(com.theendupdate.registry.ModBlocks.SHADOW_ALTAR)) {
                            return checkPos;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
