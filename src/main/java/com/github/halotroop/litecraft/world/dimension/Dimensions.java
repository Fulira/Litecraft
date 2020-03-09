package com.github.halotroop.litecraft.world.dimension;

import com.github.halotroop.litecraft.types.block.Blocks;
import com.github.halotroop.litecraft.world.gen.EarthChunkGenerator;
import com.github.halotroop.litecraft.world.gen.modifier.*;

public final class Dimensions {
	public static final Dimension<EarthChunkGenerator> OVERWORLD = new EarthDimension(0, "earth")
			.addWorldModifier(new CavesModifier())
			.addWorldModifier(new OreModifier(-500, 500, 2, 3, 5, OreModifier.rockToOre(Blocks.IRON_ORE), (x, y, z, rand) -> 6))
			.addWorldModifier(new GenericTreeModifier());
}
