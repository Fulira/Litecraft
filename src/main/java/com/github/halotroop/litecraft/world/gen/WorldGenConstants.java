package com.github.halotroop.litecraft.world.gen;

public interface WorldGenConstants {
	// modify these!
	/**
	 * The level at and below which water generates in the overworld.
	 */
	int SEA_LEVEL = -1;
	/**
	 * POS_SHIFT for chunk block storage. Change this to change chunk size.
	 */
	int POS_SHIFT = 4;
	// don't modify these!
	int DOUBLE_SHIFT = POS_SHIFT * 2;
	int CHUNK_SIZE = (int) Math.pow(2, POS_SHIFT);
	int MAX_POS = CHUNK_SIZE - 1;
}
