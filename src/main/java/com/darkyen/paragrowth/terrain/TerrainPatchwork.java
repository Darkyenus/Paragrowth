package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.paragrowth.terrain.generator.TerrainGenerator;

/**
 * A collection of terrain patches.
 */
public class TerrainPatchwork implements RenderableProvider {

    private final Camera camera;
    private final TerrainPatch[] patches;

    public TerrainPatchwork(int patchAmount, Camera camera, TerrainGenerator terrainGenerator) {
        this.camera = camera;
        this.patches = new TerrainPatch[patchAmount * patchAmount];

        int i = 0;
        for (int y = 0; y < patchAmount; y++) {
            for (int x = 0; x < patchAmount; x++) {
                patches[i++] = new TerrainPatch(x * TerrainPatch.PATCH_WIDTH, y * TerrainPatch.PATCH_HEIGHT, terrainGenerator);
            }
        }
    }

    public final float heightAt(float x, float y) {
        //TODO Use prepared height maps in patches
        return 1f;
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final Frustum frustum = camera.frustum;
        for (TerrainPatch patch: patches){
            if (frustum.boundsInFrustum(patch.boundingBox)) {
                final Renderable renderable = pool.obtain();
                patch.fillRenderable(renderable);
                renderables.add(renderable);
            }
        }
    }
}
