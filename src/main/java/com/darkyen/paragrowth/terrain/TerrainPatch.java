package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.darkyen.paragrowth.terrain.generator.TerrainProvider;

/**
 * Piece of terrain made from equilateral triangles, each with own flat color.
 *
 * Fits into single renderable.
 */
class TerrainPatch implements Disposable {

    /*
    triangles = (size-1)^2*2
    indices = (size-1)^2*2*3
    max indices = 2^16
    => size <= 105.512, patch size must be odd
    => size = 105
     */

    static final int PATCH_SIZE = 105;
    static final int PATCH_UNIT_SIZE = PATCH_SIZE - 1;

    static final float X_STEP = 1f;
    static final float X_STAGGER = 0.5f;
    static final float Y_STEP = (float)(Math.sqrt(3.0) / 2.0);

    static final float PATCH_WIDTH = PATCH_UNIT_SIZE * X_STEP;
    static final float PATCH_HEIGHT = PATCH_UNIT_SIZE * Y_STEP;

    private static final float MAX_MAP_HEIGHT = 200f;
    private static final float PATCH_RADIUS_X = (PATCH_WIDTH + X_STEP) * 0.5f;
    private static final float PATCH_RADIUS_Y = (PATCH_HEIGHT + Y_STEP) * 0.5f;
    private static final float PATCH_RADIUS_Z = (MAX_MAP_HEIGHT) * 0.5f;
    private static final float PATCH_RADIUS = (float) Math.sqrt(PATCH_RADIUS_X*PATCH_RADIUS_X + PATCH_RADIUS_Y*PATCH_RADIUS_Y + PATCH_RADIUS_Z*PATCH_RADIUS_Z);

    private static final int TRIANGLE_COUNT = PATCH_UNIT_SIZE * PATCH_UNIT_SIZE * 2;
    private static final int INDEX_COUNT = TRIANGLE_COUNT * 3;
    private static final int VERTEX_COUNT = PATCH_SIZE * PATCH_SIZE + PATCH_UNIT_SIZE * PATCH_UNIT_SIZE;
    private static final int VERTEX_SIZE_FLOATS = 3+1;

    /*
    Arrangement:
    0 \/\/\/\/\/\/\ odd
    1 /\/\/\/\/\/\/ even
    2 \/\/\/\/\/\/\
    3 /\/\/\/\/\/\/
    4

    X step: 1
    X stagger: 0.5
    Y step: sqrt(3)/2
     */

    private final Vector3 center = new Vector3();
    final Matrix4 transform = new Matrix4();
    private final Mesh mesh;
    private final Material terrainMaterial;

    final float[] heightMap = new float[PATCH_SIZE * PATCH_SIZE];

    private static final Vector3 inFrustum_center = new Vector3();
    boolean inFrustum(Frustum frustum, float offX, float offY) {
        final Vector3 center = inFrustum_center.set(this.center).mul(transform);
        return frustum.boundsInFrustum(center.x + offX, center.y + offY, center.z, PATCH_RADIUS_X, PATCH_RADIUS_Y, PATCH_RADIUS_Z);
    }

    TerrainPatch(float xOffset, float yOffset, TerrainProvider generator) {
        this.center.set(xOffset + PATCH_WIDTH*0.5f, yOffset + PATCH_HEIGHT*0.5f, MAX_MAP_HEIGHT*0.5f);
        final float[] vertices = new float[VERTEX_COUNT * VERTEX_SIZE_FLOATS];
        mesh = new Mesh(true, true, VERTEX_COUNT, INDEX_COUNT, new VertexAttributes(
                VertexAttribute.Position(),//3
                VertexAttribute.ColorPacked()//1
        ));

        //Generate vertices
        {
            final float X_HALF_STEP = X_STEP * 0.5f;
            final float Y_HALF_STEP = Y_STEP * 0.5f;

            int h = 0;
            int v = 0;
            float yPos = yOffset;
            // Stepping through hourglass middles
            for (int y = 1; y < PATCH_SIZE; y += 2) {
                float xPos = xOffset;
                // Do a line of top X that makes the first row

                // Top of even row
                float height = heightMap[h++] = generator.getHeight(xPos, yPos);
                for (int x = 0; x < PATCH_UNIT_SIZE; x++) {

                    // Top left of red
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = generator.getColor(xPos + X_HALF_STEP, yPos + Y_HALF_STEP);

                    xPos += X_STEP;
                    height = heightMap[h++] = generator.getHeight(xPos, yPos);

                    // Top of green
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = generator.getColor(xPos, yPos + Y_HALF_STEP);
                }

                yPos += Y_STEP;
                xPos = xOffset + X_STAGGER;
                height = heightMap[h++] = generator.getHeight(xPos, yPos);

                // Top of odd row
                for (int x = 0; x < PATCH_UNIT_SIZE; x++) {
                    // Top of dark red
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = generator.getColor(xPos, yPos + Y_HALF_STEP);

                    xPos += X_STEP;
                    height = heightMap[h++] = generator.getHeight(xPos, yPos);

                    // Top right of dark green
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = generator.getColor(xPos - X_HALF_STEP, yPos + Y_HALF_STEP);
                }

                yPos += Y_STEP;
            }

            // Do one more bottom row, without colors
            final float NO_COLOR = Color.MAGENTA.toFloatBits();
            float xPos = xOffset;
            float height = heightMap[h++] = generator.getHeight(xPos, yPos);
            for (int x = 0; x < PATCH_UNIT_SIZE; x++) {
                // Top left of red
                vertices[v++] = xPos;
                vertices[v++] = yPos;
                vertices[v++] = height;
                vertices[v++] = NO_COLOR;

                xPos += X_STEP;
                height = heightMap[h++] = generator.getHeight(xPos, yPos);

                // Top of green
                vertices[v++] = xPos;
                vertices[v++] = yPos;
                vertices[v++] = height;
                vertices[v++] = NO_COLOR;
            }

            mesh.setVertices(vertices);
        }

        //Generate indices
        {
            final int ROW_AMOUNT = PATCH_SIZE + PATCH_SIZE - 2;
            final short[] indices = new short[INDEX_COUNT];
            int i = 0;

            // Do all of the double-strips
            for (int y = 0; y < PATCH_UNIT_SIZE; y += 2) {
                // First Red
                indices[i++] = (short) (y * ROW_AMOUNT);
                indices[i++] = (short) (y * ROW_AMOUNT + 1);
                indices[i++] = (short) (y * ROW_AMOUNT + ROW_AMOUNT);

                // Other Red
                for (int x = 1; x < PATCH_UNIT_SIZE; x++) {
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT - 1);
                }

                // All Green
                for (int x = 0; x < PATCH_UNIT_SIZE; x++) {
                    indices[i++] = (short) (x*2 + 1 + y * ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT);
                }

                // All Dark Red
                for (int x = 0; x < PATCH_UNIT_SIZE; x++) {
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT);
                }

                // All Dark Green
                for (int x = 0; x < PATCH_UNIT_SIZE; x++) {
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT);
                }
            }

            mesh.setIndices(indices);
        }

        terrainMaterial = new Material();
    }

    void fillRenderable(Renderable renderable) {
        renderable.meshPart.set(null, mesh, 0, INDEX_COUNT, GL20.GL_TRIANGLES);
        renderable.material = terrainMaterial;
        renderable.worldTransform.set(transform);
        renderable.meshPart.center.set(center);
        renderable.meshPart.radius = PATCH_RADIUS;
        renderable.userData = this;

        renderable.shader = TerrainShader.get(renderable);
    }

    public void dispose() {
        mesh.dispose();
    }
}
