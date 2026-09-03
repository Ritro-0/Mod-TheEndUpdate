package com.theendupdate.mixin;

import com.theendupdate.TheEndUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

@Mixin(SmithingMenu.class)
public abstract class SmithingForceTrimMixin {

	@Inject(method = "createResult", at = @At("TAIL"))
	private void theendupdate$forceVoidstarTrim(CallbackInfo ci) {
		SmithingMenu self = (SmithingMenu) (Object) this;
		try {
			ItemStack template = self.getSlot(0).getItem();
			ItemStack base = self.getSlot(1).getItem();
			ItemStack addition = self.getSlot(2).getItem();

			if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
				return;
			}

			TagKey<Item> TAG_TRIMMABLE_ARMOR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "trimmable_armor"));
			TagKey<Item> TAG_TRIM_TEMPLATES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "trim_templates"));

			boolean baseTrimmable = base.is(TAG_TRIMMABLE_ARMOR);
			boolean templateOk = template.is(TAG_TRIM_TEMPLATES);
			Identifier addId = BuiltInRegistries.ITEM.getKey(addition.getItem());
			boolean isVoidstarAddition = addId.equals(Identifier.fromNamespaceAndPath("theendupdate", "voidstar_ingot"));
			boolean isSpectralAddition = addId.equals(Identifier.fromNamespaceAndPath("theendupdate", "spectral_debris")) || addId.equals(Identifier.fromNamespaceAndPath("theendupdate", "spectral_cluster"));
			boolean isGravititeAddition = addId.equals(Identifier.fromNamespaceAndPath("theendupdate", "pure_gravitite"));
			boolean isTardigradeShellAddition = addId.equals(Identifier.fromNamespaceAndPath("theendupdate", "tardigrade_shell_brick"));
			if (!baseTrimmable || !templateOk || !(isVoidstarAddition || isSpectralAddition || isGravititeAddition || isTardigradeShellAddition)) {
				return;
			}

			ItemStack out = self.getSlot(3).getItem();
			if (!out.isEmpty() && out.get(DataComponents.TRIM) != null) {
				return;
			}

			ContainerLevelAccess ctx = resolveContext(self);
			if (ctx == null) {
				return;
			}

			ctx.execute((world, pos) -> {
				var patterns = world.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
				var materials = world.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);

				Identifier templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
				String path = templateId.getPath();
				int cut = path.indexOf("_armor_trim_smithing_template");
				if (cut <= 0) {
					return;
				}
				Identifier patternId = Identifier.fromNamespaceAndPath(templateId.getNamespace(), path.substring(0, cut));
				var optPattern = patterns.get(patternId);
				if (optPattern.isEmpty()) {
					return;
				}

				Identifier materialId =
					isVoidstarAddition ? Identifier.fromNamespaceAndPath("theendupdate", "voidstar") :
					(isGravititeAddition ? Identifier.fromNamespaceAndPath("theendupdate", "gravitite") :
					(isTardigradeShellAddition ? Identifier.fromNamespaceAndPath("theendupdate", "tardigrade_shell") :
					(addId.getPath().equals("spectral_cluster") ? Identifier.fromNamespaceAndPath("theendupdate", "spectral_cluster") : Identifier.fromNamespaceAndPath("theendupdate", "spectral"))));
				var optMaterial = materials.get(materialId);
				if (optMaterial.isEmpty()) {
					return;
				}

				Holder<TrimPattern> pattern = optPattern.get();
				Holder<TrimMaterial> material = optMaterial.get();

				ItemStack result = base.copy();
				result.setCount(1);
				result.set(DataComponents.TRIM, new ArmorTrim(material, pattern));
				// set TRIM_TYPE via reflection so the model override predicate triggers (mapping-safe)
				try {
					float modelIndex;
					modelIndex = 0.1f; // shared predicate value for spectral/voidstar/gravitite
					Class<?> dct = Class.forName("net.minecraft.component.DataComponentTypes");
					Object trimType = dct.getField("TRIM_TYPE").get(null);
					var set = ItemStack.class.getMethod("set", Class.forName("net.minecraft.component.DataComponentType"), Object.class);
					set.invoke(result, trimType, Float.valueOf(modelIndex));
				} catch (Throwable tt) {
					TheEndUpdate.LOGGER.info("[SmithingForce] Unable to set TRIM_TYPE reflectively: {}", tt.toString());
				}
				self.getSlot(3).setByPlayer(result);
			});
		} catch (Throwable t) {
		}
	}

	@Unique
	private static ContainerLevelAccess resolveContext(SmithingMenu self) {
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
}


