package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.paragrowth.terrain.generator.Noise;

import java.util.Random;

/**
 *
 */
public class DoodadWorld implements RenderableProvider {
    private static final int PATCH_SIZE = 256;

    private static final int DOODADS_PER_PATCH = 256;
    private static final MeshBuilder MESH_BUILDER = new MeshBuilder();
    private static final VertexAttributes MESH_ATTRIBUTES = new VertexAttributes(
            VertexAttribute.Position(),//3
            VertexAttribute.ColorPacked()//1
    );

    private final Camera camera;
    private final Mesh[] patches;
    private final BoundingBox[] patchBoundingBoxes;

    public DoodadWorld(Camera camera, long seed, float[][] noise) {
        this.camera = camera;
        final int worldWidth = noise.length;
        final int worldHeight = noise[0].length;
        final int patchesX = MathUtils.ceil(worldWidth / PATCH_SIZE);
        final int patchesY = MathUtils.ceil(worldHeight / PATCH_SIZE);

        this.patches = new Mesh[patchesX * patchesY];
        this.patchBoundingBoxes = new BoundingBox[this.patches.length];

        final RandomXS128 random = new RandomXS128(seed);

        int i = 0;
        for (int x = 0; x < patchesX; x++) {
            for (int y = 0; y < patchesY; y++) {
                final Mesh mesh = buildPatch(random, noise, x * PATCH_SIZE, y * PATCH_SIZE);
                this.patches[i] = mesh;
                if (mesh != null) {
                    final BoundingBox box = new BoundingBox();
                    mesh.calculateBoundingBox(box);
                    this.patchBoundingBoxes[i] = box;
                }
                i++;
            }
        }
    }

    private Mesh buildPatch(Random random, float[][] noise, float baseX, float baseY) {
        final MeshBuilder builder = MESH_BUILDER;
        boolean begun = false;

        for (int i = 0; i < DOODADS_PER_PATCH; i++) {
            final float x = baseX + random.nextFloat() * PATCH_SIZE;
            final float y = baseY + random.nextFloat() * PATCH_SIZE;
            //TODO Cull if too close to other doodads

            final float z = Noise.getHeight(noise, x, y);
            // Cull doodads in water or too close
            if (z <= 0.1f) {
                continue;
            }

            if (!begun) {
                builder.begin(MESH_ATTRIBUTES);
                begun = true;
            }
            Doodads.STICK.instantiate(random, x, y, z).build(builder);
        }

        if (!begun) {
            return null;
        }
        return builder.end();
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final Mesh[] patches = this.patches;
        final Frustum frustum = camera.frustum;

        for (int i = 0; i < patches.length; i++) {
            final BoundingBox box = this.patchBoundingBoxes[i];
            if (box != null && frustum.boundsInFrustum(box)) {
                final Mesh patch = patches[i];

                final Renderable renderable = pool.obtain();
                renderable.meshPart.set(null, patch, 0, patch.getNumIndices(), GL20.GL_TRIANGLES);
                renderable.worldTransform.idt();
                box.getCenter(renderable.meshPart.center);
                renderable.meshPart.radius = PATCH_SIZE;

                renderable.shader = DoodadShader.get(renderable);
                renderables.add(renderable);
            }
        }
    }
}
