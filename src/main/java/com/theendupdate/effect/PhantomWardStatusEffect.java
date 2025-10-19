package com.theendupdate.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Phantom Ward - A status effect that repels regular phantoms
 * (but not King Phantoms)
 */
public class PhantomWardStatusEffect extends StatusEffect {
    public PhantomWardStatusEffect() {
        super(
            StatusEffectCategory.BENEFICIAL,
            0x8B0000 // Deep blood red color (RGB: 139, 0, 0)
        );
    }

    // No update logic needed - the repelling is handled in phantom AI
    // Modern versions of Minecraft don't require these methods
}

