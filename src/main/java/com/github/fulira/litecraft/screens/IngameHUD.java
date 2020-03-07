package com.github.fulira.litecraft.screens;

import org.joml.*;

import com.github.fulira.litecraft.Litecraft;
import com.github.fulira.litecraft.world.gen.WorldGenConstants;
import com.github.hydos.ginger.common.api.*;
import com.github.hydos.ginger.common.font.GUIText;
import com.github.hydos.ginger.common.screen.Screen;
import com.github.hydos.ginger.opengl.api.GingerGL;

public class IngameHUD extends Screen implements WorldGenConstants {
	private GUIText debugText;
	private GUIText positionText;
	// TODO: Add Vulkan text renderer
	private GingerEngine engine = GingerGL.getInstance();
	private Litecraft litecraft = Litecraft.getInstance();

	public IngameHUD() {
		debugText = ((GingerGL) engine).registerText("Loading...", 2, new Vector2f(0, 0), 1f, true, "debugInfo");
		debugText.setBorderWidth(0.5f);
		positionText = ((GingerGL) engine).registerText("Loading...", 2, new Vector2f(0, -0.1f), 1f, true, "posInfo");
		positionText.setBorderWidth(0.5f);
	}

	@Override
	public void render() {

	}

	@Override
	public void tick() {
//		long maxMemory = Runtime.getRuntime().maxMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		long usedMemory = (totalMemory - freeMemory) / 1024 / 1024;
		Vector4i dbg = litecraft.dbgStats;
		Vector3f position = GingerRegister.getInstance().game.data.playerObject.getPosition();
		Vector3i chunkPosition = new Vector3i((int)position.x >> POS_SHIFT, (int)position.y >> POS_SHIFT, (int)position.z >> POS_SHIFT);
		debugText.setText("FPS: " + dbg.x() + " UPS: " + dbg.y() + " TPS: " + dbg.z() + " TWL: " + dbg.w() + " Mem: "
				+ usedMemory + "MB");
		positionText.setText("Position: " + (int) position.x() + ", " + (int) position.y() + ", " + (int) position.z() +
				" Chunk Position: " + chunkPosition.x() + ", " + chunkPosition.y() + ", " + chunkPosition.z());
	}

	@Override
	public void cleanup() {
		debugText.remove();
		positionText.remove();
	}
}
