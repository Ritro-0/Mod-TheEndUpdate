package com.theendupdate.mixin;

import com.theendupdate.entity.goal.AvoidEnderChrysanthemumGoal;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderEntity.class)
public abstract class SpiderEntityAvoidChrysanthemumMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void theendupdate$addEnderChrysanthemumAvoidGoal(CallbackInfo ci) {
        // Mirror piglin repellent behavior: avoid radius ~7 blocks
        ((MobEntityAccessor) (Object) this).theendupdate$getGoalSelector()
            .add(1, new AvoidEnderChrysanthemumGoal((PathAwareEntity) (Object) this, 7, 1.1));
    }
}


