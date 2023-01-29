package com.likesdinosaurs.maphelpers;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MapHelpersMathHelper {
	public static int getSizeFromScale(int scale) {
		return switch (scale) {
			case 0 -> 128;
			case 1 -> 256;
			case 2 -> 512;
			case 3 -> 1024;
			case 4 -> 2048;
			default -> 128;
		};
	}

	public static Vec3d roundVec3d(Vec3d vec3d) {
		return new Vec3d(Math.round(vec3d.x), Math.round(vec3d.y), Math.round(vec3d.z));
	}

	public static Vec3d floorVec3d(Vec3d vec3d) {
		return new Vec3d(Math.floor(vec3d.x), Math.floor(vec3d.y), Math.floor(vec3d.z));
	}

	public static Box makeBoxFull(Box box) {
		Vec3d start = new Vec3d(Math.floor(box.minX), Math.floor(box.minY), Math.floor(box.minZ));
		Vec3d end = new Vec3d(Math.floor(box.maxX) + 1, Math.floor(box.maxY) + 1, Math.floor(box.maxZ) + 1);
		return new Box(start, end);
	}

	public static Vec3d rotateVector(Vec3d vector, Direction dir, int rotation) {
		switch (dir) {
			case UP -> {
				vector = vector.rotateY(-MathHelper.HALF_PI * rotation);
				return vector;
			}
			case DOWN -> {
				vector = vector.rotateZ(MathHelper.HALF_PI * 2);
				vector = vector.rotateY(MathHelper.HALF_PI * 2);
				vector = vector.rotateY(MathHelper.HALF_PI * rotation);
				return vector;
			}
			case NORTH -> {
				vector = vector.rotateX(MathHelper.HALF_PI);
				vector = vector.rotateZ(MathHelper.HALF_PI * 2);
				vector = vector.rotateZ(MathHelper.HALF_PI * rotation);
				return vector;
			}
			case SOUTH -> {
				vector = vector.rotateX(-MathHelper.HALF_PI);
				vector = vector.rotateZ(MathHelper.HALF_PI * rotation);
				return vector;
			}
			case WEST -> {
				vector = vector.rotateX(-MathHelper.HALF_PI);
				vector = vector.rotateY(-MathHelper.HALF_PI);
				vector = vector.rotateX(MathHelper.HALF_PI * rotation);
				return vector;
			}
			case EAST -> {
				vector = vector.rotateX(-MathHelper.HALF_PI);
				vector = vector.rotateY(MathHelper.HALF_PI);
				vector = vector.rotateX(MathHelper.HALF_PI * rotation);
				return vector;
			}
			default -> {
				return vector;
			}
		}
	}

	public static Vec3d unrotateVector(Vec3d vector, Direction dir, int rotation) {
		switch (dir) {
			case UP -> {
				vector = vector.rotateY(MathHelper.HALF_PI * rotation);
				return vector;
			}
			case DOWN -> {
				vector = vector.rotateY(-MathHelper.HALF_PI * rotation);
				vector = vector.rotateY(-MathHelper.HALF_PI * 2);
				vector = vector.rotateZ(-MathHelper.HALF_PI * 2);
				return vector;
			}
			case NORTH -> {
				vector = vector.rotateZ(-MathHelper.HALF_PI * rotation);
				vector = vector.rotateZ(-MathHelper.HALF_PI * 2);
				vector = vector.rotateX(-MathHelper.HALF_PI);
				return vector;
			}
			case SOUTH -> {
				vector = vector.rotateZ(-MathHelper.HALF_PI * rotation);
				vector = vector.rotateX(MathHelper.HALF_PI);
				return vector;
			}
			case WEST -> {
				vector = vector.rotateX(-MathHelper.HALF_PI * rotation);
				vector = vector.rotateY(MathHelper.HALF_PI);
				vector = vector.rotateX(MathHelper.HALF_PI);
				return vector;
			}
			case EAST -> {
				vector = vector.rotateX(-MathHelper.HALF_PI * rotation);
				vector = vector.rotateY(-MathHelper.HALF_PI);
				vector = vector.rotateX(MathHelper.HALF_PI);
				return vector;
			}
			default -> {
				return vector;
			}
		}
	}
}
