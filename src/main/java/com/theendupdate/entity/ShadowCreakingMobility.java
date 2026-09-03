package com.theendupdate.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Anti-cheese pathfinding from the legacy Shadow Creaking: jumping, manual shoves around
 * obstacles, suffocation escape, and blink teleports when a target is walled off.
 */
final class ShadowCreakingMobility {
	private static final int JUMP_COOLDOWN_TICKS = 20;
	private static final int STUCK_SHOVE_TICKS = 10;
	private static final int STUCK_TELEPORT_TICKS = 120;
	private static final int TELEPORT_COOLDOWN_TICKS = 120;
	private static final int MOBILITY_GRACE_TICKS = 100;
	private static final double TELEPORT_MIN_RADIUS = 5.0;
	private static final double TELEPORT_MAX_RADIUS = 10.0;
	private static final double MELEE_REACH = 2.0;

	private final ShadowCreakingEntity mob;
	private int jumpCooldownTicks;
	private boolean isJumping;
	private int jumpStateTicks;
	private int noProgressTicks;
	private int stuckTeleportNoProgressTicks;
	private int stuckTeleportNoApproachTicks;
	private double prevDistanceToTarget;
	private int teleportCooldownTicks;
	private int mobilityGraceTicks;
	private double lastX;
	private double lastZ;

	ShadowCreakingMobility(ShadowCreakingEntity mob) {
		this.mob = mob;
		this.lastX = mob.getX();
		this.lastZ = mob.getZ();
	}

	void onMobilityEnabled() {
		this.resetStuckCounters();
		this.lastX = this.mob.getX();
		this.lastZ = this.mob.getZ();
		this.prevDistanceToTarget = Double.MAX_VALUE;
		this.mobilityGraceTicks = MOBILITY_GRACE_TICKS;
	}

	void tick() {
		if (this.mob.level().isClientSide()) {
			return;
		}
		if (this.jumpCooldownTicks > 0) {
			this.jumpCooldownTicks--;
		}
		if (this.teleportCooldownTicks > 0) {
			this.teleportCooldownTicks--;
		}
		if (this.mobilityGraceTicks > 0) {
			this.mobilityGraceTicks--;
		}
		if (this.isJumping) {
			this.jumpStateTicks++;
			if (this.mob.onGround()
				|| this.jumpStateTicks > 20
				|| (Math.abs(this.mob.getDeltaMovement().y) < 0.1 && this.jumpStateTicks > 5)) {
				this.isJumping = false;
				this.jumpStateTicks = 0;
			}
		}
		if (this.mob.isWeepingAngelActive() && !this.mob.isActive()) {
			this.resetStuckCounters();
			return;
		}
		LivingEntity target = this.mob.getTarget();
		if (target != null
			&& target.isAlive()
			&& !this.mob.isInCombatWarmup()
			&& this.mob.getCombatPhase() == ShadowCreakingEntity.PHASE_IDLE) {
			this.tickStuckTracking(target);
		}
	}

	void tickStuckTracking(LivingEntity target) {
		if (!this.isWalledOffFromTarget(target)) {
			this.stuckTeleportNoProgressTicks = 0;
			this.stuckTeleportNoApproachTicks = 0;
			this.lastX = this.mob.getX();
			this.lastZ = this.mob.getZ();
			this.prevDistanceToTarget = this.mob.position().distanceTo(target.position());
			return;
		}
		double moved = Math.hypot(this.mob.getX() - this.lastX, this.mob.getZ() - this.lastZ);
		double dist3d = this.mob.position().distanceTo(target.position());
		if (moved < 0.003) {
			this.stuckTeleportNoProgressTicks++;
		} else {
			this.stuckTeleportNoProgressTicks = 0;
		}
		if (this.prevDistanceToTarget - dist3d > 0.02) {
			this.stuckTeleportNoApproachTicks = 0;
		} else {
			this.stuckTeleportNoApproachTicks++;
		}
		this.lastX = this.mob.getX();
		this.lastZ = this.mob.getZ();
		this.prevDistanceToTarget = dist3d;
	}

	/**
	 * @return true when this tick applied manual movement (caller should skip vanilla pathing)
	 */
	boolean enhanceChase(LivingEntity target) {
		if (!this.mob.canUseMobilityEnhancements() || this.mob.isLevitating() || this.mob.isInCombatWarmup()) {
			return false;
		}
		if (!this.mob.canMove() || this.mob.isBusy()) {
			return false;
		}

		double dx = target.getX() - this.mob.getX();
		double dz = target.getZ() - this.mob.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 1.0E-4) {
			return false;
		}

		if (!this.mob.getNavigation().isInProgress()) {
			this.mob.getNavigation().moveTo(target, 1.0);
		}

		if (this.shouldJumpToReachTarget(target) && this.mob.onGround() && this.jumpCooldownTicks <= 0 && !this.isJumping) {
			this.performJump(target);
			this.jumpCooldownTicks = JUMP_COOLDOWN_TICKS;
		}

		boolean manualMove = false;
		if (!this.isJumping) {
			double moved = Math.hypot(this.mob.getX() - this.lastX, this.mob.getZ() - this.lastZ);
			if (dist > 1.0 && moved < 0.005) {
				this.noProgressTicks++;
			} else {
				this.noProgressTicks = 0;
			}

			double dist3d = this.mob.position().distanceTo(target.position());
			boolean walledOff = this.isWalledOffFromTarget(target);
			if (walledOff && moved < 0.003) {
				this.stuckTeleportNoProgressTicks++;
			} else if (!walledOff) {
				this.stuckTeleportNoProgressTicks = 0;
			}
			if (walledOff) {
				if (this.prevDistanceToTarget - dist3d > 0.02) {
					this.stuckTeleportNoApproachTicks = 0;
				} else {
					this.stuckTeleportNoApproachTicks++;
				}
			} else {
				this.stuckTeleportNoApproachTicks = 0;
			}

			if (this.noProgressTicks >= STUCK_SHOVE_TICKS && dist > 1.0E-4) {
				double ndx = dx / dist;
				double ndz = dz / dist;
				if (this.isBlockingPath(ndx, ndz)) {
					Vec3 better = this.findBetterPathDirection(target);
					if (better != null) {
						ndx = better.x;
						ndz = better.z;
						dist = Math.sqrt(ndx * ndx + ndz * ndz);
					}
				}
				float desiredYaw = (float) (Math.toDegrees(Math.atan2(ndz, ndx)) - 90.0);
				this.mob.setYRot(desiredYaw);
				this.mob.yBodyRot = desiredYaw;
				double base = this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
				double speed = Math.max(0.12, base * 0.65);
				if (this.isBlockingPath(ndx, ndz) && this.mob.onGround()) {
					this.performJump(target);
				}
				this.mob.setDeltaMovement(ndx * speed, this.mob.getDeltaMovement().y, ndz * speed);
				this.mob.move(MoverType.SELF, new Vec3(ndx * speed, 0.0, ndz * speed));
				this.mob.setSprinting(true);
				this.noProgressTicks = 0;
				manualMove = true;
			}

			boolean stuckLong = this.stuckTeleportNoProgressTicks >= STUCK_TELEPORT_TICKS
				|| this.stuckTeleportNoApproachTicks >= STUCK_TELEPORT_TICKS;
			if (walledOff
				&& stuckLong
				&& this.mobilityGraceTicks <= 0
				&& this.teleportCooldownTicks <= 0) {
				this.tryBlinkTeleportToTarget(target);
			}
		}

		this.lastX = this.mob.getX();
		this.lastZ = this.mob.getZ();
		this.prevDistanceToTarget = this.mob.position().distanceTo(target.position());
		return manualMove;
	}

	boolean tryEscapeSuffocation(ServerLevel level) {
		Entity preferred = null;
		LivingEntity target = this.mob.getTarget();
		if (target instanceof Player player && player.isAlive()) {
			preferred = player;
		}
		if (preferred == null) {
			Player nearest = level.getNearestPlayer(this.mob, 64.0);
			if (nearest != null && nearest.isAlive()) {
				preferred = nearest;
			}
		}

		Vec3 dest = null;
		if (preferred != null) {
			dest = this.findSafeSpotAroundEntity(level, preferred, 5);
		}
		if (dest == null) {
			dest = this.findSafeSpotAroundPos(level, this.mob.position(), 3);
		}
		if (dest == null) {
			dest = new Vec3(this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ());
		}

		this.mob.snapTo(dest.x, dest.y, dest.z, this.mob.getYRot(), this.mob.getXRot());
		this.mob.setDeltaMovement(0.0, 0.0, 0.0);
		this.mob.fallDistance = 0.0F;
		this.spawnExplosionVfx(level, dest.x, dest.y + 0.6, dest.z);
		return true;
	}

	private boolean canReachTarget(Entity target) {
		double dx = this.mob.getX() - target.getX();
		double dy = this.mob.getY() - target.getY();
		double dz = this.mob.getZ() - target.getZ();
		if (Math.sqrt(dx * dx + dy * dy + dz * dz) > MELEE_REACH) {
			return false;
		}
		Vec3 attackerPos = this.mob.getEyePosition();
		Vec3 targetPos = target.getBoundingBox().getCenter();
		return this.mob.level().clip(new ClipContext(
			attackerPos,
			targetPos,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			this.mob
		)).getType() == HitResult.Type.MISS;
	}

	boolean isStuckOnTarget() {
		return this.stuckTeleportNoProgressTicks >= STUCK_TELEPORT_TICKS
			|| this.stuckTeleportNoApproachTicks >= STUCK_TELEPORT_TICKS;
	}

	boolean isBlockedFromTarget(Entity target) {
		return this.isWalledOffFromTarget(target);
	}

	private boolean isWalledOffFromTarget(Entity target) {
		if (!this.canSee(target)) {
			return true;
		}
		if (this.canReachTarget(target)) {
			return false;
		}
		if (target instanceof LivingEntity living && this.canChargeReachTarget(living)) {
			return false;
		}
		double dx = target.getX() - this.mob.getX();
		double dz = target.getZ() - this.mob.getZ();
		double dist = Math.hypot(dx, dz);
		if (dist < 1.0E-4) {
			return false;
		}
		double ndx = dx / dist;
		double ndz = dz / dist;
		return this.isBlockingPath(ndx, ndz);
	}

	boolean canChargeReachTarget(LivingEntity target) {
		double dx = target.getX() - this.mob.getX();
		double dz = target.getZ() - this.mob.getZ();
		double dist = Math.hypot(dx, dz);
		if (dist < 1.0E-4) {
			return true;
		}
		double ndx = dx / dist;
		double ndz = dz / dist;
		return this.isBodyHeightPathClear(ndx, ndz, dist + 5.0);
	}

	private boolean isBodyHeightPathClear(double dirX, double dirZ, double length) {
		double height = this.mob.getBbHeight();
		for (double factor : new double[] {0.35, 0.65}) {
			Vec3 start = this.mob.position().add(0.0, height * factor, 0.0);
			Vec3 end = start.add(dirX * length, 0.0, dirZ * length);
			if (this.mob.level().clip(new ClipContext(
				start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob
			)).getType() != HitResult.Type.MISS) {
				return false;
			}
		}
		return true;
	}

	void tryBlinkTeleportToTarget(Entity target) {
		if (this.mobilityGraceTicks > 0) {
			return;
		}
		if (!(this.mob.level() instanceof ServerLevel level) || !target.isAlive()) {
			return;
		}
		Vec3 dest = this.findSafeTeleportPosNear(level, target);
		if (dest == null) {
			return;
		}

		double ox = this.mob.getX();
		double oy = this.mob.getY();
		double oz = this.mob.getZ();
		this.mob.spawnSoulBurstAndDamage();
		this.spawnExplosionVfx(level, ox, oy + 1.0, oz);

		double dx = target.getX() - dest.x;
		double dz = target.getZ() - dest.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		this.mob.snapTo(dest.x, dest.y, dest.z, yaw, this.mob.getXRot());
		this.mob.setDeltaMovement(0.0, 0.0, 0.0);
		this.mob.fallDistance = 0.0F;
		this.spawnExplosionVfx(level, dest.x, dest.y + 1.0, dest.z);
		this.mob.resumeAiAfterBlinkTeleport();
		this.resetStuckCounters();
		this.teleportCooldownTicks = TELEPORT_COOLDOWN_TICKS;
	}

	private boolean canSee(Entity target) {
		Vec3 from = this.mob.getEyePosition();
		Vec3 to = target.getBoundingBox().getCenter();
		return this.mob.level().clip(new ClipContext(
			from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob
		)).getType() == HitResult.Type.MISS;
	}

	private void resetStuckCounters() {
		this.noProgressTicks = 0;
		this.stuckTeleportNoProgressTicks = 0;
		this.stuckTeleportNoApproachTicks = 0;
	}

	private void performJump(Entity target) {
		if (!this.mob.onGround()) {
			return;
		}
		this.isJumping = true;
		this.jumpStateTicks = 0;
		double vx = 0.0;
		double vz = 0.0;
		if (target != null) {
			Vec3 start = this.mob.position();
			Vec3 end = new Vec3(target.getX(), this.mob.getY(), target.getZ());
			double distance = start.distanceTo(end);
			if (distance > 0.1) {
				Vec3 direct = end.subtract(start).normalize();
				boolean blocked = false;
				for (double i = 0.5; i < Math.min(1.5, distance); i += 0.3) {
					Vec3 check = start.add(direct.scale(i));
					if (!this.mob.level().noCollision(this.mob, new AABB(
						check.subtract(0.3, 0.0, 0.3), check.add(0.3, 2.0, 0.3)))) {
						blocked = true;
						break;
					}
				}
				double speed = 0.5 + Math.min(distance * 0.1, 0.3);
				if (!blocked) {
					vx = direct.x * speed;
					vz = direct.z * speed;
				} else {
					Vec3 best = this.findBestJumpDirection(target);
					if (best != null) {
						vx = best.x * speed;
						vz = best.z * speed;
					} else {
						vx = direct.x * 0.3;
						vz = direct.z * 0.3;
					}
				}
			}
		}
		this.mob.setDeltaMovement(vx, 0.6, vz);
	}

	private boolean shouldJumpToReachTarget(Entity target) {
		Vec3 start = this.mob.position();
		Vec3 end = new Vec3(target.getX(), target.getY(), target.getZ());
		Vec3 direction = end.subtract(start);
		double distance = direction.length();
		if (distance < 0.1) {
			return false;
		}
		direction = direction.normalize();
		if (end.y > start.y + 0.5) {
			for (double i = 0.5; i < Math.min(distance, 3.0); i += 0.5) {
				Vec3 testPos = start.add(direction.scale(i));
				if (!this.mob.level().noCollision(this.mob, new AABB(
					testPos.subtract(0.3, 0.0, 0.3), testPos.add(0.3, 1.0, 0.3)))) {
					int height = this.getObstacleHeight(testPos);
					if (height > 0 && height <= 2) {
						Vec3 above = testPos.add(0.0, height, 0.0);
						if (this.mob.level().noCollision(this.mob, new AABB(
							above.subtract(0.3, 0.0, 0.3), above.add(0.3, 1.5, 0.3)))) {
							return true;
						}
					}
					return false;
				}
			}
		}
		Vec3 front = start.add(direction.scale(0.8));
		if (!this.mob.level().noCollision(this.mob, new AABB(
			front.subtract(0.3, 0.0, 0.3), front.add(0.3, 1.0, 0.3)))) {
			int height = this.getObstacleHeight(front);
			if (height > 0 && height <= 2) {
				Vec3 above = front.add(0.0, height, 0.0);
				return this.mob.level().noCollision(this.mob, new AABB(
					above.subtract(0.3, 0.0, 0.3), above.add(0.3, 1.5, 0.3)));
			}
			return false;
		}
		if (this.noProgressTicks >= STUCK_SHOVE_TICKS) {
			Vec3 landing = start.add(direction.scale(1.5));
			return this.mob.level().noCollision(this.mob, new AABB(
				landing.subtract(0.3, 0.0, 0.3), landing.add(0.3, 2.0, 0.3)));
		}
		return false;
	}

	private int getObstacleHeight(Vec3 basePos) {
		int height = 0;
		for (int i = 0; i < 10; i++) {
			Vec3 check = basePos.add(0.0, i, 0.0);
			if (!this.mob.level().noCollision(this.mob, new AABB(
				check.subtract(0.3, 0.0, 0.3), check.add(0.3, 1.0, 0.3)))) {
				height = i + 1;
			} else {
				break;
			}
		}
		return height;
	}

	private boolean isBlockingPath(double dx, double dz) {
		Vec3 current = this.mob.position();
		Vec3 check = current.add(dx * 1.5, 0.0, dz * 1.5);
		if (this.mob.level().noCollision(this.mob, new AABB(
			check.subtract(0.3, 0.0, 0.3), check.add(0.3, 1.0, 0.3)))) {
			return false;
		}
		int height = this.getObstacleHeight(check);
		if (height > 2) {
			return true;
		}
		if (height > 0) {
			Vec3 above = check.add(0.0, height, 0.0);
			return !this.mob.level().noCollision(this.mob, new AABB(
				above.subtract(0.3, 0.0, 0.3), above.add(0.3, 2.0, 0.3)));
		}
		return false;
	}

	private Vec3 findBetterPathDirection(Entity target) {
		Vec3 current = this.mob.position();
		Vec3 targetPos = target.position();
		Vec3 direct = targetPos.subtract(current).normalize();
		double[] angles = {
			Math.atan2(direct.z, direct.x),
			Math.atan2(direct.z, direct.x) + Math.PI / 4,
			Math.atan2(direct.z, direct.x) - Math.PI / 4,
			Math.atan2(direct.z, direct.x) + Math.PI / 2,
			Math.atan2(direct.z, direct.x) - Math.PI / 2,
			Math.atan2(direct.z, direct.x) + 3 * Math.PI / 4,
			Math.atan2(direct.z, direct.x) - 3 * Math.PI / 4,
			Math.atan2(direct.z, direct.x) + Math.PI
		};
		Vec3 best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (double angle : angles) {
			Vec3 testDir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			for (double distance : new double[] {1.5, 2.5, 3.5}) {
				Vec3 testPos = current.add(testDir.scale(distance));
				if (!this.mob.level().noCollision(this.mob, new AABB(
					testPos.subtract(0.3, 0.0, 0.3), testPos.add(0.3, 2.0, 0.3)))) {
					continue;
				}
				double improvement = current.distanceTo(targetPos) - testPos.distanceTo(targetPos);
				double angleDiff = Math.abs(angle - Math.atan2(direct.z, direct.x));
				if (angleDiff > Math.PI) {
					angleDiff = 2 * Math.PI - angleDiff;
				}
				double score = improvement * 0.6 + (1.0 - angleDiff / Math.PI) * 0.3 + distance * 0.01;
				if (score > bestScore) {
					bestScore = score;
					best = testDir;
				}
			}
		}
		return best;
	}

	private Vec3 findBestJumpDirection(Entity target) {
		Vec3 start = this.mob.position();
		Vec3 end = new Vec3(target.getX(), this.mob.getY(), target.getZ());
		Vec3 direct = end.subtract(start).normalize();
		double directAngle = Math.atan2(direct.z, direct.x);
		double[] offsets = {
			0, Math.PI / 6, -Math.PI / 6, Math.PI / 4, -Math.PI / 4,
			Math.PI / 3, -Math.PI / 3, Math.PI / 2, -Math.PI / 2
		};
		Vec3 best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (double offset : offsets) {
			double angle = directAngle + offset;
			Vec3 testDir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			for (double jumpDist = 1.5; jumpDist <= 3.0; jumpDist += 0.5) {
				Vec3 landing = start.add(testDir.scale(jumpDist));
				if (!this.mob.level().noCollision(this.mob, new AABB(
					landing.subtract(0.3, 0.0, 0.3), landing.add(0.3, 2.0, 0.3)))) {
					continue;
				}
				boolean hasGround = false;
				for (double down = 0.0; down <= 2.0; down += 0.5) {
					Vec3 ground = landing.subtract(0.0, down, 0.0);
					if (!this.mob.level().noCollision(this.mob, new AABB(
						ground.subtract(0.3, -0.1, 0.3), ground.add(0.3, 0.0, 0.3)))) {
						hasGround = true;
						break;
					}
				}
				if (!hasGround) {
					continue;
				}
				double improvement = start.distanceTo(end) - landing.distanceTo(end);
				double score = improvement * 0.5 + (1.0 - Math.abs(offset) / Math.PI) * 0.25 + jumpDist * 0.05;
				if (score > bestScore) {
					bestScore = score;
					best = testDir;
				}
			}
		}
		return best;
	}

	private Vec3 findSafeTeleportPosNear(ServerLevel level, Entity target) {
		float width = this.mob.getBbWidth();
		float height = this.mob.getBbHeight();
		double baseX = target.getX();
		double baseY = target.getY();
		double baseZ = target.getZ();

		for (double radius = TELEPORT_MIN_RADIUS; radius <= TELEPORT_MAX_RADIUS; radius += 0.5) {
			int steps = Math.max(16, (int) (radius * 8));
			for (int i = 0; i < steps; i++) {
				double ang = (2.0 * Math.PI * i) / steps;
				double tx = baseX + Math.cos(ang) * radius;
				double tz = baseZ + Math.sin(ang) * radius;
				for (double yOff = -2.0; yOff <= 2.0; yOff += 0.5) {
					double ty = baseY + yOff;
					AABB tryBox = new AABB(
						tx - width / 2, ty, tz - width / 2,
						tx + width / 2, ty + height, tz + width / 2
					);
					if (level.noCollision(tryBox) && this.isValidSpawnPosition(level, tryBox, ty)) {
						return new Vec3(tx, ty, tz);
					}
				}
			}
		}
		return null;
	}

	private Vec3 findSafeSpotAroundEntity(ServerLevel level, Entity around, int maxRadius) {
		float width = this.mob.getBbWidth();
		float height = this.mob.getBbHeight();
		double baseX = around.getX();
		double baseY = around.getY();
		double baseZ = around.getZ();
		AABB box = new AABB(
			baseX - width / 2, baseY, baseZ - width / 2,
			baseX + width / 2, baseY + height, baseZ + width / 2
		);
		if (level.noCollision(box) && this.isValidSpawnPosition(level, box, baseY)) {
			return new Vec3(baseX, baseY, baseZ);
		}
		for (int radius = 1; radius <= maxRadius; radius++) {
			int steps = Math.max(8, radius * 14);
			for (int i = 0; i < steps; i++) {
				double ang = (2.0 * Math.PI * i) / steps;
				double tx = baseX + Math.cos(ang) * radius;
				double tz = baseZ + Math.sin(ang) * radius;
				for (double yOff = -1.0; yOff <= 1.0; yOff += 0.5) {
					double ty = baseY + yOff;
					AABB tryBox = new AABB(
						tx - width / 2, ty, tz - width / 2,
						tx + width / 2, ty + height, tz + width / 2
					);
					if (level.noCollision(tryBox) && this.isValidSpawnPosition(level, tryBox, ty)) {
						return new Vec3(tx, ty, tz);
					}
				}
			}
		}
		return null;
	}

	private Vec3 findSafeSpotAroundPos(ServerLevel level, Vec3 pos, int maxRadius) {
		float width = this.mob.getBbWidth();
		float height = this.mob.getBbHeight();
		for (int radius = 1; radius <= maxRadius; radius++) {
			int steps = Math.max(8, radius * 14);
			for (int i = 0; i < steps; i++) {
				double ang = (2.0 * Math.PI * i) / steps;
				double tx = pos.x + Math.cos(ang) * radius;
				double tz = pos.z + Math.sin(ang) * radius;
				for (double yOff = -1.0; yOff <= 1.0; yOff += 0.5) {
					double ty = pos.y + yOff;
					AABB tryBox = new AABB(
						tx - width / 2, ty, tz - width / 2,
						tx + width / 2, ty + height, tz + width / 2
					);
					if (level.noCollision(tryBox) && this.isValidSpawnPosition(level, tryBox, ty)) {
						return new Vec3(tx, ty, tz);
					}
				}
			}
		}
		return null;
	}

	private boolean isValidSpawnPosition(ServerLevel level, AABB entityBox, double yPos) {
		AABB groundCheck = new AABB(
			entityBox.minX, yPos - 2.0, entityBox.minZ,
			entityBox.maxX, yPos, entityBox.maxZ
		);
		boolean hasGroundBelow = !level.noCollision(groundCheck);
		AABB immediateGround = new AABB(
			entityBox.minX, yPos - 0.5, entityBox.minZ,
			entityBox.maxX, yPos, entityBox.maxZ
		);
		boolean hasImmediateGround = !level.noCollision(immediateGround);
		return hasGroundBelow || hasImmediateGround;
	}

	private void spawnExplosionVfx(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.POOF, x, y, z, 10, 0.6, 0.4, 0.6, 0.02);
	}
}
