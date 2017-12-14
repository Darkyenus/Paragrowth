package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.TextAnalyzer;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.font.Font;
import com.darkyen.paragrowth.font.FontLoader;
import com.darkyen.paragrowth.font.GlyphLayout;
import com.darkyen.paragrowth.util.ColorKt;

import java.util.Random;

/**
 *
 */
public final class WriteState extends ScreenAdapter implements InputProcessor {

    private static final Font FONT2 = FontLoader.loadFont(Gdx.files.local("Avara.ttf"), 50, 2f);
    private static final GlyphLayout FONT2_GLYPHS = new GlyphLayout(FONT2, true);
    private static final Font FONT = FontLoader.loadFont(Gdx.files.local("Avara.ttf"), 50, 1f);
    private static final GlyphLayout FONT_GLYPHS = new GlyphLayout(FONT, true);

    private final ScreenViewport viewport = new ScreenViewport();

    private final SpriteBatch batch = ParagrowthMain.batch();
    private GlyphLayout glyphLayout = FONT_GLYPHS;
    private final StringBuilder text = new StringBuilder();
    private int caret = 0;
    private long lastTypeTime = 0;
    private static final long KEEP_ON_AFTER_TYPING_MS = 700;

    private Drawable caretDrawable;

    private void updateGlyphLayout() {
        glyphLayout.setText(text, ColorKt.getWhite(), viewport.getWorldWidth() * 0.8f, Align.left);
    }

    @Override
    public void show() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.input.setInputProcessor(this);

        if (caretDrawable == null) {
            caretDrawable = ParagrowthMain.skin().getDrawable("cursor");
        }
    }

    private final Rectangle caretRect = new Rectangle();
    @Override
    public void render(float delta) {
        viewport.apply();
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        final float textX = viewport.getWorldWidth() * 0.1f;
        final float textY = Math.max(viewport.getWorldHeight() * 0.9f, viewport.getWorldHeight() * 0.1f + glyphLayout.height);
        glyphLayout.draw(batch, textX, textY);

        final long now = System.currentTimeMillis();
        if (((now - lastTypeTime) % 1000) < 500 || (lastTypeTime + KEEP_ON_AFTER_TYPING_MS) >= now) {
            glyphLayout.getCaretPosition(caretRect, caret);
            caretDrawable.draw(batch, textX + caretRect.x, textY + caretRect.y, 3f, caretRect.height);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        glyphLayout = Gdx.graphics.getBackBufferWidth() > Gdx.graphics.getWidth() ? FONT2_GLYPHS : FONT_GLYPHS;

        updateGlyphLayout();
    }

    private void setCaret(int newCaret) {
        if (newCaret < 0) {
            caret = 0;
        } else if (newCaret > text.length()) {
            caret = text.length();
        } else {
            caret = newCaret;
        }
        lastTypeTime = System.currentTimeMillis();
    }

    private void insert(char c) {
        text.insert(caret, c);
        setCaret(caret + 1);
        updateGlyphLayout();
    }

    private void backspace() {
        if (caret > 0) {
            text.deleteCharAt(caret - 1);
            setCaret(caret - 1);
        }
        updateGlyphLayout();
    }

    private void delete() {
        if (caret < text.length()) {
            text.deleteCharAt(caret);
            setCaret(caret);
        }
        updateGlyphLayout();
    }

    @Override
    public boolean keyTyped(char character) {
        if (character == 8) {
            backspace();
        } else {
            insert(character);
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.LEFT) {
            setCaret(caret - 1);
            return true;
        } else if (keycode == Input.Keys.RIGHT) {
            setCaret(caret + 1);
            return true;
        } else if (keycode == Input.Keys.ESCAPE) {
            final WorldCharacteristics c = text.length() == 0 ? WorldCharacteristics.random() : WorldCharacteristics.fromText(text);
            ParagrowthMain.INSTANCE.setScreen(new WanderState(c));
            return true;
        } else if (keycode == Input.Keys.F3) {
            System.out.println(WorldCharacteristics.fromText(text));
        } else if (keycode == Input.Keys.F4) {
            final StringBuilder newText = new StringBuilder();
            final TextAnalyzer.Words words = new TextAnalyzer.Words(text);
            final TextAnalyzer analyzer = TextAnalyzer.get();

            while (true) {
                final int mark = words.mark();
                final Color color = analyzer.getColor(words, new Random());
                final int postColor = words.mark();
                if (color == null) {
                    assert mark == postColor;
                    final String next = words.next();
                    if (next == null) {
                        break;
                    }
                    newText.append(next).append(' ');
                } else {
                    newText.append('{').append('#').append(color.toString()).append('}');
                    words.rollback(mark);
                    while (words.mark() < postColor) {
                        newText.append(words.next()).append(' ');
                    }
                    newText.append("{}");
                }
            }

            text.setLength(0);
            text.append(newText);
            updateGlyphLayout();
            setCaret(text.length());
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
