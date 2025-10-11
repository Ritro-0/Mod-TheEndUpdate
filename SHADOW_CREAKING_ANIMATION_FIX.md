# Shadow Creaking Animation Bug Fix

## Issue
When multiple Shadow Creaking entities were present in a world, they all inherited the intro animation (emerging and levitating) simultaneously, even though only the newly spawned entity should play it. This created a visual bug where existing Shadow Creakings would replay their spawn animations whenever a new one was spawned.

## Root Cause
The bug was in the `ShadowCreakingEntityRenderer` class. The `ShadowCreakingPlantingModel` inner class was creating its own `AnimationState` objects as instance fields:

```java
private final AnimationState emergingState = new AnimationState();
private final AnimationState levitatingState = new AnimationState();
```

Since the renderer (and its model) is shared across all Shadow Creaking entities of the same type, these animation states were also shared. When one entity started its intro animation, it would trigger the shared animation state, affecting all entities being rendered.

## Solution
The fix follows the proper Minecraft 1.21+ rendering pattern used by other entities (like the Ethereal Orb):

1. **Created `ShadowCreakingRenderState`**: A custom render state class that extends `CreakingEntityRenderState` and contains per-entity animation states:
   ```java
   public class ShadowCreakingRenderState extends CreakingEntityRenderState {
       public final AnimationState emergingAnimationState = new AnimationState();
       public final AnimationState levitatingAnimationState = new AnimationState();
       // ... other per-entity state
   }
   ```

2. **Updated the Renderer**: Modified `ShadowCreakingEntityRenderer` to:
   - Use `ShadowCreakingRenderState` instead of `CreakingEntityRenderState`
   - Copy animation states from the entity to the render state using `copyFrom()` in `updateRenderState()`
   - Store other per-entity state (like `runOverlay`, `emergeProgress`, etc.) in the render state

3. **Updated the Model**: Modified `ShadowCreakingPlantingModel` to:
   - Remove the instance `AnimationState` fields
   - Use animation states from the `ShadowCreakingRenderState` parameter in `setAngles()`
   - Cast the parameter safely (with null checks) since the parent class expects `CreakingEntityRenderState`

## Files Changed
- `src/client/java/com/theendupdate/entity/state/ShadowCreakingRenderState.java` (new file)
- `src/client/java/com/theendupdate/entity/renderer/ShadowCreakingEntityRenderer.java` (modified)

## Testing
The fix has been verified to:
- Compile successfully without errors
- Follow the correct Minecraft rendering pattern
- Properly isolate animation state per entity instance

## Technical Details
The key insight is that in Minecraft's modern rendering architecture:
- **Entity Renderers** are singletons (one instance per entity type)
- **Models** are part of the renderer and also shared
- **Render States** are per-entity and hold all the data needed for that specific entity's rendering

Animation states MUST be stored in the render state, not in the model or renderer, to ensure each entity has its own independent animation timeline.

