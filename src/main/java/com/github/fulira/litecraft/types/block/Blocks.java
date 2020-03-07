package com.github.fulira.litecraft.types.block;

import java.util.ArrayList;

import com.github.fulira.litecraft.types.block.Block.Properties;
import com.github.fulira.litecraft.types.block.Ore.Rock;

public final class Blocks {
	public static final ArrayList<Block> BLOCKS = new ArrayList<Block>();

	public static final Block AIR = new Block(new Properties("air").visible(false).fullCube(false));
	// "cubes/soil/grass/grass_top2.png" top
	// "cubes/soil/grass/grass_side.png" side
	// "cubes/soil/dirt.png" bottom
	public static final Block GRASS = new Block("cubes/soil/grass/grass_side.png", new Properties("grass").caveCarveThreshold(0.04f));
	public static final Block DIRT = new Block("cubes/soil/dirt.png", new Properties("dirt").caveCarveThreshold(0.05f));
	public static final Block ANDESITE = new RockBlock("cubes/stone/basic/andesite.png", Rock.ANDESITE, new Properties("andesite").caveCarveThreshold(0.08f));
	public static final Block DIORITE = new RockBlock("cubes/stone/basic/diorite.png", Rock.DIORITE, new Properties("diorite").caveCarveThreshold(0.05f));
	public static final Block GRANITE = new RockBlock("cubes/stone/basic/granite.png", Rock.GRANITE, new Properties("granite").caveCarveThreshold(0.06f));
	public static final Block GNEISS = new RockBlock("cubes/stone/basic/gneiss.png", Rock.GNEISS, new Properties("gneiss").caveCarveThreshold(0.09f));
	public static final Block WATER = new Block("cubes/liquid/water_static.png", new Properties("water"));
	public static final Ore IRON_ORE = new Ore("iron_ore"); // hematite

	public static Block init() {
		return AIR;
	}
}
