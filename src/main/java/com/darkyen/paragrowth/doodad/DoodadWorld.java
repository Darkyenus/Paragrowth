package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.WorldSpecifics;
import com.darkyen.paragrowth.render.GlBuffer;
import com.darkyen.paragrowth.render.GlVertexArrayObject;
import com.darkyen.paragrowth.render.ModelBuilder;
import com.darkyen.paragrowth.render.RenderBatch;
import com.darkyen.paragrowth.render.RenderModel;
import com.darkyen.paragrowth.render.Renderable;
import com.darkyen.paragrowth.util.DebugRenderKt;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 *
 */
public class DoodadWorld implements Renderable, Disposable {
    private static final int PATCH_SIZE = 256;

    private static final int DOODADS_PER_PATCH = 256;

    private final Camera camera;

    private final int patchCount;
    private final DoodadPatch[] patches;
    private final Array<DoodadInstance>[] doodadInstances;

    public DoodadWorld(Camera camera, long seed, WorldSpecifics world) {
        this.camera = camera;
        final int minPatchX = MathUtils.floor(world.offsetX / PATCH_SIZE);
        final int minPatchY = MathUtils.floor(world.offsetY / PATCH_SIZE);
        final int maxPatchX = MathUtils.ceil((world.offsetX + world.sizeX()) / PATCH_SIZE);
        final int maxPatchY = MathUtils.ceil((world.offsetY + world.sizeY()) / PATCH_SIZE);

        this.patches = new DoodadPatch[(maxPatchX - minPatchX) * (maxPatchY - minPatchY)];
        //noinspection unchecked
        this.doodadInstances = new Array[this.patches.length];

        final RandomXS128 random = new RandomXS128(seed);

        final Array<Doodad> doodadSet = Doodads.createDoodadSet(random, world.characteristics);
        Array<DoodadInstance> patchInstances = new Array<>(DoodadInstance.class);
        int totalDoodads = 0;
        int i = 0;
        for (int x = minPatchX; x < maxPatchX; x++) {
            for (int y = minPatchY; y < maxPatchX; y++) {
                final DoodadPatch patch = buildPatch(random, world, x * PATCH_SIZE, y * PATCH_SIZE, doodadSet, patchInstances, world.characteristics);
                if (patch == null)
                    continue;

                this.patches[i] = patch;
                this.doodadInstances[i] = patchInstances;

                totalDoodads += patchInstances.size;
                patchInstances = new Array<>(DoodadInstance.class);
                i++;
            }
        }
        patchCount = i;

        System.out.println("Generated "+totalDoodads+" doodads");
    }

    private static DoodadPatch buildPatch(Random random, WorldSpecifics noise, float baseX, float baseY, Array<Doodad> doodadSet, Array<DoodadInstance> instances, WorldCharacteristics characteristics) {
        final ModelBuilder builder = new ModelBuilder(3 + 1);

        for (int i = 0; i < DOODADS_PER_PATCH; i++) {
            final float x = baseX + random.nextFloat() * PATCH_SIZE;
            final float y = baseY + random.nextFloat() * PATCH_SIZE;

            final float z = noise.getHeight(x, y);
            // Cull doodads in water or (TODO:) too close
            if (z <= 0.1f) {
                continue;
            }

            final DoodadInstance instance = doodadSet.get(random.nextInt(doodadSet.size)).instantiate(random, x, y, z, characteristics);
            instances.add(instance);
            instance.build(builder, random, characteristics);
        }

        if (builder.getIndices().size == 0) {
            return null;
        }

        final GlBuffer vertices = builder.createVertexBuffer(true);
        final GlBuffer indices = builder.createIndexBuffer(true);
        final GlVertexArrayObject vao = new GlVertexArrayObject(indices, DoodadShaderKt.getDOODAD_ATTRIBUTES(),
                new GlVertexArrayObject.Binding(vertices, 4, 0),
                new GlVertexArrayObject.Binding(vertices, 4, 3));
        final DoodadPatch patch = new DoodadPatch();
        patch.indices = indices;
        patch.vertices = vertices;
        patch.vao = vao;
        patch.count = builder.getIndices().size;
        builder.computeBoundingBox3D(0, 4, patch.boundingBox);
        return patch;
    }

    @Override
    public void render(@NotNull RenderBatch batch, @NotNull Camera camera) {
        final DoodadPatch[] patches = this.patches;
        final Frustum frustum = camera.frustum;

        for (int i = 0; i < patchCount; i++) {
            final DoodadPatch patch = patches[i];
            if (!frustum.boundsInFrustum(patch.boundingBox)) {
                continue;
            }

            final RenderModel model = batch.render();
            model.setVao(patch.vao);
            model.setPrimitiveType(GL20.GL_TRIANGLES);
            model.setCount(patch.count);
            model.setOrder(camera.position.dst2(patch.boundingBox.getCenterX(), patch.boundingBox.getCenterY(), patch.boundingBox.getCenterZ()));
            model.setShader(DoodadShader.INSTANCE);
        }
    }

    public void renderDebug(ImmediateModeRenderer renderer) {
        final Frustum frustum = camera.frustum;

        for (int i = 0; i < patchCount; i++) {
            final BoundingBox box = this.patches[i].boundingBox;
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

    @Override
    public void dispose() {
        final DoodadPatch[] patches = this.patches;
        for (int i = 0; i < patchCount; i++) {
            final DoodadPatch patch = patches[i];
            patch.indices.dispose();
            patch.vertices.dispose();
            patch.vao.dispose();
        }
    }

    private static class DoodadPatch {
        final BoundingBox boundingBox = new BoundingBox();

        GlBuffer indices;
        GlBuffer vertices;
        GlVertexArrayObject vao;
        int count;
    }
}
