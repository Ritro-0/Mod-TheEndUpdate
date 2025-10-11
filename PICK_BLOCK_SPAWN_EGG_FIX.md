# Pick Block Spawn Egg Fix

## Issue
When using middle-click (pick block) on Shadow Creaking entities (all three variants: normal, mini, tiny) or Ethereal Orb entities in creative mode, the spawn egg was not being added to the player's inventory.

## Root Cause
The entities did not override the `getPickBlockStack()` method, which is called by Minecraft when a player middle-clicks an entity in creative mode. Without this override, the game has no way of knowing which item to give the player.

## Solution
Added the `getPickBlockStack()` method override to all custom entities:

### Shadow Creaking Entities
- **ShadowCreakingEntity**: Returns `SHADOW_CREAKING_SPAWN_EGG`
- **MiniShadowCreakingEntity**: Returns `MINI_SHADOW_CREAKING_SPAWN_EGG`  
- **TinyShadowCreakingEntity**: Returns `TINY_SHADOW_CREAKING_SPAWN_EGG`

### Ethereal Orb
- **EtherealOrbEntity**: Returns `ETHEREAL_ORB_SPAWN_EGG`

## Implementation
Each entity now includes:
```java
@Override
public ItemStack getPickBlockStack() {
    // Return the appropriate spawn egg when middle-clicked in creative mode
    return new ItemStack(com.theendupdate.registry.ModItems.XXX_SPAWN_EGG);
}
```

## Files Changed
- `src/main/java/com/theendupdate/entity/ShadowCreakingEntity.java`
- `src/main/java/com/theendupdate/entity/MiniShadowCreakingEntity.java`
- `src/main/java/com/theendupdate/entity/TinyShadowCreakingEntity.java`
- `src/main/java/com/theendupdate/entity/EtherealOrbEntity.java`

## Testing
The fix has been verified to:
- Compile successfully without errors
- Properly return the corresponding spawn egg for each entity variant
- Work in creative mode when middle-clicking entities

## Behavior
When you middle-click (pick block) on any of these entities in creative mode:
- The cursor will now switch to the entity's spawn egg
- If you have an empty hotbar slot, the spawn egg will be added to your inventory
- If you already have the spawn egg in your hotbar, it will switch to that slot
- This matches vanilla Minecraft behavior for all mob spawn eggs

