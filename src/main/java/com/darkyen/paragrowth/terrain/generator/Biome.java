package com.darkyen.paragrowth.terrain.generator;

import java.awt.*;

/**
 * @author Darkyen
 */
public enum Biome {
    SEA(new Color(0.0f, 0.0f, 0.5019608f)),
    RIVER(new Color(0.5019608f, 0.0f, 0.007843138f)),
    DESERT(new Color(0.5019608f, 0.49019608f, 0.20784314f)),
    GRASSLAND(new Color(0.050980393f, 0.5019608f, 0.0f)),
    TUNDRA(new Color(0.49019608f, 0.49411765f, 0.5019608f));

    public final Color debugColor;

    Biome(Color debugColor) {
        this.debugColor = debugColor;
    }
}
