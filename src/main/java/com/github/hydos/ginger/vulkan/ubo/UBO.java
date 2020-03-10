package com.github.hydos.ginger.vulkan.ubo;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;

import java.nio.ByteBuffer;
import java.util.*;

import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import com.github.hydos.ginger.vulkan.utils.AlignmentUtils;

public class UBO
{
	//set by engine
	public VkWriteDescriptorSet writeDescriptorSet;
	
	//set by user
	public int bindIndex;
	public int shaderType = VK_SHADER_STAGE_VERTEX_BIT;
	
	public List<VKUBOData> uboData;
	
	public UBO() {
		uboData = new ArrayList<VKUBOData>();
	}
	
	
	
	
	public static abstract class VKUBOData{
		public abstract void storeDataInMemory(int offset, ByteBuffer buffer);
	}
	
	public static class VKMat4UboData extends VKUBOData{
		
		public Matrix4f mat4;
		
		final int mat4Size = 16 * Float.BYTES;
		
		@Override
		public void storeDataInMemory(int offset, ByteBuffer buffer){
			if(offset == 0) {
				mat4.get(0, buffer);
			}else {
				mat4.get(AlignmentUtils.alignas(mat4Size*offset, AlignmentUtils.alignof(mat4)), buffer);
			}
			
			
		}
		
	}
	
}

