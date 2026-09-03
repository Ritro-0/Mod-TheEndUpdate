package com.theendupdate.mixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a slight multiplicative boost to block breaking speed when the held item is an axe
 * that has the theendupdate:chopping enchantment. The goal is to make Efficiency V
 * netherite axes reliably insta-mine wood-like blocks by nudging the effective speed.
 *
 * Mapping-safe strategy: provide multiple @Inject signatures with require=0 so only the
 * existing one applies depending on Yarn/mappings.
 */
@Mixin(Player.class)
public abstract class PlayerEntityMiningSpeedMixin {

    private static final float CHOPPING_SPEED_MULTIPLIER = 2.00f; // ensure insta-mine threshold

    private static float applyChoppingBoostIfEligible(Player self, float original, BlockState state) {
        if (original <= 0.0f) return original;
        ItemStack held = self.getMainHandItem();
        if (held == null || held.isEmpty()) return original;
        if (!held.is(ItemTags.AXES)) return original;

        ItemEnchantments ench = held.get(DataComponents.ENCHANTMENTS);
        if (ench == null) return original;
        String enchStr = ench.toString();
        if (!enchStr.contains("theendupdate:chopping")) return original;
        float boosted = original * CHOPPING_SPEED_MULTIPLIER;
        return boosted;
    }

    // current Yarn (1.21.8) maps this as getDestroySpeed(BlockState)
    @Inject(
        method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void theendupdate$choppingSpeedBoostStateOnly(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float boosted = applyChoppingBoostIfEligible(self, cir.getReturnValueF(), state);
        if (boosted != cir.getReturnValueF()) {
            cir.setReturnValue(boosted);
        }
    }

}


