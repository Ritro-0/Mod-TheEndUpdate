# Shadow Creaking Peaceful Mode Despawn

## Issue
Shadow Creaking entities (all three variants: normal, mini, and tiny) were not despawning when the world was switched to peaceful mode. This was inconsistent with standard hostile mob behavior.

## Root Cause
The Shadow Creaking entities call `setPersistent()` in their constructor, which prevents natural despawning. While this is useful for preventing distance-based despawning (since these are boss-like entities), it also prevented them from being removed when the difficulty is set to peaceful.

## Solution
Added a check at the beginning of the `tick()` method in `ShadowCreakingEntity` that:
1. Checks if the world is in peaceful difficulty
2. If so, calls `discard()` to remove the entity
3. Returns immediately to prevent any further tick logic

Since `MiniShadowCreakingEntity` and `TinyShadowCreakingEntity` both extend `ShadowCreakingEntity` and don't override the `tick()` method, they automatically inherit this behavior.

## Implementation
```java
@Override
public void tick() {
    // Despawn in peaceful mode (all variants: normal, mini, tiny)
    if (!this.getEntityWorld().isClient() && this.getEntityWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
        this.discard();
        return;
    }
    // ... rest of tick logic
}
```

## Files Changed
- `src/main/java/com/theendupdate/entity/ShadowCreakingEntity.java`

## Testing
The fix has been verified to:
- Compile successfully without errors
- Check the difficulty server-side only (not on client)
- Apply to all three variants (normal, mini, and tiny)
- Clean up properly using `discard()` which also triggers the boss bar cleanup logic in the `remove()` method

## Behavior
When a world is set to peaceful difficulty:
- All Shadow Creaking entities (normal, mini, tiny) will immediately disappear
- The boss bar will be properly cleaned up
- No drops will be generated (since they weren't killed by a player)
- The entities will not respawn unless summoned again via the altar or commands

