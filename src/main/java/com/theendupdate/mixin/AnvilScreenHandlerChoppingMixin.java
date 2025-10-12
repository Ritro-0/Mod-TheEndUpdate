package com.theendupdate.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Prevents the chopping enchantment from being applied to non-axes via anvil.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerChoppingMixin {

    @Inject(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Inventory;setStack(ILnet/minecraft/item/ItemStack;)V", ordinal = 0), cancellable = true)
    private void theendupdate$preventChoppingOnNonAxes(CallbackInfo ci) {
        try {
            AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
            
            // Get input inventory to check what's being combined
            Inventory input = theendupdate$getField(self, "input", Inventory.class);
            if (input == null) return;
            
            ItemStack leftInput = input.getStack(0);
            ItemStack rightInput = input.getStack(1);
            
            if (leftInput.isEmpty() && rightInput.isEmpty()) return;
            
            // Get context using reflection
            ScreenHandlerContext context = theendupdate$getContext(self);
            if (context == null) return;
            
            // Check if we're trying to apply chopping to a non-axe
            boolean[] shouldCancel = {false};
            context.run((world, pos) -> {
                try {
                    var enchantmentRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                    Identifier choppingId = Identifier.of("theendupdate", "chopping");
                    RegistryEntry<Enchantment> choppingEnchantment = enchantmentRegistry.getEntry(choppingId).orElse(null);
                    
                    if (choppingEnchantment == null) return;
                    
                    // Check if the right input (book) has chopping
                    boolean rightHasChopping = false;
                    if (rightInput.isOf(net.minecraft.item.Items.ENCHANTED_BOOK)) {
                        ItemEnchantmentsComponent rightEnch = rightInput.get(DataComponentTypes.STORED_ENCHANTMENTS);
                        if (rightEnch != null && rightEnch.getLevel(choppingEnchantment) > 0) {
                            rightHasChopping = true;
                        }
                    }
                    
                    // Check if the left input has chopping (for combining enchanted items)
                    boolean leftHasChopping = false;
                    if (!leftInput.isEmpty()) {
                        ItemEnchantmentsComponent leftEnch = leftInput.get(DataComponentTypes.ENCHANTMENTS);
                        if (leftEnch != null && leftEnch.getLevel(choppingEnchantment) > 0) {
                            leftHasChopping = true;
                        }
                    }
                    
                    // If we're trying to add chopping to something, check if it's an axe
                    if ((rightHasChopping || leftHasChopping) && !leftInput.isEmpty()) {
                        if (!leftInput.isIn(net.minecraft.registry.tag.ItemTags.AXES)) {
                            // Invalid combination - cancel the operation
                            shouldCancel[0] = true;
                        }
                    }
                } catch (Exception ignored) {}
            });
            
            if (shouldCancel[0]) {
                // Cancel the operation by clearing the output and canceling the injection
                Inventory output = theendupdate$getField(self, "output", Inventory.class);
                if (output != null) {
                    output.setStack(0, ItemStack.EMPTY);
                }
                ci.cancel();
            }
        } catch (Exception ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    @Unique
    private static ScreenHandlerContext theendupdate$getContext(AnvilScreenHandler self) {
        try {
            Class<?> c = self.getClass();
            while (c != null) {
                for (String name : new String[] { "context", "field_17639" }) {
                    try {
                        Field f = c.getDeclaredField(name);
                        f.setAccessible(true);
                        Object val = f.get(self);
                        if (val instanceof ScreenHandlerContext ctx) return ctx;
                    } catch (NoSuchFieldException ignored) { }
                }
                for (Field f : c.getDeclaredFields()) {
                    if (ScreenHandlerContext.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(self);
                        if (val instanceof ScreenHandlerContext ctx) return ctx;
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

