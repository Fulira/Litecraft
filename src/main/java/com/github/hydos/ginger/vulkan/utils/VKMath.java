package com.github.hydos.ginger.vulkan.utils;

public class VKMath
{
	
	public static int clamp(int min, int max, int value) {
		return Math.max(min, Math.min(max, value));
	}
	
}
