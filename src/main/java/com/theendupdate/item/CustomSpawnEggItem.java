package com.theendupdate.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import com.theendupdate.TemplateMod;
import java.util.List;

public class CustomSpawnEggItem extends Item {
    private final EntityType<?> entityType;
    
    public CustomSpawnEggItem(EntityType<?> type, Item.Settings settings) {
        super(settings);
        this.entityType = type;
    }
    
    public EntityType<?> getEntityType() {
        return this.entityType;
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }
        
        ItemStack itemStack = context.getStack();
        BlockPos blockPos = context.getBlockPos();
        Direction direction = context.getSide();
        BlockPos spawnPos = blockPos.offset(direction);
        
        Entity entity = this.entityType.create(serverWorld, 
            (e) -> {
                if (e != null && itemStack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) {
                    e.setCustomName(itemStack.getName());
                }
            },
            spawnPos, SpawnReason.MOB_SUMMONED, true, !blockPos.equals(spawnPos));
            
        if (entity != null) {
            TemplateMod.LOGGER.info("Created entity: {}, now adding to world...", entity);
            // CRITICAL: Actually add the entity to the world!
            boolean added = serverWorld.spawnEntity(entity);
            TemplateMod.LOGGER.info("Entity added to world: {} (success: {})", entity, added);
            
            if (added) {
                itemStack.decrementUnlessCreative(1, context.getPlayer());
                world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
                return ActionResult.CONSUME;
            } else {
                TemplateMod.LOGGER.error("Failed to add entity to world!");
            }
        } else {
            TemplateMod.LOGGER.warn("Failed to create entity from spawn egg!");
        }
        
        return ActionResult.FAIL;
    }
}


