package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.math.MathUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Darkyen
 */
public class TerrainIO {

    private static float getBrightness(Terrain terrain, int x, int y){
        float myHeight = terrain.getHeight(x,y);
        float topHeight = terrain.getHeight(x, y-1);
        float leftHeight = terrain.getHeight(x-1, y);
        final float MAX_HEIGHT_DELTA = 1f;
        return MathUtils.clamp(((topHeight + leftHeight) * 0.5f - myHeight + 0.5f)/ MAX_HEIGHT_DELTA, 0f, 1f) * 0.8f + 0.1f;
    }

    public static BufferedImage toImage(Terrain terrain){
        final BufferedImage result = new BufferedImage(terrain.size, terrain.size, BufferedImage.TYPE_4BYTE_ABGR);
        terrain.foreach((x,y) -> {
            Biome biome = terrain.getBiome(x,y);
            float brightness = getBrightness(terrain, x,y);
            result.setRGB(x,y,
                    new Color(
                            biome.debugColor.getRed() / 255f * brightness,
                            biome.debugColor.getGreen() / 255f * brightness,
                            biome.debugColor.getBlue() / 255f * brightness
                    ).getRGB());
        });
        return result;
    }
}
