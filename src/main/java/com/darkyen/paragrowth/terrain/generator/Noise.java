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

import java.util.Random;

import static com.badlogic.gdx.math.MathUtils.lerp;

/**
 * Copied from https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/g3d/voxel/PerlinNoiseGenerator.java
 * and altered.
 * Adapted from <a href="http://devmag.org.za/2009/04/25/perlin-noise/">http://devmag.org.za/2009/04/25/perlin-noise/</a>
 *
 * @author badlogic
 */
public final class Noise {

    private static float[][] generateWhiteNoise(int width, int height, long seed) {
        Random random = new Random(seed);
        float[][] noise = new float[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                noise[x][y] = random.nextFloat();
            }
        }
        return noise;
    }

    private static float[][] generateSmoothNoise(float[][] baseNoise, int octave) {
        final int width = baseNoise.length;
        final int height = baseNoise[0].length;
        final float[][] smoothNoise = new float[width][height];

        final int samplePeriod = 1 << octave;
        final float sampleFrequency = 1.0f / samplePeriod;
        for (int x = 0; x < width; x++) {
            int sample_x0 = (x >>> octave) << octave;
            int sample_x1 = (sample_x0 + samplePeriod) % width;
            float horizontal_blend = (x - sample_x0) * sampleFrequency;

            for (int y = 0; y < height; y++) {
                int sample_y0 = (y >>> octave) << octave;
                int sample_y1 = (sample_y0 + samplePeriod) % height;
                float vertical_blend = (y - sample_y0) * sampleFrequency;
                float top = lerp(baseNoise[sample_x0][sample_y0], baseNoise[sample_x1][sample_y0], horizontal_blend);
                float bottom = lerp(baseNoise[sample_x0][sample_y1], baseNoise[sample_x1][sample_y1], horizontal_blend);
                //noinspection SuspiciousNameCombination
                smoothNoise[x][y] = lerp(top, bottom, vertical_blend);
            }
        }

        return smoothNoise;
    }

    public static float[][] generatePerlinNoise(float[][] baseNoise, int octaveCount, float persistence) {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        final float[][] perlinNoise = new float[width][height];

        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;

        for (int octave = octaveCount - 1; octave >= 0; octave--) {
            final float[][] smoothNoise = generateSmoothNoise(baseNoise, octave);
            amplitude *= persistence;
            totalAmplitude += amplitude;

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    perlinNoise[i][j] += smoothNoise[i][j] * amplitude;
                }
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] /= totalAmplitude;
            }
        }

        return perlinNoise;
    }

    public static float[][] generateSimplexNoise(int width, int height,
                                                 long seed, float initialHeight,
                                                 float initialScale, float scaleMultiplier,
                                                 int octaveCount, float initialAmplitude, float amplitudeMultiplier) {
        final OpenSimplexNoise noise = new OpenSimplexNoise();

        final float[][] result = new float[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[x][y] = initialHeight;
            }
        }

        float amplitude = initialAmplitude;
        float scale = initialScale;


        for (int octave = 0; octave < octaveCount; octave++) {
            noise.initialize(seed + octave);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    result[x][y] += noise.evaluate(x * scale, y * scale) * amplitude;
                }
            }

            scale *= scaleMultiplier;
            amplitude *= amplitudeMultiplier;
        }

        return result;
    }

    public static float[][] generatePerlinNoise(int width, int height, int octaveCount, float persistence, long seed) {
        float[][] baseNoise = generateWhiteNoise(width, height, seed);
        return generatePerlinNoise(baseNoise, octaveCount, persistence);
    }

    public static float[][] generateHydraulicNoise(int size, long seed, int iterations, float step) {
        final Random r = new RandomXS128(seed);
        final float[][] m = new float[size][size]; // Map

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
                        m[x][y] += step;
                    } else {
                        m[x][y] -= step;

                    }

                    e += a;
                }
            }
        }

        return m;
    }

    public static float[][] islandize(float[][] noise, float scale, float offset) {
        final int width = noise.length;
        final int height = noise[0].length;

        for (int x = 0; x < width; x++) {
            final float xFactor = (float) Math.cos((((float)x / width) - 0.5f) * Math.PI);
            for (int y = 0; y < height; y++) {
                final float yFactor = (float) Math.cos((((float)y / height) - 0.5f) * Math.PI);

                final float factor = xFactor * yFactor;

                noise[x][y] = noise[x][y] * factor * factor * scale + offset;
            }
        }

        return noise;
    }

    public static float[][] islandize(float[][] noise,float offset) {
        final int width = noise.length;
        final int height = noise[0].length;

        for (int x = 0; x < width; x++) {
            final float xFactor = (float) Math.cos((((float)x / width) - 0.5f) * Math.PI);
            for (int y = 0; y < height; y++) {
                final float yFactor = (float) Math.cos((((float)y / height) - 0.5f) * Math.PI);

                final float factor = xFactor * yFactor;

                noise[x][y] = noise[x][y] * factor * factor + offset;
            }
        }

        return noise;
    }

    public static float[][] max(float[][] noise, float minValue) {
        final int width = noise.length;
        final int height = noise[0].length;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final float h = noise[x][y];
                if (h < minValue) {
                    noise[x][y] = minValue;
                }
            }
        }

        return noise;
    }

    public static float getHeight(float[][] noise, float x, float y) {
        int lowX = (int) x;
        int highX = lowX + 1;
        float alphaX = x - lowX;
        if (lowX < 0) {
            lowX = highX = 0;
            alphaX = 0f;
        } else if (highX >= noise.length) {
            lowX = highX = noise.length - 1;
            alphaX = 0f;
        }

        int lowY = (int) y;
        int highY = lowY + 1;
        float alphaY = y - lowY;
        if (lowY < 0) {
            lowY = highY = 0;
            alphaY = 0f;
        } else if (highY >= noise[0].length) {
            lowY = highY = noise[0].length - 1;
            alphaY = 0f;
        }

        final float bottomX = lerp(noise[lowX][lowY], noise[highX][lowY], alphaX);
        final float topX = lerp(noise[lowX][highY], noise[highX][highY], alphaX);
        return lerp(bottomX, topX, alphaY);
    }
}
