package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.darkyen.paragrowth.util.VectorUtils;

import java.util.Random;

/**
 * @author Darkyen
 */
public class Doodad {

    /**
     * Initial width of the plant
     */
    final VarFloat initialWidth = new VarFloat();

    /**
     * Base length of the plant from which the real length is derived
     */
    final VarFloat rootLength = new VarFloat();

    /**
     * How many sides does the trunk n-gon have
     */
    final VarFloat trunkSides = new VarFloat(3, 6);
    {
        trunkSides.setRange(3.5f, 5.5f);
    }

    /**
     * Root node of the doodad, starting at origin
     */
    TrunkNode firstNode;

    final VarFloat initialBranchingFactor = new VarFloat(0);

    DoodadInstance instantiate(Random random, float x, float y, float z) {
        final DoodadInstance instance = new DoodadInstance(initialWidth.get(random), trunkSides.getInt(random));
        instance.position.set(x, y, z);
        final float rootLength = this.rootLength.get(random);
        instance.root = firstNode.instantiate(random, instance.position, Vector3.Z, instance.rootWidth, rootLength, initialBranchingFactor.get(random), 0);
        return instance;
    }

    /**
     * Extruded N-gon
     */
    static class TrunkNode {

        /**
         * previous length * lengthFactor = length of trunk
         */
        final VarFloat lengthFactor = new VarFloat();
        /**
         * previous width * widthFactor = end width of trunk
         */
        final VarFloat widthFactor = new VarFloat();

        /**
         * 0 -> straight
         * 1 -> 90 degree bend in random direction
         */
        final VarFloat skew = new VarFloat(0f, 1f);

        /**
         * branchingFactor * previous branching factor * branching roll = real branching roll
         */
        final VarFloat branchingFactor = new VarFloat(0);

        /**
         * Index:
         * N -> Roll needed for the N-th branch in branches to grow
         */
        final FloatArray branchingProbability = new FloatArray();
        final Array<TrunkNode> branches = new Array<>(TrunkNode.class);


        private static final int MAX_BRANCHING_DEPTH = 10;

        private static final Vector3 instantiate_bitangent = new Vector3();
        private static final Matrix3 instantiate_mat = new Matrix3();
        DoodadInstance.TrunkInstance instantiate(Random random, Vector3 previousEnd, Vector3 previousDirection, float previousWidth, float previousLength, float previousBranchingFactor, int depth) {
            final DoodadInstance.TrunkInstance instance = new DoodadInstance.TrunkInstance(widthFactor.getFactored(random, previousWidth));
            final float length = lengthFactor.getFactored(random, previousLength);
            final float skew = this.skew.get(random);

            // Make end is correct from origin
            final Vector3 end = instance.end.set(1f - skew, 0f, skew).nor();
            // Rotate randomly
            end.rotateRad(random.nextFloat() * MathUtils.PI2, 0f, 0f, 1f);
            // end now contains normalized direction, save for branches
            instance.direction.set(end);
            // Extend by length
            end.scl(length);
            // Turn to fit on previous direction
            final Vector3 tangent = VectorUtils.generateTangent(previousDirection);
            final Vector3 biTangent = instantiate_bitangent.set(previousDirection).crs(tangent);
            final Matrix3 mat = TrunkNode.instantiate_mat;
            mat.val[Matrix3.M00] = previousDirection.x;
            mat.val[Matrix3.M01] = previousDirection.y;
            mat.val[Matrix3.M02] = previousDirection.z;
            mat.val[Matrix3.M10] = tangent.x;
            mat.val[Matrix3.M11] = tangent.y;
            mat.val[Matrix3.M12] = tangent.z;
            mat.val[Matrix3.M20] = biTangent.x;
            mat.val[Matrix3.M21] = biTangent.y;
            mat.val[Matrix3.M22] = biTangent.z;

            end.mul(mat);//TODO Or traMul???

            // Translate on previous end
            end.add(previousEnd);

            if (depth < MAX_BRANCHING_DEPTH) {
                // Generate branches, if any
                for (int i = 0; i < branchingProbability.size; i++) {
                    final float factor = branchingFactor.getFactored(random, previousBranchingFactor);
                    final float roll = random.nextFloat() * factor;
                    if (roll < branchingProbability.items[i]) {
                        // Generate the branch

                        final DoodadInstance.TrunkInstance branch = branches.items[i].instantiate(random, end,
                                instance.direction, instance.endWidth, length, factor, depth + 1);
                        instance.children.add(branch);
                    }
                }
            }

            return instance;
        }
    }

    /**
     * Multiple times extruded N-gon, closed with extrusion to zero
     */
    static class HullLeaf {

        final VarFloat sides = new VarFloat(1, 6);

        final VarFloat length = new VarFloat();
    }

    /**
     * Some free triangles
     */
    static class TriangleLeaf {

    }

    static class VarFloat {
        /**
         * Base value
         */
        float value = 1;
        /**
         * Standard deviation
         * 0 -> always value
         * 1 -> most values are in range value +- 1
         */
        float deviation = 0;
        final float min;
        final float max;

        VarFloat(float min, float max) {
            this.min = min;
            this.max = max;
        }

        VarFloat(float min) {
            this.min = min;
            this.max = Float.POSITIVE_INFINITY;
        }

        VarFloat() {
            this.min = 0.1f;
            this.max = Float.POSITIVE_INFINITY;
        }

        void setRange(float min, float max) {
            value = (min + max) * 0.5f;
            deviation = max - value;
        }

        void set(float value, float deviation) {
            this.value = value;
            this.deviation = deviation;
        }

        float get(Random random) {
            final float rawResult = (float) (value + random.nextGaussian() * 0.5 * deviation);
            if (rawResult < min) {
                return min;
            } else if (rawResult > max) {
                return max;
            } else {
                return rawResult;
            }
        }

        int getInt(Random random) {
            return Math.round(get(random));
        }

        float getFactored(Random random, float base) {
            return get(random) * base;
        }
    }
}
