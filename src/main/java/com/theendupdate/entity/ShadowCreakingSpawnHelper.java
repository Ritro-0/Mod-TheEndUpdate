package com.theendupdate.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds nearby valid spawn positions when a Shadow Creaking splits on death. */
public final class ShadowCreakingSpawnHelper {
	private ShadowCreakingSpawnHelper() {
	}

	public static void spawnEntitiesWithValidPositions(
		ServerLevel world,
		List<? extends ShadowCreakingEntity> entities,
		double baseX,
		double baseY,
		double baseZ
	) {
		if (entities.isEmpty()) {
			return;
		}

		ShadowCreakingEntity sample = entities.getFirst();
		float width = sample.getBbWidth();
		float height = sample.getBbHeight();
		List<Vec3> validPositions = new ArrayList<>();
		int searchRadius = 5;

		for (double radius = 1.0; radius <= searchRadius && validPositions.size() < entities.size(); radius += 0.5) {
			int angleSteps = Math.max(8, (int) (radius * 8));
			for (int i = 0; i < angleSteps && validPositions.size() < entities.size(); i++) {
				double angle = (2.0 * Math.PI * i) / angleSteps;
				double offsetX = Math.cos(angle) * radius;
				double offsetZ = Math.sin(angle) * radius;
				for (double yOffset = -1.0; yOffset <= 1.0 && validPositions.size() < entities.size(); yOffset += 0.5) {
					double testX = baseX + offsetX;
					double testY = baseY + yOffset;
					double testZ = baseZ + offsetZ;
					AABB testBox = new AABB(
						testX - width / 2, testY, testZ - width / 2,
						testX + width / 2, testY + height, testZ + width / 2
					);
					if (!world.noCollision(testBox) || !isValidSpawnPosition(world, testBox, testY)) {
						continue;
					}
					boolean tooClose = false;
					for (Vec3 existing : validPositions) {
						double dist = Math.hypot(testX - existing.x, testZ - existing.z);
						if (dist < 1.0) {
							tooClose = true;
							break;
						}
					}
					if (!tooClose) {
						validPositions.add(new Vec3(testX, testY, testZ));
					}
				}
			}
		}

		Vec3 fallback = validPositions.isEmpty()
			? new Vec3(baseX, baseY, baseZ)
			: validPositions.getFirst();

		for (int i = 0; i < entities.size(); i++) {
			Vec3 pos = i < validPositions.size() ? validPositions.get(i) : fallback;
			ShadowCreakingEntity entity = entities.get(i);
			entity.snapTo(pos.x, pos.y, pos.z, entity.getYRot(), 0.0F);
			world.addFreshEntity(entity);
		}
	}

	private static boolean isValidSpawnPosition(ServerLevel world, AABB box, double y) {
		BlockPos floor = BlockPos.containing(box.getCenter().x, y - 0.1, box.getCenter().z);
		if (!world.getBlockState(floor).isSolidRender()) {
			return false;
		}
		return world.noCollision(box);
	}
}
