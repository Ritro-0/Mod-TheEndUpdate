# Ethereal Orb Entity - Complete Setup Guide

## Files Created

### 1. **Model File** (`EtherealOrbEntityModel.java`)
This file contains the 3D model structure for your ethereal orb. It includes:
- Central core sphere
- Rotating outer ring
- Three orbiting smaller spheres
- Basic animation setup in the `setAngles` method

### 2. **Renderer File** (`EtherealOrbEntityRenderer.java`)
This file handles how the entity is displayed in-game:
- Applies the texture
- Handles lighting (always glowing)
- Adds visual effects like the glowing aura
- Controls transformations like bobbing and rotation

### 3. **Animations File** (`EtherealOrbAnimations.java`)
This file contains various animation methods you can use:
- Idle floating animation
- Damage shake effects
- Spawning/death animations
- Special effects like energy charging
- Environmental reactions

## How to Use These Files

### Step 1: Update the Model
In `EtherealOrbEntityModel.java`, you need to add getter methods at the bottom:

```java
// Add these methods at the end of the class
public ModelPart getCore() { return core; }
public ModelPart getOuterRing() { return outerRing; }
public ModelPart getOrbitGroup() { return orbitGroup; }
public ModelPart getInnerOrb1() { return innerOrb1; }
public ModelPart getInnerOrb2() { return innerOrb2; }
public ModelPart getInnerOrb3() { return innerOrb3; }
```

### Step 2: Register in Client
Update `TemplateModClient.java` to register the renderer:

```java
// In onInitializeClient() method:
EntityRendererRegistry.register(
    ModEntities.ETHEREAL_ORB, 
    EtherealOrbEntityRenderer::new
);

EntityModelLayerRegistry.registerModelLayer(
    EtherealOrbEntityModel.ETHEREAL_ORB_LAYER,
    EtherealOrbEntityModel::getTexturedModelData
);
```

### Step 3: Create Texture
Create a texture file at:
`src/main/resources/assets/theendupdate/textures/entity/ethereal_orb.png`

Texture layout (64x32 pixels):
- 0,0 to 32,16: Core sphere texture
- 32,0 to 56,14: Outer ring texture  
- 0,16 to 12,28: Small orb texture (reused for all 3)

### Step 4: Optional Glow Texture
For the glow effect, create:
`src/main/resources/assets/theendupdate/textures/entity/ethereal_orb_glow.png`

## Customization Tips

### Modify Animations
In `EtherealOrbEntityModel.setAngles()`:
```java
// Example: Use the advanced animations
EtherealOrbAnimations.applyAllAnimations(this, entity, state.age, 0);
```

### Change Model Size
Adjust the cuboid dimensions in `getTexturedModelData()`:
```java
// Make core bigger
.cuboid(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F)
```

### Add More Orbs
Add more orbiting spheres in the model:
```java
orbitGroup.addChild(
    "inner_orb_4",
    ModelPartBuilder.create()
        .uv(0, 16)
        .cuboid(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
    ModelTransform.of(0.0F, 6.0F, 0.0F, 0, 0, 0)
);
```

### Change Colors
The glow and particle colors can be modified in the entity class or renderer.

## Troubleshooting

If you get compilation errors:
1. Make sure all imports are correct for Minecraft 1.21.8
2. The `LivingEntityRenderState` is the correct state class for 1.21.8
3. If `pivotY` doesn't work, use `ModelTransform.of()` to set positions

## Testing
1. Run the game with the mod
2. Use `/summon theendupdate:ethereal_orb` 
3. The entity should appear with animations
4. If it's just a white cube, check the texture path

Feel free to modify these files to create your unique ethereal orb appearance!
