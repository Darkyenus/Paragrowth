package com.darkyen.paragrowth;

import com.badlogic.gdx.math.MathUtils;

/**
 * Parameters of the world, derived from whatever.
 */
public class WorldCharacteristics {

    /**
     * Size of the world.
     *
     * From 0 to +inf.
     * 0 -> Tiny
     * 100 -> Medium
     * 1000 -> Large
     */
    public float size;

    /**
     * Mood of the world, positive values are light, negative values are dark.
     *
     * From -1 to 1.
     */
    public float mood;

    /**
     * Coherence of the world. Controls smoothness of the terrain, glitches, etc.
     *
     * From 0 to 1.
     */
    public float coherence;

    /**
     * Random seed of the world.
     */
    public long seed;

    public static WorldCharacteristics random() {
        final WorldCharacteristics c = new WorldCharacteristics();
        c.size = MathUtils.random(1f, 100f);
        c.mood = MathUtils.random(-1f, 1f);
        c.coherence = MathUtils.random(0f, 1f);
        c.seed = MathUtils.random.nextLong();
        return c;
    }

    public static WorldCharacteristics fromText(CharSequence text) {
        return WorldCharacteristics.random();
    }

    @Override
    public String toString() {
        return "WorldCharacteristics{" +
                "size=" + size +
                ", mood=" + mood +
                ", coherence=" + coherence +
                ", seed=" + seed +
                '}';
    }
}
