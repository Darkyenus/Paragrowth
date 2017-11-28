package com.darkyen.paragrowth.world.terrain;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

/**
 *
 */
public class HydraulicNoise {

    public static float[][] generate(int size, long seed, int iterations, float step){
        final Random r = new RandomXS128(seed);
        int s = 1 << size + 1; // Side
        final float[][] m = new float[s][s]; // Map

        final Vector2 planeOrigin = new Vector2();
        final Vector2 planeDirection = new Vector2();

        for (int i = 0; i < iterations; i++) {
            planeOrigin.set(r.nextInt(s), r.nextInt(s));
            planeDirection.setAngleRad(r.nextFloat()*MathUtils.PI2).add(planeOrigin);

            for (int x = 0; x < s; x++) {
                for (int y = 0; y < s; y++) {
                    m[x][y] += Intersector.pointLineSide(planeOrigin.x, planeOrigin.y, planeDirection.x, planeDirection.y, x, y) * step;
                }
            }
        }

        return m;
    }
}
