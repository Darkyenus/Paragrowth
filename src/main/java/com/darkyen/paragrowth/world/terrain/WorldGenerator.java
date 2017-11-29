package com.darkyen.paragrowth.world.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.NumberUtils;
import com.darkyen.paragrowth.world.doodad.DoodadLibrary;
import com.darkyen.paragrowth.world.doodad.DoodadWorld;

import java.util.Random;

/**
 * @author Darkyen
 */
public class WorldGenerator {

    public static class World {
        public final DoodadWorld doodadWorld;
        public final TerrainPatchwork terrain;

        public World(DoodadWorld doodadWorld, TerrainPatchwork terrain) {
            this.doodadWorld = doodadWorld;
            this.terrain = terrain;
        }
    }

    /**
     * @param camera to use in DoodadWorld and TerrainPatchwork
     * @param size in patches in one dimension
     */
    public static World generate(Camera camera, int size, long seed, DoodadLibrary doodadLibrary){
        final Random random = new RandomXS128(seed);
        final int patchSize = 256;
        final int patchTextureSize = 2048;
        final int patchTextureRatio = patchTextureSize/patchSize;

        final float[][] terrainNoise = HydraulicNoise.generate(8, seed, 1000, 0.004f);//DiamondSquare.generate(8, seed, 0f, 400f);//Noise.generatePerlinNoise(patchSize * size, patchSize * size, 13, seed);
        final int terrainNoiseSize = patchSize * size - size;
        final float terrainNoiseScale = 50f;

        final DoodadWorld doodadWorld = new DoodadWorld(camera);
        final Pixmap[][] terrainTextures = new Pixmap[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Pixmap pixmap = new Pixmap(patchTextureSize, patchTextureSize, Pixmap.Format.RGB888);
                pixmap.setColor(0.1f,0.95f, 0.1f, 1.0f);
                pixmap.fill();
                for (int i = 0; i < 10000; i++) {
                    int featX = random.nextInt(patchTextureSize);
                    int featY = random.nextInt(patchTextureSize);
                    pixmap.setColor(random.nextInt() & 0xFFFFFF);
                    pixmap.drawCircle(featX,featY,8);
                    Matrix4 doodadPosition = doodadWorld.addDoodad(doodadLibrary.DOODADS[random.nextInt(doodadLibrary.DOODADS.length)]);
                    float doodadX = x * size + featX/patchTextureRatio, doodadY = y * size + featY/patchTextureRatio;
                    doodadPosition.setToTranslation(doodadX, doodadY, Noise.getHeight(terrainNoise, terrainNoiseSize, doodadX, doodadY) * terrainNoiseScale);
                }
                terrainTextures[x][y] = pixmap;
            }
        }

        final TerrainPatchwork terrainPatchwork = new TerrainPatchwork(size, camera, new TerrainGenerator() {
            @Override
            public float getHeight(float x, float y) {
                return Noise.getHeight(terrainNoise, terrainNoiseSize, x, y) * terrainNoiseScale;
            }

            @Override
            public float getColor(float x, float y) {
                return NumberUtils.intToFloatColor(Color.rgba8888(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f));
            }
        });

        return new World(doodadWorld, terrainPatchwork);
    }
}
