package darkyenus.lowscape;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import darkyenus.lowscape.game.LowscapeState;

/**
 * @author Darkyen
 */
public class LowscapeMain extends Game {

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
        LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = "Lowscape";
        configuration.width = 800;
        configuration.height = 600;

        new LwjglApplication(new LowscapeMain(),configuration);
    }

}
