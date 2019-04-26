package com.darkyen.paragrowth;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.darkyen.paragrowth.terrain.generator.Noise;
import com.darkyen.paragrowth.terrain.generator.OpenSimplexNoise;
import com.darkyen.paragrowth.util.ColorKt;

/**
 * Stateless thread-safe characterization of a world, specific realization of {@link WorldCharacteristics}.
 */
public class WorldSpecifics {

    public final WorldCharacteristics characteristics;

    public final float offsetX, offsetY;
    public final Noise noise;

    private final OpenSimplexNoise colorNoise = new OpenSimplexNoise();
    private static final float COLOR_NOISE_SCALE_BEACH = 0.2f;
    private static final float COLOR_NOISE_SCALE_TERRAIN = 0.02f;

    public final float waterColor;

    private final float[] beachColor;
    private final float[] terrainColor;

    public WorldSpecifics(WorldCharacteristics characteristics, float centerX, float centerY, boolean centerForSpawn) {
        this.characteristics = characteristics;
        colorNoise.initialize(characteristics.seed);

        {
            final RandomXS128 random = new RandomXS128(characteristics.seed);
            waterColor = characteristics.getRandomFudgedColor(random, WorldColors.WATER);
            beachColor = new float[]{
                    characteristics.getRandomFudgedColor(random, WorldColors.BEACH),
                    characteristics.getRandomFudgedColor(random, WorldColors.BEACH)
            };

            terrainColor = new float[]{
                    characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN),
                    characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN),
                    characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN),
                    characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN)
            };
        }

        final int worldSize = (int)(MathUtils.clamp((float)Math.sqrt(characteristics.size), 1f, 30f) * 100f);

        noise = Noise.generateSimplexNoise(worldSize, worldSize,
                characteristics.seed, 1f,
                1f/200f, 2f, 5, 40f, 0.5f, -1f);
        noise.islandize(1f, -1f);
        noise.max(-1f);

        if (centerForSpawn) {
            final Vector3 v = new Vector3();
            final RandomXS128 RNG = setupInitialPosition_RNG;
            RNG.setSeed(characteristics.seed);
            noise.findRandomPositionInHeightRange(v, RNG, 0.1f, Float.POSITIVE_INFINITY);
            this.offsetX = centerX - v.x;
            this.offsetY = centerY - v.y;
        } else {
            this.offsetX = centerX - worldSize * 0.5f;
            this.offsetY = centerY - worldSize * 0.5f;
        }
    }

    public int sizeX() {
        return noise.sizeX;
    }

    public int sizeY() {
        return noise.sizeY;
    }

    public float getHeight(float x, float y) {
        return noise.getHeight(x - offsetX, y - offsetY);
    }

    public void getNormal(Vector3 to, float x, float y) {
        noise.getNormal(to, x - offsetX, y - offsetY);
    }

    private static final RandomXS128 setupInitialPosition_RNG = new RandomXS128();
    public boolean findInitialPosition(Vector3 pos) {
        final RandomXS128 RNG = setupInitialPosition_RNG;
        RNG.setSeed(characteristics.seed);

        boolean success = noise.findRandomPositionInHeightRange(pos, RNG, 0.1f, Float.POSITIVE_INFINITY);
        pos.add(offsetX, offsetY, 0f);
        return success;
    }

    public WorldColorQuery queryColors() {
        return new WorldColorQuery();
    }

    /**
     * Stateful, non-thread safe query for world's colors.
     */
    public class WorldColorQuery {
        private final RandomXS128 randomForColors;

        private WorldColorQuery() {
            this.randomForColors = new RandomXS128(characteristics.seed);
        }

        public float getColor(float x, float y) {
            final float height = noise.getHeight(x - offsetX, y - offsetY);
            if (height <= 0f) {
                return waterColor;
            }
            final float[] colorBase;
            final float colorNoiseScale;
            if (height < 1f) {
                colorBase = beachColor;
                colorNoiseScale = COLOR_NOISE_SCALE_BEACH;
            } else {
                colorBase = terrainColor;
                colorNoiseScale = COLOR_NOISE_SCALE_TERRAIN;
            }

            final float alpha = colorNoise.evaluatePositive(x * colorNoiseScale, y * colorNoiseScale, height * colorNoiseScale);
            return ColorKt.lerpHSBAndFudge(colorBase, alpha, randomForColors, characteristics.coherence, 0.6f);
        }
    }


}
