package com.theendupdate.mixin;

import com.theendupdate.world.VoidBloomChorusGrowthFeature;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChorusFlowerBlock.class)
public class ChorusFlowerBlockMixin {
    
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void onChorusFlowerRandomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        // Try to grow a Void Bloom when chorus flower ticks (indicating it's active/mature)
        VoidBloomChorusGrowthFeature.tryGrow(world, pos, random);
    }
}
