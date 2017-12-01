package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 */
public class TerrainTest {

    private static float getBrightness(TerrainProvider terrain, float x, float y){
        float myHeight = terrain.getHeight(x,y);
        float topHeight = terrain.getHeight(x, y-1);
        float leftHeight = terrain.getHeight(x-1, y);
        final float MAX_HEIGHT_DELTA = 1f;
        return MathUtils.clamp(((topHeight + leftHeight) * 0.5f - myHeight + 0.5f)/ MAX_HEIGHT_DELTA, 0f, 1f) * 0.8f + 0.1f;
    }

    private static float to01(float f) {
        return (float) ((Math.tanh(f) + 1f) / 2f);
    }

    private static BufferedImage toImage(TerrainProvider terrain, float scale){
        final BufferedImage result = new BufferedImage(MathUtils.ceil(terrain.getWidth() * scale), MathUtils.ceil(terrain.getHeight() * scale), BufferedImage.TYPE_4BYTE_ABGR);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                final float qX = x / scale;
                final float qY = y / scale;
                float myHeight = terrain.getHeight(qX,qY);
                float topHeight = terrain.getHeight(qX, qY-1);
                float leftHeight = terrain.getHeight(qX-1, qY);

                result.setRGB(x, y, new Color(to01(myHeight), to01(myHeight - topHeight), to01(myHeight - leftHeight)).getRGB());
            }
        }
        return result;
    }

    private final static int WINDOW_SIZE = 800;
    public static void main(String[] args){
        final TerrainProvider terrainProviderSimplex = new TerrainProvider() {

            final OpenSimplexNoise n = new OpenSimplexNoise(System.currentTimeMillis());

            @Override
            public float getWidth() {
                return 100;
            }

            @Override
            public float getHeight() {
                return 100;
            }

            @Override
            public float getHeight(float x, float y) {
                return (float)n.evaluate((double)x, (double)y);
            }

            @Override
            public float getColor(float x, float y) {
                return 0;
            }
        };

        final TerrainProvider terrainProviderDiamond = new TerrainProvider() {

            final float[][] noise = DiamondSquare.generate(8, System.currentTimeMillis(), 1f, 10f);

            @Override
            public float getWidth() {
                return noise.length;
            }

            @Override
            public float getHeight() {
                return noise.length;
            }

            @Override
            public float getHeight(float x, float y) {
                return Noise.getHeight(noise, x, y);
            }

            @Override
            public float getColor(float x, float y) {
                return 0;
            }
        };

        final TerrainProvider terrainProviderHydraulic = new TerrainProvider() {

            final float[][] noise = Noise.generateHydraulicNoise(100, System.currentTimeMillis(), 500, 0.005f);

            @Override
            public float getWidth() {
                return noise.length;
            }

            @Override
            public float getHeight() {
                return noise.length;
            }

            @Override
            public float getHeight(float x, float y) {
                return Noise.getHeight(noise, x, y);
            }

            @Override
            public float getColor(float x, float y) {
                return 0;
            }
        };

        final TerrainProvider terrainProviderHydraulicPerlin = new TerrainProvider() {

            final float[][] noise = Noise.islandize(Noise.generatePerlinNoise(Noise.generateHydraulicNoise(100, 55, 500, 0.005f), 3, 0.5f), 1f, 0f);

            @Override
            public float getWidth() {
                return noise.length;
            }

            @Override
            public float getHeight() {
                return noise.length;
            }

            @Override
            public float getHeight(float x, float y) {
                return Noise.getHeight(noise, x, y);
            }

            @Override
            public float getColor(float x, float y) {
                return 0;
            }
        };

        BufferedImage doubleBuffer = toImage(terrainProviderHydraulicPerlin, 20f);

        JFrame frame = new JFrame("Terrain Test");
        Canvas canvas = new Canvas(){

            @Override
            public void paint(Graphics g) {
                g.drawImage(doubleBuffer, 0, 0, WINDOW_SIZE, WINDOW_SIZE, null);
            }
        };

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_SIZE,WINDOW_SIZE);
        canvas.setSize(WINDOW_SIZE, WINDOW_SIZE);
        frame.setResizable(false);
        frame.add(canvas);

        frame.setVisible(true);
    }
}
