package com.theendupdate.mixin;

import com.theendupdate.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackTrimMaterialsMixin {

	@Inject(method = "isIn(Lnet/minecraft/registry/tag/TagKey;)Z", at = @At("HEAD"), cancellable = true)
	private void theendupdate$acceptVoidstarInTrimMaterials(TagKey<?> tag, CallbackInfoReturnable<Boolean> cir) {
		try {
			Identifier tagId = ((TagKey<?>) tag).id();
			// Accept any armor trim template as being in trim_templates to ensure template slot validates
			if ("minecraft".equals(tagId.getNamespace()) && "trim_templates".equals(tagId.getPath())) {
				ItemStack self = (ItemStack) (Object) this;
				Identifier selfId = net.minecraft.registry.Registries.ITEM.getId(self.getItem());
				if (selfId != null && selfId.getPath().endsWith("_armor_trim_smithing_template")) {
					cir.setReturnValue(true);
				}
			}
		} catch (Throwable ignored) {}
	}
}


