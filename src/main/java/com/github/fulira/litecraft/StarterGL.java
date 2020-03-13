package com.github.fulira.litecraft;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class StarterGL {
	// private static final boolean usingEclipse = false;
	public static void main(String[] args) {
		System.out.println("GLFW version: " + GLFW.glfwGetVersionString());
		System.out.println("LWJGL version: " + Version.getVersion());
		// Put SoundSystem version here
		// TODO: Put a commandline reader here to check for desired width, height, and
		// frame limit!
		new Litecraft(1280, 720, 60);
	}
}
