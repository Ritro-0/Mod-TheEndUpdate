package com.theendupdate.mixin;

import com.theendupdate.accessor.CowEntityAnimationAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MushroomCow.class)
public abstract class MooshroomEntityMixin implements CowEntityAnimationAccessor {
    
    @Unique
    private static final EntityDataAccessor<Long> theendupdate$ANIMATION_START_TIME = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.LONG);
    
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void theendupdate$initAnimationData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(theendupdate$ANIMATION_START_TIME, 0L);
    }
    
    @Override
    public long theendupdate$getAnimationStartTime() {
        MushroomCow self = (MushroomCow) (Object) this;
        return self.getEntityData().get(theendupdate$ANIMATION_START_TIME);
    }
    
    @Override
    public void theendupdate$setAnimationStartTime(long time) {
        MushroomCow self = (MushroomCow) (Object) this;
        self.getEntityData().set(theendupdate$ANIMATION_START_TIME, time);
    }
}

