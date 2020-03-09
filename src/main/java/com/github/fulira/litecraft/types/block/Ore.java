package com.github.fulira.litecraft.types.block;

import java.util.*;
import java.util.function.*;

import com.github.fulira.litecraft.types.block.Block.Properties;

public final class Ore {
	public Ore(String name) {
		this(name, p -> p, r -> true);
	}

	public Ore(String name, UnaryOperator<Properties> oreProperties) {
		this(name, oreProperties, r -> true);
	}

	public Ore(String name, UnaryOperator<Properties> oreProperties, Predicate<Rock> rockPredicate) {
		for (Rock r : Rock.values()) { // make an ore block for every rock
			if (rockPredicate.test(r)) {
				String textureLoc = "cubes/ore/" + name + "/" + r.name + ".png";
				oreBlocks.put(r, new Block(textureLoc,
						oreProperties.apply(r.makeBase(name))));
			}
		}
	}

	private Map<Rock, Block> oreBlocks = new EnumMap<>(Rock.class);

	public Block getOreBlock(Rock rock) {
		return oreBlocks.get(rock);
	}

	public static enum Rock { // stores data about rocks for making ore blocks
		ANDESITE("andesite", p -> p.caveCarveThreshold(0.08f)),
		DIORITE("diorite", p -> p.caveCarveThreshold(0.05f)),
		GNEISS("gneiss", p -> p.caveCarveThreshold(0.06f)),
		GRANITE("granite", p -> p.caveCarveThreshold(0.09f));

		private Rock(String name, UnaryOperator<Properties> base) {
			this.name = name;
			this.base = base;
		}

		public Properties makeBase(String ore) {
			return this.base.apply(new Properties(ore + "_" + this.name));
		}

		private final String name;
		private final UnaryOperator<Properties> base;
	}
}