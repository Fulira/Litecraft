package com.github.hydos.ginger.vulkan.ubo;

import static org.lwjgl.vulkan.VK12.*;

import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class UBO
{
	//set by engine
	public VkWriteDescriptorSet writeDescriptorSet;
	
	//set by user
	public int bindIndex;
	public int shaderType = VK_SHADER_STAGE_VERTEX_BIT;
	
	
}
