package com.theendupdate.mixin;

import com.theendupdate.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.theendupdate.block.VoidSapBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityWallSlidingMixin {

    // TAIL - runs after vanilla movement so our changes aren't overridden
    @Inject(method = "move", at = @At("TAIL"))
    private void applyVoidSapWallSliding(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        Level world = entity.level();

        // server side, falling, normal movement only
        if (world.isClientSide()
            || movementType != MoverType.SELF
            || !entity.horizontalCollision
            || entity.getDeltaMovement().y >= 0) {
            return;
        }

        AABB entityBox = entity.getBoundingBox();
        BlockPos minPos = new BlockPos((int) Math.floor(entityBox.minX - 0.5), 
                                       (int) Math.floor(entityBox.minY), 
                                       (int) Math.floor(entityBox.minZ - 0.5));
        BlockPos maxPos = new BlockPos((int) Math.ceil(entityBox.maxX + 0.5), 
                                       (int) Math.ceil(entityBox.maxY + 1.0), 
                                       (int) Math.ceil(entityBox.maxZ + 0.5));

        boolean isNearWallVoidSap = false;

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = world.getBlockState(pos);

            if (state.is(ModBlocks.VOID_SAP)) {
                if (isEntityTouchingVoidSapWall(state, pos, entity)) {
                    isNearWallVoidSap = true;
                    break;
                }
            }
        }

        if (isNearWallVoidSap) {
            Vec3 velocity = entity.getDeltaMovement();
            double newY = Math.max(velocity.y - 0.05, -0.25);
            entity.setDeltaMovement(velocity.x * 0.7, newY, velocity.z * 0.7);

            // needs explicit sync, setDeltaMovement alone doesn't push to client
            entity.needsSync = true;

            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entity));
            }

            // no fall damage here, same as honey blocks
            entity.fallDistance = 0;
        }
    }

    // horizontal faces only, wall sliding is for the sides not top/bottom
    private boolean isEntityTouchingVoidSapWall(BlockState state, BlockPos pos, Entity entity) {
        AABB entityBox = entity.getBoundingBox();
        double tolerance = 0.2;

        // north (-Z)
        if (state.getValue(VoidSapBlock.NORTH)) {
            double blockFaceZ = pos.getZ();
            if (entityBox.minZ <= blockFaceZ + tolerance && entityBox.maxZ >= blockFaceZ - tolerance &&
                entityBox.maxX >= pos.getX() && entityBox.minX <= pos.getX() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // south (+Z)
        if (state.getValue(VoidSapBlock.SOUTH)) {
            double blockFaceZ = pos.getZ() + 1.0;
            if (entityBox.maxZ >= blockFaceZ - tolerance && entityBox.minZ <= blockFaceZ + tolerance &&
                entityBox.maxX >= pos.getX() && entityBox.minX <= pos.getX() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // west (-X)
        if (state.getValue(VoidSapBlock.WEST)) {
            double blockFaceX = pos.getX();
            if (entityBox.minX <= blockFaceX + tolerance && entityBox.maxX >= blockFaceX - tolerance &&
                entityBox.maxZ >= pos.getZ() && entityBox.minZ <= pos.getZ() + 1.0 &&
                entityBox.maxY >= pos.getY() && entityBox.minY <= pos.getY() + 1.0) {
                return true;
            }
        }

        // east (+X)
        if (state.getValue(VoidSapBlock.EAST)) {
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
