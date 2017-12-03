package com.darkyen.paragrowth.util;

import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public class VectorUtils {

    private static final Vector3 generateTangent_c1 = new Vector3();
    private static final Vector3 generateTangent_c2 = new Vector3();
    public static Vector3 generateTangent(Vector3 normal) {
        // https://stackoverflow.com/questions/5255806/how-to-calculate-tangent-and-binormal
        final Vector3 c1 = generateTangent_c1.set(normal).crs(1f, 0f, 0f);
        final Vector3 c2 = generateTangent_c2.set(normal).crs(0f, 1f, 0f);
        if (c1.len2() > c2.len2()) {
            return c1;
        } else {
            return c2;
        }
    }

}
