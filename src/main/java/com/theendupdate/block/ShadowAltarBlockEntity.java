package com.theendupdate.block;

import com.theendupdate.registry.ModEntities;
import com.theendupdate.entity.ShadowCreakingBossBarRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import java.util.UUID;

public class ShadowAltarBlockEntity extends BlockEntity {
	private static final int ACTIVE_DURATION_TICKS = 15 * 20; // 15 seconds
	private static final int COOLDOWN_TICKS = 120 * 20; // 2 minutes
	private static final int MIN_RANGE = 8; // strictly greater than 7
	private static final int MAX_RANGE = 20;
	private static final int CLEARANCE_HEIGHT = 4;

	private int activeTicksRemaining;
	private int cooldownTicksRemaining;
	private BlockPos targetPos;
	private UUID pendingEntityUuid; // UUID for the entity that will spawn

	public ShadowAltarBlockEntity(BlockPos pos, BlockState state) {
		super(com.theendupdate.registry.ModBlockEntities.SHADOW_ALTAR, pos, state);
	}

	public boolean canActivate() {
		return this.activeTicksRemaining <= 0 && this.cooldownTicksRemaining <= 0;
	}

	public boolean tryStart(ServerWorld world) {
		if (!canActivate()) return false;
		BlockPos found = findSpawnTarget(world, this.pos);
		if (found == null) return false;
		this.targetPos = found;
		this.activeTicksRemaining = ACTIVE_DURATION_TICKS;
		
		// Generate UUID for the entity that will spawn and start boss bar charging
		this.pendingEntityUuid = UUID.randomUUID();
		ShadowCreakingBossBarRegistry.createChargingBossBar(this.pendingEntityUuid, world);
		
		markDirty();
		return true;
	}

	public static void tick(World world, BlockPos pos, BlockState state, ShadowAltarBlockEntity altar) {
		if (world.isClient) return;
		ServerWorld server = (ServerWorld) world;

		if (altar.cooldownTicksRemaining > 0) {
			altar.cooldownTicksRemaining--;
			emitCoreParticles(server, pos);
			return;
		}

		if (altar.activeTicksRemaining > 0) {
			altar.activeTicksRemaining--;
			emitCoreParticles(server, pos);
			if (altar.targetPos != null) {
				emitSoulLine(server, pos, altar.targetPos, world.getRandom());
			}
			if (altar.activeTicksRemaining == 0) {
				// Spawn entity at target and enter cooldown
				if (altar.targetPos != null) {
					EntityType<?> type = ModEntities.SHADOW_CREAKING;
					var spawned = type.spawn(server, null, altar.targetPos, SpawnReason.TRIGGERED, true, false);
					if (spawned instanceof com.theendupdate.entity.ShadowCreakingEntity sce) {
						try { sce.addCommandTag("theendupdate:spawned_by_altar"); } catch (Throwable ignored) {}
						
					// Continue using the existing charging boss bar and add the entity to it
					if (altar.pendingEntityUuid != null) {
						com.theendupdate.entity.ShadowCreakingBossBarManager chargingManager = 
							com.theendupdate.entity.ShadowCreakingBossBarRegistry.getBossBar(altar.pendingEntityUuid);
						if (chargingManager != null) {
							// Transfer the charging boss bar to the entity and start tracking it
							sce.bossBarManager = chargingManager;
							chargingManager.startBossFight(sce, true);
							
							// Update the registry to track this boss bar under the entity's UUID instead
							com.theendupdate.entity.ShadowCreakingBossBarRegistry.transferBossBar(altar.pendingEntityUuid, sce.getUuid());
							
							com.theendupdate.TemplateMod.LOGGER.info("Transferred charging boss bar to spawned entity {} with {} charging ticks", 
								sce.getUuid(), chargingManager.chargingTicks);
						} else {
							// Fallback: charging manager not found, create a new one
							com.theendupdate.TemplateMod.LOGGER.warn("Charging boss bar not found for UUID {}, creating new one", altar.pendingEntityUuid);
							sce.initializeBossBar(true);
						}
					} else {
						// Fallback: no pending UUID, create a new boss bar
						sce.initializeBossBar(true);
					}
					}
				}
				altar.targetPos = null;
				altar.pendingEntityUuid = null;
				altar.cooldownTicksRemaining = COOLDOWN_TICKS;
			}
		}
	}

	private static void emitCoreParticles(ServerWorld world, BlockPos pos) {
		Vec3d center = Vec3d.ofCenter(pos);
		world.spawnParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 2, 0.10, 0.10, 0.10, 0.01);
	}

	private static void emitSoulLine(ServerWorld world, BlockPos from, BlockPos to, Random random) {
		Vec3d a = Vec3d.ofCenter(from);
		Vec3d b = Vec3d.ofCenter(to).add(0, 0.1, 0);
		Vec3d delta = b.subtract(a);
		double length = delta.length();
		if (length < 0.001) return;
		Vec3d dir = delta.multiply(1.0 / length);
		int points = Math.max(8, MathHelper.floor(length * 6.0));
		for (int i = 0; i < points; i++) {
			double t = (i + random.nextDouble() * 0.25) / points;
			Vec3d p = a.add(dir.multiply(length * t));
			world.spawnParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	private static BlockPos findSpawnTarget(ServerWorld world, BlockPos origin) {
		Random random = world.getRandom();
		// Sample candidates biased outward to satisfy >7 distance
		for (int attempts = 0; attempts < 180; attempts++) {
			int dx = random.nextBetween(-MAX_RANGE, MAX_RANGE);
			int dz = random.nextBetween(-MAX_RANGE, MAX_RANGE);
			if (dx * dx + dz * dz < MIN_RANGE * MIN_RANGE) continue;
			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;
			int y = origin.getY();
			// Probe up and down a little to find ground near the altar's Y
			for (int dy = -6; dy <= 6; dy++) {
				BlockPos base = new BlockPos(x, y + dy, z);
				if (!isClearForSpawn(world, base)) continue;
				return base;
			}
		}
		// Fallback radial search if random sampling failed
		int r2 = MAX_RANGE * MAX_RANGE;
		for (int dx = -MAX_RANGE; dx <= MAX_RANGE; dx++) {
			for (int dz = -MAX_RANGE; dz <= MAX_RANGE; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 < MIN_RANGE * MIN_RANGE || d2 > r2) continue;
				BlockPos base = origin.add(dx, 0, dz);
				for (int dy = -6; dy <= 6; dy++) {
					BlockPos b = base.up(dy);
					if (isClearForSpawn(world, b)) return b;
				}
			}
		}
		return null;
	}

	private static boolean isClearForSpawn(ServerWorld world, BlockPos base) {
		// Ensure feet position and 3 blocks above are clear, and solid/support below
		BlockPos below = base.down();
		if (!world.getBlockState(below).isSolidBlock(world, below)) return false;
		for (int i = 0; i < CLEARANCE_HEIGHT; i++) {
			BlockPos p = base.up(i);
			if (!world.isAir(p)) return false;
		}
		// basic safety: avoid fluids and avoid outside world border
		if (!world.getWorldBorder().contains(new Box(base))) return false;
		return true;
	}
}


