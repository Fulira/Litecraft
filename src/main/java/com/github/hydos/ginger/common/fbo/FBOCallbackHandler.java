package com.github.hydos.ginger.common.fbo;

public abstract class FBOCallbackHandler
{
	public void cleanUp()
	{}

	public abstract void render(int texture);

	public void resize()
	{}
}
