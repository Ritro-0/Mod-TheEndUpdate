package com.theendupdate.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Repels regular phantoms, but not King Phantoms.
 */
public class PhantomWardStatusEffect extends MobEffect {
    public PhantomWardStatusEffect() {
        super(
            MobEffectCategory.BENEFICIAL,
            0x8B0000 // deep blood red
        );
    }

    // repelling logic lives in phantom AI, nothing to tick here
}

