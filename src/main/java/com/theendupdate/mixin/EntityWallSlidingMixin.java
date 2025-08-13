package com.theendupdate.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import com.theendupdate.registry.ModBlocks;
import com.theendupdate.block.VoidSapBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityWallSlidingMixin {

    // Apply velocity changes at TAIL - after vanilla movement logic to prevent override
    @Inject(method = "move", at = @At("TAIL"))
    private void applyVoidSapWallSliding(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        World world = entity.getWorld();
        
        // Only process on server side, for falling entities, and during normal movement
        if (world.isClient || entity.getVelocity().y >= 0 || movementType != MovementType.SELF) return;

        // Check if entity is near any void sap blocks
        Box entityBox = entity.getBoundingBox();
        BlockPos minPos = new BlockPos((int) Math.floor(entityBox.minX - 0.5), 
                                       (int) Math.floor(entityBox.minY), 
                                       (int) Math.floor(entityBox.minZ - 0.5));
        BlockPos maxPos = new BlockPos((int) Math.ceil(entityBox.maxX + 0.5), 
                                       (int) Math.ceil(entityBox.maxY + 1.0), 
                                       (int) Math.ceil(entityBox.maxZ + 0.5));

        boolean isNearWallVoidSap = false;

        // Check all blocks in the expanded area around the entity
        for (BlockPos pos : BlockPos.iterate(minPos, maxPos)) {
            BlockState state = world.getBlockState(pos);
            
            if (state.isOf(ModBlocks.VOID_SAP)) {
                // Check if entity is actually touching a wall face of void sap
                if (isEntityTouchingVoidSapWall(state, pos, entity)) {
                    isNearWallVoidSap = true;
                    break;
                }
            }
        }

        if (isNearWallVoidSap) {
            // Apply wall sliding at half the intensity of honey blocks
            // Honey blocks use ~0.4, so half intensity = 0.7 (less sticky than honey)
            Vec3d velocity = entity.getVelocity();
            double newY = Math.max(velocity.y * 0.7, -0.15); // Half honey block intensity
            entity.setVelocity(velocity.x, newY, velocity.z);
            
            // CRITICAL: Mark velocity as dirty for proper client-server sync
            entity.velocityDirty = true;
            
            // Send velocity update packet to client for players (ensures client sees the change)
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(entity));
            }
            
            // Reset fall distance to prevent fall damage (like honey blocks)
            entity.fallDistance = 0;
        }
    }

    // Check if entity is touching wall faces (horizontal faces) of void sap
    private boolean isEntityTouchingVoidSapWall(BlockState state, BlockPos pos, Entity entity) {
        Box entityBox = entity.getBoundingBox();
        double tolerance = 0.2; // Tolerance for wall contact detection

        // Only check horizontal faces (NORTH, SOUTH, EAST, WEST) for wall sliding
        // Wall sliding happens when falling against the sides, not top/bottom

        // Check NORTH face (negative Z direction)
        if (state.get(VoidSapBlock.NORTH)) {
            double blockFaceZ = pos.getZ();
            if (entityBox.minZ <= blockFaceZ + tolerance && entityBox.maxZ >= blockFaceZ - tolerance &&
                entityBox.maxX >= pos.getX() && entityBox.minX <= pos.getX() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // Check SOUTH face (positive Z direction)
        if (state.get(VoidSapBlock.SOUTH)) {
            double blockFaceZ = pos.getZ() + 1.0;
            if (entityBox.maxZ >= blockFaceZ - tolerance && entityBox.minZ <= blockFaceZ + tolerance &&
                entityBox.maxX >= pos.getX() && entityBox.minX <= pos.getX() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // Check WEST face (negative X direction)
        if (state.get(VoidSapBlock.WEST)) {
            double blockFaceX = pos.getX();
            if (entityBox.minX <= blockFaceX + tolerance && entityBox.maxX >= blockFaceX - tolerance &&
                entityBox.maxZ >= pos.getZ() && entityBox.minZ <= pos.getZ() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // Check EAST face (positive X direction)
        if (state.get(VoidSapBlock.EAST)) {
            double blockFaceX = pos.getX() + 1.0;
            if (entityBox.maxX >= blockFaceX - tolerance && entityBox.minX <= blockFaceX + tolerance &&
                entityBox.maxZ >= pos.getZ() && entityBox.minZ <= pos.getZ() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        return false;
    }
}
