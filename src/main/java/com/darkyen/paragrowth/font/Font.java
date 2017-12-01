package com.darkyen.paragrowth.font;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;

/**
 * Represents data for simple bitmap Font.
 */
@SuppressWarnings("WeakerAccess")
public final class Font implements Disposable {

    /** Textures of font's pages. */
    public final Texture[] pages;
    /** Typographic line height */
    public final int lineHeight;
    public final int lineGap;
    /** Distance from top to baseline  */
    public final int ascent;
    /** The distance from baseline to bottom */
    public final int descent;
    /** Loaded glyphs */
    public final IntMap<Glyph> glyphs;
    /** The glyph to display for characters not in the font. May be null. */
    public final Glyph missingGlyph;

    Font(Texture[] pages, int lineHeight, int lineGap, int ascent, int descent, IntMap<Glyph> glyphs) {
        this.pages = pages;
        this.lineHeight = lineHeight;
        this.lineGap = lineGap;
        this.ascent = ascent;
        this.descent = descent;
        this.glyphs = glyphs;
        this.missingGlyph = glyphs.get(0);
    }

    public boolean isWhitespace(int codepoint) {
        if (codepoint <= 0) return false;
        final Glyph glyph = glyphs.get(codepoint, missingGlyph);
        if (glyph == null) {
            return Character.isWhitespace(codepoint);
        } else {
            return glyph.u == glyph.u2 || glyph.v == glyph.v2;
        }
    }

    @Override
    public void dispose() {
        for (int i = 0; i < pages.length; i++) {
            pages[i].dispose();
            pages[i] = null;
        }
    }
}
