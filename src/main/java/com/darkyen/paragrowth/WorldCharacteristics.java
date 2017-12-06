package com.darkyen.paragrowth;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.darkyen.paragrowth.util.ColorKt;

import java.util.Random;

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
     * Colors to be used in the world. Contains nulls to use random/natural color.
     */
    public final Array<Color> colors = new Array<>(Color.class);

    /**
     * @return random color or null, directly from {@link #colors}
     */
    public Color getRandomColor(Random random) {
        if (colors.size == 0) {
            return null;
        }
        return colors.get(random.nextInt(colors.size));
    }

    private boolean shouldUseCustomColor(Random random) {
        if (colors.size != 0) {
            final double customColorChance = Math.pow(1f - size / (colors.size + size), 0.25);
            if (random.nextFloat() < customColorChance) {
                return true;
            }
        }
        return false;
    }

    public float possiblyReplaceColor(Random random, float color) {
        if (shouldUseCustomColor(random)) {
            return ColorKt.fudge(getRandomColor(random).toFloatBits(), random, coherence, 1f);
        }
        return color;
    }

    public float getRandomFudgedColor(Random random, Color[][] template) {
        Color baseColor = null;

        if (shouldUseCustomColor(random)) {
            baseColor = getRandomColor(random);
        }

        if (baseColor == null) {
            baseColor = WorldColors.pick(template, random, mood);
        }

        return ColorKt.fudge(baseColor.toFloatBits(), random, coherence, 1f);
    }

    /**
     * Random seed of the world.
     */
    public long seed;

    public static WorldCharacteristics random() {
        final WorldCharacteristics c = new WorldCharacteristics();
        c.size = MathUtils.random(1f, 100f);
        c.mood = MathUtils.random(-1f, 1f);
        c.coherence = MathUtils.random(0f, 1f);
        final int colors = MathUtils.random((int)c.size);
        c.colors.ensureCapacity(colors);
        for (int i = 0; i < colors; i++) {
            c.colors.add(new Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f));
        }
        c.seed = MathUtils.random.nextLong();
        return c;
    }

    public static WorldCharacteristics fromText(CharSequence text) {
        final WorldCharacteristics c = new WorldCharacteristics();
        final int length = text.length();

        c.size = (int) Math.round(Math.pow(length, 0.6));
        final TextAnalyzer textAnalyzer = TextAnalyzer.get();
        c.mood = textAnalyzer.analyzePositivityAndNegativity(text);
        c.coherence = textAnalyzer.analyzeCoherence(text);
        textAnalyzer.analyzeColors(c.colors, text);

        // Fill up with random bytes, hopefully
        long seed = length * length * 31;
        seed = seed * seed;
        seed = seed * seed;
        seed = seed * seed;
        for (int i = 0; i < length; i++) {
            seed = 31 * seed + text.charAt(i);
        }
        c.seed = seed;
        return c;
    }

    @Override
    public String toString() {
        return "WorldCharacteristics{" +
                "size=" + size +
                ", mood=" + mood +
                ", coherence=" + coherence +
                ", colors=" + colors +
                ", seed=" + seed +
                '}';
    }
}
