package com.github.halotroop.litecraft;

import org.joml.*;

import com.github.halotroop.litecraft.save.LitecraftSave;
import com.github.halotroop.litecraft.screens.*;
import com.github.halotroop.litecraft.types.block.Blocks;
import com.github.halotroop.litecraft.types.entity.PlayerEntity;
import com.github.halotroop.litecraft.util.RelativeDirection;
import com.github.halotroop.litecraft.world.World;
import com.github.hydos.ginger.engine.common.Constants;
import com.github.hydos.ginger.engine.common.api.GingerRegister;
import com.github.hydos.ginger.engine.common.api.game.*;
import com.github.hydos.ginger.engine.common.cameras.*;
import com.github.hydos.ginger.engine.common.elements.objects.*;
import com.github.hydos.ginger.engine.common.font.FontType;
import com.github.hydos.ginger.engine.common.info.RenderAPI;
import com.github.hydos.ginger.engine.common.io.Window;
import com.github.hydos.ginger.engine.common.obj.ModelLoader;
import com.github.hydos.ginger.engine.opengl.api.*;
import com.github.hydos.ginger.engine.opengl.render.MasterRenderer;
import com.github.hydos.ginger.engine.opengl.render.models.TexturedModel;
import com.github.hydos.ginger.engine.opengl.utils.GlLoader;

import tk.valoeghese.gateways.client.io.*;

public class Litecraft extends Game
{
	private static Litecraft INSTANCE;
	private World world;
	private LitecraftSave save;
	private GingerGL engine;
	public PlayerEntity playerEntity;
	private Camera camera;
	public int fps, ups, tps;
	public Vector4i dbgStats = new Vector4i();
	private long frameTimer;

	public int threadWaitlist = 0;

	public Litecraft()
	{
		Litecraft.INSTANCE = this;
		// set constants
		this.setupConstants();
		this.setupGinger(1280, 720, 60);
		Blocks.init(); // make sure blocks are initialised
		this.frameTimer = System.currentTimeMillis();
		setupKeybinds(); // setup keybinds
		// start the game loop
		this.engine.startGameLoop();
	}

	@Override
	public void exit()
	{
		engine.openScreen(new ExitGameScreen());
		render(); // Render the exit game screen
		if (this.world != null)
		{
			System.out.println("Saving chunks...");
			long time = System.currentTimeMillis();
			this.world.unloadAllChunks();
			this.getSave().saveGlobalData(this.world.getSeed(), this.playerEntity);
			System.out.println("Saved world in " + (System.currentTimeMillis() - time) + " milliseconds");
		}
		engine.cleanup();
		System.exit(0);
	}

	/**
	 * Things that ARE rendering: Anything that results in something being drawn to the frame buffer
	 * Things that are NOT rendering: Things that happen to update between frames but do not result in things being drawn to the screen
	 */
	@Override
	public void render()
	{
		fps += 1;
		if (System.currentTimeMillis() > frameTimer + 1000) updateDebugStats();
		// Render shadows
		GingerRegister.getInstance().masterRenderer.renderShadowMap(data.entities, data.lights.get(0));
		// If there's a world, render it!
		if (this.world != null) this.engine.renderWorld(this);
		// Render any overlays (GUIs, HUDs)
		this.engine.renderOverlays(this);
		// Put what's stored in the inactive framebuffer on the screen
		Window.swapBuffers();
	}
	
	// Updates the debug stats once per real-time second, regardless of how many frames have been rendered
	private void updateDebugStats()
	{
		this.dbgStats.set(fps, ups, tps, threadWaitlist);
		this.fps=0;
		this.ups=0;
		this.tps=0;
		this.frameTimer += 1000;
	}
	
	public void update()
	{
		Input.invokeAllListeners();
	}

	private void setupConstants()
	{
		Constants.movementSpeed = 0.5f; // movement speed
		Constants.turnSpeed = 0.00006f; // turn speed
		Constants.gravity = new Vector3f(0, -0.0000000005f, 0); // compute gravity as a vec3f
		Constants.jumpPower = 0.00005f; // jump power
	}

	// set up Ginger3D engine stuff
	private void setupGinger(int windowWidth, int windowHeight, float frameCap)
	{
		if (engine == null) // Prevents this from being run more than once on accident.
		{
			Window.create(windowWidth, windowHeight, "Litecraft", frameCap, RenderAPI.OpenGL); // create window
			// set up the gateways keybind key tracking
			KeyCallbackHandler.trackWindow(Window.getWindow());
			MouseCallbackHandler.trackWindow(Window.getWindow());
			// set up ginger utilities
			GingerUtils.init();
			//Set the player model
			TexturedModel playerModel = ModelLoader.loadGenericCube("block/cubes/stone/brick/stonebrick.png");
			Light sun = new Light(new Vector3f(0, 105, 0), new Vector3f(0.9765625f, 0.98828125f, 0.05859375f), new Vector3f(0.002f, 0.002f, 0.002f));
			FontType font = new FontType(GlLoader.loadFontAtlas("candara.png"), "candara.fnt");
			this.engine = new GingerGL();
			this.playerEntity = new PlayerEntity(playerModel, new Vector3f(0, 0, -3), 0, 180f, 0, new Vector3f(0.2f, 0.2f, 0.2f));
			this.camera = new FirstPersonCamera(playerEntity);
			this.playerEntity.setVisible(false);
			this.data = new GameData(this.playerEntity, this.camera, 20);
			this.data.handleGuis = false;
			this.engine.setup(new MasterRenderer(this.camera), INSTANCE);
			this.engine.setGlobalFont(font);
			this.data.lights.add(sun);
			this.data.entities.add(this.playerEntity);
		}
	}

	private void setupKeybinds()
	{
		Input.addPressCallback(Keybind.EXIT, this::exit);
		Input.addInitialPressCallback(Keybind.FULLSCREEN, Window::fullscreen);
		Input.addInitialPressCallback(Keybind.WIREFRAME, GingerRegister.getInstance()::toggleWireframe);
		Input.addPressCallback(Keybind.MOVE_FORWARD, () -> this.playerEntity.move(RelativeDirection.FORWARD));
		Input.addPressCallback(Keybind.MOVE_BACKWARD, () -> this.playerEntity.move(RelativeDirection.BACKWARD));
		Input.addPressCallback(Keybind.STRAFE_LEFT, () -> this.playerEntity.move(RelativeDirection.LEFT));
		Input.addPressCallback(Keybind.STRAFE_RIGHT, () -> this.playerEntity.move(RelativeDirection.RIGHT));
		Input.addPressCallback(Keybind.FLY_UP, () -> this.playerEntity.move(RelativeDirection.UP));
		Input.addPressCallback(Keybind.FLY_DOWN, () -> this.playerEntity.move(RelativeDirection.DOWN));
	}

	/**
	 * Things that should be ticked: Entities when deciding an action, in-game timers (such as smelting), the in-game time
	 * Things that should not be ticked: Rendering, input, player movement
	 */ 
	@Override
	public void tick()
	{
		// Open the title screen if it's not already open.
		if (GingerRegister.getInstance().currentScreen == null && world == null)
			engine.openScreen(new TitleScreen());
		
		if (data.playerEntity != null && data.camera != null)
		{
			data.playerEntity.updateMovement();
			data.camera.updateMovement();
		}
	}
	
	// @formatter=off
	public static Litecraft getInstance()
	{ return INSTANCE; }

	public Camera getCamera()
	{ return this.camera; }

	public LitecraftSave getSave()
	{ return save; }

	public World getWorld()
	{ return this.world; }
	
	public void changeWorld(World world)
	{ this.world = world; }
	
	public void setSave(LitecraftSave save)
	{ this.save = save; }
}