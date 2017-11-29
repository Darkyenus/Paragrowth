package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.RandomXS128;

import java.util.Random;

/**
 * @author Darkyen
 */
public final class DiamondSquare {

    public static float[][] generate(int size, long seed, float around, float middle){
        final Random r = new RandomXS128(seed);
        int s = 1 << size + 1; // Side
        final float[][] m = new float[s][s]; // Map

        for (int i = 0; i < size; i++) {
            m[i][0] = around;
            m[i][s-1] = around;
            m[0][i] = around;
            m[s-1][i] = around;
        }
        m[s/2][s/2] = middle;

        s >>= 1;
        for (int iteration = 2; iteration < size; iteration++) {
            int hs = s >> 1; //half side
            for (int i = 0; i < iteration-1; i++) {
                // Do betweens
                // Top
                m[i * s + hs][i * s] = (m[i * s][i * s] + m[i * s + s][i * s]) / 2f;
                //Bottom
                m[i * s + hs][i * s + s] = (m[i * s][i * s + s] + m[i * s + s][i * s + s]) / 2f;
                //Left
                m[i * s][i * s + hs] = (m[i * s][i * s] + m[i * s][i * s + s]) / 2f;
                //Right
                m[i * s + s][i * s + hs] = (m[i * s + s][i * s] + m[i * s + s][i * s + s]) / 2f;

                // Do middle from betweens
                m[i*s + hs][i*s + hs] = (m[i * s + hs][i * s] + m[i * s + hs][i * s + s] + m[i * s][i * s + hs] + m[i * s + s][i * s + hs]) / 4f;

            }
            s = hs;
        }

        return m;
    }
}
