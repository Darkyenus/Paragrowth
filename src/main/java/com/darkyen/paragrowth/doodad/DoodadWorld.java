package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.render.MeshBuilding;
import com.darkyen.paragrowth.terrain.generator.Noise;
import com.darkyen.paragrowth.util.DebugRenderKt;

import java.util.Random;

/**
 *
 */
public class DoodadWorld implements RenderableProvider {
    private static final int PATCH_SIZE = 256;

    private static final int DOODADS_PER_PATCH = 256;

    private final Camera camera;

    private final int patchCount;
    private final Mesh[] patches;
    private final Array<DoodadInstance>[] doodadInstances;
    private final BoundingBox[] patchBoundingBoxes;

    public DoodadWorld(Camera camera, long seed, float[][] noise, WorldCharacteristics characteristics) {
        this.camera = camera;
        final int worldWidth = noise.length;
        final int worldHeight = noise[0].length;
        final int patchesX = (worldWidth + PATCH_SIZE - 1) / PATCH_SIZE;
        final int patchesY = (worldHeight + PATCH_SIZE - 1) / PATCH_SIZE;

        this.patches = new Mesh[patchesX * patchesY];
        //noinspection unchecked
        this.doodadInstances = new Array[this.patches.length];
        this.patchBoundingBoxes = new BoundingBox[this.patches.length];

        final RandomXS128 random = new RandomXS128(seed);

        final Array<Doodad> doodadSet = Doodads.createDoodadSet(random, characteristics);
        Array<DoodadInstance> patchInstances = new Array<>(DoodadInstance.class);
        int totalDoodads = 0;
        int i = 0;
        for (int x = 0; x < patchesX; x++) {
            for (int y = 0; y < patchesY; y++) {
                final Mesh mesh = buildPatch(random, noise, x * PATCH_SIZE, y * PATCH_SIZE, doodadSet, patchInstances, characteristics);
                if (mesh == null)
                    continue;

                this.patches[i] = mesh;
                this.patchBoundingBoxes[i] = mesh.calculateBoundingBox();
                this.doodadInstances[i] = patchInstances;

                totalDoodads += patchInstances.size;
                patchInstances = new Array<>(DoodadInstance.class);
                i++;
            }
        }
        patchCount = i;

        System.out.println("Generated "+totalDoodads+" doodads");
    }

    private Mesh buildPatch(Random random, float[][] noise, float baseX, float baseY, Array<Doodad> doodadSet, Array<DoodadInstance> instances, WorldCharacteristics characteristics) {
        final MeshBuilder builder = MeshBuilding.MESH_BUILDER;
        boolean begun = false;

        for (int i = 0; i < DOODADS_PER_PATCH; i++) {
            final float x = baseX + random.nextFloat() * PATCH_SIZE;
            final float y = baseY + random.nextFloat() * PATCH_SIZE;

            final float z = Noise.getHeight(noise, x, y);
            // Cull doodads in water or (TODO:) too close
            if (z <= 0.1f) {
                continue;
            }

            if (!begun) {
                builder.begin(MeshBuilding.POSITION3_COLOR1_ATTRIBUTES);
                begun = true;
            }

            final DoodadInstance instance = doodadSet.get(random.nextInt(doodadSet.size)).instantiate(random, x, y, z, characteristics);
            instances.add(instance);
            instance.build(builder, random, characteristics);
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

        for (int i = 0; i < patchCount; i++) {
            final BoundingBox box = this.patchBoundingBoxes[i];
            if (!frustum.boundsInFrustum(box)) {
                continue;
            }

            final Mesh patch = patches[i];

            final Renderable renderable = pool.obtain();
            renderable.meshPart.set(null, patch, 0, patch.getNumIndices(), GL20.GL_TRIANGLES);
            renderable.worldTransform.idt();
            box.getCenter(renderable.meshPart.center);
            renderable.meshPart.radius = PATCH_SIZE;

            // TODO(jp): Uncomment
            //renderable.shader = DoodadShader.INSTANCE;
            renderables.add(renderable);
        }
    }

    public void renderDebug(ImmediateModeRenderer renderer) {
        final Frustum frustum = camera.frustum;

        for (int i = 0; i < patchCount; i++) {
            final BoundingBox box = this.patchBoundingBoxes[i];
            final boolean shown = frustum.boundsInFrustum(box);

            DebugRenderKt.forEdges(box, (x1, y1, z1, x2, y2, z2) -> {
                renderer.color(shown ? Color.GREEN : Color.RED);
                renderer.vertex(x1, y1, z1);
                renderer.color(shown ? Color.GREEN : Color.RED);
                renderer.vertex(x2, y2, z2);

                return null;
            });
        }
    }
}
