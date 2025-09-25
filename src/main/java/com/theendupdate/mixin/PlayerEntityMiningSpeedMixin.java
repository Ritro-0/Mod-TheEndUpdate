package com.theendupdate.mixin;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
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
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMiningSpeedMixin {

    private static final float CHOPPING_SPEED_MULTIPLIER = 2.00f; // ensure insta-mine threshold

    private static float applyChoppingBoostIfEligible(PlayerEntity self, float original, BlockState state) {
        if (original <= 0.0f) return original;
        ItemStack held = self.getMainHandStack();
        if (held == null || held.isEmpty()) return original;
        if (!held.isIn(ItemTags.AXES)) return original;

        ItemEnchantmentsComponent ench = held.get(DataComponentTypes.ENCHANTMENTS);
        if (ench == null) return original;
        String enchStr = ench.toString();
        if (!enchStr.contains("theendupdate:chopping")) return original;
        float boosted = original * CHOPPING_SPEED_MULTIPLIER;
        return boosted;
    }

    // Target current Yarn (1.21.8) signature: getBlockBreakingSpeed(BlockState)
    @Inject(
        method = "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F",
        at = @At("RETURN"),
        cancellable = true,
        require = 1
    )
    private void theendupdate$choppingSpeedBoostStateOnly(BlockState state, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        float boosted = applyChoppingBoostIfEligible(self, cir.getReturnValueF(), state);
        if (boosted != cir.getReturnValueF()) {
            cir.setReturnValue(boosted);
        }
    }

}


