package io.github.hydos.ginger.engine.api;

import java.util.*;

import org.joml.Vector4f;

import io.github.hydos.ginger.engine.cameras.Camera;
import io.github.hydos.ginger.engine.elements.GuiTexture;
import io.github.hydos.ginger.engine.elements.objects.*;
import io.github.hydos.ginger.engine.terrain.Terrain;

/*
 * Used for storing essential engine game data so main class isn't messy
 * Also in general used with Game Class
 */
public class GameData {
	
	public List<GuiTexture> guis;
	public List<Entity> entities;
	public List<Light> lights;
	public List<Entity> normalMapEntities;
	public List<Terrain> flatTerrains;
	public Player player;
	public Camera camera;
	public Vector4f clippingPlane;
	
	public GameData(Player player, Camera camera) {
		clippingPlane = new Vector4f(0, -1, 0, 100000);
		guis = new ArrayList<GuiTexture>();
		entities = new ArrayList<Entity>();
		lights = new ArrayList<Light>();
		normalMapEntities = new ArrayList<Entity>();
		flatTerrains = new ArrayList<Terrain>();
		this.player = player;
		this.camera = camera;
	}

}
