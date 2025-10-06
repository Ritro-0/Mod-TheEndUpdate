package com.theendupdate.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecorationType;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

/**
 * Explorer-style map that points to a distant Shadowlands target using a red X marker.
 * For now, target is the nearest End City as a placeholder distant End structure.
 * Later, this can be switched to a custom structure (e.g., hollow shadow tree/altar) once registered.
 */
public class ShadowHuntersMapItem extends Item {
    public ShadowHuntersMapItem(Settings settings) {
        super(settings);
    }
    
    public static ActionResult handleMapCycle(ServerWorld serverWorld, ServerPlayerEntity player, ItemStack mapStack, Hand hand) {
        BlockPos origin = player.getBlockPos();
        
        // Check if structure exists
        BlockPos target = locateHollowTreeStatic(serverWorld, origin);
        if (target == null) {
            com.theendupdate.TemplateMod.LOGGER.warn("Shadow Hunter's Map: No hollow shadow tree found");
            return ActionResult.CONSUME;
        }
        
        // Get current index and increment/decrement
        boolean previous = player.isSneaking();
        net.minecraft.component.type.NbtComponent customData = mapStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        net.minecraft.nbt.NbtCompound tag = customData.copyNbt();
        int oldIdx = tag.getInt("theendupdate_map_idx").orElse(0);
        int idx = Math.max(0, oldIdx + (previous ? -1 : 1));
        
        com.theendupdate.TemplateMod.LOGGER.info("Shadow Hunter's Map: old index={}, new index={}, sneaking={}", oldIdx, idx, previous);
        
        // Find the Nth nearest structure
        BlockPos nthTarget = locateNthHollowTreeStatic(serverWorld, origin, idx);
        if (nthTarget == null) {
            com.theendupdate.TemplateMod.LOGGER.warn("Shadow Hunter's Map: Could not find structure at index {}", idx);
            return ActionResult.CONSUME;
        }
        
        // Update the map state
        MapState state = net.minecraft.item.FilledMapItem.getMapState(mapStack, serverWorld);
        if (state != null) {
            // Clear old decorations
            try {
                java.lang.reflect.Method removeDecorations = MapState.class.getDeclaredMethod("removeDecorations");
                removeDecorations.setAccessible(true);
                removeDecorations.invoke(state);
            } catch (Exception e) {
                // Continue if reflection fails
            }
            
            // Add new decoration with stable id so it persists and does not duplicate
            try {
                addShadowHunterDecoration(state, serverWorld, nthTarget.getX() + 0.5, nthTarget.getZ() + 0.5, Math.PI);
            } catch (Throwable t) {
                com.theendupdate.TemplateMod.LOGGER.error("Failed to add map decoration: " + t.getMessage(), t);
            }
        }
        
        // Save new index and target coords for persistence
        tag.putInt("theendupdate_map_idx", idx);
        try {
            tag.putInt("theendupdate_target_x", nthTarget.getX());
            tag.putInt("theendupdate_target_z", nthTarget.getZ());
            try { tag.putString("theendupdate_target_dim", serverWorld.getRegistryKey().getValue().toString()); } catch (Throwable ignoredDim) {}
        } catch (Throwable ignored) {}
        mapStack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        
        com.theendupdate.TemplateMod.LOGGER.info("Shadow Hunter's Map: Saved index {} to NBT, updated map to point to {}", idx, nthTarget);
        
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        if (world.isClient) return ActionResult.SUCCESS;
        ServerWorld serverWorld = (ServerWorld) world;
        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack inHand = player.getStackInHand(hand);

        // Simplified: find nearest hollow tree and create a filled map once, consuming the empty map
        return createNewMapAndConsume(serverWorld, player, inHand, hand);
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        // Keep the original map in the crafting grid; copy so NBT persists
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private ItemStack findExistingShadowHuntersMap(ServerPlayerEntity player) {
        // Prefer offhand/mainhand first
        ItemStack mainHand = player.getMainHandStack();
        if (isShadowHuntersFilledMap(mainHand)) return mainHand;
        ItemStack offHand = player.getOffHandStack();
        if (isShadowHuntersFilledMap(offHand)) return offHand;

        // Search inventory
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (isShadowHuntersFilledMap(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private boolean isShadowHuntersFilledMap(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(net.minecraft.item.Items.FILLED_MAP)) return false;
        net.minecraft.component.type.NbtComponent custom = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        return custom != null && custom.contains("theendupdate_sh_map");
    }

    private ActionResult handleFilledMapReroll(ServerWorld serverWorld, ServerPlayerEntity player, ItemStack mapStack) {
        // Check if this is a Shadow Hunter's Map (via NBT tag)
        net.minecraft.component.type.NbtComponent customData = mapStack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData == null || !customData.contains("theendupdate_sh_map")) {
            return ActionResult.PASS; // Not our map, let vanilla logic proceed
        }

        // Find nearest altar by scanning outward in chunk rings
        BlockPos origin = player.getBlockPos();
        BlockPos target = locateHollowTreeStatic(serverWorld, origin);
        if (target == null) {
            return ActionResult.CONSUME;
        }

        // Shift+right-click: go to previous target; normal right-click: next target
        boolean previous = player.isSneaking();
        // Track an index in NBT so rerolls cycle deterministically per item stack
        net.minecraft.nbt.NbtCompound tag = customData.copyNbt();
        int oldIdx = tag.getInt("theendupdate_map_idx").orElse(0);
        int idx = Math.max(0, oldIdx + (previous ? -1 : 1));
        
        com.theendupdate.TemplateMod.LOGGER.info("Shadow Hunter's Map: old index={}, new index={}, sneaking={}", oldIdx, idx, previous);

        // Compute the Nth-nearest target by sampling concentric locate attempts with incremental bias
        BlockPos nthTarget = locateNthHollowTreeStatic(serverWorld, origin, idx);
        if (nthTarget == null) {
            return ActionResult.CONSUME;
        }

        // Recenter by replacing the existing filled map with a new one centered on nthTarget
        recenterAndReplaceFilledMap(serverWorld, player, mapStack, nthTarget, idx);
        return ActionResult.SUCCESS;
    }

    private ActionResult createNewMap(ServerWorld serverWorld, ServerPlayerEntity player, ItemStack inHand) {
        // Find nearest hollow tree
        BlockPos origin = player.getBlockPos();
        BlockPos target = locateHollowTreeStatic(serverWorld, origin);
        if (target == null) {
            return ActionResult.CONSUME;
        }

        // Create a new filled map centered near the target, scale 4 (1:4)
        int scale = 4;
        int centerX = target.getX();
        int centerZ = target.getZ();

        ItemStack mapStack = net.minecraft.item.FilledMapItem.createMap(serverWorld, centerX, centerZ, (byte) scale, true, true);
        // Add red X decoration (use treasure-like target icon)
        MapState state = net.minecraft.item.FilledMapItem.getMapState(mapStack, serverWorld);
        if (state != null) {
            try {
                addShadowHunterDecoration(state, serverWorld, target.getX() + 0.5, target.getZ() + 0.5, 0.0);
            } catch (Throwable ignored) {}
        }

        // Mark the filled map so future right-clicks on the filled map can reroll the target
        try {
            net.minecraft.component.type.NbtComponent data = mapStack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT);
            net.minecraft.nbt.NbtCompound tag = data.copyNbt();
            tag.putInt("theendupdate_sh_map", 1);
            tag.putInt("theendupdate_target_x", target.getX());
            tag.putInt("theendupdate_target_z", target.getZ());
            try { tag.putString("theendupdate_target_dim", serverWorld.getRegistryKey().getValue().toString()); } catch (Throwable ignoredDim) {}
            mapStack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        } catch (Throwable ignored) {}

        // Non-consuming legacy path
        if (!player.getInventory().insertStack(mapStack)) {
            player.dropItem(mapStack, false);
        }

        return ActionResult.CONSUME;
    }

    private ActionResult createNewMapAndConsume(ServerWorld serverWorld, ServerPlayerEntity player, ItemStack inHand, Hand hand) {
        // Find nearest hollow tree
        BlockPos origin = player.getBlockPos();
        BlockPos target = locateHollowTreeStatic(serverWorld, origin);
        if (target == null) {
            return ActionResult.CONSUME;
        }

        // Create filled map centered near target
        int scale = 4;
        ItemStack filled = net.minecraft.item.FilledMapItem.createMap(serverWorld, target.getX(), target.getZ(), (byte) scale, true, true);
        
        // Convert to our custom filled map item for proper recipe remainder handling
        ItemStack customFilled = new ItemStack(com.theendupdate.registry.ModItems.SHADOW_HUNTERS_FILLED_MAP);
        // Copy the map ID from the vanilla filled map
        try {
            net.minecraft.component.type.MapIdComponent mapId = filled.get(net.minecraft.component.DataComponentTypes.MAP_ID);
            if (mapId != null) {
                customFilled.set(net.minecraft.component.DataComponentTypes.MAP_ID, mapId);
            }
        } catch (Throwable ignored) {}

        // Add marker decoration
        MapState state = net.minecraft.item.FilledMapItem.getMapState(filled, serverWorld);
        if (state != null) {
            try {
                addShadowHunterDecoration(state, serverWorld, target.getX() + 0.5, target.getZ() + 0.5, Math.PI);
            } catch (Throwable t) {
                com.theendupdate.TemplateMod.LOGGER.error("Failed to add map decoration in createNewMapAndConsume: " + t.getMessage(), t);
            }
        }

        // Tag as Shadow Hunter map for texture/icon purposes and persist target
        try {
            net.minecraft.component.type.NbtComponent data = customFilled.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT);
            net.minecraft.nbt.NbtCompound tag = data.copyNbt();
            tag.putInt("theendupdate_sh_map", 1);
            tag.putInt("theendupdate_target_x", target.getX());
            tag.putInt("theendupdate_target_z", target.getZ());
            try { tag.putString("theendupdate_target_dim", serverWorld.getRegistryKey().getValue().toString()); } catch (Throwable ignoredDim) {}
            customFilled.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(tag));
        } catch (Throwable ignored) {}

        // Consume one empty map from player's hand and give the filled map back
        if (inHand.getCount() <= 1) {
            // Replace the held stack with the filled map so the player immediately sees it
            player.setStackInHand(hand, customFilled);
        } else {
            inHand.decrement(1);
            if (!player.getInventory().insertStack(customFilled)) {
                player.dropItem(customFilled, false);
            }
        }

        return ActionResult.SUCCESS;
    }

    private void recenterAndReplaceFilledMap(ServerWorld serverWorld, ServerPlayerEntity player, ItemStack oldMap, BlockPos newCenter, int idx) {
        final int scale = 4; // keep consistent scale
        int centerX = newCenter.getX();
        int centerZ = newCenter.getZ();

        ItemStack newMap = net.minecraft.item.FilledMapItem.createMap(serverWorld, centerX, centerZ, (byte) scale, true, true);

        // Add our decoration
        MapState newState = net.minecraft.item.FilledMapItem.getMapState(newMap, serverWorld);
        if (newState != null) {
            try {
                addShadowHunterDecoration(newState, serverWorld, newCenter.getX() + 0.5, newCenter.getZ() + 0.5, Math.PI);
            } catch (Throwable t) {
                com.theendupdate.TemplateMod.LOGGER.error("Failed to add map decoration (recenter): " + t.getMessage(), t);
            }
        }

        // Tag as Shadow Hunter's map and carry over index
        try {
            net.minecraft.component.type.NbtComponent data = newMap.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT);
            net.minecraft.nbt.NbtCompound newTag = data.copyNbt();
            newTag.putInt("theendupdate_sh_map", 1);
            newTag.putInt("theendupdate_map_idx", idx);
            newTag.putInt("theendupdate_target_x", newCenter.getX());
            newTag.putInt("theendupdate_target_z", newCenter.getZ());
            try { newTag.putString("theendupdate_target_dim", serverWorld.getRegistryKey().getValue().toString()); } catch (Throwable ignoredDim) {}
            newMap.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(newTag));
        } catch (Throwable ignored) {}

        // Replace old map in-place if possible
        boolean replaced = false;
        if (player.getMainHandStack() == oldMap) {
            player.setStackInHand(Hand.MAIN_HAND, newMap);
            replaced = true;
        } else if (player.getOffHandStack() == oldMap) {
            player.setStackInHand(Hand.OFF_HAND, newMap);
            replaced = true;
        } else {
            int slot = player.getInventory().getSlotWithStack(oldMap);
            if (slot >= 0) {
                player.getInventory().setStack(slot, newMap);
                replaced = true;
            }
        }

        if (!replaced) {
            // Fallback: remove one old, insert new
            player.getInventory().removeOne(oldMap);
            if (!player.getInventory().insertStack(newMap)) {
                player.dropItem(newMap, false);
            }
        }

        com.theendupdate.TemplateMod.LOGGER.info("Shadow Hunter's Map: Recentered map to {} and replaced existing filled map", newCenter);
    }

    private static BlockPos locateNthHollowTreeStatic(ServerWorld world, BlockPos origin, int n) {
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        net.minecraft.util.Identifier id = com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_KEY.getValue();
        var entry = registry.getEntry(id).orElse(null);
        if (entry == null) return null;
        
        // Find structures iteratively, searching from beyond each found structure
        java.util.List<BlockPos> foundStructures = new java.util.ArrayList<>();
        int requiredStructures = n + 1;
        BlockPos currentSearchPos = origin;
        
        com.theendupdate.TemplateMod.LOGGER.info("Searching for {} structures starting from {}", requiredStructures, origin);
        
        // Keep searching until we have enough structures or we've searched too far
        for (int i = 0; i < requiredStructures && i < 20; i++) { // Max 20 structures
            // Search from current position with skipExistingChunks=false for better results
            com.mojang.datafixers.util.Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(
                world,
                RegistryEntryList.of(entry),
                currentSearchPos,
                100, // Search radius in chunks
                false
            );
            
            if (pair != null) {
                BlockPos found = pair.getFirst();
                
                // Check if we've already found this structure (within 16 blocks)
                boolean isDuplicate = false;
                for (BlockPos existing : foundStructures) {
                    if (existing.getSquaredDistance(found) < 256) { // 16^2 = 256
                        isDuplicate = true;
                        break;
                    }
                }
                
                if (!isDuplicate) {
                    foundStructures.add(found);
                    com.theendupdate.TemplateMod.LOGGER.info("Found structure #{} at {}", foundStructures.size(), found);
                    
                    // For next search, start from a point beyond this structure
                    // Calculate direction from origin to found structure
                    int dx = found.getX() - origin.getX();
                    int dz = found.getZ() - origin.getZ();
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    
                    if (distance > 10) {
                        // Move search position beyond this structure
                        double scale = (distance + 500) / distance; // Add 500 blocks
                        currentSearchPos = origin.add((int)(dx * scale), 0, (int)(dz * scale));
                    } else {
                        // If structure is very close, search in a different direction
                        int angle = i * 60; // Spread searches in different directions
                        double rad = Math.toRadians(angle);
                        currentSearchPos = origin.add((int)(Math.cos(rad) * (i + 1) * 500), 0, (int)(Math.sin(rad) * (i + 1) * 500));
                    }
                } else {
                    // Duplicate found, try searching from a different angle
                    int angle = (i * 73) % 360; // Use prime number for better spread
                    double rad = Math.toRadians(angle);
                    int dist = (i + 1) * 300;
                    currentSearchPos = origin.add((int)(Math.cos(rad) * dist), 0, (int)(Math.sin(rad) * dist));
                    com.theendupdate.TemplateMod.LOGGER.info("Duplicate found, trying new search position: {}", currentSearchPos);
                }
            } else {
                // No structure found, try a different direction
                int angle = (i * 73) % 360;
                double rad = Math.toRadians(angle);
                int dist = (i + 1) * 500;
                currentSearchPos = origin.add((int)(Math.cos(rad) * dist), 0, (int)(Math.sin(rad) * dist));
                com.theendupdate.TemplateMod.LOGGER.info("No structure found, trying new search position: {}", currentSearchPos);
            }
        }
        
        if (foundStructures.isEmpty()) {
            com.theendupdate.TemplateMod.LOGGER.warn("No hollow shadow trees found near {}", origin);
            return null;
        }
        
        com.theendupdate.TemplateMod.LOGGER.info("Found {} total structures, requesting index {}", foundStructures.size(), n);
        
        // Sort by distance from origin (nearest first)
        foundStructures.sort((a, b) -> {
            double distA = origin.getSquaredDistance(a);
            double distB = origin.getSquaredDistance(b);
            return Double.compare(distA, distB);
        });
        
        // Wrap around if n exceeds the number of found structures
        int index = n % foundStructures.size();
        BlockPos result = foundStructures.get(index);
        com.theendupdate.TemplateMod.LOGGER.info("Returning structure at index {} (mod {}): {}", n, index, result);
        return result;
    }
    private static BlockPos locateHollowTreeStatic(ServerWorld world, BlockPos origin) {
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        net.minecraft.util.Identifier id = com.theendupdate.registry.ModStructures.SHADOW_HOLLOW_TREE_KEY.getValue();
        var entry = registry.getEntry(id).orElse(null);
        if (entry == null) return null;
        com.mojang.datafixers.util.Pair<BlockPos, RegistryEntry<Structure>> pair = world.getChunkManager().getChunkGenerator().locateStructure(
            world,
            RegistryEntryList.of(entry),
            origin,
            512,
            false
        );
        return pair == null ? null : pair.getFirst();
    }
    
    // Restore the map decoration from stored NBT (called on player join or when needed)
    public static void restoreDecorationFromNbt(ServerWorld serverWorld, ItemStack filledMap) {
        if (filledMap == null || filledMap.isEmpty() || !filledMap.isOf(net.minecraft.item.Items.FILLED_MAP)) return;
        net.minecraft.component.type.NbtComponent custom = filledMap.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (custom == null || !custom.contains("theendupdate_sh_map")) return;
        net.minecraft.nbt.NbtCompound tag = custom.copyNbt();
        int tx = tag.getInt("theendupdate_target_x").orElse(0);
        int tz = tag.getInt("theendupdate_target_z").orElse(0);
        if (tx == 0 && tz == 0) {
            // Fallback to map center if target not stored yet (older maps)
            MapState state = net.minecraft.item.FilledMapItem.getMapState(filledMap, serverWorld);
            if (state != null) {
                try {
                    java.lang.reflect.Method getCenterX = MapState.class.getDeclaredMethod("getCenterX");
                    java.lang.reflect.Method getCenterZ = MapState.class.getDeclaredMethod("getCenterZ");
                    getCenterX.setAccessible(true);
                    getCenterZ.setAccessible(true);
                    Object cx = getCenterX.invoke(state);
                    Object cz = getCenterZ.invoke(state);
                    if (cx instanceof Integer && cz instanceof Integer) {
                        tx = (Integer) cx;
                        tz = (Integer) cz;
                    }
                } catch (Throwable ignored) {}
            }
            if (tx == 0 && tz == 0) return;
        }
        MapState state = net.minecraft.item.FilledMapItem.getMapState(filledMap, serverWorld);
        if (state == null) return;
        try { addShadowHunterDecoration(state, serverWorld, tx + 0.5, tz + 0.5, Math.PI); } catch (Throwable ignored) {}
    }

    // Add/update the Shadow Hunter map decoration at a fixed id so duplicates aren't created
    private static void addShadowHunterDecoration(MapState state, ServerWorld serverWorld, double x, double z, double rot) throws Exception {
        RegistryEntry<MapDecorationType> marker = RegistryEntry.of(com.theendupdate.registry.ModMapDecorations.SHADOW_HUNTER);
        java.lang.reflect.Method m = MapState.class.getDeclaredMethod(
            "addDecoration",
            RegistryEntry.class,
            Class.forName("net.minecraft.world.WorldAccess"),
            String.class,
            double.class,
            double.class,
            double.class,
            Class.forName("net.minecraft.text.Text")
        );
        m.setAccessible(true);
        m.invoke(state, marker, serverWorld, "theendupdate_shadow_hunter_target", x, z, rot, null);
        try {
            java.lang.reflect.Method markDirty = MapState.class.getDeclaredMethod("markDirty");
            markDirty.setAccessible(true);
            markDirty.invoke(state);
        } catch (Throwable t) {
            try {
                java.lang.reflect.Method setDirty = MapState.class.getDeclaredMethod("setDirty", boolean.class);
                setDirty.setAccessible(true);
                setDirty.invoke(state, true);
            } catch (Throwable ignored) {}
        }
    }
}


