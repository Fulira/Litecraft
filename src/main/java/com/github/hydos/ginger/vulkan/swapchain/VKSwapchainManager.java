package com.github.hydos.ginger.vulkan.swapchain;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import com.github.hydos.ginger.VulkanExample.QueueFamilyIndices;
import com.github.hydos.ginger.VulkanExample.SwapChainSupportDetails;
import com.github.hydos.ginger.common.io.Window;
import com.github.hydos.ginger.vulkan.VKVariables;
import com.github.hydos.ginger.vulkan.managers.VKCommandBufferManager;
import com.github.hydos.ginger.vulkan.managers.VKUBOManager;
import com.github.hydos.ginger.vulkan.render.VKRenderManager;
import com.github.hydos.ginger.vulkan.render.pipelines.VKPipelineManager;
import com.github.hydos.ginger.vulkan.utils.VKUtils;

public class VKSwapchainManager
{
	
    public static void cleanupSwapChain() {

        vkDestroyImageView(VKVariables.device, VKVariables.colorImageView, null);
        vkDestroyImage(VKVariables.device, VKVariables.colorImage, null);
        vkFreeMemory(VKVariables.device, VKVariables.colorImageMemory, null);

        vkDestroyImageView(VKVariables.device, VKVariables.depthImageView, null);
        vkDestroyImage(VKVariables.device, VKVariables.depthImage, null);
        vkFreeMemory(VKVariables.device, VKVariables.depthImageMemory, null);

        VKUBOManager.uniformBuffers.forEach(ubo -> vkDestroyBuffer(VKVariables.device, ubo, null));
        VKUBOManager.uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(VKVariables.device, uboMemory, null));

        vkDestroyDescriptorPool(VKVariables.device, VKUBOManager.descriptorPool, null);

        VKVariables.swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(VKVariables.device, framebuffer, null));

        vkFreeCommandBuffers(VKVariables.device, VKVariables.commandPool, VKUtils.asPointerBuffer(VKVariables.commandBuffers));

        vkDestroyPipeline(VKVariables.device, VKVariables.graphicsPipeline, null);

        vkDestroyPipelineLayout(VKVariables.device, VKVariables.pipelineLayout, null);

        vkDestroyRenderPass(VKVariables.device, VKVariables.renderPass, null);

        VKVariables.swapChainImageViews.forEach(imageView -> vkDestroyImageView(VKVariables.device, imageView, null));

        vkDestroySwapchainKHR(VKVariables.device, VKVariables.swapChain, null);
    }
    
    public static void recreateSwapChain() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            while(width.get(0) == 0 && height.get(0) == 0) {
                glfwGetFramebufferSize(Window.getWindow(), width, height);
                glfwWaitEvents();
            }
        }

        vkDeviceWaitIdle(VKVariables.device);

        VKSwapchainManager.cleanupSwapChain();

        createSwapChainObjects();
    }
    
    public static void createSwapChain() {

        try(MemoryStack stack = stackPush()) {

            SwapChainSupportDetails swapChainSupport = VKUtils.querySwapChainSupport(VKVariables.physicalDevice, stack);

            VkSurfaceFormatKHR surfaceFormat = VKUtils.chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = VKUtils.chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = VKUtils.chooseSwapExtent(swapChainSupport.capabilities);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

            if(swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(VKVariables.surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            QueueFamilyIndices indices = VKUtils.findQueueFamilies(VKVariables.physicalDevice);

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(VKVariables.device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            VKVariables.swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(VKVariables.device, VKVariables.swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(VKVariables.device, VKVariables.swapChain, imageCount, pSwapchainImages);

            VKVariables.swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
            	VKVariables.swapChainImages.add(pSwapchainImages.get(i));
            }

            VKVariables.swapChainImageFormat = surfaceFormat.format();
            VKVariables.swapChainExtent = VkExtent2D.create().set(extent);
        }
    }
    
    
    /**
     * i tried organising it but if i change the order everything breaks
     */
    public static void createSwapChainObjects() {
    	createSwapChain();
    	VKUtils.createImageViews();
    	VKRenderManager.createRenderPass();
        VKPipelineManager.createGraphicsPipeline();
        VKUtils.createColorResources();
        VKUtils.createDepthResources();
        VKUtils.createFramebuffers();
        VKUtils.createUniformBuffers();
        VKUtils.createDescriptorPool();
        VKUBOManager.createUBODescriptorSets();
        VKCommandBufferManager.createCommandBuffers();
    }
	
}
