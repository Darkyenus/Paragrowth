package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.RandomXS128;

/**
 * @author Darkyen
 */
public final class DiamondSquare {

    private static void doDiamond(float[][] m, int x, int y, int off, RandomXS128 random, float magnitude) {
        m[x][y] = (m[x-off][y-off] + m[x+off][y-off] + m[x-off][y+off] + m[x+off][y+off]) * 0.25f
                + (random.nextFloat() * 2f - 1f) * magnitude;
    }

    private static void doSquare(float[][] m, int s, int x, int y, int off, RandomXS128 random, float magnitude) {
        int negX = x - off;
        int posX = x + off;
        int negY = y - off;
        int posY = y + off;

        if (negX < 0) {
            negX += s;
        } else if (posX >= s) {
            posX -= s;
        }

        if (negY < 0) {
            negY += s;
        } else if (posY >= s) {
            posY -= s;
        }

        m[x][y] = (m[negX][y] + m[posX][y] + m[x][negY] + m[x][posY]) * 0.25f + (random.nextFloat() * 2f - 1f) * magnitude;
    }

    public static float[][] generate(int sizePower, long seed, float corners, float middle){
        final RandomXS128 r = new RandomXS128(seed);
        int s = (1 << sizePower) + 1; // Side
        final float[][] m = new float[s][s]; // Map
        m[0][0] = corners;
        m[s-1][0] = corners;
        m[0][s-1] = corners;
        m[s-1][s-1] = corners;

        boolean first = true;
        float magnitude = 1f;
        for (int off = s >>> 1; off > 0; off >>>= 1) {
            if (first) {
                first = false;
                m[s/2][s/2] = middle;
            } else {
                for (int x = off; x < s; x += off + off) {
                    for (int y = off; y < s; y += off + off) {
                        doDiamond(m, x, y, off, r, magnitude);
                    }
                }
            }


            boolean square = false;
            for (int x = 0; x < s; x += off) {
                for (int y = 0; y < s; y += off) {
                    if (square) {
                        doSquare(m, s, x, y, off, r, magnitude);
                    }

                    square = !square;
                }
            }

            magnitude *= 0.5f;
        }

        return m;
    }
}
