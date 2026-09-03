package com.theendupdate.mixin;

import com.theendupdate.world.OuterEndBiomes;
import com.theendupdate.world.OuterEndLayout;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces Shadowlands continents and whole-cell Mirelands biome paint.
 */
@Mixin(value = TheEndBiomeSource.class, priority = 2000)
public class TheEndBiomeSourceMixin {
    @Inject(method = "create", at = @At("TAIL"))
    private static void theendupdate$captureBiomes(
        HolderGetter<Biome> biomes,
        CallbackInfoReturnable<TheEndBiomeSource> cir
    ) {
        OuterEndBiomes.init(biomes);
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void theendupdate$addCustomBiomes(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), OuterEndBiomes.customBiomes()));
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true, order = 10000)
    private void theendupdate$shadowlandsContinents(
        int biomeX,
        int biomeY,
        int biomeZ,
        Climate.Sampler sampler,
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
}
