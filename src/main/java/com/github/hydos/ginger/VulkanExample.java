package com.github.hydos.ginger;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

import java.io.File;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.*;

import org.joml.*;
import org.lwjgl.vulkan.*;

import com.github.hydos.ginger.common.info.RenderAPI;
import com.github.hydos.ginger.common.io.Window;
import com.github.hydos.ginger.vulkan.*;
import com.github.hydos.ginger.vulkan.elements.VKRenderObject;
import com.github.hydos.ginger.vulkan.io.VKWindow;
import com.github.hydos.ginger.vulkan.managers.VKUBOManager;
import com.github.hydos.ginger.vulkan.managers.VKTextureManager;
import com.github.hydos.ginger.vulkan.model.VKModelLoader;
import com.github.hydos.ginger.vulkan.model.VKModelLoader.VKMesh;
import com.github.hydos.ginger.vulkan.render.Frame;
import com.github.hydos.ginger.vulkan.swapchain.VKSwapchainManager;
import com.github.hydos.ginger.vulkan.ubo.*;
import com.github.hydos.ginger.vulkan.ubo.UBO.VKMat4UboData;
import com.github.hydos.ginger.vulkan.utils.*;

public class VulkanExample {

	public static final int UINT32_MAX = 0xFFFFFFFF;
	public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

	public static final int MAX_FRAMES_IN_FLIGHT = 2;

	public static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

	public static class QueueFamilyIndices {

		public Integer graphicsFamily;
		public Integer presentFamily;

		public boolean isComplete() {
			return graphicsFamily != null && presentFamily != null;
		}

		public int[] unique() {
			return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
		}
	}

	public static class SwapChainSupportDetails {

		public VkSurfaceCapabilitiesKHR capabilities;
		public VkSurfaceFormatKHR.Buffer formats;
		public IntBuffer presentModes;

	}

	public static class UniformBufferObject {

		public static final int SIZEOF = 3 * 16 * Float.BYTES;

		public VKMat4UboData model;
		public VKMat4UboData view;
		public VKMat4UboData proj;

		public UniformBufferObject() {
			model = new VKMat4UboData();
			view = new VKMat4UboData();
			proj = new VKMat4UboData();
			model.mat4 = new Matrix4f();
			view.mat4 = new Matrix4f();
			proj.mat4 = new Matrix4f();
		}
	}

	public void run() {
		initWindow();
		initVulkan();
		mainLoop();
		GingerVK.getInstance().cleanup();
		VKUtils.cleanup();
	}

	private void loadTestModel() {

		File modelFile = new File(ClassLoader.getSystemClassLoader().getResource("models/chalet.obj").getFile());
		File modelFile2 = new File(ClassLoader.getSystemClassLoader().getResource("models/bunnyChalet.obj").getFile());

		VKMesh model = VKModelLoader.loadModel(modelFile, aiProcess_FlipUVs | aiProcess_DropNormals);
		VKMesh model2 = VKModelLoader.loadModel(modelFile2, aiProcess_FlipUVs | aiProcess_DropNormals);
		VKRenderObject object = new VKRenderObject(model, new Vector3f(), 1, 1, 1, new Vector3f());
		VKRenderObject object2 = new VKRenderObject(model2, new Vector3f(), 1, 2, 1, new Vector3f());
		GingerVK.getInstance().entityRenderer.processEntity(object);
		GingerVK.getInstance().entityRenderer.processEntity(object2);
	}

	private void initWindow() {
		Window.create(1200, 800, "Vulkan Ginger2", 61, RenderAPI.Vulkan);
		glfwSetFramebufferSizeCallback(Window.getWindow(), this::framebufferResizeCallback);
	}

	private void framebufferResizeCallback(long window, int width, int height) {
		VKVariables.framebufferResize = true;
	}

	private void initVulkan() {
		VKRegister.createInstance();
		VKWindow.createSurface();
		GingerVK.init();
		GingerVK.getInstance().createRenderers();
		VKDeviceManager.pickPhysicalDevice();
		VKDeviceManager.createLogicalDevice();
		VKUtils.createCommandPool();
		VKTextureManager.createTextureImage();
		VKTextureManager.createTextureImageView();
		VKTextureManager.createTextureSampler();
		loadTestModel();

//		//create the projection and view matrix ubo
		UBO viewProjUbo = new UBO();
		viewProjUbo.bindIndex = 0;
		
		VKUBOManager.addUBO(viewProjUbo);
		VKUBOManager.createUBODescriptorSetLayout();
		
		VKSwapchainManager.createSwapChainObjects();
		VKUtils.createSyncObjects();
	}

	private void mainLoop() {
		while (!Window.closed()) {
			if (Window.shouldRender()) {
				Frame.drawFrame();
			}
			glfwPollEvents();
		}

		// Wait for the device to complete all operations before release resources
		vkDeviceWaitIdle(VKVariables.device);
	}

	public static void main(String[] args) {

		VulkanExample app = new VulkanExample();

		app.run();
	}

}
