package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    private static final Color DEEP_SEA = Color.BLUE.darker().darker().darker();
    private static final Color SEA_SURFACE = Color.BLUE;
    private static final Color SAND = Color.YELLOW;
    private static final Color LUSH_GRASS = Color.GREEN;
    private static final Color DARK_GRASS = Color.GREEN.darker().darker();

    private static Color map(Color lowColor, Color highColor, float low, float high, float value) {
        final float alpha = (value - low) / (high - low);
        final float clampedAlpha = (float) ((Math.tanh(alpha * 2f - 1f) + 1f) * 0.5f);
        return new Color(MathUtils.lerp(lowColor.getRed()/255f, highColor.getRed()/255f, clampedAlpha),
                MathUtils.lerp(lowColor.getGreen()/255f, highColor.getGreen()/255f, clampedAlpha),
                MathUtils.lerp(lowColor.getBlue()/255f, highColor.getBlue()/255f, clampedAlpha), 1f);
    }

    private static Color toTerrainColor(float height) {
        if (height <= 0) {
            return map(DEEP_SEA, SEA_SURFACE, -100f, 0f, height);
        }
        if (height <= 10f) {
            return map(SAND, LUSH_GRASS, 0f, 10f, height);
        }
        return map(LUSH_GRASS, DARK_GRASS, 10f, 100f, height);
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
                /*
                float topHeight = terrain.getHeight(qX, qY-1);
                float leftHeight = terrain.getHeight(qX-1, qY);

                result.setRGB(x, y, new Color(to01(myHeight), to01(myHeight - topHeight), to01(myHeight - leftHeight)).getRGB());
                */

                result.setRGB(x, y, toTerrainColor(myHeight).getRGB());
            }
        }
        return result;
    }

    private static TerrainProvider terrainProviderSimplexRaw() {
        return new TerrainProvider() {

            final OpenSimplexNoise n = new OpenSimplexNoise();
            {
                n.initialize(System.currentTimeMillis());
            }

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
                return n.evaluate(x, y);
            }

            @Override
            public float getColor(float x, float y) {
                return 0;
            }
        };
    }

    private static TerrainProvider terrainProviderSimplex() {
        return new TerrainProvider() {

            final int width = 100;
            final int height = 100;
            final float[][] noise = Noise.islandize(Noise.generateSimplexNoise(width, height,
                    System.currentTimeMillis(), 1f,
                    1f/80f, 2f, 5, 40f, 0.5f), -0.1f);

            @Override
            public float getWidth() {
                return width;
            }

            @Override
            public float getHeight() {
                return height;
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
    }

    private static TerrainProvider terrainProviderDiamond() {
        return new TerrainProvider() {

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
    }

    private static TerrainProvider terrainProviderHydraulic() {
        return new TerrainProvider() {

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
    }

    private static TerrainProvider terrainProviderHydraulicPerlin() {
        return new TerrainProvider() {

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
    }

    private final static int WINDOW_SIZE = 800;
    public static void main(String[] args){

        JFrame frame = new JFrame("Terrain Test");
        Canvas canvas = new Canvas(){

            private TerrainProvider terrainProvider() {
                return terrainProviderSimplex();
            }

            private float scale = 3f;
            private BufferedImage image = toImage(terrainProvider(), scale);

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        image = toImage(terrainProvider(), scale);
                        repaint();
                    }
                });
            }

            @Override
            public void paint(Graphics g) {
                g.drawImage(image, 0, 0, WINDOW_SIZE, WINDOW_SIZE, null);
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
