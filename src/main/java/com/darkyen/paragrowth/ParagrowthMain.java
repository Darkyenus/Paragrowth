package com.darkyen.paragrowth;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.darkyen.paragrowth.game.LowscapeState;

/**
 * @author Darkyen
 */
public class ParagrowthMain extends Game {

    public static final AssetManager assetManager = new AssetManager(new LocalFileHandleResolver());

    @Override
    public void create() {
        final SpriteBatch spriteBatch = new SpriteBatch();
        assetManager.load("UISkin.json",Skin.class);
        assetManager.load("World.atlas",TextureAtlas.class);

        assetManager.finishLoading();

        final Skin defaultSkin = assetManager.get("UISkin.json");

        setScreen(new LowscapeState(spriteBatch, defaultSkin));
    }

    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Lowscape");
        configuration.setWindowedMode(800, 600);

        new Lwjgl3Application(new ParagrowthMain(),configuration);
    }

}
