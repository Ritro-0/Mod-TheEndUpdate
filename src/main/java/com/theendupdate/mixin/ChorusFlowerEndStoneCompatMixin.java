package com.theendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerEndStoneCompatMixin {

    @WrapOperation(
        method = {"canSurvive", "randomTick"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean theendupdate$acceptEndMireAndMoldForChorusFlowerSupport(
        BlockState instance,
        TagKey<?> tag,
        Operation<Boolean> original
    ) {
        boolean vanilla = original.call(instance, tag);
        if (vanilla || tag != BlockTags.SUPPORTS_CHORUS_FLOWER) {
            return vanilla;
        }
        return instance.is(ModBlocks.END_MIRE) || instance.is(ModBlocks.MOLD_BLOCK);
    }
}
