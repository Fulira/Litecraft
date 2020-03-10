package com.github.hydos.ginger.vulkan.utils;

public class VKMath
{
	
	public static int clamp(int min, int max, int value) {
		return Math.max(min, Math.min(max, value));
	}
	
	public static double log2(double n) {
		return Math.log(n) / Math.log(2);
	}
	
}
