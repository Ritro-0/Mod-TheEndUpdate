package com.theendupdate.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ShulkerEntity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ShulkerSpawnQueue {
    private record SpawnRequest(RegistryKey<World> worldKey, BlockPos pos, Direction face, int attempts) {}

    private static final Queue<SpawnRequest> QUEUE = new ConcurrentLinkedQueue<>();

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            int processed = 0;
            int maxPerTick = 32;
            SpawnRequest req;
            while (processed < maxPerTick && (req = QUEUE.poll()) != null) {
                if (!world.getRegistryKey().equals(req.worldKey)) {
                    // Not this world; push back for the proper world tick
                    QUEUE.add(req);
                    continue;
                }

                // Ensure chunk is loaded before spawning
                if (!world.isChunkLoaded(req.pos)) {
                    if (req.attempts() < 20) {
                        QUEUE.add(new SpawnRequest(req.worldKey, req.pos, req.face, req.attempts() + 1));
                    }
                    continue;
                }

                ShulkerEntity shulker = EntityType.SHULKER.spawn(world, req.pos, net.minecraft.entity.SpawnReason.STRUCTURE);
                if (shulker == null) {
                    continue;
                }
                shulker.setPersistent();
                // Rely on default orientation; attached face setter may be private or obfuscated
                shulker.refreshPositionAndAngles(req.pos, 0.0F, 0.0F);
                world.spawnEntity(shulker);
                processed++;
            }
        });
    }

    public static void enqueue(ServerWorld world, BlockPos pos, Direction face) {
        QUEUE.add(new SpawnRequest(world.getRegistryKey(), pos.toImmutable(), face, 0));
    }

    private ShulkerSpawnQueue() {}
}


