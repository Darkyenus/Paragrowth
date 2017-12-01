package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.paragrowth.game.ParagrowthState;
import com.darkyen.paragrowth.terrain.generator.TerrainProvider;

import static com.darkyen.paragrowth.terrain.TerrainPatch.*;

/**
 * A collection of terrain patches.
 */
public class TerrainPatchwork implements RenderableProvider {

    private final Camera camera;

    private final int patchAmountX;
    private final int patchAmountY;
    private final TerrainPatch[] patches;

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
        return patches[patchY * patchAmountY + patchX].heightMap[inPatchY * PATCH_SIZE + inPatchX];
    }

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

        ParagrowthState.extraStats.append("gridX: ").append(skewedX).append("\n");
        ParagrowthState.extraStats.append("gridY: ").append(gridY).append("\n");
        ParagrowthState.extraStats.append("baseX: ").append(baseX).append("\n");
        ParagrowthState.extraStats.append("baseY: ").append(baseY).append("\n");


        /*
        We are somewhere in a triangle, with straight on top or on bottom.
        We want to find "global coordinates" of vertices of this triangle and interpolation factors.

        Also, we need interpolation factor for the base and point.
         */

        final int trigBaseY;
        final int trigPointX;
        final int trigPointY;

        if (odd) {
            if (yFrac < 1f - xFrac) {
                trigBaseY = baseY;

                trigPointX = baseX;
                trigPointY = baseY + 1;
                ParagrowthState.extraStats.append("Odd low\n");
            } else {
                trigBaseY = baseY + 1;

                trigPointX = baseX + 1;
                trigPointY = baseY;
                ParagrowthState.extraStats.append("Odd high\n");
            }
        } else {
            if (yFrac < xFrac) {
                trigBaseY = baseY;

                trigPointX = baseX + 1;
                trigPointY = baseY + 1;
                ParagrowthState.extraStats.append("Even low\n");
            } else {
                trigBaseY = baseY + 1;

                trigPointX = baseX;
                trigPointY = baseY;
                ParagrowthState.extraStats.append("Even high\n");
            }

        }

        // Convert to barycentric
        // https://en.wikipedia.org/wiki/Barycentric_coordinate_system
        // P1 = base left, P2 = base right, P3 = point
        final int x1 = 0;
        final int y1 = trigBaseY - baseY;
        final int x2 = 1;
        final int y2 = y1;
        final int x3 = trigPointX - baseX;
        final int y3 = trigPointY - baseY;

        final float xp = xFrac;
        final float yp = yFrac;

        final float detT = (y2 - y3)*(x1 - x3) + (x3 - x2)*(y1 - y3);
        final float a1 = ((y2 - y3)*(xp - x3) + (x3 - x2)*(yp - y3)) / detT;
        final float a2 = ((y3 - y1)*(xp - x3) + (x1 - x3)*(yp - y3)) / detT;
        final float a3 = 1f - a1 - a2;

        final float hBaseLeft = heightAtVertex(baseX, trigBaseY);
        final float hBaseRight = heightAtVertex(baseX + 1, trigBaseY);
        final float hPoint = heightAtVertex(trigPointX, trigPointY);

        ParagrowthState.extraStats.append("L: ").append(a1).append('\n');
        ParagrowthState.extraStats.append("R: ").append(a2).append('\n');
        ParagrowthState.extraStats.append("P: ").append(a3).append('\n');

        ParagrowthState.extraStats.append("hL: ").append(hBaseLeft).append('\n');
        ParagrowthState.extraStats.append("hR: ").append(hBaseRight).append('\n');
        ParagrowthState.extraStats.append("hP: ").append(hPoint).append('\n');

        return hBaseLeft * a1 + hBaseRight * a2 + hPoint * a3;
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
