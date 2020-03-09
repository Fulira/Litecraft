package com.github.halotroop.litecraft.world.gen.modifier;

import java.util.Random;
import java.util.function.Predicate;

import com.github.halotroop.litecraft.types.block.Block;
import com.github.halotroop.litecraft.world.BlockAccess;

public abstract class FoliageModifier implements WorldModifier {
	public FoliageModifier(Predicate<Block> canGrowOn, FoliageStarter starter) {
		//this.canGrowOn = canGrowOn;
		//this.starter = starter;
	}

	//private final Predicate<Block> canGrowOn; FIXME: use this so
	//private final FoliageStarter starter;

	@Override
	public void modifyWorld(BlockAccess world, Random rand, int chunkStartX, int chunkStartY, int chunkStartZ) {
		//int count = this.starter.countTarget(chunkStartX, chunkStartY, chunkStartZ, rand);

		// TODO generation
	}

	protected abstract boolean canGenerate(BlockAccess world, Random rand, int startX, int startY, int startZ);
	protected abstract boolean generateFoliage(BlockAccess world, Random rand, int startX, int startY, int startZ);
	protected abstract boolean generateExtras(BlockAccess world, Random rand, int startX, int startY, int startZ);

	@Override
	public void initialize(long seed) {
		// TODO Auto-generated method stub
		
	}

	@FunctionalInterface
	public static interface FoliageStarter {
		int countTarget(int chunkStartX, int chunkStartY, int chunkStartZ, Random rand);
	}
}
