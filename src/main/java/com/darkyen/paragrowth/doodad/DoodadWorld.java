package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * @author Darkyen
 */
public class DoodadWorld implements RenderableProvider {

    private final Array<Doodad> doodads = new Array<>(false, 128, Doodad.class);
    private final Array<Matrix4> doodadPositions = new Array<>(false, 128, Matrix4.class);
    private final Camera camera;

    public DoodadWorld(Camera camera) {
        this.camera = camera;
    }

    public Matrix4 addDoodad(Doodad doodad){
        Matrix4 position = new Matrix4();
        doodads.add(doodad);
        doodadPositions.add(position);
        return position;
    }

    private final Vector3 getRenderables_tmp = new Vector3();
    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        Array<Doodad> doodads = this.doodads;
        Array<Matrix4> doodadPositions = this.doodadPositions;

        final Vector3 tmp = getRenderables_tmp;
        final Frustum frustum = camera.frustum;

        for (int i = 0, s = doodads.size; i < s; i++) {
            Doodad doodad = doodads.get(i);
            Matrix4 position = doodadPositions.get(i);

            position.getTranslation(tmp);
            if(frustum.sphereInFrustum(tmp, doodad.radius)){
                Renderable renderable = doodad.getRenderable(pool);
                renderable.worldTransform.set(position);

                renderables.add(renderable);
            }
        }
    }
}
