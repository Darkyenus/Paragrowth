package com.darkyen.paragrowth;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
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

        final int worldSize = (int) MathUtils.clamp(Math.pow(Math.log(characteristics.size + 1f), 3f) * 10f + 10f, 10f, 500f);

        noise = Noise.islandize(Noise.generatePerlinNoise(Noise.generateHydraulicNoise(worldSize, characteristics.seed, 500, 0.005f), 3, 0.5f), 250f, -1f);
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
        final float height = Noise.getHeight(noise, x, y);
        if (height < 0f) {
            return 0f;
        }
        return height;
    }

    @Override
    public float getColor(float x, float y) {
        if (true) {
            return NumberUtils.intToFloatColor((255 << 24) | (MathUtils.random.nextInt(256) << 16) | (MathUtils.random.nextInt(256) << 8) | MathUtils.random.nextInt(256));
        }

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
