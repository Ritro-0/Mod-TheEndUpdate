package com.theendupdate.mixin;

import com.theendupdate.TemplateMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(SmithingScreenHandler.class)
public abstract class SmithingForceTrimMixin {

	@Inject(method = "updateResult", at = @At("TAIL"))
	private void theendupdate$forceVoidstarTrim(CallbackInfo ci) {
		SmithingScreenHandler self = (SmithingScreenHandler) (Object) this;
		try {
			ItemStack template = self.getSlot(0).getStack();
			ItemStack base = self.getSlot(1).getStack();
			ItemStack addition = self.getSlot(2).getStack();

			if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
				return;
			}

			TagKey<Item> TAG_TRIMMABLE_ARMOR = TagKey.of(RegistryKeys.ITEM, Identifier.of("minecraft", "trimmable_armor"));
			TagKey<Item> TAG_TRIM_TEMPLATES = TagKey.of(RegistryKeys.ITEM, Identifier.of("minecraft", "trim_templates"));

			boolean baseTrimmable = base.isIn(TAG_TRIMMABLE_ARMOR);
			boolean templateOk = template.isIn(TAG_TRIM_TEMPLATES);
			Identifier addId = Registries.ITEM.getId(addition.getItem());
			boolean isVoidstarAddition = addId.equals(Identifier.of("theendupdate", "voidstar_ingot"));
			boolean isSpectralAddition = addId.equals(Identifier.of("theendupdate", "spectral_debris")) || addId.equals(Identifier.of("theendupdate", "spectral_cluster"));
			boolean isGravititeAddition = addId.equals(Identifier.of("theendupdate", "pure_gravitite"));
			boolean isTardShellAddition = addId.equals(Identifier.of("theendupdate", "tard_shell_brick"));
			// Removed debug logging
			if (!baseTrimmable || !templateOk || !(isVoidstarAddition || isSpectralAddition || isGravititeAddition || isTardShellAddition)) {
				return;
			}

			ItemStack out = self.getSlot(3).getStack();
			if (!out.isEmpty() && out.get(DataComponentTypes.TRIM) != null) {
				// Removed debug logging
				return;
			}

			ScreenHandlerContext ctx = resolveContext(self);
			if (ctx == null) {
				return;
			}

			ctx.run((world, pos) -> {
				var patterns = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
				var materials = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);

				Identifier templateId = Registries.ITEM.getId(template.getItem());
				String path = templateId.getPath();
				int cut = path.indexOf("_armor_trim_smithing_template");
				if (cut <= 0) {
					return;
				}
				Identifier patternId = Identifier.of(templateId.getNamespace(), path.substring(0, cut));
				var optPattern = patterns.getEntry(patternId);
				if (optPattern.isEmpty()) {
					return;
				}

				Identifier materialId =
					isVoidstarAddition ? Identifier.of("theendupdate", "voidstar") :
					(isGravititeAddition ? Identifier.of("theendupdate", "gravitite") :
					(isTardShellAddition ? Identifier.of("theendupdate", "tard_shell") :
					(addId.getPath().equals("spectral_cluster") ? Identifier.of("theendupdate", "spectral_cluster") : Identifier.of("theendupdate", "spectral"))));
				var optMaterial = materials.getEntry(materialId);
				if (optMaterial.isEmpty()) {
					return;
				}

				RegistryEntry<ArmorTrimPattern> pattern = optPattern.get();
				RegistryEntry<ArmorTrimMaterial> material = optMaterial.get();

				ItemStack result = base.copy();
				result.setCount(1);
				result.set(DataComponentTypes.TRIM, new ArmorTrim(material, pattern));
				// Ensure model override predicate triggers: set TRIM_TYPE via reflection (mapping-safe)
				try {
					float modelIndex;
					modelIndex = 0.1f; // unify predicate for spectral + voidstar + gravitite
					Class<?> dct = Class.forName("net.minecraft.component.DataComponentTypes");
					Object trimType = dct.getField("TRIM_TYPE").get(null);
					var set = ItemStack.class.getMethod("set", Class.forName("net.minecraft.component.DataComponentType"), Object.class);
					set.invoke(result, trimType, Float.valueOf(modelIndex));
					// Removed debug logging
				} catch (Throwable tt) {
					TemplateMod.LOGGER.info("[SmithingForce] Unable to set TRIM_TYPE reflectively: {}", tt.toString());
				}
				self.getSlot(3).setStack(result);
				// Removed debug logging
			});
		} catch (Throwable t) {
		}
	}

	@Unique
	private static ScreenHandlerContext resolveContext(SmithingScreenHandler self) {
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
}


