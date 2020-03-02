package com.github.halotroop.litecraft.screens;

import org.joml.*;

import com.github.halotroop.litecraft.Litecraft;
import com.github.hydos.ginger.engine.common.api.GingerRegister;
import com.github.hydos.ginger.engine.common.font.GUIText;
import com.github.hydos.ginger.engine.common.screen.Screen;
import com.github.hydos.ginger.engine.opengl.api.*;

public class IngameHUD extends Screen
{
	private GUIText debugText;
	private GUIText positionText;
	private GingerGL ginger3D = GingerGL.getInstance();
	private Litecraft litecraft = Litecraft.getInstance();
	
	public IngameHUD()
	{
		debugText = ginger3D.registerText("Loading...", 2, new Vector2f(0, 0), 1f, true, "debugInfo");
		debugText.setBorderWidth(0.5f);
		positionText = ginger3D.registerText("Loading...", 2, new Vector2f(0, -0.1f), 1f, true, "debugInfo");
		positionText.setBorderWidth(0.5f);
	}
	
	@Override
	public void render()
	{
		
	}

	@Override
	public void tick()
	{
//		long maxMemory = Runtime.getRuntime().maxMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		long usedMemory = totalMemory - freeMemory;
		Vector4i dbg = litecraft.dbgStats;
		Vector3f position = GingerRegister.getInstance().game.data.player.getPosition();
		debugText.setText("FPS: " + dbg.x() + " UPS: " + dbg.y + " TPS: " + dbg.z + " Mem: " + (usedMemory / 1024 / 1024) + "MB / " + (totalMemory / 1024 / 1024) + " MB");
		positionText.setText("Position " + (int) position.x() + ", " + (int) position.y() + ", "+ (int) position.z() );
	}

	@Override
	public void close()
	{
		debugText.remove();
		positionText.remove();
	}
}
