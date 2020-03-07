package com.github.hydos.ginger.vulkan.io;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

import java.nio.LongBuffer;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;

import com.github.hydos.ginger.common.io.Window;
import com.github.hydos.ginger.vulkan.VKVariables;

public class VKWindow
{
	public static void createSurface() {

		try(MemoryStack stack = MemoryStack.stackPush()) {

			LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

			if(GLFWVulkan.glfwCreateWindowSurface(VKVariables.instance, Window.getWindow(), null, pSurface) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create window surface");
			}

			VKVariables.surface = pSurface.get(0);
		}
	}
	
}
