package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.font.Font;
import com.darkyen.paragrowth.font.FontLoader;
import com.darkyen.paragrowth.font.Glyph;
import com.darkyen.paragrowth.font.GlyphLayout;
import com.darkyen.paragrowth.util.ColorKt;

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

    private void updateGlyphLayout() {
        glyphLayout.setText(text, ColorKt.getWhite(), viewport.getWorldWidth() * 0.8f, Align.left);
    }

    @Override
    public void show() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        viewport.apply();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        final Glyph a = FONT2.glyphs.get('A');
        batch.draw(FONT2.pages[0], 100, 100, 100, 100);
        batch.draw(FONT2.pages[a.pageIndex], Gdx.input.getX(), Gdx.input.getY(), 100f, 100f, a.u, a.v, a.u2, a.v2);

        glyphLayout.draw(batch, viewport.getWorldWidth() * 0.1f, viewport.getWorldHeight()*0.5f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        glyphLayout = Gdx.graphics.getBackBufferWidth() > Gdx.graphics.getWidth() ? FONT2_GLYPHS : FONT_GLYPHS;

        updateGlyphLayout();
    }

    @Override
    public boolean keyTyped(char character) {
        if (character == 8) {
            text.setLength(Math.max(0, text.length() - 1));
        } else {
            text.append(character);
        }
        updateGlyphLayout();
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.DEL && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))) {
            text.setLength(0);
            updateGlyphLayout();
            return true;
        } else if (keycode == Input.Keys.ESCAPE) {
            ParagrowthMain.INSTANCE.setScreen(new WanderState(WorldCharacteristics.fromText(text)));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
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
