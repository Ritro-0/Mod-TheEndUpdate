package com.theendupdate.mixin;

import com.theendupdate.entity.goal.AvoidEnderChrysanthemumGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Spider.class)
public abstract class SpiderEntityAvoidChrysanthemumMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void theendupdate$addEnderChrysanthemumAvoidGoal(CallbackInfo ci) {
        // Mirror piglin repellent behavior: avoid radius ~7 blocks
        ((MobEntityAccessor) (Object) this).theendupdate$getGoalSelector()
            .addGoal(1, new AvoidEnderChrysanthemumGoal((PathfinderMob) (Object) this, 7, 1.1));
    }
}


