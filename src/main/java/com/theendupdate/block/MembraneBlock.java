package com.theendupdate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MembraneBlock extends Block {
    public MembraneBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (!world.isClientSide()) {
            checkForRitual(world, pos);
            // frequent (every 2 ticks) so fire is detected quickly
            world.scheduleTick(pos, this, 2);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.tick(state, world, pos, random);
        checkForRitual(world, pos);
        world.scheduleTick(pos, this, 2);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false; // scheduled ticks instead of random ticks
    }
    
    private void checkForRitual(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        
        boolean nearFire = serverWorld.getBlockState(pos.above()).is(Blocks.FIRE);
        
        if (nearFire) {
            if (isPartOfCorrectFormation(serverWorld, pos)) {
                if (allFormationBlocksOnFire(serverWorld, pos)) {
                    performKingPhantomRitual(serverWorld, pos);
                }
            }
        }
    }

    private boolean isPartOfCorrectFormation(Level world, BlockPos pos) {
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
    
    private boolean tryFormationAtOffset(Level world, BlockPos centerPos, int offsetX, int offsetZ, int rotation) {
        BlockPos topLeft = centerPos.offset(offsetX, 0, offsetZ);
        
        // Check 3x2 rectangle based on rotation
        // Rotation 0: SOUTH (X=3 wide, Z=2 deep, tail extends +Z)
        // Rotation 1: NORTH (X=3 wide, Z=2 deep, tail extends -Z)
        // Rotation 2: EAST  (Z=3 wide, X=2 deep, tail extends +X)
        // Rotation 3: WEST  (Z=3 wide, X=2 deep, tail extends -X)
        
        int membraneCount = 0;
        
        if (rotation == 0) { // SOUTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, z)).is(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.offset(1, 0, 2)).is(this)) return false;
            if (!world.getBlockState(topLeft.offset(1, 0, 3)).is(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.offset(0, 0, 2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(2, 0, 2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(0, 0, 3)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(2, 0, 3)).is(this)) return false;
            
        } else if (rotation == 1) { // NORTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, -z)).is(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.offset(1, 0, -2)).is(this)) return false;
            if (!world.getBlockState(topLeft.offset(1, 0, -3)).is(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.offset(0, 0, -2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(2, 0, -2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(0, 0, -3)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(2, 0, -3)).is(this)) return false;
            
        } else if (rotation == 2) { // EAST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, z)).is(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.offset(2, 0, 1)).is(this)) return false;
            if (!world.getBlockState(topLeft.offset(3, 0, 1)).is(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.offset(2, 0, 0)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(2, 0, 2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(3, 0, 0)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(3, 0, 2)).is(this)) return false;
            
        } else if (rotation == 3) { // WEST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.offset(-x, 0, z)).is(this)) return false;
                    membraneCount++;
                }
            }
            // Extension
            if (!world.getBlockState(topLeft.offset(-2, 0, 1)).is(this)) return false;
            if (!world.getBlockState(topLeft.offset(-3, 0, 1)).is(this)) return false;
            membraneCount += 2;
            // Check sides are empty
            if (world.getBlockState(topLeft.offset(-2, 0, 0)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(-2, 0, 2)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(-3, 0, 0)).is(this)) return false;
            if (world.getBlockState(topLeft.offset(-3, 0, 2)).is(this)) return false;
        }
        
        return true;
    }

    private boolean allFormationBlocksOnFire(Level world, BlockPos centerPos) {
        // Try all 4 rotations
        for (int rotation = 0; rotation < 4; rotation++) {
            int maxOffsetX = (rotation >= 2) ? 3 : 2;
            int maxOffsetZ = (rotation >= 2) ? 2 : 3;
            
            for (int offsetX = -maxOffsetX; offsetX <= maxOffsetX; offsetX++) {
                for (int offsetZ = -maxOffsetZ; offsetZ <= maxOffsetZ; offsetZ++) {
                    if (!tryFormationAtOffset(world, centerPos, offsetX, offsetZ, rotation)) {
                        continue;
                    }
                    
                    // valid formation found, now check fire on every block
                    if (checkAllBlocksOnFire(world, centerPos.offset(offsetX, 0, offsetZ), rotation)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean checkAllBlocksOnFire(Level world, BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, z).above()).is(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.offset(1, 0, 2).above()).is(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.offset(1, 0, 3).above()).is(Blocks.FIRE)) return false;
            
        } else if (rotation == 1) { // NORTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, -z).above()).is(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.offset(1, 0, -2).above()).is(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.offset(1, 0, -3).above()).is(Blocks.FIRE)) return false;
            
        } else if (rotation == 2) { // EAST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.offset(x, 0, z).above()).is(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.offset(2, 0, 1).above()).is(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.offset(3, 0, 1).above()).is(Blocks.FIRE)) return false;
            
        } else if (rotation == 3) { // WEST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    if (!world.getBlockState(topLeft.offset(-x, 0, z).above()).is(Blocks.FIRE)) return false;
                }
            }
            if (!world.getBlockState(topLeft.offset(-2, 0, 1).above()).is(Blocks.FIRE)) return false;
            if (!world.getBlockState(topLeft.offset(-3, 0, 1).above()).is(Blocks.FIRE)) return false;
        }
        
        return true;
    }

    private void performKingPhantomRitual(ServerLevel world, BlockPos centerPos) {
        // Try all 4 rotations
        for (int rotation = 0; rotation < 4; rotation++) {
            int maxOffsetX = (rotation >= 2) ? 3 : 2;
            int maxOffsetZ = (rotation >= 2) ? 2 : 3;
            
            for (int offsetX = -maxOffsetX; offsetX <= maxOffsetX; offsetX++) {
                for (int offsetZ = -maxOffsetZ; offsetZ <= maxOffsetZ; offsetZ++) {
                    if (!tryFormationAtOffset(world, centerPos, offsetX, offsetZ, rotation)) {
                        continue;
                    }
                    
                    BlockPos topLeft = centerPos.offset(offsetX, 0, offsetZ);
                    breakFormationBlocks(world, topLeft, rotation);
                    BlockPos spawnPos = getFormationCenter(topLeft, rotation);
                    
                    // explosion goes off before the phantom spawns so it doesn't hurt itself
                    world.explode(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 
                        3.0f, false, Level.ExplosionInteraction.NONE);
                    
                    com.theendupdate.entity.KingPhantomEntity kingPhantom = 
                        new com.theendupdate.entity.KingPhantomEntity(com.theendupdate.registry.ModEntities.KING_PHANTOM, world);
                    
                    kingPhantom.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
                    kingPhantom.setInvulnerable(true);
                    world.addFreshEntity(kingPhantom);
                    
                    // invulnerable for its first 2 seconds so nothing kills it on spawn
                    world.getServer().execute(() -> {
                        try {
                            Thread.sleep(2000);
                            kingPhantom.setInvulnerable(false);
                        } catch (InterruptedException e) {
                        }
                    });
                    
                    return;
                }
            }
        }
    }
    
    private void breakFormationBlocks(ServerLevel world, BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos pos = topLeft.offset(x, 0, z);
                    world.destroyBlock(pos, false);
                    world.destroyBlock(pos.above(), false);
                }
            }
            world.destroyBlock(topLeft.offset(1, 0, 2), false);
            world.destroyBlock(topLeft.offset(1, 0, 2).above(), false);
            world.destroyBlock(topLeft.offset(1, 0, 3), false);
            world.destroyBlock(topLeft.offset(1, 0, 3).above(), false);
            
        } else if (rotation == 1) { // NORTH
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockPos pos = topLeft.offset(x, 0, -z);
                    world.destroyBlock(pos, false);
                    world.destroyBlock(pos.above(), false);
                }
            }
            world.destroyBlock(topLeft.offset(1, 0, -2), false);
            world.destroyBlock(topLeft.offset(1, 0, -2).above(), false);
            world.destroyBlock(topLeft.offset(1, 0, -3), false);
            world.destroyBlock(topLeft.offset(1, 0, -3).above(), false);
            
        } else if (rotation == 2) { // EAST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    BlockPos pos = topLeft.offset(x, 0, z);
                    world.destroyBlock(pos, false);
                    world.destroyBlock(pos.above(), false);
                }
            }
            world.destroyBlock(topLeft.offset(2, 0, 1), false);
            world.destroyBlock(topLeft.offset(2, 0, 1).above(), false);
            world.destroyBlock(topLeft.offset(3, 0, 1), false);
            world.destroyBlock(topLeft.offset(3, 0, 1).above(), false);
            
        } else if (rotation == 3) { // WEST
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 2; x++) {
                    BlockPos pos = topLeft.offset(-x, 0, z);
                    world.destroyBlock(pos, false);
                    world.destroyBlock(pos.above(), false);
                }
            }
            world.destroyBlock(topLeft.offset(-2, 0, 1), false);
            world.destroyBlock(topLeft.offset(-2, 0, 1).above(), false);
            world.destroyBlock(topLeft.offset(-3, 0, 1), false);
            world.destroyBlock(topLeft.offset(-3, 0, 1).above(), false);
        }
    }
    
    private BlockPos getFormationCenter(BlockPos topLeft, int rotation) {
        if (rotation == 0) { // SOUTH
            return topLeft.offset(1, 0, 0);
        } else if (rotation == 1) { // NORTH
            return topLeft.offset(1, 0, 0);
        } else if (rotation == 2) { // EAST
            return topLeft.offset(0, 0, 1);
        } else { // WEST
            return topLeft.offset(0, 0, 1);
        }
    }
}

