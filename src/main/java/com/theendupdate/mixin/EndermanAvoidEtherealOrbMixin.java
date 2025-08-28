package com.theendupdate.mixin;

import com.theendupdate.entity.EtherealOrbEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntity.class)
public class EndermanAvoidEtherealOrbMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void theendupdate$avoidEtherealOrb(CallbackInfo ci) {
        EndermanEntity self = (EndermanEntity)(Object)this;
        GoalSelector selector = ((MobEntityAccessor) self).theendupdate$getGoalSelector();
        selector.add(2, new FleeEntityGoal<>(self, EtherealOrbEntity.class, 12.0f, 1.0, 1.25));
    }
}


