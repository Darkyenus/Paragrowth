package com.darkyen.paragrowth.font;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Packs font rasterized by stb true-type library
 */
public final class FontLoader {

	private static ByteBuffer loadFile(FileHandle file) {
		try {
			if (!file.exists()) throw new RuntimeException("File not found: " + file.file().getAbsolutePath());
			final ByteBuffer data = ByteBuffer.allocateDirect((int)file.length());
			final InputStream in = file.read();
			final byte[] buffer = new byte[4096];
			while (true) {
				final int size = in.read(buffer);
				if (size == -1) break;
				data.put(buffer, 0, size);
			}
			in.close();
			data.flip();
			return data;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Font loadFont (FileHandle font, int size, float scale) {
		final STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
		final ByteBuffer fontByteData = loadFile(font);
		if (!stbtt_InitFont(fontInfo, fontByteData, stbtt_GetFontOffsetForIndex(fontByteData, 0)))
			throw new RuntimeException("Init failed");

		final float ascent, descent, lineGap;
		{
			final IntBuffer ascentB = MemoryUtil.memAllocInt(1);
			final IntBuffer descentB = MemoryUtil.memAllocInt(1);
			final IntBuffer lineGapB = MemoryUtil.memAllocInt(1);
			stbtt_GetFontVMetrics(fontInfo, ascentB, descentB, lineGapB);

			final float metricScale = stbtt_ScaleForPixelHeight(fontInfo, size);

			ascent = ascentB.get(0) * metricScale;
			descent = descentB.get(0) * metricScale;
			lineGap = lineGapB.get(0) * metricScale;
		}

		final Array<GlyphData> glyphs = new Array<>();
		final int padding = 1;

		int glyphSurfaceArea = 0;
		for (int codePoint = 0; codePoint < 0x10FFFF; codePoint++) {
			final int glyphIndex = stbtt_FindGlyphIndex(fontInfo, codePoint);
			if (glyphIndex == 0 && codePoint != 0) continue;

			final GlyphData glyph = GlyphData.create(fontInfo, codePoint, glyphIndex, size, scale);
			glyphs.add(glyph);
			if (glyph.pixmap != null) {
				glyphSurfaceArea += (glyph.pixmap.getWidth() + padding) * (glyph.pixmap.getHeight() + padding);
			}
		}

		glyphs.sort((a, b) -> {
			if (a.pixmap == null && b.pixmap == null) {
				return 0;
			} else if (a.pixmap == null) {
				return -1;
			} else if (b.pixmap == null) {
				return 1;
			} else {
				return PixmapPacker.SORT_COMPARATOR.compare(a.pixmap, b.pixmap);
			}
		});

		// Guess best texture size
		glyphSurfaceArea += glyphSurfaceArea / 10;// Add 10% of packing garbage
		final int pageSurfaceAreaPower = findPower(MathUtils.nextPowerOfTwo(glyphSurfaceArea));
		final int pageHeightPower = pageSurfaceAreaPower / 2;
		final int pageWidthPower = pageSurfaceAreaPower - pageHeightPower;
		final int pageWidth = Math.max(64, 1 << pageWidthPower);
		final int pageHeight = Math.max(64, 1 << pageHeightPower);

		final PixmapPacker packer = new PixmapPacker(pageWidth, pageHeight, Pixmap.Format.Alpha, padding);

		for (GlyphData glyph : glyphs) {
			final Pixmap pixmap = glyph.pixmap;
			if (pixmap == null) continue;
			final PixmapPacker.Page page = packer.pack(pixmap, glyph.packed);
			glyph.page = page.index;
		}

		{
			final int pageCount = packer.pages.size;
			final Texture[] fontPages = new Texture[pageCount];
			for (int i = 0; i < pageCount; i++) {
				fontPages[i] = new Texture(packer.pages.get(i).image, Pixmap.Format.RGBA8888, false);
			}

			final IntMap<Glyph> fontGlyphs = new IntMap<>();
			for (GlyphData glyph : glyphs) {
				final int codePoint = glyph.codePoint;
				final int pageIndex = glyph.page;

				final int pX = (int) glyph.packed.x;
				final int pY = (int) glyph.packed.y;
				final int pWidth = (int) glyph.packed.width;
				final int pHeight = (int) glyph.packed.height;

				final int xOffset = glyph.pixmapXOff;
				final int yOffset = glyph.pixmapYOff;
				final int xAdvance = Math.round(glyph.advanceWidth);
				final int leftSideBearing = Math.round(glyph.leftSideBearing);

				final Texture page = fontPages[pageIndex];

				final Glyph fontGlyph = new Glyph(codePoint, pageIndex,
						pX / (float) page.getWidth(), pY / (float) page.getHeight(),
						(pX + pWidth) / (float) page.getWidth(), (pY + pHeight) / (float) page.getHeight(),
						glyph.width, glyph.height,
						xOffset, yOffset, xAdvance, leftSideBearing);

				fontGlyphs.put(codePoint, fontGlyph);
			}

			final int fontLineGap = Math.round(lineGap);
			final int fontAscent = Math.round(ascent);
			final int fontDescent = -Math.abs(Math.round(descent));//Just to make sure it is negative
			final int fontLineHeight = fontAscent - fontDescent + fontLineGap;

			return new Font(fontPages, fontLineHeight, fontLineGap, fontAscent, fontDescent, fontGlyphs);
		}
	}

	private static final class GlyphData {
		int codePoint;
		float advanceWidth, leftSideBearing;
		float width, height;
		int pixmapXOff, pixmapYOff;
		Pixmap pixmap;

		int page = 0;
		final Rectangle packed = new Rectangle();

		static GlyphData create(STBTTFontinfo fontInfo, int codePoint, int glyphIndex, int size, float scale) {
			final GlyphData result = new GlyphData();
			result.codePoint = codePoint;

			try (MemoryStack stack = stackPush()) {
				final IntBuffer advanceWidthBuf = stack.mallocInt(1);
				final IntBuffer leftSideBearingBuf = stack.mallocInt(1);

				final IntBuffer widthBuf = stack.mallocInt(1);
				final IntBuffer heightBuf = stack.mallocInt(1);
				final IntBuffer xOffBuf = stack.mallocInt(1);
				final IntBuffer yOffBuf = stack.mallocInt(1);

				stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidthBuf, leftSideBearingBuf);
				final float rawScale = stbtt_ScaleForPixelHeight(fontInfo, size);
				result.advanceWidth = advanceWidthBuf.get(0) * rawScale;
				result.leftSideBearing = leftSideBearingBuf.get(0) * rawScale;

				if (stbtt_IsGlyphEmpty(fontInfo, glyphIndex)) {
					result.pixmap = null;
				} else {
					final ByteBuffer bitmap = stbtt_GetGlyphBitmap(fontInfo, 0f, rawScale * scale, glyphIndex, widthBuf, heightBuf, xOffBuf, yOffBuf);
					result.pixmapXOff = Math.round(xOffBuf.get(0) / scale);
					result.pixmapYOff = Math.round(yOffBuf.get(0) / scale);
					int pixmapWidth = Math.round(widthBuf.get(0));
					int pixmapHeight = Math.round(heightBuf.get(0));
					result.width = pixmapWidth / scale;
					result.height = pixmapHeight / scale;

					if (bitmap != null) {
						final Pixmap pixmap = new Pixmap(pixmapWidth, pixmapHeight, Pixmap.Format.Alpha);
						final ByteBuffer pixels = pixmap.getPixels();
						pixels.put(bitmap);
						pixels.flip();
						result.pixmap = pixmap;
					}
				}
			}

			return result;
		}
	}

	private static int findPower(int number) {
    	int power = 0;
    	while (number != 0) {
    		number >>= 1;
    		power++;
		}
		return power;
	}
}
