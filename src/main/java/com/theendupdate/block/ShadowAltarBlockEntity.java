package com.theendupdate.block;

import com.theendupdate.entity.ShadowCreakingEntity;
import com.theendupdate.registry.ModEntities;
import com.theendupdate.entity.ShadowCreakingBossBarRegistry;
import com.theendupdate.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShadowAltarBlockEntity extends BlockEntity {
	private static final int ACTIVE_DURATION_TICKS = 15 * 20; // 15 seconds
	/** Soul-particle buildup at the spawn site before the boss appears. */
	public static final int ALTAR_SUMMON_TICKS = 70;
	private static final int COOLDOWN_TICKS = 120 * 20; // 2 minutes
	private static final int MIN_RANGE = 8; // strictly greater than 7
	private static final int MAX_RANGE = 20;
	private static final int CLEARANCE_HEIGHT = 4;
	private static final double ALTAR_LIT_SOUND_RANGE_SQ = 64.0 * 64.0;

	private int activeTicksRemaining;
	private int summonTicksRemaining;
	private int cooldownTicksRemaining;
	private BlockPos targetPos;
	private UUID pendingEntityUuid;

	public ShadowAltarBlockEntity(BlockPos pos, BlockState state) {
		super(com.theendupdate.registry.ModBlockEntities.SHADOW_ALTAR, pos, state);
	}

	public boolean canActivate() {
		return this.activeTicksRemaining <= 0 && this.summonTicksRemaining <= 0 && this.cooldownTicksRemaining <= 0;
	}

	public boolean tryStart(ServerLevel world) {
		if (!canActivate()) return false;
		BlockPos found = findSpawnTarget(world, this.worldPosition);
		if (found == null) return false;
		this.targetPos = found;
		this.activeTicksRemaining = ACTIVE_DURATION_TICKS;

		this.pendingEntityUuid = UUID.randomUUID();
		ShadowCreakingBossBarRegistry.createChargingBossBar(this.pendingEntityUuid, world, this.worldPosition);

		playAltarLitForNearbyPlayers(world, this.worldPosition);

		setChanged();
		return true;
	}

	private static void playAltarLitForNearbyPlayers(ServerLevel world, BlockPos altarPos) {
		Vec3 center = Vec3.atCenterOf(altarPos);
		long seed = world.getRandom().nextLong();
		var sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SHADOW_ALTAR_LIT);
		for (ServerPlayer player : world.players()) {
			if (player.position().distanceToSqr(center) <= ALTAR_LIT_SOUND_RANGE_SQ) {
				// send directly to each listener, unattenuated - world.playSound excludes its first arg from hearing it
				player.connection.send(new ClientboundSoundPacket(
					sound,
					SoundSource.BLOCKS,
					player.getX(),
					player.getY(),
					player.getZ(),
					1.0F,
					1.0F,
					seed
				));
			}
		}
	}

	public static void tick(Level world, BlockPos pos, BlockState state, ShadowAltarBlockEntity altar) {
		if (world.isClientSide()) return;
		ServerLevel server = (ServerLevel) world;

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
				altar.summonTicksRemaining = ALTAR_SUMMON_TICKS;
			}
			return;
		}

		if (altar.summonTicksRemaining > 0) {
			altar.summonTicksRemaining--;
			emitCoreParticles(server, pos);
			if (altar.targetPos != null) {
				emitSoulLine(server, pos, altar.targetPos, world.getRandom());
				emitSummonParticles(server, altar.targetPos, altar.summonTicksRemaining);
			}
			if (altar.summonTicksRemaining == 0) {
				spawnBossAtTarget(server, altar);
				altar.targetPos = null;
				altar.pendingEntityUuid = null;
				altar.cooldownTicksRemaining = COOLDOWN_TICKS;
			}
		}
	}

	private static void spawnBossAtTarget(ServerLevel server, ShadowAltarBlockEntity altar) {
		if (altar.targetPos == null) {
			return;
		}
		EntityType<?> type = ModEntities.SHADOW_CREAKING;
		var spawned = type.spawn(server, null, altar.targetPos, EntitySpawnReason.TRIGGERED, true, false);
		if (!(spawned instanceof ShadowCreakingEntity sce)) {
			return;
		}
		try {
			sce.addTag("theendupdate:spawned_by_altar");
		} catch (Throwable ignored) {
		}
		sce.beginSpawnReveal();

		if (altar.pendingEntityUuid != null) {
			com.theendupdate.entity.ShadowCreakingBossBarManager chargingManager =
				com.theendupdate.entity.ShadowCreakingBossBarRegistry.getBossBar(altar.pendingEntityUuid);
			if (chargingManager != null) {
				sce.bossBarManager = chargingManager;
				chargingManager.startBossFight(sce, true);
				com.theendupdate.entity.ShadowCreakingBossBarRegistry.transferBossBar(altar.pendingEntityUuid, sce.getUUID());
				com.theendupdate.TheEndUpdate.LOGGER.info(
					"Transferred charging boss bar to spawned entity {} with {} charging ticks",
					sce.getUUID(), chargingManager.chargingTicks);
			} else {
				com.theendupdate.TheEndUpdate.LOGGER.warn(
					"Charging boss bar not found for UUID {}, creating new one", altar.pendingEntityUuid);
				sce.initializeBossBar(true);
			}
		} else {
			sce.initializeBossBar(true);
		}
	}

	private static void emitSummonParticles(ServerLevel world, BlockPos target, int ticksRemaining) {
		Vec3 center = Vec3.atCenterOf(target);
		float progress = 1.0F - ticksRemaining / (float) ALTAR_SUMMON_TICKS;
		RandomSource random = world.getRandom();
		int soulCount = 4 + Mth.floor(progress * 16.0F);
		for (int i = 0; i < soulCount; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double radius = 0.3 + progress * 2.2 + random.nextDouble() * 0.6;
			double height = random.nextDouble() * (0.5 + progress * 3.5);
			world.sendParticles(
				ParticleTypes.SOUL,
				center.x + Math.cos(angle) * radius,
				center.y + height,
				center.z + Math.sin(angle) * radius,
				1, 0.0, 0.06, 0.0, 0.03);
		}
		if (progress > 0.5F) {
			world.sendParticles(
				ParticleTypes.SOUL_FIRE_FLAME,
				center.x, center.y + 0.5 + progress, center.z,
				1 + (int) (progress * 4.0F), 0.35, 0.25, 0.35, 0.01);
		}
		if (ticksRemaining <= 8) {
			for (int i = 0; i < 6; i++) {
				double theta = random.nextDouble() * Math.PI * 2.0;
				double radius = 1.0 + random.nextDouble() * 2.0;
				world.sendParticles(
					ParticleTypes.SOUL_FIRE_FLAME,
					center.x + Math.cos(theta) * radius,
					center.y + 0.2,
					center.z + Math.sin(theta) * radius,
					1, 0.0, 0.1, 0.0, 0.02);
			}
		}
	}

	private static void emitCoreParticles(ServerLevel world, BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		world.sendParticles(ParticleTypes.SOUL, center.x, center.y, center.z, 2, 0.10, 0.10, 0.10, 0.01);
	}

	private static void emitSoulLine(ServerLevel world, BlockPos from, BlockPos to, RandomSource random) {
		Vec3 a = Vec3.atCenterOf(from);
		Vec3 b = Vec3.atCenterOf(to).add(0, 0.1, 0);
		Vec3 delta = b.subtract(a);
		double length = delta.length();
		if (length < 0.001) return;
		Vec3 dir = delta.scale(1.0 / length);
		int points = Math.max(8, Mth.floor(length * 6.0));
		for (int i = 0; i < points; i++) {
			double t = (i + random.nextDouble() * 0.25) / points;
			Vec3 p = a.add(dir.scale(length * t));
			world.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	private static BlockPos findSpawnTarget(ServerLevel world, BlockPos origin) {
		RandomSource random = world.getRandom();
		for (int attempts = 0; attempts < 180; attempts++) {
			int dx = random.nextIntBetweenInclusive(-MAX_RANGE, MAX_RANGE);
			int dz = random.nextIntBetweenInclusive(-MAX_RANGE, MAX_RANGE);
			if (dx * dx + dz * dz < MIN_RANGE * MIN_RANGE) continue;
			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;
			int y = origin.getY();
			for (int dy = -6; dy <= 6; dy++) {
				BlockPos base = new BlockPos(x, y + dy, z);
				if (!isClearForSpawn(world, base)) continue;
				return base;
			}
		}
		// random sampling failed, fall back to exhaustive radial search
		int r2 = MAX_RANGE * MAX_RANGE;
		for (int dx = -MAX_RANGE; dx <= MAX_RANGE; dx++) {
			for (int dz = -MAX_RANGE; dz <= MAX_RANGE; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 < MIN_RANGE * MIN_RANGE || d2 > r2) continue;
				BlockPos base = origin.offset(dx, 0, dz);
				for (int dy = -6; dy <= 6; dy++) {
					BlockPos b = base.above(dy);
					if (isClearForSpawn(world, b)) return b;
				}
			}
		}
		return null;
	}

	private static boolean isClearForSpawn(ServerLevel world, BlockPos base) {
		BlockPos below = base.below();
		if (!world.getBlockState(below).isRedstoneConductor(world, below)) return false;
		for (int i = 0; i < CLEARANCE_HEIGHT; i++) {
			BlockPos p = base.above(i);
			if (!world.isEmptyBlock(p)) return false;
		}
		if (!world.getWorldBorder().isWithinBounds(new AABB(base))) return false;
		return true;
	}

	// clears the pending boss bar if the altar gets broken mid-charge
	public void cleanup() {
		if (this.pendingEntityUuid != null) {
			ShadowCreakingBossBarRegistry.removeBossBar(this.pendingEntityUuid);
			this.pendingEntityUuid = null;
		}
	}
}
