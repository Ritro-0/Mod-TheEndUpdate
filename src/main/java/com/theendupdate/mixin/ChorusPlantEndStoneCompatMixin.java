package com.theendupdate.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.theendupdate.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("target")
@Mixin(ChorusPlantBlock.class)
public class ChorusPlantEndStoneCompatMixin {

    @WrapOperation(method = "canPlaceAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean theendupdate$acceptEndMireAndMoldForEndStone_canPlaceAt(BlockState instance, Block block, Operation<Boolean> original) {
        boolean vanilla = original.call(instance, block);
        if (vanilla) return true;
        if (block == Blocks.END_STONE) {
            return instance.isOf(ModBlocks.END_MIRE) || instance.isOf(ModBlocks.MOLD_BLOCK);
        }
        return false;
    }

    @WrapOperation(method = "getStateForNeighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private boolean theendupdate$acceptEndMireAndMoldForEndStone_getStateForNeighborUpdate(BlockState instance, Block block, Operation<Boolean> original) {
        boolean vanilla = original.call(instance, block);
        if (vanilla) return true;
        if (block == Blocks.END_STONE) {
            return instance.isOf(ModBlocks.END_MIRE) || instance.isOf(ModBlocks.MOLD_BLOCK);
        }
        return false;
    }

    
}


