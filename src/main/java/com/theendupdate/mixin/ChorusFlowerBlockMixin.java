package com.theendupdate.mixin;

import com.theendupdate.world.VoidBloomChorusGrowthFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerBlockMixin {
    
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void onChorusFlowerRandomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // Suppress blooms inside Shadowlands
        if (com.theendupdate.world.ShadowlandsRegion.isInRegion(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        VoidBloomChorusGrowthFeature.tryGrow(world, pos, random);
    }
}
