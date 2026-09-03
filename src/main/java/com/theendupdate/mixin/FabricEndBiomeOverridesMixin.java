package com.theendupdate.mixin;

import com.theendupdate.registry.ModWorldgen;
import com.theendupdate.world.OuterEndBiomes;
import com.theendupdate.world.OuterEndLayout;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces our biome cells, and strips Fabric's tiny leftover Mirelands slivers.
 */
@Mixin(targets = "net.fabricmc.fabric.impl.biome.TheEndBiomeData$Overrides", priority = 2000)
public class FabricEndBiomeOverridesMixin {
    @Inject(
        method = "pick(IIILnet/minecraft/world/level/biome/Climate$Sampler;Lnet/minecraft/core/Holder;)Lnet/minecraft/core/Holder;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void theendupdate$protectShadowlands(
        int biomeX,
        int biomeY,
        int biomeZ,
        Climate.Sampler sampler,
        Holder<Biome> vanillaBiome,
        CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        int blockX = QuartPos.toBlock(biomeX);
        int blockZ = QuartPos.toBlock(biomeZ);
        if (!OuterEndLayout.isShadowlands(blockX, blockZ) && !OuterEndLayout.isMirelands(blockX, blockZ)) {
            return;
        }
        Holder<Biome> biome = OuterEndBiomes.biomeAt(biomeX, biomeY, biomeZ, sampler);
        if (biome != null) {
            cir.setReturnValue(biome);
        }
    }

    @Inject(
        method = "pick(IIILnet/minecraft/world/level/biome/Climate$Sampler;Lnet/minecraft/core/Holder;)Lnet/minecraft/core/Holder;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void theendupdate$stripMireSlivers(
        int biomeX,
        int biomeY,
        int biomeZ,
        Climate.Sampler sampler,
        Holder<Biome> vanillaBiome,
        CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        int blockX = QuartPos.toBlock(biomeX);
        int blockZ = QuartPos.toBlock(biomeZ);
        if (OuterEndLayout.isMirelands(blockX, blockZ) || OuterEndLayout.isShadowlands(blockX, blockZ)) {
            return;
        }
        Holder<Biome> picked = cir.getReturnValue();
        if (picked == null) {
            return;
        }
        if (picked.is(ModWorldgen.MIRELANDS_HIGHLANDS_KEY)
            || picked.is(ModWorldgen.MIRELANDS_MIDLANDS_KEY)
            || picked.is(ModWorldgen.MIRELANDS_BARRENS_KEY)) {
            cir.setReturnValue(vanillaBiome);
        }
    }
}
