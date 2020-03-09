package com.github.halotroop.litecraft.types.entity;

import org.joml.Vector3f;

import com.github.hydos.ginger.common.elements.objects.GLRenderObject;
import com.github.hydos.ginger.opengl.render.models.GLTexturedModel;

public abstract class Entity extends GLRenderObject {
	public Entity(GLTexturedModel model, Vector3f position, float rotX, float rotY, float rotZ, Vector3f scale) {
		super(model, position, rotX, rotY, rotZ, scale);
	}
}
