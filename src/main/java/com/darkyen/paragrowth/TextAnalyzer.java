package com.darkyen.paragrowth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public final class TextAnalyzer {
    private static final String LOG = "TextAnalyzer";
    private TextAnalyzer() {
    }

    private final FileHandle wordsFolder = Gdx.files.local("words");

    private final Set<String> positiveWords = loadWordSet("positive.txt");
    private final Set<String> negativeWords = loadWordSet("negative.txt");

    private Set<String> loadWordSet(String fileName) {
        Set<String> result = new HashSet<>();
        try (BufferedReader reader = wordsFolder.child(fileName).reader(4096, "UTF-8")) {
            while (true) {
                final String word = reader.readLine();
                if (word == null) {
                    break;
                }
                if (!word.isEmpty()) {
                    result.add(word);
                }
            }
            Gdx.app.log(LOG, "Loaded "+result.size()+" words from "+fileName);
        } catch (IOException e) {
            Gdx.app.error(LOG, "Failed to load word set "+fileName, e);
        }
        return result;
    }

    private static final Pattern WORD_SPLIT = Pattern.compile("[\\W]+");

    public float analyzePositivityAndNegativity(CharSequence text) {
        final String[] words = WORD_SPLIT.split(text);
        if (words.length == 0) {
            return 0f;
        }
        int score = 0;
        for (String s : words) {
            final String word = s.toLowerCase();
            if (positiveWords.contains(word)) {
                score++;
            }
            if (negativeWords.contains(word)) {
                score--;
            }
        }

        return Math.copySign((float)Math.pow((double)Math.abs(score) / words.length, 1.0/4.0), score);
    }

    public float analyzeCoherence(CharSequence text) {
        int upperCase = 0;
        int lowerCase = 0;
        int whitespace = 0;
        int other = 0;

        IntArray wordLengths = new IntArray();
        long totalWordLengths = 0;
        int currentWordLength = 0;
        boolean inWord = false;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (Character.isLowerCase(c)) {
                lowerCase++;
            } else if (Character.isUpperCase(c)) {
                upperCase++;
            } else if (Character.isWhitespace(c)) {
                whitespace++;
            }  else if (!Character.isAlphabetic(c)) {
                other++;
            }

            if (Character.isWhitespace(c)) {
                if (inWord) {
                    // End word
                    inWord = false;
                    wordLengths.add(currentWordLength);
                    totalWordLengths += currentWordLength;
                }
                //else still waiting for word start
            } else {
                if (inWord) {
                    // Word continues
                    currentWordLength++;
                } else {
                    // Word starts
                    inWord = true;
                    currentWordLength = 1;
                }
            }
        }
        if (inWord) {
            // End last word
            wordLengths.add(currentWordLength);
            totalWordLengths += currentWordLength;
        }

        wordLengths.sort();
        final float averageWordLength;
        final int medianWordLength;
        if (wordLengths.size != 0) {
            averageWordLength = (float)((double)totalWordLengths / (double)wordLengths.size);
            medianWordLength = wordLengths.get(wordLengths.size / 2);
        } else {
            averageWordLength = 0f;
            medianWordLength = 0;
        }

        float upperCasePart = (float)upperCase / text.length();
        float lowerCasePart = (float)lowerCase / text.length();
        float whitespacePart = (float)whitespace / text.length();
        float otherPart = (float)other / text.length();

        /*
        Sample values:
        The Man Behind the Bars by Winifred Louise Taylor
        Upper: 0.018088255
        Lower: 0.775302
        White: 0.18113212
        Other: 0.025477638

        The Power of Movement in Plants by Charles Darwin and Sir Francis Darwin
        Upper: 0.015339996
        Lower: 0.7606471
        White: 0.1741886
        Other: 0.049824286

        Mix of multiple books
        Average: 4.592311
        Median: 4
        Upper: 0.019235987
        Lower: 0.74673826
        White: 0.18391201
        Other: 0.050113782
         */

//        System.out.println("Average: "+averageWordLength);
//        System.out.println("Median: "+medianWordLength);
//        System.out.println("Upper: "+upperCasePart);
//        System.out.println("Lower: "+lowerCasePart);
//        System.out.println("White: "+whitespacePart);
//        System.out.println("Other: "+otherPart);

        final float expectedAverage = 4.59f;
        final float expectedMedian = 4f;
        final float expectedUpper = 0.0185f;
        final float expectedLower = 0.746f;
        final float expectedWhite = 0.18f;
        final float expectedOther = 0.04f;

        final float averageDiff = averageWordLength - expectedAverage;
        final float medianDiff = medianWordLength - expectedMedian;
        final float upperDiff = upperCasePart - expectedUpper;
        final float lowerDiff = lowerCasePart - expectedLower;
        final float whiteDiff = whitespacePart - expectedWhite;
        final float otherDiff = otherPart - expectedOther;

        final float variance = averageDiff * averageDiff
                + medianDiff * medianDiff
                + upperDiff * upperDiff
                + lowerDiff * lowerDiff
                + whiteDiff * whiteDiff
                + otherDiff * otherDiff;

        return 20f / (variance + 20f);
    }

    private static TextAnalyzer INSTANCE;

    public static TextAnalyzer get() {
        if (INSTANCE == null) {
            INSTANCE = new TextAnalyzer();
        }
        return INSTANCE;
    }
}
