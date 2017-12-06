package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.util.ColorKt;
import com.darkyen.paragrowth.util.VectorUtils;

import java.util.Random;

/**
 * @author Darkyen
 */
class Doodad {

    static float MIN_WIDTH = 0.01f;

    final String tag;

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

    Doodad(String tag) {
        this.tag = tag;
    }

    DoodadInstance instantiate(Random random, float x, float y, float z, WorldCharacteristics characteristics) {
        final float trunkColor = characteristics.possiblyReplaceColor(random, ColorKt.hsb(trunkColorHue.get(random), trunkColorSaturation.get(random), trunkColorBrightness.get(random), 1f));
        final DoodadInstance instance = new DoodadInstance(this, initialWidth.get(random), trunkSides.getInt(random), trunkColor);
        instance.position.set(x, y, z);
        final float rootLength = this.rootLength.get(random);
        instance.root = firstNode.instantiate(random, instance.position, Vector3.Z, instance.rootWidth, rootLength, initialBranchingFactor.get(random), 0, characteristics);
        return instance;
    }

    /**
     * Extruded N-gon
     */
    static class TrunkNode {

        final String tag;
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
        private final FloatArray branchingProbability = new FloatArray();
        private final Array<TrunkNode> branches = new Array<>(TrunkNode.class);

        TrunkNode(String tag) {
            this.tag = tag;
        }

        void addBranch(float probability, TrunkNode branch) {
            branchingProbability.add(probability);
            branches.add(branch);
        }

        private final FloatArray leafProbability = new FloatArray();
        private final Array<Leaf> leaves = new Array<>(Leaf.class);

        void addLeaf(float probability, Leaf leaf) {
            leafProbability.add(probability);
            leaves.add(leaf);
        }


        private static final int MAX_BRANCHING_DEPTH = 4;

        DoodadInstance.TrunkInstance instantiate(Random random, Vector3 previousEnd, Vector3 previousDirection,
                                                 float previousWidth, float previousLength, float previousBranchingFactor,
                                                 int depth, WorldCharacteristics characteristics) {
            final float length = lengthFactor.getFactored(random, previousLength);
            final DoodadInstance.TrunkInstance instance = new DoodadInstance.TrunkInstance(tag, length, widthFactor.getFactored(random, previousWidth));
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
                    final float roll = random.nextFloat();
                    if (roll < branchingProbability.items[i] * factor) {
                        // Generate the branch

                        final DoodadInstance.TrunkInstance branch = branches.items[i].instantiate(random, end,
                                instance.direction, instance.endWidth, length, factor,depth + 1, characteristics);
                        instance.trunkChildren.add(branch);
                    }
                }
            }

            for (int i = 0; i < leafProbability.size; i++) {
                final float factor = branchingFactor.getFactored(random, previousBranchingFactor);
                final float roll = random.nextFloat();
                if (roll < leafProbability.items[i] * factor) {
                    // Generate the leaf

                    final DoodadInstance.LeafInstance leaf = leaves.items[i].instantiate(random, instance, characteristics);
                    instance.leafChildren.add(leaf);
                }
            }

            return instance;
        }
    }

    interface Leaf {
        DoodadInstance.LeafInstance instantiate(Random random, DoodadInstance.TrunkInstance ofTrunk, WorldCharacteristics characteristics);
    }

    /**
     * Multiple times extruded N-gon, closed with caps
     */
    static class HullLeaf implements Leaf {

        final String tag;

        final VarFloat sides = new VarFloat(3, 6);
        {
            sides.setRange(3.5f, 5.5f);
        }

        final VarFloat length = new VarFloat(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        final VarFloat extraLengthFromTrunkFactor = new VarFloat(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        {
            extraLengthFromTrunkFactor.set(0f, 0f);
        }

        final VarFloat roundness = new VarFloat(0f, 1f);

        final VarFloat widest = new VarFloat(0f, 1f);

        final VarFloat width = new VarFloat();

        final VarFloat hue = new VarFloat(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        final VarFloat saturation = new VarFloat(0f, 1f);
        final VarFloat brightness = new VarFloat(0f, 1f);

        HullLeaf(String tag) {
            this.tag = tag;
        }

        @Override
        public DoodadInstance.HullLeafInstance instantiate(Random random, DoodadInstance.TrunkInstance ofTrunk, WorldCharacteristics characteristics) {
            final float length = this.length.get(random) + extraLengthFromTrunkFactor.getFactored(random, ofTrunk.length);
            final float widest = this.widest.get(random);
            final float roundness = this.roundness.get(random);
            final int ringsPre = Math.round(widest * length * roundness);
            final int ringsPost = Math.round((1f - widest) * length * roundness);

            final DoodadInstance.HullLeafInstance instance = new DoodadInstance.HullLeafInstance(
                    tag, Math.round(sides.get(random)), ringsPre, ringsPost, widest, width.get(random), characteristics.possiblyReplaceColor(random, ColorKt.hsb(hue.get(random), saturation.get(random), brightness.get(random), 1f)));

            instance.end.set(ofTrunk.end).mulAdd(ofTrunk.direction, length);

            return instance;
        }
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
