package com.theendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerEndStoneCompatMixin {

    @WrapOperation(
        method = {"canPlaceAt", "randomTick"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z")
    )
    private boolean theendupdate$acceptEndMireAndMoldForEndStone(BlockState instance, Block block, Operation<Boolean> original) {
        boolean vanilla = original.call(instance, block);
        if (vanilla) return true;

        if (block == Blocks.END_STONE) {
            return instance.isOf(ModBlocks.END_MIRE) || instance.isOf(ModBlocks.MOLD_BLOCK);
        }
        return false;
    }
}


