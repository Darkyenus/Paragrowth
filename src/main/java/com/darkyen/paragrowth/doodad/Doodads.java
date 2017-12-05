package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.utils.Array;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.WorldColors;
import com.darkyen.paragrowth.util.ColorKt;

import java.util.Random;

import static com.darkyen.paragrowth.util.VectorUtils.map;

/**
 *
 */
public class Doodads {

    private static Doodad createDeadTree(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad();
        tree.initialWidth.setRange(0.5f, 1.5f);
        tree.rootLength.setRange(4f, 7f);
        tree.initialBranchingFactor.setRange(1f, 1.3f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode();
        trunk.branches.add(trunk);
        trunk.branchingProbability.add(1.2f);
        trunk.branches.add(trunk);
        trunk.branchingProbability.add(0.7f);

        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(0.7f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(map(characteristics.coherence + (random.nextFloat() - 0.5f) * 0.3f, 0f, 1f, 0.7f, 0.25f), 0.1f);

        tree.firstNode = trunk;
        return tree;
    }

    private static Doodad createPineTreeA(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad();
        tree.initialWidth.setRange(0.3f, 1f);
        tree.rootLength.setRange(0.4f, 2f);
        tree.initialBranchingFactor.setRange(0f, 0f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode();
        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(1f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0f, 0f);
        tree.firstNode = trunk;

        final Doodad.TrunkNode trunkCap = new Doodad.TrunkNode();
        trunkCap.branchingFactor.setRange(0.4f, 0.9f);
        trunkCap.lengthFactor.set(1f, 0.2f);
        trunkCap.widthFactor.set(0.7f, 0.2f);
        trunkCap.skew.set(0f, 0f);
        trunk.addBranch(10f, trunkCap);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf();
        leaf.roundness.set(0f, 0f);
        leaf.length.setRange(9f, 17f);
        leaf.sides.setRange(3f, 5f);
        leaf.width.setRange(3f, 5f);
        leaf.widest.setRange(0.1f, 0.15f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.6f, 0.2f);
        trunk.addLeaf(10f, leaf);

        return tree;
    }

    private static Doodad createPineTreeB(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad();
        tree.initialWidth.setRange(0.3f, 1f);
        tree.rootLength.setRange(0.4f, 2f);
        tree.initialBranchingFactor.setRange(0f, 0f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode();
        trunk.branchingFactor.set(1f, 0f);
        trunk.lengthFactor.set(1f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0f, 0f);
        tree.firstNode = trunk;

        final Doodad.TrunkNode innerTrunk = new Doodad.TrunkNode();
        innerTrunk.branchingFactor.setRange(0.4f, 0.9f);
        innerTrunk.lengthFactor.set(1f, 0.2f);
        innerTrunk.widthFactor.set(0.7f, 0.2f);
        innerTrunk.skew.set(0f, 0f);
        trunk.addBranch(Float.POSITIVE_INFINITY, innerTrunk);
        innerTrunk.addBranch(1f, innerTrunk);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf();
        leaf.roundness.set(0f, 0f);
        leaf.length.setRange(0.2f, 0.1f);
        leaf.extraLengthFromTrunkFactor.setRange(1f, 2f);
        leaf.sides.setRange(3f, 5f);
        leaf.width.setRange(3f, 5f);
        leaf.widest.setRange(0.5f, 0.05f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.6f, 0.2f);
        trunk.addLeaf(Float.POSITIVE_INFINITY, leaf);
        innerTrunk.addLeaf(Float.POSITIVE_INFINITY, leaf);

        return tree;
    }

    private static Doodad createSpike(Random random, WorldCharacteristics characteristics) {
        final Doodad spike = new Doodad();
        spike.initialWidth.setRange(0.5f, 3f);
        spike.rootLength.setRange(7f, 8f-characteristics.mood*3f);
        spike.initialBranchingFactor.setRange(1f, 1.3f);
        spike.trunkColorHue.set((ColorKt.getHueRed()) * 0.5f, 0.2f);
        spike.trunkColorBrightness.set(0.05f, 0.05f);
        spike.trunkColorSaturation.set(0.05f, 0.05f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode();
        trunk.branches.add(trunk);
        trunk.branchingProbability.add(1.2f);
        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(0.7f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0.1f, 0.05f);
        spike.firstNode = trunk;

        return spike;
    }

    public static Array<Doodad> createDoodadSet(Random random, WorldCharacteristics characteristics) {
        final Array<Doodad> doodads = new Array<>();
        doodads.add(createDeadTree(random, characteristics));
        doodads.add(createDeadTree(random, characteristics));
        doodads.add(createDeadTree(random, characteristics));

        float mood = characteristics.mood;
        while (mood < 0.1f) {
            mood += 0.1f;
            doodads.add(createSpike(random, characteristics));
        }

        mood = characteristics.mood;
        while (mood > -0.1f) {
            mood -= 0.1f;
            switch (random.nextInt(2)) {
                case 0:
                    doodads.add(createPineTreeA(random, characteristics));
                    break;
                case 1:
                    doodads.add(createPineTreeB(random, characteristics));
                    break;
            }
        }

        return doodads;
    }
}
