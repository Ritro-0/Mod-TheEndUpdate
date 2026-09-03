package com.theendupdate.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import com.theendupdate.TheEndUpdate;
import java.util.List;

public class CustomSpawnEggItem extends Item {
    private final EntityType<?> entityType;
    
    public CustomSpawnEggItem(EntityType<?> type, Item.Properties settings) {
        super(settings);
        this.entityType = type;
    }
    
    public EntityType<?> getEntityType() {
        return this.entityType;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }
        
        ItemStack itemStack = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos spawnPos = blockPos.relative(direction);
        
        Entity entity = this.entityType.create(serverWorld, 
            (e) -> {
                if (e != null && itemStack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    e.setCustomName(itemStack.getHoverName());
                }
            },
            spawnPos, EntitySpawnReason.MOB_SUMMONED, true, !blockPos.equals(spawnPos));
            
        if (entity != null) {
            if (TheEndUpdate.DEBUG_MODE) {
                TheEndUpdate.LOGGER.info("Created entity: {}, now adding to world...", entity);
            }
            boolean added = serverWorld.addFreshEntity(entity);
            if (TheEndUpdate.DEBUG_MODE) {
                TheEndUpdate.LOGGER.info("Entity added to world: {} (success: {})", entity, added);
            }
            
            if (added) {
                itemStack.consume(1, context.getPlayer());
                world.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
                return InteractionResult.CONSUME;
            } else {
                TheEndUpdate.LOGGER.error("Failed to add entity to world!");
            }
        } else {
            TheEndUpdate.LOGGER.warn("Failed to create entity from spawn egg!");
        }
        
        return InteractionResult.FAIL;
    }
}


