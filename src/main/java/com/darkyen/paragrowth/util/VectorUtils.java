package com.darkyen.paragrowth.util;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;

/**
 *
 */
public class VectorUtils {

    private static final Vector3 generateTangent_c1 = new Vector3();
    private static final Vector3 generateTangent_c2 = new Vector3();
    public static Vector3 generateTangent(Vector3 normal) {
        // https://stackoverflow.com/questions/5255806/how-to-calculate-tangent-and-binormal
        final Vector3 c1 = generateTangent_c1.set(0f, 1f, 0f).crs(normal);
        final Vector3 c2 = generateTangent_c2.set(0f, 0f, 1f).crs(normal);
        if (c1.len2() > c2.len2()) {
            return c1;
        } else {
            return c2;
        }
    }

    private static final Vector3 toNormalSpace_bitangent = new Vector3();
    private static final Matrix3 toNormalSpace_mat = new Matrix3();

    public static void toNormalSpace(Vector3 vector, Vector3 normal) {
        final Vector3 tangent = VectorUtils.generateTangent(normal);
        final Vector3 biTangent = toNormalSpace_bitangent.set(normal).crs(tangent);
        final Matrix3 mat = toNormalSpace_mat;
        mat.val[Matrix3.M00] = tangent.x;
        mat.val[Matrix3.M01] = tangent.y;
        mat.val[Matrix3.M02] = tangent.z;
        mat.val[Matrix3.M10] = biTangent.x;
        mat.val[Matrix3.M11] = biTangent.y;
        mat.val[Matrix3.M12] = biTangent.z;
        mat.val[Matrix3.M20] = normal.x;
        mat.val[Matrix3.M21] = normal.y;
        mat.val[Matrix3.M22] = normal.z;

        vector.mul(mat);
    }

}
