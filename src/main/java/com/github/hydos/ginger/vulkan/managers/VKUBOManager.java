package com.github.hydos.ginger.vulkan.managers;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;
import java.util.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import com.github.hydos.ginger.VulkanExample.UniformBufferObject;
import com.github.hydos.ginger.vulkan.VKVariables;
import com.github.hydos.ginger.vulkan.ubo.UBO;
import com.github.hydos.ginger.vulkan.ubo.UBO.*;

public class VKUBOManager {
	
	public static List<UBO> ubos;
    
    public static long descriptorPool;
    public static long descriptorSetLayout;
    public static List<Long> descriptorSets;
	
    public static int staticUboOffset = 0;
    
    public static List<Long> uniformBuffers; //FIXME: may be the answer to all problems
    public static List<Long> uniformBuffersMemory;
    
    public static void addUBO(UBO ubo) {
		if(ubos == null) {
			ubos = new ArrayList<UBO>();
		}
		ubos.add(ubo);
    }
    
	public static void createUBODescriptorSets() {
		if(ubos == null) {//it shouldnt be but just in case
			ubos = new ArrayList<UBO>();
		}
		try (MemoryStack stack = stackPush()) {

			LongBuffer layouts = stack.mallocLong(VKVariables.swapChainImages.size());
			for (int i = 0; i < layouts.capacity(); i++) {
				layouts.put(i, descriptorSetLayout);
			}

			VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
			allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
			allocInfo.descriptorPool(descriptorPool);
			allocInfo.pSetLayouts(layouts);

			LongBuffer pDescriptorSets = stack.mallocLong(VKVariables.swapChainImages.size());

			if (vkAllocateDescriptorSets(VKVariables.device, allocInfo, pDescriptorSets) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate descriptor sets");
			}

			descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

			VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.callocStack(1, stack);
			bufferInfo.offset(0);
			bufferInfo.range(UniformBufferObject.SIZEOF);

			VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.callocStack(1, stack);
			imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			imageInfo.imageView(VKVariables.textureImageView);
			imageInfo.sampler(VKVariables.textureSampler);

			
			VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.callocStack(2, stack); //+1 is for fragment shader texture
			
			for(UBO ubo : ubos) {
				VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(ubo.bindIndex);
				uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
				uboDescriptorWrite.dstBinding(0);
				uboDescriptorWrite.dstArrayElement(0);
				uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
				uboDescriptorWrite.descriptorCount(1);
				uboDescriptorWrite.pBufferInfo(bufferInfo);
				ubo.writeDescriptorSet = uboDescriptorWrite;
			}

			//sepereate because its a texture thing TODO: make this changeable may be good for things
			VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
			samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
			samplerDescriptorWrite.dstBinding(1);
			samplerDescriptorWrite.dstArrayElement(0);
			samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			samplerDescriptorWrite.descriptorCount(1);
			samplerDescriptorWrite.pImageInfo(imageInfo);

			for (int i = 0; i < pDescriptorSets.capacity(); i++) {

				long descriptorSet = pDescriptorSets.get(i);

				bufferInfo.buffer(uniformBuffers.get(i));

				for(UBO ubo : ubos) {
					ubo.writeDescriptorSet.dstSet(descriptorSet);
				}
				samplerDescriptorWrite.dstSet(descriptorSet);

				vkUpdateDescriptorSets(VKVariables.device, descriptorWrites, null);

				descriptorSets.add(descriptorSet);
			}
		}
	}
	
	public static void createUBODescriptorSetLayout() {
		if(ubos == null) {//it shouldnt be but just in case
			ubos = new ArrayList<UBO>();
		}
		try(MemoryStack stack = stackPush()) {

			VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(ubos.size() + 1, stack); //create binding buffer on stack
			
			for(UBO ubo : ubos) {
				VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.bindIndex);
				uboLayoutBinding.binding(ubo.bindIndex); //set the binding number
				uboLayoutBinding.descriptorCount(1);
				uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
				uboLayoutBinding.pImmutableSamplers(null);
				uboLayoutBinding.stageFlags(ubo.shaderType);
			}

			VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
			samplerLayoutBinding.binding(1);
			samplerLayoutBinding.descriptorCount(1);
			samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			samplerLayoutBinding.pImmutableSamplers(null);
			samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

			VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
			layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
			layoutInfo.pBindings(bindings);

			LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

			if(vkCreateDescriptorSetLayout(VKVariables.device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create descriptor set layout");
			}
			descriptorSetLayout = pDescriptorSetLayout.get(0);
		}
	}
	
	private static void putUBODataInMemory(ByteBuffer buffer, VKUBOData ubodata) {
		ubodata.storeDataInMemory(staticUboOffset, buffer);
		staticUboOffset++;
	}
	
	public static void resetUBOOffset() {
		staticUboOffset = 0;
	}
	
	public static void updateProjAndViewUniformBuffer() {
		try(MemoryStack stack = stackPush()) {

			UniformBufferObject ubo = new UniformBufferObject();
			ubo.model.mat4.rotate((float) (glfwGetTime() * Math.toRadians(90)), 0.0f, 0.0f, 1.0f);
			ubo.view.mat4.lookAt(2.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
			ubo.proj.mat4.perspective((float) Math.toRadians(45),
				(float)VKVariables.swapChainExtent.width() / (float)VKVariables.swapChainExtent.height(), 0.1f, 10.0f);
			ubo.proj.mat4.m11(ubo.proj.mat4.m11() * -1);

			PointerBuffer data = stack.mallocPointer(1);
			vkMapMemory(VKVariables.device, currentUBOMemAddress(VKVariables.currentImageIndex), 0, 3 * VKMat4UboData.mat4Size * 3, 0, data);
			{
				putUBODataInMemory(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo.model);
				putUBODataInMemory(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo.view);
				putUBODataInMemory(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo.proj);
			}
			resetUBOOffset();
			vkUnmapMemory(VKVariables.device, uniformBuffersMemory.get(VKVariables.currentImageIndex));
		}
	}

	private static long currentUBOMemAddress(int currentImageIndex){
		return uniformBuffersMemory.get(currentImageIndex); 
	}

}
