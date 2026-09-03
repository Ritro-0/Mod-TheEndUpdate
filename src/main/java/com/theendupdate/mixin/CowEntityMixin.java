package com.theendupdate.mixin;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.cow.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Cow.class)
public abstract class CowEntityMixin implements CowEntityAnimationAccessor {
    
    @Unique
    private static final EntityDataAccessor<Long> theendupdate$ANIMATION_START_TIME = SynchedEntityData.defineId(Cow.class, EntityDataSerializers.LONG);
    
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void theendupdate$initAnimationData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(theendupdate$ANIMATION_START_TIME, 0L);
    }
    
    @Override
    public long theendupdate$getAnimationStartTime() {
        Cow self = (Cow) (Object) this;
        return self.getEntityData().get(theendupdate$ANIMATION_START_TIME);
    }
    
    @Override
    public void theendupdate$setAnimationStartTime(long time) {
        Cow self = (Cow) (Object) this;
        self.getEntityData().set(theendupdate$ANIMATION_START_TIME, time);
    }
}

