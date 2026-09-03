package com.theendupdate.world;

import com.theendupdate.entity.EyesEntity;
import com.theendupdate.registry.ModEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Spawns Eyes around Shadowlands players on a 0.5–15s cadence, up to 8 nearby.
 */
public final class ShadowlandsEyesSpawner {
    private static final int MAX_NEARBY = 8;
    private static final int ATTEMPTS_PER_SPAWN = 22;
    private static final double SEARCH_RANGE = 80.0;
    private static final double MIN_HORIZONTAL = 20.0;
    private static final double MAX_HORIZONTAL = 52.0;
    private static final float NEAR_GROUND_CHANCE = 0.05f;
    private static final float GROUP_CHANCE = 0.38f;

    private static final Map<UUID, Long> NEXT_SPAWN_AT = new HashMap<>();

    private ShadowlandsEyesSpawner() {}

    public static void init() {
        ServerTickEvents.END_LEVEL_TICK.register(ShadowlandsEyesSpawner::onWorldTick);
    }

    private static void onWorldTick(ServerLevel world) {
        if (!world.dimension().equals(Level.END)) {
            return;
        }

        RandomSource random = world.getRandom();
        long time = world.getGameTime();
        for (ServerPlayer player : world.players()) {
            UUID id = player.getUUID();
            if (player.isSpectator() || !player.isAlive() || !isInShadowlands(world, player)) {
                NEXT_SPAWN_AT.remove(id);
                continue;
            }

            if (!NEXT_SPAWN_AT.containsKey(id)) {
                NEXT_SPAWN_AT.put(id, time + randomDelayTicks(random));
                continue;
            }

            long nextAt = NEXT_SPAWN_AT.get(id);
            if (time < nextAt) {
                continue;
            }

            int nearby = countNearbyEyes(world, player);
            if (nearby >= MAX_NEARBY) {
                NEXT_SPAWN_AT.put(id, time + 20L);
                continue;
            }

            int wanted = 1;
            if (random.nextFloat() < GROUP_CHANCE) {
                wanted = 2 + random.nextInt(4);
            }
            wanted = Math.min(wanted, MAX_NEARBY - nearby);

            int spawned = spawnCluster(world, player, random, wanted);
            NEXT_SPAWN_AT.put(id, time + (spawned > 0 ? randomDelayTicks(random) : 20L));
        }
    }

    private static long randomDelayTicks(RandomSource random) {
        return 10L + random.nextInt(291);
    }

    private static boolean isInShadowlands(ServerLevel world, ServerPlayer player) {
        if (ShadowlandsBiomeIdentity.isShadowlands(world.getBiome(player.blockPosition()))) {
            return true;
        }
        return OuterEndLayout.isShadowlands(player.getBlockX(), player.getBlockZ());
    }

    private static int spawnCluster(ServerLevel world, ServerPlayer player, RandomSource random, int wanted) {
        double dist = MIN_HORIZONTAL + random.nextDouble() * (MAX_HORIZONTAL - MIN_HORIZONTAL);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double cx = player.getX() + Math.cos(angle) * dist;
        double cz = player.getZ() + Math.sin(angle) * dist;
        double cy = pickSpawnY(world, player, cx, cz, random);

        int spawned = 0;
        for (int i = 0; i < wanted; i++) {
            double x = cx;
            double y = cy;
            double z = cz;
            if (i > 0) {
                x += (random.nextDouble() - 0.5) * 10.0;
                y += (random.nextDouble() - 0.5) * 6.0;
                z += (random.nextDouble() - 0.5) * 10.0;
            }
            if (tryPlaceEyes(world, x, y, z, random)) {
                spawned++;
            }
        }
        return spawned;
    }

    private static boolean tryPlaceEyes(ServerLevel world, double x, double y, double z, RandomSource random) {
        int minY = world.getMinY() + 8;
        int maxY = world.getMinY() + world.getHeight() - 8;
        if (y < minY || y > maxY) {
            return false;
        }
        for (int attempt = 0; attempt < ATTEMPTS_PER_SPAWN; attempt++) {
            double px = x + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 6.0);
            double py = y + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 4.0);
            double pz = z + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 6.0);
            BlockPos pos = BlockPos.containing(px, py, pz);
            if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (!world.isEmptyBlock(pos) || !world.getFluidState(pos).isEmpty()) {
                continue;
            }
            EyesEntity eyes = new EyesEntity(ModEntities.EYES, world);
            eyes.setDisplayScale(1.0F + random.nextFloat() * 5.0F);
            eyes.snapTo(px, py, pz, random.nextFloat() * 360.0F, 0.0F);
            if (world.addFreshEntity(eyes)) {
                return true;
            }
        }
        return false;
    }

    private static double pickSpawnY(ServerLevel world, ServerPlayer player, double x, double z, RandomSource random) {
        int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        boolean hasGround = surfaceY > world.getMinY() + 16;
        if (hasGround && random.nextFloat() < NEAR_GROUND_CHANCE) {
            return surfaceY + 3.0 + random.nextDouble() * 5.0;
        }
        double highBase = hasGround ? surfaceY + 16.0 : player.getY() + 16.0;
        highBase = Math.max(highBase, player.getY() + 14.0);
        return highBase + random.nextDouble() * 18.0;
    }

    private static int countNearbyEyes(ServerLevel world, Player player) {
        AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
        int count = 0;
        for (EyesEntity ignored : world.getEntitiesOfClass(EyesEntity.class, box, EyesEntity::isAlive)) {
            count++;
        }
        return count;
    }
}
