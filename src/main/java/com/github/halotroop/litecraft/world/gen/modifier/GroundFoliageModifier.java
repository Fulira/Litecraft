package com.github.halotroop.litecraft.world.gen.modifier;

import java.util.*;
import java.util.function.Predicate;

import org.joml.Vector3i;

import com.github.fulira.litecraft.types.block.*;
import com.github.fulira.litecraft.world.BlockAccess;
import com.github.fulira.litecraft.world.gen.WorldGenConstants;

public abstract class GroundFoliageModifier implements WorldModifier, WorldGenConstants {
	public GroundFoliageModifier(Predicate<Block> canGrowOn, FoliageStarter starter) {
		this.canGrowOn = canGrowOn;
		this.starter = starter;
	}

	private final Predicate<Block> canGrowOn;
	private final FoliageStarter starter;

	@Override
	public void modifyWorld(BlockAccess world, Random rand, int chunkStartX, int chunkStartY, int chunkStartZ) {
		int count = this.starter.countTarget(chunkStartX, chunkStartY, chunkStartZ, rand);
		List<Vector3i> positions = new ArrayList<>();

		// get valid spawn positions
		for (int xo = 0; xo < CHUNK_SIZE; ++xo) {
			int totalX = chunkStartX + xo;

			for (int zo = 0; zo < CHUNK_SIZE; ++zo) {
				int totalZ = chunkStartZ + zo;
				boolean flag = canGrowOn.test(world.getBlock(totalX, chunkStartY - 1, totalZ));

				for (int yo = 0; yo < CHUNK_SIZE; ++yo) {
					int totalY = chunkStartY + yo;
					Block block = world.getBlock(totalX, totalY, totalZ);

					if (flag) {
						if (block == Blocks.AIR) {
							flag = false;
							positions.add(new Vector3i(totalX, totalY, totalZ));
						}
					}

					flag = canGrowOn.test(block);
				}
			}
		}

		int randBound = positions.size();
		while ((count --> 0) && !positions.isEmpty()) {
			int index = rand.nextInt(randBound--);
			Vector3i position = positions.get(index);
			positions.remove(index);

			int startX = position.x();
			int startY = position.y();
			int startZ = position.z();

			if (this.canGenerate(world, rand, startX, startY, startZ)) {
				int foliageResultCode = this.generateFoliage(world, rand, startX, startY, startZ);
				this.generateExtras(world, rand, startX, startY, startZ, foliageResultCode);
			}
		}
	}

	protected abstract boolean canGenerate(BlockAccess world, Random rand, int startX, int startY, int startZ);
	protected abstract int generateFoliage(BlockAccess world, Random rand, int startX, int startY, int startZ);
	protected abstract void generateExtras(BlockAccess world, Random rand, int startX, int startY, int startZ, int foliageResultCode);

	@Override
	public void initialize(long seed) {
		// TODO Auto-generated method stub

	}

	@FunctionalInterface
	public static interface FoliageStarter {
		int countTarget(int chunkStartX, int chunkStartY, int chunkStartZ, Random rand);
	}
}
