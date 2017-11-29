package com.darkyen.paragrowth;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.darkyen.paragrowth.game.ParagrowthState;

/**
 * @author Darkyen
 */
public class ParagrowthMain extends Game {

    public static final AssetManager assetManager = new AssetManager(new LocalFileHandleResolver());

    @Override
    public void create() {
        final ShaderProgram batchShader = new ShaderProgram(
                Gdx.files.local("default_vert.glsl"),
                Gdx.files.local("default_frag.glsl")
        );
        if (!batchShader.isCompiled()) {
            throw new IllegalStateException("batchShader did not compile:\n"+batchShader.getLog());
        }
        final SpriteBatch spriteBatch = new SpriteBatch(1000, batchShader);
        assetManager.load("UISkin.json",Skin.class);
        assetManager.load("World.atlas",TextureAtlas.class);

        assetManager.finishLoading();

        final Skin defaultSkin = assetManager.get("UISkin.json");

        setScreen(new ParagrowthState(spriteBatch, defaultSkin));
    }

    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Lowscape");
        configuration.useOpenGL3(true, 3, 2);
        configuration.setWindowedMode(800, 600);

        new Lwjgl3Application(new ParagrowthMain(),configuration);
    }

}
