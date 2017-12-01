package com.darkyen.paragrowth.terrain.generator;

/**
 * Provides constant terrain.
 */
public interface TerrainProvider {

    /**
     * Get amount of units from 0 along X axis the terrain spans.
     */
    float getWidth();

    /**
     * Get amount of units from 0 along Y axis the terrain spans.
     */
    float getHeight();

    float getHeight(float x, float y);

    float getColor(float x, float y);

}
