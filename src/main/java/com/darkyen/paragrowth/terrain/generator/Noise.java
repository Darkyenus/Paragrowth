/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;

import java.util.Arrays;
import java.util.Random;

import static com.badlogic.gdx.math.MathUtils.lerp;

/**
 * Copied from https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/g3d/voxel/PerlinNoiseGenerator.java
 * and altered.
 * Adapted from <a href="http://devmag.org.za/2009/04/25/perlin-noise/">http://devmag.org.za/2009/04/25/perlin-noise/</a>
 */
public final class Noise {

    private final float[] values;
    private final float defaultValue;
    public final int sizeX, sizeY;

    private Noise(float[] values, float defaultValue, int sizeX, int sizeY) {
        assert values.length == sizeX * sizeY;
        this.values = values;
        this.defaultValue = defaultValue;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    public float getHeight(float x, float y) {
        int lowX = (int) x;
        int highX = lowX + 1;
        final int sizeX = this.sizeX;
        if (lowX < 0 || highX >= sizeX) {
            return defaultValue;
        }
        float alphaX = x - lowX;

        int lowY = (int) y;
        int highY = lowY + 1;
        if (lowY < 0 || highY >= sizeY) {
            return defaultValue;
        }
        float alphaY = y - lowY;

        final int lowYI = lowY * sizeX;
        final int highYI = lowYI + sizeX;

        final float[] values = this.values;
        final float bottomX = lerp(values[lowX + lowYI], values[highX + lowYI], alphaX);
        final float topX = lerp(values[lowX + highYI], values[highX + highYI], alphaX);
        return lerp(bottomX, topX, alphaY);
    }

    public void getNormal(Vector3 to, float x, float y) {
        float here = getHeight(x, y);
        float up = getHeight(x + 1f, y);
        float right = getHeight(x, y + 1f);

        to.set(1f, 0f, up - here).crs(0f, 1f, right - here).nor();
    }

    public boolean findRandomPositionInHeightRange(Vector3 to, Random RNG, float min, float max) {
        float x = 0f;
        float y = 0f;
        float z = 0f;
        boolean success = false;

        // Not a big deal, usually found within 5 attempts
        for (int i = 0; i < 100; i++) {
            x = RNG.nextFloat() * sizeX;
            y = RNG.nextFloat() * sizeY;
            z = getHeight(x, y);
            if (z >= min && z <= max) {
                success = true;
                break;
            }
        }

        to.set(x, y, z);
        return success;
    }

    public Noise islandize(float scale, float offset) {
        final int width = sizeX;
        final int height = sizeY;
        final float[] values = this.values;

        int outI = 0;
        for (int x = 0; x < width; x++) {
            final float xFactor = (float) Math.cos((((float)x / (width-1f)) - 0.5f) * Math.PI);
            for (int y = 0; y < height; y++) {
                final float yFactor = (float) Math.cos((((float)y / (height-1f)) - 0.5f) * Math.PI);

                final float factor = xFactor * yFactor;

                values[outI] = values[outI] * factor * factor * scale + offset;
                outI++;
            }
        }

        return this;
    }

    public Noise max(float minValue) {
        final float[] values = this.values;
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(values[i], minValue);
        }

        return this;
    }


    private static Noise generateWhiteNoise(int sizeX, int sizeY, long seed) {
        Random random = new Random(seed);
        float[] noise = new float[sizeX * sizeY];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = random.nextFloat();
        }
        return new Noise(noise, 0f, sizeX, sizeY);
    }

    private static Noise generateSmoothNoise(Noise baseNoise, int octave) {
        final int width = baseNoise.sizeX;
        final int height = baseNoise.sizeY;
        final float[] baseValues = baseNoise.values;
        final float[] smoothNoise = new float[baseValues.length];

        final int samplePeriod = 1 << octave;
        final float sampleFrequency = 1.0f / samplePeriod;
        int outI = 0;
        for (int x = 0; x < width; x++) {
            int sample_x0 = (x >>> octave) << octave;
            int sample_x1 = (sample_x0 + samplePeriod) % width;
            float horizontal_blend = (x - sample_x0) * sampleFrequency;

            for (int y = 0; y < height; y++) {
                int sample_y0 = (y >>> octave) << octave;
                int sample_y1 = (sample_y0 + samplePeriod) % height;
                float vertical_blend = (y - sample_y0) * sampleFrequency;
                float top = lerp(baseValues[sample_x0 + width * sample_y0], baseValues[sample_x1 + width * sample_y0], horizontal_blend);
                float bottom = lerp(baseValues[sample_x0 + width * sample_y1], baseValues[sample_x1 + width * sample_y1], horizontal_blend);
                smoothNoise[outI++] = lerp(top, bottom, vertical_blend);
            }
        }

        return new Noise(smoothNoise, baseNoise.defaultValue, width, height);
    }

    public static Noise generatePerlinNoise(Noise baseNoise, int octaveCount, float persistence) {
        final float[] perlinNoise = new float[baseNoise.values.length];

        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;

        for (int octave = octaveCount - 1; octave >= 0; octave--) {
            final float[] smoothNoise = generateSmoothNoise(baseNoise, octave).values;
            amplitude *= persistence;
            totalAmplitude += amplitude;

            for (int i = 0; i < perlinNoise.length; i++) {
                perlinNoise[i] += smoothNoise[i] * amplitude;
            }
        }

        totalAmplitude = 1f / totalAmplitude;
        for (int i = 0; i < perlinNoise.length; i++) {
            perlinNoise[i] *= totalAmplitude;
        }

        return new Noise(perlinNoise, baseNoise.defaultValue, baseNoise.sizeX, baseNoise.sizeY);
    }

    public static Noise generateSimplexNoise(int width, int height,
                                                 long seed, float initialHeight,
                                                 float initialScale, float scaleMultiplier,
                                                 int octaveCount, float initialAmplitude, float amplitudeMultiplier, float defaultHeight) {
        final OpenSimplexNoise noise = new OpenSimplexNoise();

        final float[] result = new float[width * height];
        Arrays.fill(result, initialHeight);


        float amplitude = initialAmplitude;
        float scale = initialScale;


        for (int octave = 0; octave < octaveCount; octave++) {
            noise.initialize(seed + octave);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[x + width * y] += noise.evaluate(x * scale, y * scale) * amplitude;
                }
            }

            scale *= scaleMultiplier;
            amplitude *= amplitudeMultiplier;
        }

        return new Noise(result, defaultHeight, width, height);
    }

    public static Noise generatePerlinNoise(int width, int height, int octaveCount, float persistence, long seed) {
        Noise baseNoise = generateWhiteNoise(width, height, seed);
        return generatePerlinNoise(baseNoise, octaveCount, persistence);
    }

    public static Noise generateHydraulicNoise(int size, long seed, int iterations, float step) {
        final Random r = new RandomXS128(seed);
        final float[] m = new float[size * size];

        for (int i = 0; i < iterations; i++) {
            final int line0X = r.nextInt(size);
            final int line0Y = r.nextInt(size);
            final int line1X = r.nextInt(size);
            final int line1Y = r.nextInt(size);

            final int a = line0Y - line1Y;
            final int b = line1X - line0X;
            final int c = line0X * line1Y - line1X * line0Y;

            for (int y = 0; y < size; y++) {
                int e = b * y + c;
                for (int x = 0; x < size; x++) {

                    if (e > 0) {
                        m[x + size * y] += step;
                    } else {
                        m[x + size * y] -= step;

                    }

                    e += a;
                }
            }
        }

        return new Noise(m, 0f, size, size);
    }
}
