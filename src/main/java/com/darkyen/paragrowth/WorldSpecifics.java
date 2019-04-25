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

    public WorldSpecifics(WorldCharacteristics characteristics, float offsetX, float offsetY) {
        this.characteristics = characteristics;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
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
    public void setupInitialPosition(Vector3 pos) {
        final RandomXS128 RNG = setupInitialPosition_RNG;
        RNG.setSeed(characteristics.seed);

        float x = 0f;
        float y = 0f;
        float z = 0f;
        for (int i = 0; i < 100; i++) {
            x = RNG.nextFloat() * noise.sizeX;
            y = RNG.nextFloat() * noise.sizeY;
            z = noise.getHeight(x, y);
            if (z > 0.1f) {
                break;
            }
        }

        pos.set(x + offsetX, y + offsetY, z);
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
            } else if (height < 1f) {
                return ColorKt.fudge(ColorKt.lerpHSB(beachColor, colorNoise.evaluatePositive(x * COLOR_NOISE_SCALE_BEACH, y * COLOR_NOISE_SCALE_BEACH, height * COLOR_NOISE_SCALE_BEACH)), randomForColors, characteristics.coherence, 0.6f);
            } else {
                return ColorKt.fudge(ColorKt.lerpHSB(terrainColor, colorNoise.evaluatePositive(x * COLOR_NOISE_SCALE_TERRAIN, y * COLOR_NOISE_SCALE_TERRAIN, height * COLOR_NOISE_SCALE_TERRAIN)), randomForColors, characteristics.coherence, 0.6f);
            }
        }
    }


}
