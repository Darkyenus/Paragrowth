package darkyenus.lowscape

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.{Gdx, InputProcessor, Screen, Game}
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import com.badlogic.gdx.graphics.g2d.{TextureAtlas, Batch, SpriteBatch}
import darkyenus.lowscape.game.LowscapeGameState

/**
 * Private property.
 * User: Darkyen
 * Date: 07/12/14
 * Time: 20:09
 */
object LowscapeMain extends Game with App {

  private var spriteBatch:SpriteBatch = null
  private val assetManager = new AssetManager(new LocalFileHandleResolver)
  private var worldAtlasInst:TextureAtlas = null
  private var defaultSkin:Skin = null

  @inline
  def batch:Batch = spriteBatch

  @inline
  def skin:Skin = defaultSkin

  @inline
  def worldAtlas:TextureAtlas = worldAtlasInst

  override def create(): Unit = {
    spriteBatch = new SpriteBatch()
    assetManager.load("UISkin.json",classOf[Skin])
    assetManager.load("World.atlas",classOf[TextureAtlas])

    assetManager.finishLoading()

    defaultSkin = assetManager.get[Skin]("UISkin.json")
    worldAtlasInst = assetManager.get[TextureAtlas]("World.atlas")

    setScreen(new LowscapeGameState)
  }

  //Start application

  override def setScreen(screen: Screen): Unit = {
    screen match {
      case inputProcessor:InputProcessor =>
        Gdx.input.setInputProcessor(inputProcessor)
    }
    super.setScreen(screen)
  }

  private val configuration = new LwjglApplicationConfiguration
  configuration.title = "Lowscape"
  configuration.width = 800
  configuration.height = 600

  new LwjglApplication(this,configuration)
}
