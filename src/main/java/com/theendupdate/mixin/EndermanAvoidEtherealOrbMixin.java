package com.theendupdate.mixin;

import com.theendupdate.entity.EtherealOrbEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public class EndermanAvoidEtherealOrbMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void theendupdate$avoidEtherealOrb(CallbackInfo ci) {
        EnderMan self = (EnderMan)(Object)this;
        GoalSelector selector = ((MobEntityAccessor) self).theendupdate$getGoalSelector();
        selector.addGoal(2, new AvoidEntityGoal<>(self, EtherealOrbEntity.class, 12.0f, 1.0, 1.25));
    }
}


