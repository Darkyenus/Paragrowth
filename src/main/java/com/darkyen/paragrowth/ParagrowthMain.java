package com.darkyen.paragrowth;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.darkyen.paragrowth.game.WanderState;

/**
 * @author Darkyen
 */
public class ParagrowthMain extends Game {


    public static ParagrowthMain INSTANCE;
    {
        INSTANCE = this;
    }

    private static final AssetManager assetManager = new AssetManager(new LocalFileHandleResolver());
    private static Skin skin;
    private static SpriteBatch batch;

    @Override
    public void create() {
        final ShaderProgram batchShader = new ShaderProgram(
                Gdx.files.local("default_vert.glsl"),
                Gdx.files.local("default_frag.glsl")
        );
        if (!batchShader.isCompiled()) {
            throw new IllegalStateException("batchShader did not compile:\n"+batchShader.getLog());
        }
        ParagrowthMain.batch = new SpriteBatch(1000, batchShader);
        assetManager.load("UISkin.json",Skin.class);
        assetManager.finishLoading();

        ParagrowthMain.skin = assetManager.get("UISkin.json");

        setScreen(new WanderState(WorldCharacteristics.random()));
    }

    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Lowscape");
        configuration.useOpenGL3(true, 3, 2);
        configuration.setWindowedMode(800, 600);

        new Lwjgl3Application(new ParagrowthMain(),configuration);
    }

    public static SpriteBatch batch() {
        return batch;
    }

    public static Skin skin() {
        return skin;
    }
}
