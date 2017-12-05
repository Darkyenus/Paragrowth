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

    private static Doodad createSpike(Random random, WorldCharacteristics characteristics) {
        final Doodad spike = new Doodad();
        spike.initialWidth.setRange(0.5f, 8f);
        spike.rootLength.setRange(7f, 14f);
        spike.rootLength.value *= -characteristics.mood;
        spike.initialBranchingFactor.setRange(1f, 1.3f);
        spike.trunkColorHue.set((ColorKt.getHueOrange() + ColorKt.getHueRed()) * 0.5f, 0.2f);
        spike.trunkColorBrightness.set(0.1f, 0.15f);
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

        return doodads;
    }
}
