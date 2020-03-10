package com.github.fulira.litecraft.util.noise;

import java.util.Random;

public final class OctaveTerrainNoise {
	protected SimplexNoise[] samplers;
	private double clamp;
	private double spread, amplitudeLow, amplitudeHigh;

	public OctaveTerrainNoise(Random rand, int octaves) {
		this(rand, octaves, 1D, 1D, 1D);
	}

	public OctaveTerrainNoise(Random rand, int octaves, double spread, double amplitudeHigh, double amplitudeLow) {
		this.samplers = new SimplexNoise[octaves];
		this.clamp = 1D / (1D - (1D / Math.pow(2, octaves)));
		for (int i = 0; i < octaves; ++i) {
			samplers[i] = new SimplexNoise(rand.nextLong());
		}
		this.spread = spread;
		this.amplitudeLow = amplitudeLow;
		this.amplitudeHigh = amplitudeHigh;
	}

	public double sample(double x, double y) {
		double amplSpread = 0.5D;
		double result = 0;
		for (SimplexNoise sampler : this.samplers) {
			double sample = sampler.sample(x / (amplSpread * this.spread), y / (amplSpread * this.spread)) + 1.0;
			sample = ((sample * sample) * 0.5) - 1.0;
			result += (amplSpread * sample);
			amplSpread *= 0.5D;
		}
		result = result * this.clamp;
		return result > 0 ? result * this.amplitudeHigh : result * this.amplitudeLow;
	}

	public double sample(double x, double y, double z) {
		double amplSpread = 0.5D;
		double result = 0;
		for (SimplexNoise sampler : this.samplers) {
			double divisor = amplSpread * this.spread;
			double sample = sampler.sample(x / divisor, y / divisor, z / divisor) + 1.0;
			sample = ((sample * sample) * 0.5) - 1.0;
			result += (amplSpread * sample);
			amplSpread *= 0.5D;
		}
		result = result * this.clamp;
		return result > 0 ? result * this.amplitudeHigh : result * this.amplitudeLow;
	}
}
