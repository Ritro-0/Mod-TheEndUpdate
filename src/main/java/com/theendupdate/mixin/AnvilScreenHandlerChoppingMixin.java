package com.theendupdate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Prevents the chopping enchantment from being applied to non-axes via anvil.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerChoppingMixin {

    @Inject(
        method = "createResult",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V",
            ordinal = 0
        ),
        cancellable = true
    )
    private void theendupdate$preventChoppingOnNonAxes(CallbackInfo ci) {
        try {
            AnvilMenu self = (AnvilMenu) (Object) this;

            // field name varies by mapping
            Container input = theendupdate$getField(self, "inputSlots", Container.class);
            if (input == null) {
                input = theendupdate$getField(self, "input", Container.class);
            }
            if (input == null) return;
            
            ItemStack leftInput = input.getItem(0);
            ItemStack rightInput = input.getItem(1);
            
            if (leftInput.isEmpty() && rightInput.isEmpty()) return;
            
            ContainerLevelAccess context = theendupdate$getContext(self);
            if (context == null) return;

            boolean[] shouldCancel = {false};
            context.execute((world, pos) -> {
                try {
                    var enchantmentRegistry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    Identifier choppingId = Identifier.fromNamespaceAndPath("theendupdate", "chopping");
                    Holder<Enchantment> choppingEnchantment = enchantmentRegistry.get(choppingId).orElse(null);
                    
                    if (choppingEnchantment == null) return;

                    boolean rightHasChopping = false;
                    if (rightInput.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
                        ItemEnchantments rightEnch = rightInput.get(DataComponents.STORED_ENCHANTMENTS);
                        if (rightEnch != null && rightEnch.getLevel(choppingEnchantment) > 0) {
                            rightHasChopping = true;
                        }
                    }

                    boolean leftHasChopping = false;
                    if (!leftInput.isEmpty()) {
                        ItemEnchantments leftEnch = leftInput.get(DataComponents.ENCHANTMENTS);
                        if (leftEnch != null && leftEnch.getLevel(choppingEnchantment) > 0) {
                            leftHasChopping = true;
                        }
                    }

                    if ((rightHasChopping || leftHasChopping) && !leftInput.isEmpty()) {
                        if (!leftInput.is(net.minecraft.tags.ItemTags.AXES)) {
                            shouldCancel[0] = true;
                        }
                    }
                } catch (Exception ignored) {}
            });
            
            if (shouldCancel[0]) {
                Container output = theendupdate$getField(self, "resultSlots", Container.class);
                if (output == null) {
                    output = theendupdate$getField(self, "output", Container.class);
                }
                if (output != null) {
                    output.setItem(0, ItemStack.EMPTY);
                }
                ci.cancel();
            }
        } catch (Exception ignored) {
            // fail silently, don't crash the menu
        }
    }
    
    @Unique
    private static ContainerLevelAccess theendupdate$getContext(AnvilMenu self) {
        try {
            Class<?> c = self.getClass();
            while (c != null) {
                for (String name : new String[] { "context", "field_17639" }) {
                    try {
                        Field f = c.getDeclaredField(name);
                        f.setAccessible(true);
                        Object val = f.get(self);
                        if (val instanceof ContainerLevelAccess ctx) return ctx;
                    } catch (NoSuchFieldException ignored) { }
                }
                for (Field f : c.getDeclaredFields()) {
                    if (ContainerLevelAccess.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(self);
                        if (val instanceof ContainerLevelAccess ctx) return ctx;
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) { }
        return null;
    }
    
    @Unique
    private static <T> T theendupdate$getField(Object obj, String fieldName, Class<T> type) {
        try {
            Class<?> c = obj.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (type.isInstance(val)) return type.cast(val);
                } catch (NoSuchFieldException ignored) { }
                for (Field f : c.getDeclaredFields()) {
                    if (type.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(obj);
                        if (type.isInstance(val)) return type.cast(val);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Throwable ignored) { }
        return null;
    }
}

