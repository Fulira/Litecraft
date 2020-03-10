package com.github.fulira.litecraft.world.gen.modifier;

import java.util.Random;

import com.github.fulira.litecraft.types.block.Blocks;
import com.github.fulira.litecraft.util.noise.OctaveSimplexNoise;
import com.github.fulira.litecraft.world.BlockAccess;

public final class GenericTreeModifier extends GroundFoliageModifier {
	public GenericTreeModifier() {
		super(b -> b == Blocks.GRASS, null);
		this.starter = (x, y, z, rand) -> sampleTreeCount(x, z);
	}

	private OctaveSimplexNoise noise;

	private int sampleTreeCount(float x, float z) {
		return (int) Math.floor(this.noise.sample((double) x, (double) z));
	}

	@Override
	public void initialize(long seed) {
		this.noise = new OctaveSimplexNoise(new Random(seed), 3, 200.0f, 4.0f, 1.0f);
	}

	@Override
	protected boolean canGenerate(BlockAccess world, Random rand, int startX, int startY, int startZ) {
		return true;
	}

	@Override
	protected int generateFoliage(BlockAccess world, Random rand, int startX, int startY, int startZ) {
		int height = 5 + rand.nextInt(3);
		int level = 0;

		for (int i = height - 1; i < height + 3; ++i) {
			if (level == 3) {
				int y = startY + i;
				world.setBlock(startX, y, startZ, Blocks.LEAVES);
				world.setBlock(startX + 1, y, startZ, Blocks.LEAVES);
				world.setBlock(startX - 1, y, startZ, Blocks.LEAVES);
				world.setBlock(startX, y, startZ + 1, Blocks.LEAVES);
				world.setBlock(startX, y, startZ - 1, Blocks.LEAVES);
			} else {
				int size = level < 2 ? 2 : 1;

				for (int xo = -size; xo <= size; ++xo) {
					for (int zo = -size; zo <= size; ++zo) {
						world.setBlock(startX + xo, startY + i, startZ + zo, Blocks.LEAVES);
					}
				}
			}
			++level;
		}
		return height;
	}

	@Override
	protected void generateExtras(BlockAccess world, Random rand, int startX, int startY, int startZ, int height) {
		for (int i = 0; i < height; ++i) {
			world.setBlock(startX, startY + i, startZ, Blocks.LOG);
		}
	}
}
