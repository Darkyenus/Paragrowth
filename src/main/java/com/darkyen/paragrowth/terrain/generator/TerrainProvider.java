package com.darkyen.paragrowth.terrain.generator;

import com.badlogic.gdx.math.Vector3;

/**
 * Provides constant terrain.
 */
public interface TerrainProvider {

    /**
     * Get amount of units from 0 along X axis the terrain spans.
     */
    float getSizeX();

    /**
     * Get amount of units from 0 along Y axis the terrain spans.
     */
    float getSizeY();

    float getHeight(float x, float y);

    float getColor(float x, float y);

    default void getNormal(Vector3 to, float x, float y) {
        float here = getHeight(x, y);
        float up = getHeight(x + 1f, y);
        float right = getHeight(x, y + 1f);

        to.set(1f, 0f, up - here).crs(0f, 1f, right - here).nor();
    }

}
