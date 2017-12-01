/* ******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.darkyen.paragrowth.font;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Comparator;

/**
 * Simplified version of PixmapPacker from libGDX.
 */
@SuppressWarnings("WeakerAccess")
final class PixmapPacker implements Disposable {
    public final int pageWidth, pageHeight;
    public final Format pageFormat;
    public final int padding;
    /**
     * The default <code>color</code> of the whole {@link PixmapPacker.Page} when a new one created. Helps to avoid texture
     * bleeding or to highlight the page for debugging.
     */
    public final Color transparentColor = new Color(0f, 0f, 0f, 0f);
    public final Array<Page> pages = new Array<>();

    /**
     * Creates a new ImagePacker which will insert all supplied pixmaps into one or more <code>pageWidth</code> by
     * <code>pageHeight</code> pixmaps using the specified strategy.
     *
     * @param padding the number of blank pixels to insert between pixmaps.
     */
    public PixmapPacker(int pageWidth, int pageHeight, Format pageFormat, int padding) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.pageFormat = pageFormat;
        this.padding = padding;
    }

    /**
     * Sorts the images to the optimal order they should be packed. Some packing strategies rely heavily on the images being
     * sorted.
     */
    public static final Comparator<Pixmap> SORT_COMPARATOR = (o1, o2) -> o1.getHeight() - o2.getHeight();

    /**
     * Inserts the pixmap.
     *
     * @param rect Rectangle describing the area the pixmap was rendered to. Must not be null and must be a fresh instance.
     * @return page to which the image was added
     * @throws GdxRuntimeException in case the image did not fit due to the page size being too small or providing a duplicate
     *                             name.
     */
    public Page pack(final Pixmap image, final Rectangle rect) {
        rect.set(0, 0, image.getWidth(), image.getHeight());
        if (rect.getWidth() > pageWidth || rect.getHeight() > pageHeight) {
            throw new GdxRuntimeException("Page size too small for pixmap.");
        }

        final Page page = skylinePack(this, rect);
        page.dirty = true;
        page.image.setBlending(Blending.None);
        page.image.drawPixmap(image, (int) rect.x, (int) rect.y);

        return page;
    }

    /**
     * Disposes any pixmap pages which don't have a texture. Page pixmaps that have a texture will not be disposed until their
     * texture is disposed.
     */
    public void dispose() {
        for (Page page : pages) {
            page.image.dispose();
        }
    }

    /**
     * Does bin packing by inserting in rows. This is good at packing images that have similar heights.
     *
     * @author Nathan Sweet
     */
    private Page skylinePack(PixmapPacker packer, Rectangle rect) {
        int padding = packer.padding;
        int pageWidth = packer.pageWidth - padding * 2, pageHeight = packer.pageHeight - padding * 2;
        int rectWidth = (int) rect.width + padding, rectHeight = (int) rect.height + padding;
        for (int i = 0, n = packer.pages.size; i < n; i++) {
            Page page = packer.pages.get(i);
            Row bestRow = null;
            // Fit in any row before the last.
            for (int ii = 0, nn = page.rows.size - 1; ii < nn; ii++) {
                Row row = page.rows.get(ii);
                if (row.x + rectWidth >= pageWidth) continue;
                if (row.y + rectHeight >= pageHeight) continue;
                if (rectHeight > row.height) continue;
                if (bestRow == null || row.height < bestRow.height) bestRow = row;
            }
            if (bestRow == null) {
                // Fit in last row, increasing height.
                Row row = page.rows.peek();
                if (row.y + rectHeight >= pageHeight) continue;
                if (row.x + rectWidth < pageWidth) {
                    row.height = Math.max(row.height, rectHeight);
                    bestRow = row;
                } else {
                    // Fit in new row.
                    bestRow = new Row();
                    bestRow.y = row.y + row.height;
                    bestRow.height = rectHeight;
                    page.rows.add(bestRow);
                }
            }
            rect.x = bestRow.x;
            rect.y = bestRow.y;
            bestRow.x += rectWidth;
            return page;
        }
        // Fit in new page.
        Page page = new Page(packer.pages.size, packer);
        packer.pages.add(page);
        Row row = new Row();
        row.x = padding + rectWidth;
        row.y = padding;
        row.height = rectHeight;
        page.rows.add(row);
        rect.x = padding;
        rect.y = padding;
        return page;
    }

    /**
     * @author mzechner
     * @author Nathan Sweet
     * @author Rob Rendell
     */
    public static class Page {
        public final int index;
        public final Pixmap image;
        private final Array<Row> rows = new Array<>();
        public boolean dirty = false;

        /**
         * Creates a new page filled with the color provided by the {@link PixmapPacker#transparentColor}
         */
        Page(int index, PixmapPacker packer) {
            this.index = index;
            image = new Pixmap(packer.pageWidth, packer.pageHeight, packer.pageFormat);
            image.setColor(packer.transparentColor);
            image.fill();
        }
    }

    private static class Row {
        int x, y, height;
    }
}
