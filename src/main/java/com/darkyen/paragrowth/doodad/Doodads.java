package com.darkyen.paragrowth.doodad;

/**
 *
 */
public class Doodads {
    public static final Doodad STICK = new Doodad();
    static {
        STICK.initialWidth.setRange(0.5f, 5f);
        STICK.rootLength.setRange(5f, 10f);
        STICK.initialBranchingFactor.setRange(1f, 1.3f);

        final Doodad.TrunkNode trunk = new Doodad.TrunkNode();
        trunk.branches.add(trunk);
        trunk.branchingProbability.add(1.2f);
        trunk.branchingFactor.setRange(0.4f, 0.9f);
        trunk.lengthFactor.set(0.7f, 0.2f);
        trunk.widthFactor.set(0.7f, 0.2f);
        trunk.skew.setRange(0f, 3f);
        STICK.firstNode = trunk;
    }
}
