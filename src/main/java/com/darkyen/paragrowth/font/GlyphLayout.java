
package com.darkyen.paragrowth.font;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;
import com.darkyen.paragrowth.util.ColorKt;

/**
 *
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class GlyphLayout {

	private static final char C_OPEN = '{', C_CLOSE = '}';

	private final Font font;
	private final boolean markupEnabled;

	private final FloatArray colorStack = new FloatArray(4);
	private final Array<GlyphRun> runs = new Array<>();
	public float width, height;
	public int lines = 0;

	public GlyphLayout (Font font, boolean markupEnabled) {
		this.font = font;
		this.markupEnabled = markupEnabled;
	}

	private float getTabPositionOf (Font data, float x) {
		final float tabWidth = data.lineHeight * 2.5f;
		return (MathUtils.ceil(x / tabWidth) + (MathUtils.isZero(x % tabWidth) ? 1 : 0)) * tabWidth;
	}

	private GlyphRun pushRun (float targetWidth, GlyphRun previous, float color, boolean forceNewLine) {
		final Array<GlyphRun> runs = this.runs;
		// Place previous to the correct spot
		if (previous == null) {
			// First one, clear runs
			CHAR_RUN_POOL.freeAll(runs);
			runs.clear();

			height = font.lineHeight;
			lines = 1;
		} else {
			GlyphRun wrapped = null;

			final IntArray prevCodePoints = previous.codepoints;
			if (prevCodePoints.size != 0) {
				// Set it up to the correct space
				if (runs.size == 0) {
					previous.x = 0;
					previous.lineIndex = 0;
				} else {
					final GlyphRun lastStable = runs.peek();
					previous.x = lastStable.x + lastStable.width;
					previous.lineIndex = lastStable.lineIndex;
				}

				if (forceNewLine || previous.hasLinebreak()) {
					previous.x = 0;
					previous.lineIndex++;
					height += font.lineHeight;
					lines++;
				} else if (previous.hasTab()) {
					previous.x = getTabPositionOf(font, previous.x);
				}

				// Let previous run figure out its size and we will wrap it, if needed
				previous.computeSize(font);
				if (previous.x + previous.width > targetWidth) {
					final int originalSplitPoint;
					int splitPoint = originalSplitPoint = Math.min(previous.findWrapIndexAt(targetWidth - previous.x),
						previous.codepoints.size - 1);
					while (splitPoint != 0 && !font.isWhitespace(prevCodePoints.get(splitPoint))) {
						splitPoint--;
					}

					if (splitPoint == 0) {
						final boolean firstOnLine = runs.size == 0 || runs.peek().lineIndex < previous.lineIndex;
						if (firstOnLine) {
							// This is the first run on the line and we MUST break words
							final int MIN_LETTERS = 3;
							splitPoint = Math.max(originalSplitPoint, MIN_LETTERS);
						} else {
							// Move whole run one line down
							assert !forceNewLine;
							return pushRun(targetWidth, previous, color, true);
						}
					}

					// Wrapping will have to happen
					if (splitPoint < prevCodePoints.size) {
					    // Create new run with wrapped characters
						wrapped = CHAR_RUN_POOL.obtain();
						wrapped.color = previous.color;
						wrapped.codepoints.addAll(prevCodePoints, splitPoint, prevCodePoints.size - splitPoint);
						wrapped.isWrapOverflow = true;

						prevCodePoints.size = splitPoint;
						// Recompute width of our previous, because we messed with it
						previous.computeSize(font);
					}
				}

				width = Math.max(width, previous.x + previous.width);
				runs.add(previous);
			} else {
				CHAR_RUN_POOL.free(previous);
			}

			if (wrapped != null) {
				return pushRun(targetWidth, wrapped, color, true);
			}
		}

		final GlyphRun newRun = CHAR_RUN_POOL.obtain();
		newRun.color = color;
		return newRun;
	}

	public void setText (CharSequence str, float color, float targetWidth, int hAlign) {
		setText(str, 0, str.length(), color, targetWidth, hAlign);
	}

	/** @param color of the text without markup changes
	 * @param hAlign Horizontal alignment of the text, see {@link Align}.
	 * @param targetWidth The width used for alignment, line wrapping, and truncation. May be zero if those features are not used. */
	public void setText (CharSequence str, int start, int end, float color, float targetWidth, int hAlign) {
		{// Setup & cleanup
			width = 0f;
			height = 0f;

			if (targetWidth == 0) // Simplifies code
				targetWidth = Float.POSITIVE_INFINITY;

			assert str != null;
			assert start >= 0;
			assert start <= end;
			assert end <= str.length();
		}

		final FloatArray colorStack = this.colorStack;
		GlyphRun run = pushRun(targetWidth, null, color, false);

		for (int i = start; i < end; i++) {
			final int c;
			{
				final char high = str.charAt(i);
				if (Character.isHighSurrogate(high)) {
					final char low = str.charAt(i + 1);
					if (Character.isLowSurrogate(low)) {
						i++;
						c = Character.toCodePoint(high, low);
					} else {
						System.err.println("Invalid dangling high surrogate in '" + str + "' at index " + (i + 1));
						c = 0;
					}
				} else if (Character.isLowSurrogate(high)) {
					System.err.println("Invalid dangling low surrogate in '" + str + "' at index " + i);
					c = 0;
				} else {
					c = high;
				}
			}

			markup:
			if (markupEnabled && c == C_OPEN && i + 1 < end) {
				if (str.charAt(i + 1) == C_OPEN) {
					run.pushCodepoint('\0');
					i++;
					break markup;
				}

				// Start of markup!
				final int tagContentStart = i + 1;
				int tagContentEnd = tagContentStart;
				while (true) {
					if (str.charAt(tagContentEnd) == C_CLOSE) {
						break;
					}
					tagContentEnd++;
					if (tagContentEnd == end) {
						// This markup tag is opened, but never closed, show it in full
						break markup;
					}
				}

				// Process the tag
				if (!handleColorTag(str, tagContentStart, tagContentEnd - tagContentStart, ColorKt.getAlpha(color))) {
					break markup;
				}

				// Crate new run
				if (colorStack.size == 0) {
					run = pushRun(targetWidth, run, color, false);
				} else {
					run = pushRun(targetWidth, run, colorStack.peek(), false);
				}

				// Add placeholder chars for skipped characters
				// and leave i at the index of ]
				for (; i < tagContentEnd; i++) {
					run.pushCodepoint('\0');
				}

				assert i == tagContentEnd;

				continue;
			}

			if (c == '\n' || c == '\t') {
				// Crate new run
				if (colorStack.size == 0) {
					run = pushRun(targetWidth, run, color, false);
				} else {
					run = pushRun(targetWidth, run, colorStack.peek(), false);
				}
			}

			run.pushCodepoint(c);
		}

		// Push last run
		CHAR_RUN_POOL.free(pushRun(targetWidth, run, color, false));
		colorStack.clear();

		// Align runs to center or right of targetWidth.
		final Array<GlyphRun> runs = this.runs;
		final int runsSize = runs.size;

		if ((hAlign & Align.left) == 0) { // Not left aligned, so must be center or right aligned.
			final float alignTargetWidth = targetWidth == Float.POSITIVE_INFINITY ? width : targetWidth;
			final boolean center = (hAlign & Align.center) != 0;
			float lineWidth = 0;
			int currentLine = -1;
			int lineStart = 0;
			for (int i = 0; i < runsSize; i++) {
				final GlyphRun r = runs.get(i);
				if (r.lineIndex != currentLine) {
					currentLine = r.lineIndex;
					float shift = alignTargetWidth - lineWidth;
					if (center) shift /= 2;
					while (lineStart < i)
						runs.get(lineStart++).x += shift;
					lineWidth = 0;
				}
				lineWidth = Math.max(lineWidth, r.x + r.width);
			}
			float shift = alignTargetWidth - lineWidth;
			if (center) shift /= 2;
			while (lineStart < runsSize)
				runs.get(lineStart++).x += shift;
		}
	}

	private void pushColorStack (float r, float g, float b, float a) {
		colorStack.add(ColorKt.rgb(r, g, b, a));
	}

	private int hexCharValue (char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		else if (c >= 'a' && c <= 'f')
			return 10 + (c - 'a');
		else if (c >= 'A' && c <= 'F')
			return 10 + (c - 'A');
		else
			return -1; // Unexpected character in hex color.
	}

	private float hexToFloat (char c) {
		return hexCharValue(c) / 15f;
	}

	private float hexToFloat (char c1, char c2) {
		return (hexCharValue(c1) << 4 | hexCharValue(c2)) / 255f;
	}

	private boolean handleColorTag (CharSequence tagText, int offset, int length, float defaultAlpha) {
		if (length == 0) {
			// Empty tag, pop
			if (colorStack.size > 0) {
                colorStack.pop();
                return true;
            } else
                return false;
		} else if (length == 1 && tagText.charAt(offset) == '~') {
			// [~] = pop all
			colorStack.clear();
			return true;
		} else if (tagText.charAt(offset) == '#') {
			offset++;
			length--;
			float r, g, b, a = defaultAlpha;
			switch (length) {
			case 4:
				a = hexToFloat(tagText.charAt(offset + 3));
			case 3:
				r = hexToFloat(tagText.charAt(offset));
				g = hexToFloat(tagText.charAt(offset + 1));
				b = hexToFloat(tagText.charAt(offset + 2));
				break;
			case 8:
				a = hexToFloat(tagText.charAt(offset + 6), tagText.charAt(offset + 7));
			case 6:
				r = hexToFloat(tagText.charAt(offset), tagText.charAt(offset + 1));
				g = hexToFloat(tagText.charAt(offset + 2), tagText.charAt(offset + 3));
				b = hexToFloat(tagText.charAt(offset + 4), tagText.charAt(offset + 5));
				break;
			default:
				return false;
			}
			pushColorStack(r, g, b, a);
			return true;
		} else {
			// Parse named color.
			final String colorName = tagText.subSequence(offset, offset + length).toString().toUpperCase();
			final Color color = Colors.get(colorName);
			if (color == null) {
				return false;
			} else {
				pushColorStack(color.r, color.g, color.b, color.a == 1f ? defaultAlpha : color.a);
				return true;
			}
		}
	}

	private int displayLineHeight() {
		return font.ascent + -font.descent;
	}

	public void getCaretPosition(Rectangle out, int index) {
		if (index < 0) {
			index = 0;
		}

		GlyphRun run = null;
		final Array<GlyphRun> runs = this.runs;
		for (int i = 0; i < runs.size; i++) {
			final GlyphRun runI = runs.get(i);
			final int runCodepoints = runI.codepoints.size;
			if (index <= runCodepoints) {
				run = runI;
				break;
			}
			index -= runCodepoints;
		}
		if (run == null && runs.size != 0) {
			run = runs.get(runs.size-1);
			index = run.codepoints.size;
		}

		float indexX = 0f;
		int runLineIndex = 0;
		if (run != null) {
			indexX = run.x + run.glyphXPos.get(index);
			runLineIndex = run.lineIndex;
		}

		out.x = indexX;
		out.y = -font.ascent - runLineIndex * font.lineHeight;
		out.width = 0f;
		out.height = font.lineHeight;
	}

	/**
	 * @deprecated Untested */
	@Deprecated
    public int getFirstRangeBounds(Rectangle rectangle, int start, int end){
        final Array<GlyphRun> runs = this.runs;
        if(runs.size == 0 || start > end){
            rectangle.set(0f, -displayLineHeight(), 0f, displayLineHeight());
            return 0;
        }
        // Empty ranges prefer to be on the end of the run than on the beginning of a new one
        final boolean leftAffine = start == end;
        //Find start
        int included = 0;
        int firstRunIndex;
        GlyphRun firstRun = null;
        for (firstRunIndex = 0; firstRunIndex < runs.size; firstRunIndex++) {
            final GlyphRun run = runs.get(firstRunIndex);
            if(leftAffine ? run.codepoints.size < start : run.codepoints.size <= start){
                start -= run.codepoints.size;
                end -= run.codepoints.size;
            }else{
                firstRun = run;
                break;
            }
        }
        if(firstRun == null){
            rectangle.set(0f, -font.descent, 0f, font.descent + Math.abs(font.ascent));
            return 0;
        }

        rectangle.x = firstRun.x + firstRun.glyphXPos.get(start);
        rectangle.y = firstRun.lineIndex * font.lineHeight - font.descent;

        rectangle.width = 0f;
        rectangle.height = displayLineHeight();

        GlyphRun lastRun = firstRun;
        if(end > firstRun.codepoints.size){
            included += firstRun.codepoints.size - start;

            for (int lastRunIndex = firstRunIndex+1; lastRunIndex < runs.size; lastRunIndex++) {
                final GlyphRun nextRun = runs.get(lastRunIndex);
                if(nextRun.lineIndex != firstRun.lineIndex){
                    //This run is on different line, dismiss search
                    break;
                }else if(nextRun.codepoints.size >= end){
                    //This run is good, but we need more
                    end -= nextRun.codepoints.size;
                    included += nextRun.codepoints.size;
                    lastRun = nextRun;
                }else{
                    //This is exactly the run we need
                    included += end;
                    lastRun = nextRun;
                    break;
                }
            }

            //Adjust if end is larger than all text combined
            end = Math.min(end, lastRun.codepoints.size);
        }else{
            included += end - start;
        }

        final float endX = lastRun.x + lastRun.glyphXPos.get(end);
        rectangle.width = endX - rectangle.x;

        return included;
    }

    /**
	 * @deprecated Untested */
    @Deprecated
    public int getIndexAt(float x, float y){
        final Array<GlyphRun> runs = this.runs;
        if(runs.size == 0)return -1;
        final float lineHeight = font.lineHeight;
        y -= font.ascent;
        int offset = 0;
        for (GlyphRun run : runs) {
            if(run.x <= x && run.x + run.width >= x && run.lineIndex /* y */ >= y && y >= run.lineIndex /* y */ - lineHeight){
                return offset + run.findIndexAt(x - run.x);
            }else{
                offset += run.codepoints.size;
            }
        }
        return -1;
    }

	/**
	 * @deprecated Untested */
	@Deprecated
    public int getClosestIndexTo(float x, float y) {
        final Array<GlyphRun> runs = this.runs;
        if(runs.size == 0)return 0;
        final float lineHeight = font.lineHeight;
        y -= font.ascent;

        if(y >= 0f) return 0;

        int offset = 0;
        for (GlyphRun run : runs) {
            if(y < run.lineIndex /* y */ - lineHeight){
                // This run is still above these coords
                offset += run.codepoints.size;
            } else if(y > run.lineIndex/* y */) {
                // This run is already below coords
                return offset;
            } else {
                // We are at the correct line
                if(x < run.x) {
                    // Coords to the left
                    return offset;
                } else if(x == run.x && offset != 0) {
                    // Zero-th char is actually on new line (hence +1)
                    // First line doesnt have line break char...
                    return offset + 1;
                } else if(x < run.x + run.width) {
                    // Coords directly here
                    return offset + run.findIndexAt(x - run.x);
                } else {
                    // Coords to the right
                    return offset + run.codepoints.size;
                }
            }
        }
        return offset;
    }

    /** @param x/y where upper left corner of the text should be drawn */
	public void draw (Batch batch, float x, float y) {
		for (GlyphRun run : runs) {
			final float color = run.color;

			final int glyphCount = run.glyphs.size;
			assert run.glyphXPos.size >= glyphCount;

			final float runX = run.x + x;
			final float runYBaseline = y - font.ascent - run.lineIndex * font.lineHeight - font.descent;

			for (int g = 0; g < glyphCount; g++) {
				final Glyph glyph = run.glyphs.get(g);
				if (glyph == null) continue;
				final float glyphXOffset = run.glyphXPos.get(g) + glyph.xOffset;

				batch.setColor(color);
				batch.draw(font.pages[glyph.pageIndex],
						runX + glyphXOffset,
						runYBaseline - glyph.yOffset,
						glyph.width,
						-glyph.height,
						glyph.u, glyph.v,
						glyph.u2, glyph.v2
						);
			}
		}
	}

	public void reset () {
		CHAR_RUN_POOL.freeAll(runs);
		runs.clear();

		width = 0;
		height = 0;

		lines = 0;
	}

	private static Pool<GlyphRun> CHAR_RUN_POOL = Pools.get(GlyphRun.class);

	private static final class GlyphRun implements Pool.Poolable {

		// Set by GlyphLayout
		/** Codepoints inside this run, may contain special ignore chars and cheese */
		final IntArray codepoints = new IntArray(true, 64);
		/** Position of this run. Not used for size computation, just for bookkeeping */
		float x;
		/** Color of this whole run */
		float color = ColorKt.getWhite();
		/** true - this glyph run exists because of overflow (changes how is leading whitespace treated) */
		boolean isWrapOverflow = false;
		/** On which line this run is? */
		int lineIndex = -1;

		// Set by computeSize
		/** Glyphs to be drawn at corresponding positions. May contain nulls. */
		final Array<Glyph> glyphs = new Array<>();
		/** Contains the positions of all codepoints relative to this.x + one additional element of last position */
		final FloatArray glyphXPos = new FloatArray();
		/** Width of this run */
		float width;


		/** True if this starts with \n. One run can contain only one linebreak, {@link #pushRun(float, GlyphRun, float, boolean)}
		 * takes care of that.  */
		boolean hasLinebreak () {
			return codepoints.size > 0 && codepoints.first() == '\n';
		}

		/** True if this starts with \t. One run can contain only one tab, {@link #pushRun(float, GlyphRun, float, boolean)} takes
		 * care of that.  */
		boolean hasTab () {
			return codepoints.size > 0 && codepoints.first() == '\t';
		}

		void pushCodepoint(int c) {
			codepoints.add(c);
		}

		/** Index that user probably meant when clicked position at x. */
		private int findIndexAt (float x) {
			final IntArray codepoints = this.codepoints;
			final FloatArray charXPos = this.glyphXPos;
			if (codepoints.size <= 1) return 0;
			if (x < charXPos.first()) return 0;
			if (x > charXPos.peek()) return codepoints.size;
			for (int i = codepoints.size - 1; i >= 0; i--) {
				final float thisPos = charXPos.get(i);
				final float nextPos = charXPos.get(i + 1);
				if (x > thisPos) {
					if (x - thisPos < (nextPos - x)) {
						return i;
					} else {
						return i + 1;
					}
				}
			}
			return 0;
		}

		/** Index that line should wrap at if the line end is at x. Similar to findIndexAt but rounds always down. Does not take into
		 * account content/wrap points, only raw positions. */
		private int findWrapIndexAt (float x) {
			final IntArray codepoints = this.codepoints;
			final FloatArray charXPos = this.glyphXPos;
			if (codepoints.size <= 1) return 0;
			if (x < charXPos.first()) return 0;
			if (x > charXPos.peek()) return codepoints.size;
			for (int i = codepoints.size - 1; i >= 0; i--) {
				final float thisPos = charXPos.get(i);
				if (x >= thisPos) {
					return i;
				}
			}
			return 0;
		}

		private boolean isSpecialDiscardChar (int c) {
			switch (c) {
			case '\0': // Pushed in place of color tags so the indices match input text
			case '\n':
			case '\t':
				return true;
			default:
				return false;
			}
		}

		/** Computes size of this GlyphLayout, populating the glyph  */
		void computeSize (Font font) {
			final IntArray codepoints = this.codepoints;
			final int length = codepoints.size;

			final FloatArray charXPos = this.glyphXPos;
			float nextGlyphXPos = 0f;

			final Array<Glyph> glyphs = this.glyphs;

			// Reset
			glyphs.clear();
			charXPos.clear();

			// Estimate
			glyphs.ensureCapacity(length);
			charXPos.ensureCapacity(length);

			/* Overflow newlines should ignore whitespace which overflowed */
			boolean discardWhitespace = isWrapOverflow;

			for (int i = 0; i < length; i++) {
				final int ch = codepoints.get(i);
				Glyph glyph = null;
				if (!isSpecialDiscardChar(ch)) {
					glyph = font.glyphs.get(ch, font.missingGlyph);
				}

				final float xAdvance;
				final float leftSideBearing;

				if (glyph == null || (discardWhitespace && font.isWhitespace(ch))) {
					xAdvance = 0f;
					leftSideBearing = 0f;
				} else {
					discardWhitespace = false;
					xAdvance = glyph.xAdvance;
					leftSideBearing = glyph.leftSideBearing; // Kerning would be added here
				}

				glyphs.add(glyph);
				charXPos.add(nextGlyphXPos + leftSideBearing);
				nextGlyphXPos += xAdvance;
			}

			charXPos.add(width = nextGlyphXPos);
		}

		@Override
		public void reset () {
			glyphs.clear();
			width = 0;
			codepoints.clear();
			glyphXPos.clear();
			isWrapOverflow = false;
			lineIndex = -1;
		}
	}
}
