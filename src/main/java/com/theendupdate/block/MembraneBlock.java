package com.theendupdate.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class MembraneBlock extends Block {
    public MembraneBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient()) {
            // Check for ritual when block is placed
            checkForRitual(world, pos);
            // Schedule frequent ticks to check for fire quickly
            world.scheduleBlockTick(pos, this, 2);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        super.scheduledTick(state, world, pos, random);
        checkForRitual(world, pos);
        // Keep scheduling ticks to check frequently
        world.scheduleBlockTick(pos, this, 2);
    }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return false; // Disable random ticks, use scheduled ticks instead
    }
    
    private void checkForRitual(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        
        // Check if fire is on top of this block
        boolean nearFire = serverWorld.getBlockState(pos.up()).isOf(Blocks.FIRE);
        
        if (nearFire) {
            // Check if this block is part of the correct formation
            if (isPartOfCorrectFormation(serverWorld, pos)) {
                // Check if ALL blocks in the formation are on fire
                if (allFormationBlocksOnFire(serverWorld, pos)) {
                    performKingPhantomRitual(serverWorld, pos);
                }
            }
        }
    }

    private boolean isPartOfCorrectFormation(World world, BlockPos pos) {
        // Try all 4 rotations (tail pointing NORTH, SOUTH, EAST, WEST)
        // Rotation 0: Tail pointing SOUTH
        for (int offsetX = -2; offsetX <= 2; offsetX++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
                if (tryFormationAtOffset(world, pos, offsetX, offsetZ, 0)) {
                    return true;
                }
            }
        }
        
        // Rotation 1: Tail pointing NORTH
        for (int offsetX = -2; offsetX <= 2; offsetX++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
                if (tryFormationAtOffset(world, pos, offsetX, offsetZ, 1)) {
                    return true;
                }
            }
        }
        
        // Rotation 2: Tail pointing EAST
        for (int offsetX = -3; offsetX <= 3; offsetX++) {
            for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                if (tryFormationAtOffset(world, pos, offsetX, offsetZ, 2)) {
                    return true;
                }
            }
        }
        
        // Rotation 3: Tail pointing WEST
        for (int offsetX = -3; offsetX <= 3; offsetX++) {
            for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                if (tryFormationAtOffset(world, pos, offsetX, offsetZ, 3)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean tryFormationAtOffset(World world, BlockPos centerPos, int offsetX, int offsetZ, int rotation) {
        BlockPos topLeft = centerPos.add(offsetX, 0, offsetZ);
        
        // Check 3x2 rectangle based on rotation
        // Rotation 0: SOUTH (X=3 wide, Z=2 deep, tail extends +Z)
        // Rotation 1: NORTH (X=3 wide, Z=2 deep, tail extends -Z)
        // Rotation 2: EAST  (Z=3 wide, X=2 deep, tail extends +X)
        // Rotation 3: WEST  (Z=3 wide, X=2 deep, tail extends -X)
        
        int membraneCount = 0;
        
        if (rotation == 0) { // SOUTH
            // Check 3x2 rectangle
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.add(x, 0, z)).isOf(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.add(1, 0, 2)).isOf(this)) return false;
            if (!world.getBlockState(topLeft.add(1, 0, 3)).isOf(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.add(0, 0, 2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(2, 0, 2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(0, 0, 3)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(2, 0, 3)).isOf(this)) return false;
            
        } else if (rotation == 1) { // NORTH
            // Check 3x2 rectangle
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.add(x, 0, -z)).isOf(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.add(1, 0, -2)).isOf(this)) return false;
            if (!world.getBlockState(topLeft.add(1, 0, -3)).isOf(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.add(0, 0, -2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(2, 0, -2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(0, 0, -3)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(2, 0, -3)).isOf(this)) return false;
            
        } else if (rotation == 2) { // EAST
            // Check 3x2 rectangle (Z=3 wide, X=2 deep)
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.add(x, 0, z)).isOf(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.add(2, 0, 1)).isOf(this)) return false;
            if (!world.getBlockState(topLeft.add(3, 0, 1)).isOf(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.add(2, 0, 0)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(2, 0, 2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(3, 0, 0)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(3, 0, 2)).isOf(this)) return false;
            
        } else if (rotation == 3) { // WEST
            // Check 3x2 rectangle (Z=3 wide, X=2 deep)
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.add(-x, 0, z)).isOf(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.add(-2, 0, 1)).isOf(this)) return false;
            if (!world.getBlockState(topLeft.add(-3, 0, 1)).isOf(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.add(-2, 0, 0)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(-2, 0, 2)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(-3, 0, 0)).isOf(this)) return false;
            if (world.getBlockState(topLeft.add(-3, 0, 2)).isOf(this)) return false;
        }
        
        return true;
    }

    private boolean allFormationBlocksOnFire(World world, BlockPos centerPos) {
        // Try all 4 rotations
        for (int rotation = 0; rotation < 4; rotation++) {
            int maxOffsetX = (rotation >= 2) ? 3 : 2;
            int maxOffsetZ = (rotation >= 2) ? 2 : 3;
            
            for (int offsetX = -maxOffsetX; offsetX <= maxOffsetX; offsetX++) {
                for (int offsetZ = -maxOffsetZ; offsetZ <= maxOffsetZ; offsetZ++) {
                    if (!tryFormationAtOffset(world, centerPos, offsetX, offsetZ, rotation)) {
                        continue;
                    }
                    
                    // Found a valid formation, now check if all blocks have fire on top
                    if (checkAllBlocksOnFire(world, centerPos.add(offsetX, 0, offsetZ), rotation)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean checkAllBlocksOnFire(World world, BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.add(x, 0, z).up()).isOf(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.add(1, 0, 2).up()).isOf(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.add(1, 0, 3).up()).isOf(Blocks.FIRE)) return false;
            
        } else if (rotation == 1) { // NORTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.add(x, 0, -z).up()).isOf(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.add(1, 0, -2).up()).isOf(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.add(1, 0, -3).up()).isOf(Blocks.FIRE)) return false;
            
        } else if (rotation == 2) { // EAST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.add(x, 0, z).up()).isOf(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.add(2, 0, 1).up()).isOf(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.add(3, 0, 1).up()).isOf(Blocks.FIRE)) return false;
            
        } else if (rotation == 3) { // WEST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.add(-x, 0, z).up()).isOf(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.add(-2, 0, 1).up()).isOf(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.add(-3, 0, 1).up()).isOf(Blocks.FIRE)) return false;
        }
        
        return true;
    }

    private void performKingPhantomRitual(ServerWorld world, BlockPos centerPos) {
        // Try all 4 rotations
        for (int rotation = 0; rotation < 4; rotation++) {
            int maxOffsetX = (rotation >= 2) ? 3 : 2;
            int maxOffsetZ = (rotation >= 2) ? 2 : 3;
            
            for (int offsetX = -maxOffsetX; offsetX <= maxOffsetX; offsetX++) {
                for (int offsetZ = -maxOffsetZ; offsetZ <= maxOffsetZ; offsetZ++) {
                    if (!tryFormationAtOffset(world, centerPos, offsetX, offsetZ, rotation)) {
                        continue;
                    }
                    
                    // Found the formation!
                    BlockPos topLeft = centerPos.add(offsetX, 0, offsetZ);
                    breakFormationBlocks(world, topLeft, rotation);
                    
                    // Spawn King Phantom at the center
                    BlockPos spawnPos = getFormationCenter(topLeft, rotation);
                    
                    // Add dramatic explosion effect FIRST
                    world.createExplosion(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 
                        3.0f, false, World.ExplosionSourceType.NONE);
                    
                    // Then spawn King Phantom after a short delay so it doesn't get damaged
                    com.theendupdate.entity.KingPhantomEntity kingPhantom = 
                        new com.theendupdate.entity.KingPhantomEntity(com.theendupdate.registry.ModEntities.KING_PHANTOM, world);
                    
                    kingPhantom.setPosition(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
                    kingPhantom.setInvulnerable(true); // Make invulnerable temporarily
                    world.spawnEntity(kingPhantom);
                    
                    // Remove invulnerability after 2 seconds (40 ticks)
                    world.getServer().execute(() -> {
                        try {
                            Thread.sleep(2000);
                            kingPhantom.setInvulnerable(false);
                        } catch (InterruptedException e) {
                            // Silently handle interruption
                        }
                    });
                    
                    return;
                }
            }
        }
    }
    
    private void breakFormationBlocks(ServerWorld world, BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos pos = topLeft.add(x, 0, z);
                    world.breakBlock(pos, false);
                    world.breakBlock(pos.up(), false);
                }
            }
            world.breakBlock(topLeft.add(1, 0, 2), false);
            world.breakBlock(topLeft.add(1, 0, 2).up(), false);
            world.breakBlock(topLeft.add(1, 0, 3), false);
            world.breakBlock(topLeft.add(1, 0, 3).up(), false);
            
        } else if (rotation == 1) { // NORTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos pos = topLeft.add(x, 0, -z);
                    world.breakBlock(pos, false);
                    world.breakBlock(pos.up(), false);
                }
            }
            world.breakBlock(topLeft.add(1, 0, -2), false);
            world.breakBlock(topLeft.add(1, 0, -2).up(), false);
            world.breakBlock(topLeft.add(1, 0, -3), false);
            world.breakBlock(topLeft.add(1, 0, -3).up(), false);
            
        } else if (rotation == 2) { // EAST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    BlockPos pos = topLeft.add(x, 0, z);
                    world.breakBlock(pos, false);
                    world.breakBlock(pos.up(), false);
                }
            }
            world.breakBlock(topLeft.add(2, 0, 1), false);
            world.breakBlock(topLeft.add(2, 0, 1).up(), false);
            world.breakBlock(topLeft.add(3, 0, 1), false);
            world.breakBlock(topLeft.add(3, 0, 1).up(), false);
            
        } else if (rotation == 3) { // WEST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    BlockPos pos = topLeft.add(-x, 0, z);
                    world.breakBlock(pos, false);
                    world.breakBlock(pos.up(), false);
                }
            }
            world.breakBlock(topLeft.add(-2, 0, 1), false);
            world.breakBlock(topLeft.add(-2, 0, 1).up(), false);
            world.breakBlock(topLeft.add(-3, 0, 1), false);
            world.breakBlock(topLeft.add(-3, 0, 1).up(), false);
        }
    }
    
    private BlockPos getFormationCenter(BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            return topLeft.add(1, 0, 0);
        } else if (rotation == 1) { // NORTH
            return topLeft.add(1, 0, 0);
        } else if (rotation == 2) { // EAST
            return topLeft.add(0, 0, 1);
        } else { // WEST
            return topLeft.add(0, 0, 1);
        }
    }
}

