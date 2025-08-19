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
			TemplateMod.LOGGER.info("[SmithingForce] precheck baseTrimmable={} templateOk={} addition={}", baseTrimmable, templateOk, addId);
			if (!baseTrimmable || !templateOk || !isVoidstarAddition) {
				return;
			}

			ItemStack out = self.getSlot(3).getStack();
			if (!out.isEmpty() && out.get(DataComponentTypes.TRIM) != null) {
				TemplateMod.LOGGER.info("[SmithingForce] Result already has TRIM; skipping");
				return;
			}

			ScreenHandlerContext ctx = resolveContext(self);
			if (ctx == null) {
				TemplateMod.LOGGER.info("[SmithingForce] Unable to resolve ScreenHandlerContext; cannot apply forced trim");
				return;
			}

			ctx.run((world, pos) -> {
				var patterns = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_PATTERN);
				var materials = world.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL);

				Identifier templateId = Registries.ITEM.getId(template.getItem());
				String path = templateId.getPath();
				int cut = path.indexOf("_armor_trim_smithing_template");
				if (cut <= 0) {
					TemplateMod.LOGGER.info("[SmithingForce] Could not derive pattern id from template {}", templateId);
					return;
				}
				Identifier patternId = Identifier.of(templateId.getNamespace(), path.substring(0, cut));
				var optPattern = patterns.getEntry(patternId);
				if (optPattern.isEmpty()) {
					TemplateMod.LOGGER.info("[SmithingForce] Pattern {} not found in TRIM_PATTERN registry", patternId);
					return;
				}

				var optMaterial = materials.getEntry(Identifier.of("theendupdate", "voidstar"));
				if (optMaterial.isEmpty()) {
					TemplateMod.LOGGER.info("[SmithingForce] Trim material theendupdate:voidstar not found in TRIM_MATERIAL registry");
					return;
				}

				RegistryEntry<ArmorTrimPattern> pattern = optPattern.get();
				RegistryEntry<ArmorTrimMaterial> material = optMaterial.get();

				ItemStack result = base.copy();
				result.setCount(1);
				result.set(DataComponentTypes.TRIM, new ArmorTrim(material, pattern));
				// Attempt to also set TRIM_TYPE (item model index) so item model overrides trigger reliably
				try {
					float modelIndex = -1f;
					try {
						ArmorTrimMaterial matVal = material.value();
						var m = matVal.getClass().getMethod("itemModelIndex");
						Object idx = m.invoke(matVal);
						if (idx instanceof Number n) {
							modelIndex = n.floatValue();
						}
					} catch (Throwable ignored) { }
					// Fallback for 1.21.8: use known value from data/theendupdate/trim_material/voidstar.json
					if (modelIndex < 0f) {
						modelIndex = 0.1f;
					}
					try {
						Class<?> dct = Class.forName("net.minecraft.component.DataComponentTypes");
						Object trimType = dct.getField("TRIM_TYPE").get(null);
						var set = ItemStack.class.getMethod("set", Class.forName("net.minecraft.component.DataComponentType"), Object.class);
						set.invoke(result, trimType, Float.valueOf(modelIndex));
						TemplateMod.LOGGER.info("[SmithingForce] Set TRIM_TYPE component to {}", modelIndex);
					} catch (Throwable tt) {
						TemplateMod.LOGGER.info("[SmithingForce] Unable to set TRIM_TYPE via reflection: {}", tt.toString());
					}
				} catch (Throwable ignore) { }
				self.getSlot(3).setStack(result);
				TemplateMod.LOGGER.info("[SmithingForce] Applied TRIM result pattern={} material=theendupdate:voidstar for base={} ", patternId, Registries.ITEM.getId(base.getItem()));
			});
		} catch (Throwable t) {
			TemplateMod.LOGGER.warn("[SmithingForce] Error attempting to force-apply trim", t);
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


