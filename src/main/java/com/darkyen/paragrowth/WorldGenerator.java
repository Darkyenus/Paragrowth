package com.darkyen.paragrowth;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;
import com.darkyen.paragrowth.terrain.generator.Noise;
import com.darkyen.paragrowth.terrain.generator.TerrainProvider;

/**
 * @author Darkyen
 */
public class WorldGenerator implements TerrainProvider {

    private final WorldCharacteristics characteristics;
    private final float[][] noise;

    public WorldGenerator(WorldCharacteristics characteristics) {
        this.characteristics = characteristics;

        final int worldSize = (int)(MathUtils.clamp((float)Math.sqrt(characteristics.size), 1f, 30f) * 100f);

        noise = Noise.max(Noise.islandize(Noise.generateSimplexNoise(worldSize, worldSize,
                characteristics.seed, 1f,
                1f/200f, 2f, 5, 40f, 0.5f), -1f), -1f);
    }

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

    private static final RandomXS128 setupInitialPosition_RNG = new RandomXS128();
    public void setupInitialPosition(Vector3 pos) {
        final RandomXS128 RNG = setupInitialPosition_RNG;
        RNG.setSeed(characteristics.seed);

        float x = 0f;
        float y = 0f;
        float z = 0f;
        for (int i = 0; i < 100; i++) {
            x = RNG.nextFloat() * getWidth();
            y = RNG.nextFloat() * getHeight();
            z = getHeight(x, y);
            if (z > 0.1f) {
                break;
            }
        }

        pos.set(x,y,z);
    }

    @Override
    public float getColor(float x, float y) {
        final float height = Noise.getHeight(noise, x, y);
        if (height <= 0f) {
            return Color.BLUE.toFloatBits();
        } else if (height < 1f) {
            return Color.YELLOW.toFloatBits();
        }
        //noinspection NumericOverflow
        return NumberUtils.intToFloatColor((255 << 24) | (MathUtils.random.nextInt(256) << 16) | (MathUtils.random.nextInt(256) << 8) | MathUtils.random.nextInt(256));
    }
}
