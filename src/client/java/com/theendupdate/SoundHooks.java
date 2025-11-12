package com.theendupdate;

// Sound hooks are now handled via SoundManagerMixin instead of non-existent PlaySoundCallback
// This file is kept for potential future use but the functionality is in the mixin

public final class SoundHooks {
    private SoundHooks() {}

    public static void register() {
        // Empty - functionality moved to SoundManagerMixin
    }
}


