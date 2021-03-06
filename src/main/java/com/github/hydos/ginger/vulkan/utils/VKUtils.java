package com.github.hydos.ginger.vulkan.utils;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;
import java.util.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import com.github.hydos.ginger.VulkanExample;
import com.github.hydos.ginger.VulkanExample.*;
import com.github.hydos.ginger.common.io.Window;
import com.github.hydos.ginger.vulkan.VKVariables;
import com.github.hydos.ginger.vulkan.managers.*;
import com.github.hydos.ginger.vulkan.model.VKVertex;
import com.github.hydos.ginger.vulkan.render.*;
import com.github.hydos.ginger.vulkan.swapchain.VKSwapchainManager;

public class VKUtils
{
	
	public static void cleanup() {
		VKSwapchainManager.cleanupSwapChain();

		vkDestroySampler(VKVariables.device, VKVariables.textureSampler, null);
		vkDestroyImageView(VKVariables.device, VKVariables.textureImageView, null);
		vkDestroyImage(VKVariables.device, VKVariables.textureImage, null);
		vkFreeMemory(VKVariables.device, VKVariables.textureImageMemory, null);

		vkDestroyDescriptorSetLayout(VKVariables.device, VKUBOManager.descriptorSetLayout, null);

		VKVariables.inFlightFrames.forEach(frame -> {

			vkDestroySemaphore(VKVariables.device, frame.renderFinishedSemaphore(), null);
			vkDestroySemaphore(VKVariables.device, frame.imageAvailableSemaphore(), null);
			vkDestroyFence(VKVariables.device, frame.fence(), null);
		});
		VKVariables.inFlightFrames.clear();

		vkDestroyCommandPool(VKVariables.device, VKVariables.commandPool, null);

		vkDestroyDevice(VKVariables.device, null);

		vkDestroySurfaceKHR(VKVariables.instance, VKVariables.surface, null);

		vkDestroyInstance(VKVariables.instance, null);
		
		Window.destroy();
	}

	public static void createImageViews() {

		VKVariables.swapChainImageViews = new ArrayList<>(VKVariables.swapChainImages.size());

		for(long swapChainImage : VKVariables.swapChainImages) {
			VKVariables.swapChainImageViews.add(createImageView(swapChainImage, VKVariables.swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1));
		}
	}
	
	public static int findDepthFormat() {
		return VKUtils.findSupportedFormat(
			stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
			VK_IMAGE_TILING_OPTIMAL,
			VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
	}

	public static void generateMipmaps(long image, int imageFormat, int width, int height, int mipLevels) {

		try(MemoryStack stack = stackPush()) {

			// Check if image format supports linear blitting
			VkFormatProperties formatProperties = VkFormatProperties.mallocStack(stack);
			vkGetPhysicalDeviceFormatProperties(VKVariables.physicalDevice, imageFormat, formatProperties);

			if((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
				throw new RuntimeException("Texture image format does not support linear blitting");
			}

			VkCommandBuffer commandBuffer = VKCommandBufferManager.beginSingleTimeCommands();

			VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
			barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			barrier.image(image);
			barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.dstAccessMask(VK_QUEUE_FAMILY_IGNORED);
			barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			barrier.subresourceRange().baseArrayLayer(0);
			barrier.subresourceRange().layerCount(1);
			barrier.subresourceRange().levelCount(1);

			int mipWidth = width;
			int mipHeight = height;

			for(int i = 1;i < mipLevels;i++) {

				barrier.subresourceRange().baseMipLevel(i - 1);
				barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
				barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
				barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
				barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

				vkCmdPipelineBarrier(commandBuffer,
					VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
					null,
					null,
					barrier);

				VkImageBlit.Buffer blit = VkImageBlit.callocStack(1, stack);
				blit.srcOffsets(0).set(0, 0, 0);
				blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
				blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
				blit.srcSubresource().mipLevel(i - 1);
				blit.srcSubresource().baseArrayLayer(0);
				blit.srcSubresource().layerCount(1);
				blit.dstOffsets(0).set(0, 0, 0);
				blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
				blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
				blit.dstSubresource().mipLevel(i);
				blit.dstSubresource().baseArrayLayer(0);
				blit.dstSubresource().layerCount(1);

				vkCmdBlitImage(commandBuffer,
					image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
					image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
					blit,
					VK_FILTER_LINEAR);

				barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
				barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
				barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
				barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

				vkCmdPipelineBarrier(commandBuffer,
					VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
					null,
					null,
					barrier);

				if(mipWidth > 1) {
					mipWidth /= 2;
				}

				if(mipHeight > 1) {
					mipHeight /= 2;
				}
			}

			barrier.subresourceRange().baseMipLevel(mipLevels - 1);
			barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

			vkCmdPipelineBarrier(commandBuffer,
				VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
				null,
				null,
				barrier);

			VKCommandBufferManager.endSingleTimeCommands(commandBuffer);
		}
	}

	public static int getMaxUsableSampleCount() {

		try(MemoryStack stack = stackPush()) {

			VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.mallocStack(stack);
			vkGetPhysicalDeviceProperties(VKVariables.physicalDevice, physicalDeviceProperties);

			int sampleCountFlags = physicalDeviceProperties.limits().framebufferColorSampleCounts()
				& physicalDeviceProperties.limits().framebufferDepthSampleCounts();

			if((sampleCountFlags & VK_SAMPLE_COUNT_64_BIT) != 0) {
				return VK_SAMPLE_COUNT_64_BIT;
			}
			if((sampleCountFlags & VK_SAMPLE_COUNT_32_BIT) != 0) {
				return VK_SAMPLE_COUNT_32_BIT;
			}
			if((sampleCountFlags & VK_SAMPLE_COUNT_16_BIT) != 0) {
				return VK_SAMPLE_COUNT_16_BIT;
			}
			if((sampleCountFlags & VK_SAMPLE_COUNT_8_BIT) != 0) {
				return VK_SAMPLE_COUNT_8_BIT;
			}
			if((sampleCountFlags & VK_SAMPLE_COUNT_4_BIT) != 0) {
				return VK_SAMPLE_COUNT_4_BIT;
			}
			if((sampleCountFlags & VK_SAMPLE_COUNT_2_BIT) != 0) {
				return VK_SAMPLE_COUNT_2_BIT;
			}

			return VK_SAMPLE_COUNT_1_BIT;
		}
	}

	public static long createImageView(long image, int format, int aspectFlags, int mipLevels) {

		try(MemoryStack stack = stackPush()) {

			VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.callocStack(stack);
			viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
			viewInfo.image(image);
			viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
			viewInfo.format(format);
			viewInfo.subresourceRange().aspectMask(aspectFlags);
			viewInfo.subresourceRange().baseMipLevel(0);
			viewInfo.subresourceRange().levelCount(mipLevels);
			viewInfo.subresourceRange().baseArrayLayer(0);
			viewInfo.subresourceRange().layerCount(1);

			LongBuffer pImageView = stack.mallocLong(1);

			if(vkCreateImageView(VKVariables.device, viewInfo, null, pImageView) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create texture image view");
			}

			return pImageView.get(0);
		}
	}

	public static void createImage(int width, int height, int mipLevels, int numSamples, int format, int tiling, int usage, int memProperties,
		LongBuffer pTextureImage, LongBuffer pTextureImageMemory) {

		try(MemoryStack stack = stackPush()) {

			VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
			imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
			imageInfo.imageType(VK_IMAGE_TYPE_2D);
			imageInfo.extent().width(width);
			imageInfo.extent().height(height);
			imageInfo.extent().depth(1);
			imageInfo.mipLevels(mipLevels);
			imageInfo.arrayLayers(1);
			imageInfo.format(format);
			imageInfo.tiling(tiling);
			imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
			imageInfo.usage(usage);
			imageInfo.samples(numSamples);
			imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

			if(vkCreateImage(VKVariables.device, imageInfo, null, pTextureImage) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create image");
			}

			VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
			vkGetImageMemoryRequirements(VKVariables.device, pTextureImage.get(0), memRequirements);

			VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
			allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocInfo.allocationSize(memRequirements.size());
			allocInfo.memoryTypeIndex(VKUtils.findMemoryType(memRequirements.memoryTypeBits(), memProperties));

			if(vkAllocateMemory(VKVariables.device, allocInfo, null, pTextureImageMemory) != VK_SUCCESS) {
				throw new RuntimeException("Failed to allocate image memory");
			}

			vkBindImageMemory(VKVariables.device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
		}
	}

	public static void createFramebuffers() {

		VKVariables.swapChainFramebuffers = new ArrayList<>(VKVariables.swapChainImageViews.size());

		try(MemoryStack stack = stackPush()) {

			LongBuffer attachments = stack.longs(VKVariables.colorImageView, VKVariables.depthImageView, VK_NULL_HANDLE);
			LongBuffer pFramebuffer = stack.mallocLong(1);

			// Lets allocate the create info struct once and just update the pAttachments field each iteration
			VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
			framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
			framebufferInfo.renderPass(VKVariables.renderPass);
			framebufferInfo.width(VKVariables.swapChainExtent.width());
			framebufferInfo.height(VKVariables.swapChainExtent.height());
			framebufferInfo.layers(1);

			for(long imageView : VKVariables.swapChainImageViews) {

				attachments.put(2, imageView);

				framebufferInfo.pAttachments(attachments);

				if(vkCreateFramebuffer(VKVariables.device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
					throw new RuntimeException("Failed to create framebuffer");
				}

				VKVariables.swapChainFramebuffers.add(pFramebuffer.get(0));
			}
		}
	}

	public static void createColorResources() {

		try(MemoryStack stack = stackPush()) {

			LongBuffer pColorImage = stack.mallocLong(1);
			LongBuffer pColorImageMemory = stack.mallocLong(1);

			createImage(VKVariables.swapChainExtent.width(), VKVariables.swapChainExtent.height(),
				1,
				VKVariables.msaaSamples,
				VKVariables.swapChainImageFormat,
				VK_IMAGE_TILING_OPTIMAL,
				VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
				VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
				pColorImage,
				pColorImageMemory);

			VKVariables.colorImage = pColorImage.get(0);
			VKVariables.colorImageMemory = pColorImageMemory.get(0);

			VKVariables.colorImageView = createImageView(VKVariables.colorImage, VKVariables.swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1);

			VKUtils.transitionImageLayout(VKVariables.colorImage, VKVariables.swapChainImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, 1);
		}
	}

	public static void createDepthResources() {

		try(MemoryStack stack = stackPush()) {

			int depthFormat = findDepthFormat();

			LongBuffer pDepthImage = stack.mallocLong(1);
			LongBuffer pDepthImageMemory = stack.mallocLong(1);

			createImage(
				VKVariables.swapChainExtent.width(), VKVariables.swapChainExtent.height(),
				1,
				VKVariables. msaaSamples,
				depthFormat,
				VK_IMAGE_TILING_OPTIMAL,
				VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
				VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
				pDepthImage,
				pDepthImageMemory);

			VKVariables.depthImage = pDepthImage.get(0);
			VKVariables.depthImageMemory = pDepthImageMemory.get(0);

			VKVariables.depthImageView = createImageView(VKVariables.depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1);

			// Explicitly transitioning the depth image
			VKUtils.transitionImageLayout(VKVariables.depthImage, depthFormat,
				VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
				1);

		}
	}

	public static int findSupportedFormat(IntBuffer formatCandidates, int tiling, int features) {

		try(MemoryStack stack = stackPush()) {

			VkFormatProperties props = VkFormatProperties.callocStack(stack);

			for(int i = 0; i < formatCandidates.capacity(); ++i) {

				int format = formatCandidates.get(i);

				vkGetPhysicalDeviceFormatProperties(VKVariables.physicalDevice, format, props);

				if(tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
					return format;
				} else if(tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
					return format;
				}

			}
		}

		throw new RuntimeException("Failed to find supported format");
	}

	public static void createCommandPool() {

		try(MemoryStack stack = stackPush()) {

			QueueFamilyIndices queueFamilyIndices = findQueueFamilies(VKVariables.physicalDevice);

			VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
			poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);

			LongBuffer pCommandPool = stack.mallocLong(1);

			if (vkCreateCommandPool(VKVariables.device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create command pool");
			}

			VKVariables.commandPool = pCommandPool.get(0);
		}
	}
	
	public static void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels) {

		try(MemoryStack stack = stackPush()) {

			VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
			barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
			barrier.oldLayout(oldLayout);
			barrier.newLayout(newLayout);
			barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
			barrier.image(image);

			barrier.subresourceRange().baseMipLevel(0);
			barrier.subresourceRange().levelCount(mipLevels);
			barrier.subresourceRange().baseArrayLayer(0);
			barrier.subresourceRange().layerCount(1);

			if(newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

				barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);

				if(hasStencilComponent(format)) {
					barrier.subresourceRange().aspectMask(
						barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
				}

			} else {
				barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			}

			int sourceStage;
			int destinationStage;

			if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

				barrier.srcAccessMask(0);
				barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

				sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

			} else if(oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

				barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
				barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

				sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
				destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

			} else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

				barrier.srcAccessMask(0);
				barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

				sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

			} else if(oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

				barrier.srcAccessMask(0);
				barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

				sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
				destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

			} else {
				throw new IllegalArgumentException("Unsupported layout transition");
			}

			VkCommandBuffer commandBuffer = VKCommandBufferManager.beginSingleTimeCommands();

			vkCmdPipelineBarrier(commandBuffer,
				sourceStage, destinationStage,
				0,
				null,
				null,
				barrier);

			VKCommandBufferManager.endSingleTimeCommands(commandBuffer);
		}
	}
	
	private static boolean hasStencilComponent(int format) {
		return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT;
	}

	public static void copyBufferToImage(long buffer, long image, int width, int height) {

		try(MemoryStack stack = stackPush()) {

			VkCommandBuffer commandBuffer = VKCommandBufferManager.beginSingleTimeCommands();

			VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
			region.bufferOffset(0);
			region.bufferRowLength(0);   // Tightly packed
			region.bufferImageHeight(0);  // Tightly packed
			region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
			region.imageSubresource().mipLevel(0);
			region.imageSubresource().baseArrayLayer(0);
			region.imageSubresource().layerCount(1);
			region.imageOffset().set(0, 0, 0);
			region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

			vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

			VKCommandBufferManager.endSingleTimeCommands(commandBuffer);
		}
	}

	public static void memcpy(ByteBuffer dst, ByteBuffer src, long size) {
		src.limit((int)size);
		dst.put(src);
		src.limit(src.capacity()).rewind();
	}

	public static VKBufferMesh createVertexBuffer(VKBufferMesh processedMesh) {

		try(MemoryStack stack = stackPush()) {

			long bufferSize = VKVertex.SIZEOF * processedMesh.vertices.length;

			LongBuffer pBuffer = stack.mallocLong(1);
			LongBuffer pBufferMemory = stack.mallocLong(1);
			VKBufferUtils.createBuffer(bufferSize,
				VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
				VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
				pBuffer,
				pBufferMemory);

			long stagingBuffer = pBuffer.get(0);
			long stagingBufferMemory = pBufferMemory.get(0);

			PointerBuffer data = stack.mallocPointer(1);

			vkMapMemory(VKVariables.device, stagingBufferMemory, 0, bufferSize, 0, data);
			{
				memcpy(data.getByteBuffer(0, (int) bufferSize), processedMesh.vertices);
			}
			vkUnmapMemory(VKVariables.device, stagingBufferMemory);

			VKBufferUtils.createBuffer(bufferSize,
				VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
				VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
				pBuffer,
				pBufferMemory);

			processedMesh.vertexBuffer = pBuffer.get(0);
			processedMesh.vertexBufferMemory = pBufferMemory.get(0);

			VKBufferUtils.copyBuffer(stagingBuffer, processedMesh.vertexBuffer, bufferSize);

			vkDestroyBuffer(VKVariables.device, stagingBuffer, null);
			vkFreeMemory(VKVariables.device, stagingBufferMemory, null);
			
			return processedMesh;
		}
	}

	public static VKBufferMesh createIndexBuffer(VKBufferMesh processedMesh) {

		try(MemoryStack stack = stackPush()) {

			long bufferSize = Integer.BYTES * processedMesh.indices.length;

			LongBuffer pBuffer = stack.mallocLong(1);
			LongBuffer pBufferMemory = stack.mallocLong(1);
			VKBufferUtils.createBuffer(bufferSize,
				VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
				VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
				pBuffer,
				pBufferMemory);

			long stagingBuffer = pBuffer.get(0);
			long stagingBufferMemory = pBufferMemory.get(0);

			PointerBuffer data = stack.mallocPointer(1);

			vkMapMemory(VKVariables.device, stagingBufferMemory, 0, bufferSize, 0, data);
			{
				memcpy(data.getByteBuffer(0, (int) bufferSize), processedMesh.indices);
			}
			vkUnmapMemory(VKVariables.device, stagingBufferMemory);

			VKBufferUtils.createBuffer(bufferSize,
				VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
				VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
				pBuffer,
				pBufferMemory);

			processedMesh.indexBuffer = pBuffer.get(0);
			processedMesh.indexBufferMemory = pBufferMemory.get(0);

			VKBufferUtils.copyBuffer(stagingBuffer, processedMesh.indexBuffer, bufferSize);

			vkDestroyBuffer(VKVariables.device, stagingBuffer, null);
			vkFreeMemory(VKVariables.device, stagingBufferMemory, null);
			return processedMesh;
		}
	}

	public static void createUniformBuffers() {

		try(MemoryStack stack = stackPush()) {

			VKUBOManager.uniformBuffers = new ArrayList<>(VKVariables.swapChainImages.size());
			VKUBOManager.uniformBuffersMemory = new ArrayList<>(VKVariables.swapChainImages.size());

			LongBuffer pBuffer = stack.mallocLong(1);
			LongBuffer pBufferMemory = stack.mallocLong(1);

			for(int i = 0;i < VKVariables.swapChainImages.size();i++) {
				VKBufferUtils.createBuffer(UniformBufferObject.SIZEOF,
					VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
					VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
					pBuffer,
					pBufferMemory);

				VKUBOManager.uniformBuffers.add(pBuffer.get(0));
				VKUBOManager.uniformBuffersMemory.add(pBufferMemory.get(0));
			}

		}
	}


	public static void createDescriptorPool() {

		try(MemoryStack stack = stackPush()) {

			VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(2, stack);

			VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
			uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
			uniformBufferPoolSize.descriptorCount(VKVariables.swapChainImages.size());

			VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
			textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
			textureSamplerPoolSize.descriptorCount(VKVariables.swapChainImages.size());

			VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
			poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
			poolInfo.pPoolSizes(poolSizes);
			poolInfo.maxSets(VKVariables.swapChainImages.size());

			LongBuffer pDescriptorPool = stack.mallocLong(1);

			if(vkCreateDescriptorPool(VKVariables.device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
				throw new RuntimeException("Failed to create descriptor pool");
			}

			VKUBOManager.descriptorPool = pDescriptorPool.get(0);
		}
	}

	private static void memcpy(ByteBuffer buffer, VKVertex[] vertices) {
		for(VKVertex vertex : vertices) {
			buffer.putFloat(vertex.pos.x());
			buffer.putFloat(vertex.pos.y());
			buffer.putFloat(vertex.pos.z());

			buffer.putFloat(vertex.color.x());
			buffer.putFloat(vertex.color.y());
			buffer.putFloat(vertex.color.z());

			buffer.putFloat(vertex.texCoords.x());
			buffer.putFloat(vertex.texCoords.y());
		}
	}

	private static void memcpy(ByteBuffer buffer, int[] indices) {

		for(int index : indices) {
			buffer.putInt(index);
		}

		buffer.rewind();
	}

	public static int findMemoryType(int typeFilter, int properties) {

		VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.mallocStack();
		vkGetPhysicalDeviceMemoryProperties(VKVariables.physicalDevice, memProperties);

		for(int i = 0;i < memProperties.memoryTypeCount();i++) {
			if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
				return i;
			}
		}

		throw new RuntimeException("Failed to find suitable memory type");
	}

	

	public static void createSyncObjects() {

		VKVariables.inFlightFrames = new ArrayList<>(VulkanExample.MAX_FRAMES_IN_FLIGHT);
		VKVariables.imagesInFlight = new HashMap<>(VKVariables.swapChainImages.size());

		try(MemoryStack stack = stackPush()) {

			VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
			semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

			VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
			fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

			LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
			LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
			LongBuffer pFence = stack.mallocLong(1);

			for(int i = 0;i < VulkanExample.MAX_FRAMES_IN_FLIGHT;i++) {

				if(vkCreateSemaphore(VKVariables.device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
					|| vkCreateSemaphore(VKVariables.device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
					|| vkCreateFence(VKVariables.device, fenceInfo, null, pFence) != VK_SUCCESS) {

					throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
				}

				VKVariables.inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
			}

		}
	}

	public static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
		return availableFormats.stream()
			.filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_SRGB)
			.filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
			.findAny()
			.orElse(availableFormats.get(0));
	}

	public static int chooseSwapPresentMode(IntBuffer availablePresentModes) {

		for(int i = 0;i < availablePresentModes.capacity();i++) {
			if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
				return availablePresentModes.get(i);
			}
		}

		return VK_PRESENT_MODE_FIFO_KHR;
	}

	public static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {

		if(capabilities.currentExtent().width() != VulkanExample.UINT32_MAX) {
			return capabilities.currentExtent();
		}

		IntBuffer width = stackGet().ints(0);
		IntBuffer height = stackGet().ints(0);

		glfwGetFramebufferSize(Window.getWindow(), width, height);

		VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

		VkExtent2D minExtent = capabilities.minImageExtent();
		VkExtent2D maxExtent = capabilities.maxImageExtent();

		actualExtent.width(VKMath.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
		actualExtent.height(VKMath.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

		return actualExtent;
	}

	@SuppressWarnings("unlikely-arg-type") //dont think its fixable
	public static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {

		try(MemoryStack stack = stackPush()) {

			IntBuffer extensionCount = stack.ints(0);

			vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

			VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

			return availableExtensions.stream().collect(toSet()).containsAll(VulkanExample.DEVICE_EXTENSIONS);
		}
	}

	public static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {

		SwapChainSupportDetails details = new SwapChainSupportDetails();

		details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, VKVariables.surface, details.capabilities);

		IntBuffer count = stack.ints(0);

		vkGetPhysicalDeviceSurfaceFormatsKHR(device, VKVariables.surface, count, null);

		if(count.get(0) != 0) {
			details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
			vkGetPhysicalDeviceSurfaceFormatsKHR(device, VKVariables.surface, count, details.formats);
		}

		vkGetPhysicalDeviceSurfacePresentModesKHR(device,VKVariables.surface, count, null);

		if(count.get(0) != 0) {
			details.presentModes = stack.mallocInt(count.get(0));
			vkGetPhysicalDeviceSurfacePresentModesKHR(device, VKVariables.surface, count, details.presentModes);
		}

		return details;
	}

	public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {

		QueueFamilyIndices indices = new QueueFamilyIndices();

		try(MemoryStack stack = stackPush()) {

			IntBuffer queueFamilyCount = stack.ints(0);

			vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

			VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);

			vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

			IntBuffer presentSupport = stack.ints(VK_FALSE);

			for(int i = 0;i < queueFamilies.capacity() || !indices.isComplete();i++) {

				if((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
					indices.graphicsFamily = i;
				}

				vkGetPhysicalDeviceSurfaceSupportKHR(device, i, VKVariables.surface, presentSupport);

				if(presentSupport.get(0) == VK_TRUE) {
					indices.presentFamily = i;
				}
			}

			return indices;
		}
	}

	public static PointerBuffer asPointerBuffer(Collection<String> collection) {

		MemoryStack stack = stackGet();

		PointerBuffer buffer = stack.mallocPointer(collection.size());

		collection.stream()
		.map(stack::UTF8)
		.forEach(buffer::put);

		return buffer.rewind();
	}

	public static PointerBuffer asPointerBuffer(List<? extends Pointer> list) {

		MemoryStack stack = stackGet();

		PointerBuffer buffer = stack.mallocPointer(list.size());

		list.forEach(buffer::put);

		return buffer.rewind();
	}

	public static PointerBuffer getRequiredExtensions() {

		PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

		return glfwExtensions;
	}
	
    /**
     * Translates a Vulkan {@code VkResult} value to a String describing the result.
     * 
     * @param result
     *            the {@code VkResult} value
     * 
     * @return the result description
     */
    public static String translateVulkanResult(int result) {
        switch (result) {
        // Success codes
        case VK_SUCCESS:
            return "Command successfully completed.";
        case VK_NOT_READY:
            return "A fence or query has not yet completed.";
        case VK_TIMEOUT:
            return "A wait operation has not completed in the specified time.";
        case VK_EVENT_SET:
            return "An event is signaled.";
        case VK_EVENT_RESET:
            return "An event is unsignaled.";
        case VK_INCOMPLETE:
            return "A return array was too small for the result.";
        case KHRSwapchain.VK_SUBOPTIMAL_KHR:
            return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
        case VK_ERROR_OUT_OF_HOST_MEMORY:
            return "A host memory allocation has failed.";
        case VK_ERROR_OUT_OF_DEVICE_MEMORY:
            return "A device memory allocation has failed.";
        case VK_ERROR_INITIALIZATION_FAILED:
            return "Initialization of an object could not be completed for implementation-specific reasons.";
        case VK_ERROR_DEVICE_LOST:
            return "The logical or physical device has been lost.";
        case VK_ERROR_MEMORY_MAP_FAILED:
            return "Mapping of a memory object has failed.";
        case VK_ERROR_LAYER_NOT_PRESENT:
            return "A requested layer is not present or could not be loaded.";
        case VK_ERROR_EXTENSION_NOT_PRESENT:
            return "A requested extension is not supported.";
        case VK_ERROR_FEATURE_NOT_PRESENT:
            return "A requested feature is not supported.";
        case VK_ERROR_INCOMPATIBLE_DRIVER:
            return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
        case VK_ERROR_TOO_MANY_OBJECTS:
            return "Too many objects of the type have already been created.";
        case VK_ERROR_FORMAT_NOT_SUPPORTED:
            return "A requested format is not supported on this device.";
        case VK_ERROR_SURFACE_LOST_KHR:
            return "A surface is no longer available.";
        case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
            return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
        case KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR:
            return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                    + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
        default:
            return String.format("%s [%d]", "Unknown", Integer.valueOf(result));
        }
    }
}
