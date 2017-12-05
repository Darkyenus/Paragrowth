package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.darkyen.paragrowth.util.ColorKt;
import com.darkyen.paragrowth.util.VectorUtils;

import java.util.Random;

/**
 * @author Darkyen
 */
public class Doodad {

    public static float MIN_WIDTH = 0.01f;

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

    final VarFloat trunkColorHue = new VarFloat(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    final VarFloat trunkColorSaturation = new VarFloat(0f, 1f);
    final VarFloat trunkColorBrightness = new VarFloat(0f, 1f);

    DoodadInstance instantiate(Random random, float x, float y, float z) {
        final float trunkColor = ColorKt.hsb(trunkColorHue.get(random), trunkColorSaturation.get(random), trunkColorBrightness.get(random), 1f);
        final DoodadInstance instance = new DoodadInstance(this, initialWidth.get(random), trunkSides.getInt(random), trunkColor);
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


        private static final int MAX_BRANCHING_DEPTH = 4;

        DoodadInstance.TrunkInstance instantiate(Random random, Vector3 previousEnd, Vector3 previousDirection,
                                                 float previousWidth, float previousLength, float previousBranchingFactor,
                                                 int depth) {
            final DoodadInstance.TrunkInstance instance = new DoodadInstance.TrunkInstance(widthFactor.getFactored(random, previousWidth));
            final float length = lengthFactor.getFactored(random, previousLength);
            final float skew = this.skew.get(random);

            // Make end is correct from origin
            final Vector3 end = instance.end.set(skew, 0f, 1f - skew).nor();
            // Rotate randomly
            end.rotateRad(random.nextFloat() * MathUtils.PI2, 0f, 0f, 1f);
            // end now contains normalized direction, save for branches
            instance.direction.set(end);
            // Extend by length
            end.scl(length);
            // Turn to fit on previous direction
            VectorUtils.toNormalSpace(end, previousDirection);

            // Translate on previous end
            end.add(previousEnd);

            if (depth < MAX_BRANCHING_DEPTH && instance.endWidth > MIN_WIDTH) {
                // Generate branches, if any
                for (int i = 0; i < branchingProbability.size; i++) {
                    final float factor = branchingFactor.getFactored(random, previousBranchingFactor);
                    final float roll = random.nextFloat() * factor;
                    if (roll < branchingProbability.items[i]) {
                        // Generate the branch

                        final DoodadInstance.TrunkInstance branch = branches.items[i].instantiate(random, end,
                                instance.direction, instance.endWidth, length, factor,depth + 1);
                        instance.children.add(branch);
                    }
                }
            }

            return instance;
        }

        public TrunkNode copySelfBranchOnly() {
            final TrunkNode node = new TrunkNode();
            node.lengthFactor.set(lengthFactor);
            node.widthFactor.set(widthFactor);
            node.skew.set(skew);
            node.branchingFactor.set(branchingFactor);
            for (int i = 0; i < branches.size; i++) {
                if (branches.get(i) == this) {
                    node.branchingProbability.add(branchingProbability.get(i));
                    node.branches.add(node);
                }
            }

            return node;
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

        void set(VarFloat varFloat) {
            this.value = varFloat.value;
            this.deviation = varFloat.deviation;
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
