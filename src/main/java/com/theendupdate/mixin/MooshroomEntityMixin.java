package com.theendupdate.mixin;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.MooshroomEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MooshroomEntity.class)
public abstract class MooshroomEntityMixin implements CowEntityAnimationAccessor {
    
    @Unique
    private static final TrackedData<Long> theendupdate$ANIMATION_START_TIME = DataTracker.registerData(MooshroomEntity.class, TrackedDataHandlerRegistry.LONG);
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void theendupdate$initAnimationData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(theendupdate$ANIMATION_START_TIME, 0L);
    }
    
    @Override
    public long theendupdate$getAnimationStartTime() {
        MooshroomEntity self = (MooshroomEntity) (Object) this;
        return self.getDataTracker().get(theendupdate$ANIMATION_START_TIME);
    }
    
    @Override
    public void theendupdate$setAnimationStartTime(long time) {
        MooshroomEntity self = (MooshroomEntity) (Object) this;
        self.getDataTracker().set(theendupdate$ANIMATION_START_TIME, time);
    }
}

