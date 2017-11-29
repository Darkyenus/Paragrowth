package com.darkyen.paragrowth.world.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;

/**
 * Piece of terrain made from equilateral triangles, each with own flat color.
 *
 * Fits into single renderable.
 */
class TerrainPatch {

    /*
    triangles = (size-1)^2*2
    indices = (size-1)^2*2*3
    max indices = 2^16
    => size <= 105.512, patch size must be odd
    => size = 105
     */

    public static final int PATCH_SIZE = 105;

    private static final float X_STEP = 1f;
    private static final float X_STAGGER = 0.5f;
    private static final float Y_STEP = (float)(Math.sqrt(3.0) / 2.0);

    public static final float PATCH_WIDTH = (PATCH_SIZE - 1) * X_STEP;
    public static final float PATCH_HEIGHT = (PATCH_SIZE - 1) * Y_STEP;

    private static final int TRIANGLE_COUNT = (PATCH_SIZE - 1) * (PATCH_SIZE - 1) * 2;
    private static final int INDEX_COUNT = TRIANGLE_COUNT * 3;
    private static final int VERTEX_COUNT = PATCH_SIZE * PATCH_SIZE + (PATCH_SIZE - 1) * (PATCH_SIZE - 1);
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

    private final Matrix4 transform = new Matrix4();
    final BoundingBox boundingBox = new BoundingBox();
    private final Mesh mesh;
    private final Material terrainMaterial;

    final float[] heightMap = new float[PATCH_SIZE * PATCH_SIZE];

    public TerrainPatch(float xOffset, float yOffset, TerrainGenerator generator) {
        //this.transform.translate(xOffset, yOffset, 0f);
        this.boundingBox.min.set(xOffset, yOffset, 0f);
        this.boundingBox.max.set(this.boundingBox.min).add(PATCH_WIDTH, PATCH_HEIGHT, 100f);

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
                for (int x = 0; x < PATCH_SIZE-1; x++) {

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
                    vertices[v++] = Color.GREEN.toFloatBits();//generator.getColor(xPos, yPos + Y_HALF_STEP);
                }

                yPos += Y_STEP;
                xPos = xOffset + X_STAGGER;
                height = heightMap[h++] = generator.getHeight(xPos, yPos);

                // Top of odd row
                for (int x = 0; x < PATCH_SIZE-1; x++) {
                    // Top of dark red
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = Color.MAROON.toFloatBits();//generator.getColor(xPos, yPos + Y_HALF_STEP);

                    xPos += X_STEP;
                    height = heightMap[h++] = generator.getHeight(xPos, yPos);

                    // Top right of dark green
                    vertices[v++] = xPos;
                    vertices[v++] = yPos;
                    vertices[v++] = height;
                    vertices[v++] = Color.FOREST.toFloatBits();//generator.getColor(xPos - X_HALF_STEP, yPos + Y_HALF_STEP);
                }

                yPos += Y_STEP;
            }

            // Do one more bottom row, without colors
            final float NO_COLOR = Color.MAGENTA.toFloatBits();
            float xPos = xOffset;
            float height = heightMap[h++] = generator.getHeight(xPos, yPos);
            for (int x = 0; x < PATCH_SIZE-1; x++) {
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
            for (int y = 0; y + 1 < PATCH_SIZE; y += 2) {
                // First Red
                indices[i++] = (short) (y * ROW_AMOUNT);
                indices[i++] = (short) (y * ROW_AMOUNT + 1);
                indices[i++] = (short) (y * ROW_AMOUNT + ROW_AMOUNT);

                // Other Red
                for (int x = 1; x < PATCH_SIZE-1; x++) {
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT - 1);
                }

                // All Green
                for (int x = 0; x < PATCH_SIZE-1; x++) {
                    indices[i++] = (short) (x*2 + 1 + y * ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT);
                }

                // All Dark Red
                for (int x = 0; x < PATCH_SIZE-1; x++) {
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1);
                    indices[i++] = (short) (x*2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT);
                }

                // All Dark Green
                for (int x = 0; x < PATCH_SIZE-1; x++) {
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
        renderable.userData = this;

        renderable.shader = TerrainShader.get(renderable);
    }
}
