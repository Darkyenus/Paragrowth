package com.darkyen.paragrowth;

import com.badlogic.gdx.graphics.Color;

import java.util.Random;

/**
 *
 */
public class WorldColors {

    private static Color c(String hex) {
        return Color.valueOf(hex);
    }

    public static final Color[][] WATER = {
            // Evil
            {
                c("4ec400"), // Green
                c("00006d"), // Dark blue
                c("771d09") // Lava
            },
            // Normal
            {
                c("142bbc"), // Blue
                c("2f3977"), // Gray blue
                c("1f6b4d") // Green
            },
            // Good
            {
                c("2dbcb3"), // Azure
                c("dda6c9"), // Pink
                c("69acea") // Light blue
            }
    };

    public static final Color[][] BEACH = {
            // Evil
            {
                    c("3c442e"), // Very dark green
                    c("261e1e"), // Reddish black
            },
            // Normal
            {
                    c("d6d48d"), // Sandy yellow
                    c("aaaa92"), // Rocky gray
            },
            // Good
            {
                    c("ebef92"), // Idyllic yellow
                    c("d8d19c"), // Dull yellow
                    c("bcbbaf") // Happy pebble
            }
    };

    public static final Color[][] TERRAIN = {
            // Evil
            {
                    c("4c6b4e"), // Sick green
                    c("756f62"), // Dead soil
                    c("2b1919") // Volcanic black
            },
            // Normal
            {
                    c("227516"), // Grass
                    c("227516"), // Grass
                    c("298c1a"), // Grass
                    c("31592a"), // Dark grass
                    c("5f605f"), // Stone
                    c("c9aa95") // Desert sand
            },
            // Good
            {
                    c("44c455"), // Bright green
                    c("41c679"), // Bright green-azure
                    c("62c637") // Bright green-yellow
            }
    };

    public static final Color[][] TREE_TRUNK_COLORS = {
            // Evil
            {
                    c("382a13"), // Dead tree
            },
            // Normal
            {
                    c("704f16"), // Healthy bark
            },
            // Good
            {
                    c("825a16"), // Bright bark
                    c("9e917c"), // White bark
            }
    };

    public static final Color[][][] SKYBOX = {
            // Evil
            {
                    {c("000000"), c("888888")}, // WoW death-realm
                    {c("6f1200"), c("fc8300")}, // Fire
                    {c("fd282f"), c("1c547a")}, // Blood-sunset
            },
            // Normal
            {
                    {c("8a8795"), c("273d58")}, // Somewhat dark normal sky
                    {c("96aec3"), c("2557a9")}, // Normal-colored sky
                    {c("3f585e"), c("01101b")}, // Almost night
            },
            // Good
            {
                    {c("00FFFF"), c("0000FF")}, // Programmer art
                    {c("c92200"), c("0012d4")}, // Way too saturated sunset
                    {c("fbbfbb"), c("bd70a1")}, // Suspiciously pink
            }
    };

    public static <T> T pick(T[][] template, Random r, float mood) {
        int category = (int)Math.floor((1f + mood + r.nextFloat() * 0.1f - 0.05f) * template.length);
        if (category < 0) {
            category = 0;
        } else if (category >= template.length) {
            category = template.length - 1;
        }
        final T[] group = template[category];
        return group[r.nextInt(group.length)];
    }

}
