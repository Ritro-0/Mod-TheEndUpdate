# EntityRenderState Guide for Ethereal Orb

## What is EntityRenderState?

In Minecraft 1.21.8, `EntityRenderState` is a new system that manages the rendering state of entities. It acts as a bridge between the entity's game state and the renderer, allowing for:

- **Smooth animations** with proper interpolation
- **State-based rendering** (damage, charging, etc.)
- **Performance optimization** by caching render data
- **Clean separation** between game logic and rendering

## Files Created

### 1. **EtherealOrbRenderState.java**
This is your custom render state class that extends `EntityRenderState`. It contains:

- **Animation state**: Time, pulse intensity, glow intensity
- **Entity state**: Damage status, charging status, movement speed
- **Calculated values**: Rotation speed, scale multipliers, alpha values
- **Helper methods**: For getting various render properties

### 2. **Updated Renderer**
The renderer now uses your custom state instead of the generic `LivingEntityRenderState`.

### 3. **Updated Model**
The model now receives your custom state and can access all the animation data.

## How It Works

### 1. **State Creation**
```java
@Override
public EtherealOrbRenderState createRenderState() {
    return new EtherealOrbRenderState();
}
```

### 2. **State Updates**
```java
@Override
public void updateRenderState(EtherealOrbEntity entity, EtherealOrbRenderState state, float tickDelta) {
    super.updateRenderState(entity, state, tickDelta);
    
    // Update our custom render state with entity information
    state.updateFromEntity(entity, tickDelta);
}
```

### 3. **State Usage in Renderer**
```java
@Override
protected void setupTransforms(EtherealOrbRenderState state, MatrixStack matrices, float animationProgress, float bodyYaw) {
    super.setupTransforms(state, matrices, animationProgress, bodyYaw);
    
    // Use state data for animations
    float bob = (float) Math.sin(state.getAnimationTime() * 0.1) * 0.1f;
    float rotation = state.getAnimationTime() * 2.0f * state.getRotationSpeedMultiplier();
    float scale = state.getScaleMultiplier();
    
    matrices.translate(0, bob, 0);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
    matrices.scale(scale, scale, scale);
}
```

### 4. **State Usage in Model**
```java
@Override
public void setAngles(EtherealOrbRenderState state) {
    float animationTime = state.getAnimationTime();
    float speedMultiplier = state.getRotationSpeedMultiplier();
    
    // Apply animations based on state
    this.core.yaw = animationTime * 0.02f * speedMultiplier;
    
    // Apply damage effects
    if (state.isDamaged()) {
        this.core.roll = (float) Math.sin(animationTime * 0.8) * 0.1f;
    }
}
```

## Key Benefits

### **Performance**
- Render state is cached between frames
- Only updates when entity state changes
- Smooth interpolation for animations

### **Flexibility**
- Easy to add new animation states
- Clean separation of concerns
- Reusable across different renderers

### **Maintainability**
- All render logic in one place
- Easy to debug and modify
- Clear data flow

## Adding New States

### 1. **Add State Variable**
```java
private boolean isFlying = false;
private float flightHeight = 0.0f;
```

### 2. **Add Getter Method**
```java
public boolean isFlying() {
    return isFlying;
}

public float getFlightHeight() {
    return flightHeight;
}
```

### 3. **Update in updateFromEntity**
```java
public void updateFromEntity(EtherealOrbEntity entity, float tickDelta) {
    // ... existing code ...
    
    // Update flight state
    this.isFlying = entity.isOnGround() == false;
    this.flightHeight = (float) entity.getY();
}
```

### 4. **Use in Renderer/Model**
```java
if (state.isFlying()) {
    float heightOffset = state.getFlightHeight() * 0.1f;
    matrices.translate(0, heightOffset, 0);
}
```

## Advanced Features

### **State Transitions**
```java
// Smooth transition between states
private float targetPulseIntensity = 0.0f;

public void setTargetPulseIntensity(float target) {
    this.targetPulseIntensity = target;
}

public void updatePulseIntensity() {
    float diff = targetPulseIntensity - pulseIntensity;
    this.pulseIntensity += diff * 0.1f; // Smooth interpolation
}
```

### **State Combinations**
```java
public float getCombinedEffect() {
    float effect = 1.0f;
    
    if (isDamaged) effect *= 1.5f;
    if (isCharging) effect *= 1.3f;
    if (movementSpeed > 0.5f) effect *= 1.2f;
    
    return effect;
}
```

### **State Persistence**
```java
// Store state between renders
private static final Map<UUID, EtherealOrbRenderState> stateCache = new HashMap<>();

public static EtherealOrbRenderState getCachedState(UUID entityId) {
    return stateCache.computeIfAbsent(entityId, k -> new EtherealOrbRenderState());
}
```

## Troubleshooting

### **Common Issues**

1. **State not updating**: Make sure `updateRenderState` is called
2. **Animations jerky**: Check interpolation in `updateFromEntity`
3. **Performance issues**: Verify state caching is working

### **Debug Tips**

```java
// Add debug logging
public void updateFromEntity(EtherealOrbEntity entity, float tickDelta) {
    LOGGER.info("Updating state: damage={}, charging={}, speed={}", 
        entity.hurtTime > 0, isCharging, movementSpeed);
    
    // ... rest of method
}
```

### **Testing States**

```java
// Test different states in-game
/summon theendupdate:ethereal_orb ~ ~ ~ {HurtTime:20}
/summon theendupdate:ethereal_orb ~ ~ ~ {CustomName:'"Charging Orb"'}
```

## Integration with Your Mod

### **1. Register the Renderer**
```java
// In TemplateModClient.java
EntityRendererRegistry.register(
    ModEntities.ETHEREAL_ORB, 
    EtherealOrbEntityRenderer::new
);
```

### **2. Add Data Trackers (Optional)**
```java
// In EtherealOrbEntity.java
private static final TrackedData<Boolean> IS_CHARGING = DataTracker.registerData(EtherealOrbEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

@Override
protected void initDataTracker() {
    super.initDataTracker();
    this.dataTracker.startTracking(IS_CHARGING, false);
}
```

### **3. Update State from Entity**
```java
// In EtherealOrbRenderState.java
public void updateFromEntity(EtherealOrbEntity entity, float tickDelta) {
    // ... existing code ...
    
    // Get charging state from entity
    this.isCharging = entity.getDataTracker().get(EtherealOrbEntity.IS_CHARGING);
}
```

This system gives you a powerful, flexible way to handle entity rendering with smooth animations and state-based effects!
