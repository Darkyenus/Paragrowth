package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.paragrowth.terrain.generator.TerrainProvider;

import static com.darkyen.paragrowth.terrain.TerrainPatch.*;

/**
 * A collection of terrain patches.
 */
public class TerrainPatchwork implements RenderableProvider, Disposable {

    private final Camera camera;

    private final int patchAmountX;
    private final int patchAmountY;
    private final TerrainPatch[] patches;
    private final TerrainPatch seaPatch;

    public TerrainPatchwork(Camera camera, TerrainProvider terrainProvider) {
        this.camera = camera;
        patchAmountX = MathUtils.ceilPositive(terrainProvider.getWidth() / PATCH_WIDTH);
        patchAmountY = MathUtils.ceilPositive(terrainProvider.getHeight() / PATCH_WIDTH);
        this.patches = new TerrainPatch[patchAmountX * patchAmountY];

        int i = 0;
        for (int y = 0; y < patchAmountY; y++) {
            for (int x = 0; x < patchAmountX; x++) {
                patches[i++] = new TerrainPatch(x * PATCH_WIDTH, y * PATCH_HEIGHT, terrainProvider);
            }
        }

        this.seaPatch = new TerrainPatch(-PATCH_WIDTH, -PATCH_HEIGHT, terrainProvider);
        this.seaPatch.transform.translate(PATCH_WIDTH, PATCH_HEIGHT, 0f);
    }

    private float heightAtVertex(int x, int y) {
        final int patchX = Math.floorDiv(x, PATCH_UNIT_SIZE);
        if (patchX < 0 || patchX >= patchAmountX) {
            return 0f;
        }
        final int patchY = Math.floorDiv(y, PATCH_UNIT_SIZE);
        if (patchY < 0 || patchY >= patchAmountY) {
            return 0f;
        }

        final int inPatchX = Math.floorMod(x, PATCH_UNIT_SIZE);
        final int inPatchY = Math.floorMod(y, PATCH_UNIT_SIZE);
        return patches[patchY * patchAmountX + patchX].heightMap[inPatchY * PATCH_SIZE + inPatchX];
    }

    @SuppressWarnings("UnnecessaryLocalVariable") // For easier to debug math
    public final float heightAt(float x, float y) {
        final float gridY = y / Y_STEP;
        final int baseY = (int) Math.floor(gridY);
        final float yFrac = gridY - baseY;

        final boolean odd = (baseY & 1) == 0;
        final float gridX = x / X_STEP;
        // Skew the grid to look like rectangles with alternating diagonals
        final float skewedX = gridX - (odd ? yFrac * X_STAGGER : (1f - yFrac) * X_STAGGER);

        final int baseX = (int) Math.floor(skewedX);
        final float xFrac = skewedX - baseX;


        /*
        We are somewhere in a triangle, with straight on top or on bottom.
        We want to find "global coordinates" of vertices of this triangle and interpolation factors.

        Also, we need interpolation factor for the base and point.
         */

        final int trigBaseYOff;
        final int trigPointXOff;
        final int trigPointYOff;

        if (odd) {
            if (yFrac < 1f - xFrac) {
                trigBaseYOff = 0;
                trigPointXOff = 0;
                trigPointYOff = 1;
            } else {
                trigBaseYOff = 1;
                trigPointXOff = 1;
                trigPointYOff = 0;
            }
        } else {
            if (yFrac < xFrac) {
                trigBaseYOff = 0;
                trigPointXOff = 1;
                trigPointYOff = 1;
            } else {
                trigBaseYOff = 1;
                trigPointXOff = 0;
                trigPointYOff = 0;
            }
        }

        // Convert to barycentric
        // https://en.wikipedia.org/wiki/Barycentric_coordinate_system
        // P1 = base left, P2 = base right, P3 = point
        final int x1 = 0;
        final int y1 = trigBaseYOff;
        final int x2 = 1;
        final int y2 = y1;
        final int x3 = trigPointXOff;
        final int y3 = trigPointYOff;

        final float xp = xFrac;
        final float yp = yFrac;

        final float detT = (y2 - y3)*(x1 - x3) + (x3 - x2)*(y1 - y3);
        final float a1 = ((y2 - y3)*(xp - x3) + (x3 - x2)*(yp - y3)) / detT;
        final float a2 = ((y3 - y1)*(xp - x3) + (x1 - x3)*(yp - y3)) / detT;
        final float a3 = 1f - a1 - a2;

        final float hBaseLeft = heightAtVertex(baseX, baseY + trigBaseYOff);
        final float hBaseRight = heightAtVertex(baseX + 1, baseY + trigBaseYOff);
        final float hPoint = heightAtVertex(baseX + trigPointXOff, baseY + trigPointYOff);

        return hBaseLeft * a1 + hBaseRight * a2 + hPoint * a3;
    }

    public static int worldXToPatch(float x) {
        return (int) Math.floor(x / PATCH_WIDTH);
    }

    public static int worldYToPatch(float y) {
        return (int) Math.floor(y / PATCH_HEIGHT);
    }

    private final BoundingBox getRenderables_bounds = new BoundingBox();

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final Frustum frustum = camera.frustum;
        final BoundingBox bounds = this.getRenderables_bounds.set(frustum.planePoints);

        final int lowX = (int) Math.floor((bounds.min.x - TerrainPatch.X_STEP) / PATCH_WIDTH);
        final int highX = (int) Math.ceil((bounds.max.x + TerrainPatch.X_STEP) / PATCH_WIDTH);

        final int lowY = (int) Math.floor((bounds.min.y - TerrainPatch.Y_STEP) / PATCH_HEIGHT);
        final int highY = (int) Math.ceil((bounds.max.y + TerrainPatch.Y_STEP) / PATCH_HEIGHT);

        for (int y = lowY; y <= highY; y++) {
            for (int x = lowX; x <= highX; x++) {

                if (x >= 0 && y >= 0 && x < patchAmountX && y < patchAmountY) {
                    final TerrainPatch patch = patches[patchAmountX * y + x];
                    if (patch.inFrustum(frustum, 0f, 0f)) {
                        final Renderable renderable = pool.obtain();
                        patch.fillRenderable(renderable);
                        renderables.add(renderable);
                    }
                } else {
                    final TerrainPatch patch = seaPatch;
                    final float xOff = x * PATCH_WIDTH;
                    final float yOff = y * PATCH_HEIGHT;

                    if (patch.inFrustum(frustum, xOff, yOff)) {
                        final Renderable renderable = pool.obtain();
                        patch.fillRenderable(renderable);
                        renderable.worldTransform.translate(xOff, yOff, 0f);
                        renderables.add(renderable);
                    }
                }
            }
        }
    }

    public void dispose() {
        for (TerrainPatch patch : patches) {
            patch.dispose();
        }
        seaPatch.dispose();
    }
}
