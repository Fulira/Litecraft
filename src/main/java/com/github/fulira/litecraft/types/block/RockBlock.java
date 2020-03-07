package com.github.fulira.litecraft.types.block;

import com.github.fulira.litecraft.types.block.Ore.Rock;

public class RockBlock extends Block {
	protected RockBlock(String texture, Rock rock, Properties properties) {
		super(texture, properties);
		this.rock = rock;
	}

	public final Rock rock;
}
