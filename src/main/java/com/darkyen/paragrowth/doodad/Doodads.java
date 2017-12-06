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

        final Doodad tree = new Doodad("dead tree");
        tree.initialWidth.setRange(0.5f, 1.5f);
        tree.rootLength.setRange(4f, 7f);
        tree.initialBranchingFactor.setRange(1f, 1.3f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode("dead tree.trunk");
        trunk.addBranch(1.2f, trunk);
        trunk.addBranch(0.7f, trunk);

        trunk.branchingFactor.setRange(0.2f, 0.5f);
        trunk.lengthFactor.set(0.7f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(map(characteristics.coherence + (random.nextFloat() - 0.5f) * 0.3f, 0f, 1f, 0.7f, 0.25f), 0.1f);

        final Doodad.TrunkNode rootTrunk = new Doodad.TrunkNode("dead tree.root trunk");
        rootTrunk.addBranch(Float.POSITIVE_INFINITY, trunk);

        rootTrunk.branchingFactor.setRange(0.2f, 0.5f);
        rootTrunk.lengthFactor.set(0.7f, 0.2f);
        rootTrunk.widthFactor.set(0.7f, 0.2f);
        rootTrunk.skew.set(0.0f, 0.2f);


        tree.firstNode = rootTrunk;
        return tree;
    }

    private static Doodad createLivingTree(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad("living tree");
        tree.initialWidth.setRange(0.5f, 1.5f);
        tree.rootLength.set(6f, 1f);
        tree.initialBranchingFactor.set(1.2f, 0.1f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode("living tree.trunk");
        trunk.addBranch(1f, trunk);
        trunk.addBranch(1f, trunk);

        trunk.branchingFactor.setRange(0.4f, 0.7f);
        trunk.lengthFactor.set(0.8f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0.5f, (1f - characteristics.coherence)*0.4f);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf("living tree.leaf");
        leaf.length.setRange(4f, 12f);
        leaf.width.setRange(2f, 6f);
        leaf.widest.set(0.5f, 0.1f);
        leaf.sides.set(5f, 1f);
        leaf.roundness.setRange(0.5f, 1.5f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.6f, 0.2f);
        trunk.addLeaf(10.8f, leaf);

        final Doodad.TrunkNode rootTrunk = new Doodad.TrunkNode("living tree.root trunk");
        rootTrunk.addBranch(Float.POSITIVE_INFINITY, trunk);

        rootTrunk.branchingFactor.setRange(0.2f, 0.5f);
        rootTrunk.lengthFactor.set(0.7f, 0.2f);
        rootTrunk.widthFactor.set(0.7f, 0.2f);
        rootTrunk.skew.set(0.0f, 0.2f);
        rootTrunk.addLeaf(0.9f, leaf);

        tree.firstNode = rootTrunk;
        return tree;
    }

    private static Doodad createLivingTreeBranchless(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad("living tree branchless");
        tree.initialWidth.setRange(0.5f, 1.5f);
        tree.rootLength.set(6f, 1f);
        tree.initialBranchingFactor.set(1.2f, 0.1f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf("living tree branchless.leaf");
        leaf.length.setRange(6f, 14f);
        leaf.width.setRange(3f, 7f);
        leaf.widest.set(0.5f, 0.1f);
        leaf.sides.set(5f, 1f);
        leaf.roundness.setRange(0.5f, 1.5f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.6f, 0.2f);

        final Doodad.TrunkNode rootTrunk = new Doodad.TrunkNode("living tree branchless.root trunk");

        rootTrunk.branchingFactor.setRange(0.2f, 0.5f);
        rootTrunk.lengthFactor.set(0.7f, 0.2f);
        rootTrunk.widthFactor.set(0.7f, 0.2f);
        rootTrunk.skew.set(0.0f, 0.2f);
        rootTrunk.addLeaf(Float.POSITIVE_INFINITY, leaf);

        tree.firstNode = rootTrunk;
        return tree;
    }

    private static Doodad createPineTreeA(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad("pine A");
        tree.initialWidth.setRange(0.3f, 1f);
        tree.rootLength.setRange(0.4f, 2f);
        tree.initialBranchingFactor.setRange(1f, 0f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode("pine A.trunk");
        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(1f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0f, 0f);
        tree.firstNode = trunk;

        final Doodad.TrunkNode trunkCap = new Doodad.TrunkNode("pine A.trunkCap");
        trunkCap.branchingFactor.setRange(0.4f, 0.9f);
        trunkCap.lengthFactor.set(1f, 0.2f);
        trunkCap.widthFactor.set(0.7f, 0.2f);
        trunkCap.skew.set(0f, 0f);
        trunk.addBranch(10f, trunkCap);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf("pine A.leaf");
        leaf.roundness.set(0f, 0f);
        leaf.length.setRange(9f, 17f);
        leaf.sides.setRange(3f, 5f);
        leaf.width.setRange(3f, 5f);
        leaf.widest.setRange(0.1f, 0.15f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.4f, 0.2f);
        trunk.addLeaf(10f, leaf);

        return tree;
    }

    private static Doodad createPineTreeB(Random random, WorldCharacteristics characteristics) {
        final float trunkBaseColor = characteristics.getRandomFudgedColor(random, WorldColors.TREE_TRUNK_COLORS);

        final Doodad tree = new Doodad("pine B");
        tree.initialWidth.setRange(0.3f, 1f);
        tree.rootLength.setRange(1f, 2f);
        tree.initialBranchingFactor.setRange(1f, 0f);
        tree.trunkColorHue.set(ColorKt.getHue(trunkBaseColor), 0.1f);
        tree.trunkColorSaturation.set(ColorKt.getSaturation(trunkBaseColor), 0.15f);
        tree.trunkColorBrightness.set(ColorKt.getBrightness(trunkBaseColor), 0.15f);

        final Doodad.TrunkNode rootTrunk = new Doodad.TrunkNode("pine B.rootTrunk");
        rootTrunk.branchingFactor.set(1f, 0f);
        rootTrunk.lengthFactor.set(1f, 0.2f);
        rootTrunk.widthFactor.set(0.7f, 0.2f);
        rootTrunk.skew.set(0f, 0.05f);

        final Doodad.TrunkNode innerTrunk = new Doodad.TrunkNode("pine B.innerTrunk");
        innerTrunk.branchingFactor.setRange(0.4f, 0.9f);
        innerTrunk.lengthFactor.set(1f, 0.2f);
        innerTrunk.widthFactor.set(0.7f, 0.2f);
        innerTrunk.skew.set(0f, 0.05f);

        final Doodad.HullLeaf leaf = new Doodad.HullLeaf("pine B.leaf");
        leaf.roundness.set(0f, 0f);
        leaf.length.setRange(0.2f, 0.1f);
        leaf.extraLengthFromTrunkFactor.setRange(1f, 2f);
        leaf.sides.setRange(3f, 5f);
        leaf.width.setRange(3f, 5f);
        leaf.widest.setRange(0.5f, 0.05f);
        leaf.hue.set(ColorKt.getHueGreen(), 0.1f);
        leaf.saturation.set(0.6f, 0.15f);
        leaf.brightness.set(0.5f, 0.2f);

        tree.firstNode = rootTrunk;
        rootTrunk.addBranch(Float.POSITIVE_INFINITY, innerTrunk);
        rootTrunk.addLeaf(Float.POSITIVE_INFINITY, leaf);
        innerTrunk.addBranch(1f, innerTrunk);
        innerTrunk.addLeaf(Float.POSITIVE_INFINITY, leaf);
        return tree;
    }

    private static Doodad createSpike(Random random, WorldCharacteristics characteristics) {
        final Doodad spike = new Doodad("spike");
        spike.initialWidth.setRange(0.5f, 3f);
        spike.rootLength.setRange(7f, 8f-characteristics.mood*3f);
        spike.initialBranchingFactor.setRange(1f, 1.3f);
        spike.trunkColorHue.set((ColorKt.getHueRed()) * 0.5f, 0.2f);
        spike.trunkColorBrightness.set(0.05f, 0.05f);
        spike.trunkColorSaturation.set(0.05f, 0.05f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode("spike.trunk");
        trunk.addBranch(1.2f, trunk);
        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(0.7f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.set(0.1f, 0.05f);
        spike.firstNode = trunk;

        return spike;
    }

    public static Array<Doodad> createDoodadSet(Random random, WorldCharacteristics characteristics) {
        final Array<Doodad> doodads = new Array<>();


        float mood = characteristics.mood;
        if (mood > -0.7f && mood < 0.1f) {
            doodads.add(createDeadTree(random, characteristics));
            doodads.add(createDeadTree(random, characteristics));
            doodads.add(createDeadTree(random, characteristics));
        }

        if (mood > -0.1f && mood < 0.7f) {
            doodads.add(createLivingTree(random, characteristics));
            doodads.add(createLivingTreeBranchless(random, characteristics));
            doodads.add(createLivingTreeBranchless(random, characteristics));
        }

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
