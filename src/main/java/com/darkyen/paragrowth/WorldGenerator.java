package com.darkyen.paragrowth;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.darkyen.paragrowth.doodad.DoodadWorld;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;
import com.darkyen.paragrowth.terrain.generator.Noise;
import com.darkyen.paragrowth.terrain.generator.OpenSimplexNoise;
import com.darkyen.paragrowth.terrain.generator.TerrainProvider;
import com.darkyen.paragrowth.util.ColorKt;

/**
 * @author Darkyen
 */
public class WorldGenerator implements TerrainProvider {

    private final WorldCharacteristics characteristics;
    public final DoodadWorld doodadWorld;
    public final TerrainPatchwork terrainPatchwork;
    private final float[][] noise;

    private final OpenSimplexNoise colorNoise = new OpenSimplexNoise();
    private static final float COLOR_NOISE_SCALE_BEACH = 0.2f;
    private static final float COLOR_NOISE_SCALE_TERRAIN = 0.02f;

    private final float waterColor;
    private final RandomXS128 randomForColors;
    private final float[] beachColor;
    private final float[] terrainColor;

    public WorldGenerator(Camera camera, WorldCharacteristics characteristics) {
        this.characteristics = characteristics;
        colorNoise.initialize(characteristics.seed);

        {
            final RandomXS128 random = new RandomXS128(characteristics.seed);
            this.randomForColors = random;
            waterColor = characteristics.getRandomFudgedColor(random, WorldColors.WATER); //ColorKt.fudge(pick(WorldColors.WATER, random, characteristics.mood).toFloatBits(), random, characteristics.coherence, 1f);
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

        noise = Noise.max(Noise.islandize(Noise.generateSimplexNoise(worldSize, worldSize,
                characteristics.seed, 1f,
                1f/200f, 2f, 5, 40f, 0.5f), -1f), -1f);

        doodadWorld = new DoodadWorld(camera, characteristics.seed, noise, characteristics);
        terrainPatchwork = new TerrainPatchwork(camera, this);
    }

    @Override
    public float getSizeX() {
        return noise.length;
    }

    @Override
    public float getSizeY() {
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
            x = RNG.nextFloat() * getSizeX();
            y = RNG.nextFloat() * getSizeY();
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
            return waterColor;
        } else if (height < 1f) {
            return ColorKt.fudge(ColorKt.lerpHSB(beachColor, colorNoise.evaluatePositive(x * COLOR_NOISE_SCALE_BEACH, y * COLOR_NOISE_SCALE_BEACH, height * COLOR_NOISE_SCALE_BEACH)), randomForColors, characteristics.coherence, 0.6f);
        } else {
            return ColorKt.fudge(ColorKt.lerpHSB(terrainColor, colorNoise.evaluatePositive(x * COLOR_NOISE_SCALE_TERRAIN, y * COLOR_NOISE_SCALE_TERRAIN, height * COLOR_NOISE_SCALE_TERRAIN)), randomForColors, characteristics.coherence, 0.6f);
        }
    }
}
