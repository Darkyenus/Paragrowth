package com.darkyen.paragrowth.skybox;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;

/**
 * @author Darkyen
 */
public class SkyboxRenderable extends Renderable {
    public SkyboxRenderable() {
        //Mesh
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(VertexAttributes.Usage.Position,GL20.GL_TRIANGLES);
        BoxShapeBuilder.build(meshBuilder, 1f,1f,1f);
        final Mesh mesh = meshBuilder.end();

        this.meshPart.set("sky", mesh, 0, mesh.getNumIndices(), GL20.GL_TRIANGLES);

        this.shader = SkyboxShader.get(this);
    }

    public void dispose() {
        this.meshPart.mesh.dispose();
    }
}
