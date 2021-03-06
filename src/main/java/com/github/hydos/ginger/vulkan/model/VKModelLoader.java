package com.github.hydos.ginger.vulkan.model;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.assimp.Assimp.*;

import java.io.File;
import java.nio.IntBuffer;
import java.util.*;

import org.joml.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

public class VKModelLoader {

    public static VKMesh loadModel(File file, int flags) {

        try(AIScene scene = aiImportFile(file.getAbsolutePath(), flags)) {

            if(scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + aiGetErrorString());
            }

            VKMesh model = new VKMesh();

            processNode(scene.mRootNode(), scene, model);
            
            if(model.texCoords == null) {
            	model.hasTexCoords = false;
            }
            return model;
        }
    }

    private static void processNode(AINode node, AIScene scene, VKMesh model) {

        if(node.mMeshes() != null) {
            processNodeMeshes(scene, node, model);
        }

        if(node.mChildren() != null) {

            PointerBuffer children = node.mChildren();

            for(int i = 0;i < node.mNumChildren();i++) {
                processNode(AINode.create(children.get(i)), scene, model);
            }

        }

    }

    private static void processNodeMeshes(AIScene scene, AINode node, VKMesh model) {

        PointerBuffer pMeshes = scene.mMeshes();
        IntBuffer meshIndices = node.mMeshes();

        for(int i = 0;i < meshIndices.capacity();i++) {
            AIMesh mesh = AIMesh.create(pMeshes.get(meshIndices.get(i)));
            processMesh(scene, mesh, model);
        }

    }

    private static boolean processMesh(AIScene scene, AIMesh mesh, VKMesh model) {

        processPositions(mesh, model.positions);
        boolean out = processTexCoords(mesh, model.texCoords);

        processIndices(mesh, model.indices);
        return out;
    }

    private static void processPositions(AIMesh mesh, List<Vector3fc> positions) {

        AIVector3D.Buffer vertices = requireNonNull(mesh.mVertices());

        for(int i = 0;i < vertices.capacity();i++) {
            AIVector3D position = vertices.get(i);
            positions.add(new Vector3f(position.x(), position.y(), position.z()));
        }

    }

    private static boolean processTexCoords(AIMesh mesh, List<Vector2fc> texCoords) {
    	try {
            AIVector3D.Buffer aiTexCoords = mesh.mTextureCoords(0);
            
            for(int i = 0;i < aiTexCoords.capacity();i++) {
                final AIVector3D coords = aiTexCoords.get(i);
                texCoords.add(new Vector2f(coords.x(), coords.y()));
            }
            return true;
    	}catch(Exception e) {
    		texCoords = null;
    		System.out.println("a model is missing texture coords");
    		return false;
    	}
    }

    private static void processIndices(AIMesh mesh, List<Integer> indices) {

        AIFace.Buffer aiFaces = mesh.mFaces();

        for(int i = 0;i < mesh.mNumFaces();i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer pIndices = face.mIndices();
            for(int j = 0;j < face.mNumIndices();j++) {
                indices.add(pIndices.get(j));
            }
        }

    }

    public static class VKMesh {

        public boolean hasTexCoords = true;
		public final List<Vector3fc> positions;
        public final List<Vector2fc> texCoords;
        public final List<Integer> indices;

        public VKMesh() {
            this.positions = new ArrayList<>();
            this.texCoords = new ArrayList<>();
            this.indices = new ArrayList<>();
        }
    }


}
