# Ethereal Orb Bounce Mechanic Fix

## Issues Fixed

### 1. **Ground-hugging and Downward Bias**
   - **Problem**: Orbs would constantly move downward and get stuck on the ground, bouncing back and forth but never upwards
   - **Cause**: When wandering without a home crystal, the random Y-offset (`dy`) could be negative, causing orbs to pick waypoints below their current position
   - **Fix**: Added intelligent upward bias based on height:
     - Below minimum height (8 blocks from bottom): Always moves up
     - Near minimum height (8-18 blocks): 80% chance to move up
     - At good height (18+ blocks): 60% chance to move up, allowing gentle wandering

### 2. **No Bounce Mechanics**
   - **Problem**: Orbs had no explicit bounce behavior when hitting surfaces
   - **Cause**: Code relied solely on vanilla collision resolution with no custom bounce logic
   - **Fix**: Added `handleSurfaceBounce()` method that:
     - Bounces upward when hitting the ground (0.25-0.4 bounce strength)
     - Dampens horizontal movement on ground contact (70% retention)
     - Reverses and adds upward boost when hitting walls (15% upward component)
     - Adds randomness to prevent repetitive patterns

### 3. **Ground-sticking**
   - **Problem**: Orbs would stick to the ground even when pulled up
   - **Cause**: No buoyancy/floating behavior to keep them airborne
   - **Fix**: Added gentle buoyancy when near ground:
     - Detects when orb is within 3 blocks of solid ground
     - Adds upward velocity (0.08) when moving slowly near ground
     - Only applies when not panicking or breeding

### 4. **Panic Movement Downward Bias**
   - **Problem**: When panicking, orbs could still move downward
   - **Cause**: Panic movement used same random Y-offset that could be negative
   - **Fix**: Changed panic movement to use `Math.abs()` on the Y component, ensuring orbs always flee upward when scared

## Technical Changes

### File Modified
- `src/main/java/com/theendupdate/entity/EtherealOrbEntity.java`

### Key Methods Added/Modified

1. **`handleSurfaceBounce()`** (NEW)
   - Handles collision detection and bounce physics
   - Checks for ground, ceiling, and wall collisions
   - Applies appropriate velocity changes
   - Adds buoyancy near ground

2. **`tick()`** (MODIFIED)
   - Now calls `handleSurfaceBounce()` every tick
   - Ensures bounce mechanics are always active

3. **Panic Movement** (MODIFIED)
   - Changed `dy` calculation to always be positive
   - Prevents downward panic movement

4. **Wandering Without Home** (MODIFIED)
   - Completely rewrote the random waypoint selection
   - Added height-based upward bias
   - Increased maximum wander height from 100 to 120 blocks
   - Ensures orbs prefer to stay airborne

## Testing Recommendations

1. **Ground Bounce**: Place orbs near ground level and observe bouncing behavior
2. **Wall Bounce**: Push orbs into walls and verify they bounce away with upward component
3. **No Home Float**: Remove all crystal homes in Nether/Overworld and verify orbs float properly
4. **Panic Flight**: Hurt an orb and verify it flees upward
5. **Height Maintenance**: Observe orbs over time to ensure they maintain good floating height

## Notes

- Crystal home detection still requires `ASTRAL_REMNANT` or `STELLARITH_CRYSTAL` blocks
- In Nether/Overworld without these blocks, orbs will wander but now properly float instead of sinking
- Bounce mechanics work in all dimensions
- Buoyancy system prevents long-term ground contact

