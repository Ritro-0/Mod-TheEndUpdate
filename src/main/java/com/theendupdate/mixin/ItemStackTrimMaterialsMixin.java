package com.theendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(ItemStack.class)
public class ItemStackTrimMaterialsMixin {

    /** Any vanilla trim smithing template; used to detect {@code is(ItemTags.TRIM_TEMPLATES)}-style predicates. */
    @Unique
    private static volatile Holder<Item> theendupdate$knownTrimTemplateHolder;

    @Unique
    private static Holder<Item> theendupdate$trimTemplateRefHolder() {
        if (theendupdate$knownTrimTemplateHolder == null) {
            theendupdate$knownTrimTemplateHolder = new ItemStack((ItemLike) Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE).typeHolder();
        }
        return theendupdate$knownTrimTemplateHolder;
    }

    @WrapOperation(
        method = "is(Ljava/util/function/Predicate;)Z",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z")
    )
    private boolean theendupdate$acceptVoidstarInTrimMaterials(
        Predicate<?> predicate,
        Object holderObj,
        Operation<Boolean> original
    ) {
        boolean vanilla = original.call(predicate, holderObj);
        if (vanilla) {
            return true;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(self.getItem());
        if (id == null || !id.getPath().endsWith("_armor_trim_smithing_template")) {
            return false;
        }
        return original.call(predicate, theendupdate$trimTemplateRefHolder());
    }
}
