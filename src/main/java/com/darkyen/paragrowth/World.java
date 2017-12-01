package com.darkyen.paragrowth;

import com.darkyen.paragrowth.doodad.DoodadWorld;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;

/**
 *
 */
public class World {
    public final DoodadWorld doodadWorld;
    public final TerrainPatchwork terrain;

    public World(DoodadWorld doodadWorld, TerrainPatchwork terrain) {
        this.doodadWorld = doodadWorld;
        this.terrain = terrain;
    }
}
