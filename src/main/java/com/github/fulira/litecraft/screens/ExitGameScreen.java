package com.github.fulira.litecraft.screens;

import org.joml.Vector2f;

import com.github.hydos.ginger.common.api.GingerEngine;
import com.github.hydos.ginger.common.font.GUIText;
import com.github.hydos.ginger.common.io.Window;
import com.github.hydos.ginger.common.screen.Screen;
import com.github.hydos.ginger.opengl.api.GingerGL;

public class ExitGameScreen extends Screen {
	private GUIText infoText;
	// TODO: Add Vulkan text renderer
	private GingerEngine engine = GingerGL.getInstance();

	public ExitGameScreen() {
		infoText = ((GingerGL) engine).registerText("Saving and exiting...", 3,
				new Vector2f(Window.getWidth() / 2, Window.getHeight() / 2), 1f, true, "info");
		infoText.setBorderWidth(0.5f);
	}

	@Override
	public void render() {

	}

	@Override
	public void tick() {
	}

	@Override
	public void cleanup() {
		infoText.remove();
	}
}
