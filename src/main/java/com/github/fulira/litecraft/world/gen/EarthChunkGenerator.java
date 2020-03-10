package com.github.fulira.litecraft.world.gen;

import java.util.Random;

import com.github.fulira.litecraft.types.block.*;
import com.github.fulira.litecraft.util.noise.*;
import com.github.fulira.litecraft.world.*;

public class EarthChunkGenerator implements ChunkGenerator, WorldGenConstants {
	public EarthChunkGenerator(long seed, int dimension) {
		Random rand = new Random(seed);
		this.noise = new OctaveTerrainNoise(rand, 4, 782.0, 252.0, 62.0);
		this.scaleNoise = new OctaveSimplexNoise(rand, 1, 120.0, 1.0, 1.0);
		this.stoneNoise = new OctaveSimplexNoise(rand, 1);
		this.beachNoise = new OctaveSimplexNoise(rand, 2, 52.0, 4.0, 2.0);
		this.dimension = dimension;
	}

	private final OctaveTerrainNoise noise;
	private final OctaveSimplexNoise scaleNoise;
	private final OctaveSimplexNoise stoneNoise;
	private final OctaveSimplexNoise beachNoise;
	private final int dimension;

	@Override
	public Chunk generateChunk(World world, int chunkX, int chunkY, int chunkZ) {
		Chunk chunk = new Chunk(world, chunkX, chunkY, chunkZ, this.dimension);
		final int heightOffset = SEA_LEVEL + 20;

		for (int x = 0; x < CHUNK_SIZE; x++) {
			double totalX = x + chunk.chunkStartX;

			for (int z = 0; z < CHUNK_SIZE; z++) {
				double totalZ = chunk.chunkStartZ + z;
				double scale = this.scaleNoise.sample(totalX, totalZ) + 1.0;
				double height = this.noise.sample(totalX * scale, totalZ * scale) + heightOffset;

				for (int y = 0; y < CHUNK_SIZE; y++) {
					double rockNoise = this.stoneNoise.sample(totalX / 160.0, (chunk.chunkStartY + y) / 50.0,
							totalZ / 160.0);
					int totalY = chunk.chunkStartY + y;
					int beachHeight = (int) this.beachNoise.sample(totalX, totalY) + SEA_LEVEL + 1;
					Block block = totalY <= SEA_LEVEL ? Blocks.WATER : Blocks.AIR;

					if (totalY < height - 4) {
						block = pickStone(rockNoise);
					} else if (totalY < height) {
						if (totalY < SEA_LEVEL) { // set sand when underwater
							block = Blocks.SAND;
						} else if (height < beachHeight) {
							block = Blocks.SAND; // beaches
						} else if (totalY < height - 1) {
							block = Blocks.DIRT;
						} else if (totalY < height) {
							block = Blocks.GRASS;
						}
					}

					chunk.setBlock(x, y, z, block);
				}
			}
		}
		return chunk;
	}

	private static Block pickStone(double rockNoise) {
		if (rockNoise < -0.25) {
			return Blocks.ANDESITE;
		} else if (rockNoise < 0) {
			return Blocks.DIORITE;
		} else if (rockNoise < 0.25) {
			return Blocks.GNEISS;
		} else {
			return Blocks.GRANITE;
		}
	}
}
