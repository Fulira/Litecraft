package com.github.hydos.ginger.vulkan.elements;

import org.joml.Vector3f;

import com.github.hydos.ginger.common.elements.RenderObject;
import com.github.hydos.ginger.vulkan.model.VKModelLoader.VKMesh;
import com.github.hydos.ginger.vulkan.render.VKBufferMesh;

public class VKRenderObject extends RenderObject
{
	
	private VKBufferMesh model = null;//until i add VKTextured models
	private VKMesh rawModel;//until i add VKTextured models

	public VKRenderObject(VKMesh rawModel, Vector3f position, float rotX, float rotY, float rotZ, Vector3f scale)
	{
		this.rawModel = rawModel;
		this.position = position;
		this.rotX = rotX;
		this.rotY = rotY;
		this.rotZ = rotZ;
		this.scale = scale;
	}

	public VKBufferMesh getModel()
	{ return model; }
	
	public void setModel(VKBufferMesh model)
	{ this.model = model; }

	public VKMesh getRawModel()
	{
		return rawModel; 
	}
	
}
