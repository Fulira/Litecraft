package com.gildedgames.orbis.lib.util.random;

import java.util.Random;

// XoRoShiRo128** implementation from DSI Utilities, adopted in a minimal implementation to not
// import Apache Commons.
//
// http://xoshiro.di.unimi.it/
public class XRSRRandom extends Random
{
	private static final long serialVersionUID = 1L;

	private long s0, s1;

	private static final SplitMixRandom seedUniquifier = new SplitMixRandom(System.nanoTime());

	public static long randomSeed()
	{
		final long x;

		synchronized (XRSRRandom.seedUniquifier)
		{
			x = XRSRRandom.seedUniquifier.nextLong();
		}

		return x ^ System.nanoTime();
	}

	public XRSRRandom()
	{
		this(XRSRRandom.randomSeed());
	}

	public XRSRRandom(final long seed)
	{
		this.setSeed(seed);
	}

	@Override
	public long nextLong()
	{
		final long s0 = this.s0;

		long s1 = this.s1;

		final long result = s0 + s1;

		s1 ^= s0;

		this.s0 = Long.rotateLeft(s0, 24) ^ s1 ^ s1 << 16;
		this.s1 = Long.rotateLeft(s1, 37);

		return result;
	}

	@Override
	public int nextInt()
	{
		return (int) this.nextLong();
	}

	@Override
	public int nextInt(final int n)
	{
		return (int) this.nextLong(n);
	}

	private long nextLong(final long n)
	{
		if (n <= 0)
		{
			throw new IllegalArgumentException("illegal bound " + n + " (must be positive)");
		}

		long t = this.nextLong();

		final long nMinus1 = n - 1;

		// Shortcut for powers of two--high bits
		if ((n & nMinus1) == 0)
		{
			return (t >>> Long.numberOfLeadingZeros(nMinus1)) & nMinus1;
		}

		// Rejection-based algorithm to get uniform integers in the general case
		long u = t >>> 1;

		while (u + nMinus1 - (t = u % n) < 0)
		{
			u = this.nextLong() >>> 1;
		}

		return t;

	}

	@Override
	public double nextDouble()
	{
		return Double.longBitsToDouble(0x3FFL << 52 | this.nextLong() >>> 12) - 1.0;
	}

	@Override
	public float nextFloat()
	{
		return (this.nextLong() >>> 40) * 0x1.0p-24f;
	}

	@Override
	public boolean nextBoolean()
	{
		return this.nextLong() < 0;
	}

	@Override
	public void nextBytes(final byte[] bytes)
	{
		int i = bytes.length, n;

		while (i != 0)
		{
			n = Math.min(i, 8);

			for (long bits = this.nextLong(); n-- != 0; bits >>= 8)
			{
				bytes[--i] = (byte) bits;
			}
		}

	}

	@Override
	public void setSeed(final long seed)
	{
		final SplitMixRandom r = new SplitMixRandom(seed);

		this.s0 = r.nextLong();
		this.s1 = r.nextLong();
	}

	public void setState(final long[] state)
	{
		if (state.length != 2)
		{
			throw new IllegalArgumentException("The argument array contains " + state.length + " longs instead of 2");
		}

		this.s0 = state[0];
		this.s1 = state[1];
	}
}