package darkyenus.lowscape

import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.graphics.g3d.attributes.{DepthTestAttribute, ColorAttribute, IntAttribute}
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.Config
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.{Color, GL20, PerspectiveCamera}
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.{Gdx, InputProcessor, Screen}
import darkyenus.lowscape.world.LowscapeRenderableSorter

/**
 * Private property.
 * User: Darkyen
 * Date: 07/12/14
 * Time: 20:15
 */
abstract class LowscapeState extends Screen with InputProcessor {

  val batch = LowscapeMain.batch
  val modelBatch = new ModelBatch(new LowscapeRenderableSorter)

  val worldView = new ScreenViewport(new PerspectiveCamera(90f,Gdx.graphics.getWidth,Gdx.graphics.getHeight))
  val hudView = new ScreenViewport()
  val hudStage = new Stage(hudView,batch)
  hudStage.getRoot.setTransform(false)

  def updateWorld(delta:Float)

  def renderWorld(modelBatch: ModelBatch)

  val skyboxRenderable = {
    val modelBuilder = new ModelBuilder()
    val model = modelBuilder.createBox(2f, 2f, 2f,new Material(IntAttribute.createCullFace(GL20.GL_NONE), new DepthTestAttribute(0, false)), Usage.Position)
    val instance = new ModelInstance(model)
    val renderable = new Renderable
    instance.getRenderable(renderable)

    val config = new Config()
    config.fragmentShader = Gdx.files.local("sky.frag").readString()
    config.vertexShader = Gdx.files.local("sky.vert").readString()
    renderable.shader = new DefaultShader(renderable, config)
    renderable.shader.init()
    renderable.userData = LowscapeRenderableSorter.SKYBOX
    renderable
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)
    updateWorld(delta)
    hudStage.act(delta)

    modelBatch.begin(worldView.getCamera)

    modelBatch.render(skyboxRenderable)
    renderWorld(modelBatch)

    modelBatch.end()

    hudStage.draw()
  }

  override def resize(width: Int, height: Int): Unit = {
    worldView.update(width,height)
    hudView.update(width,height,true)
  }

  override def hide(): Unit = {}

  override def show(): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def dispose(): Unit = {}

  def keyDown(keycode: Int): Boolean = {
    false
  }

  def keyUp(keycode: Int): Boolean = {
    false
  }

  def keyTyped(character: Char): Boolean = {
    false
  }

  def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    false
  }

  def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
    false
  }

  def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = {
    false
  }

  def mouseMoved(screenX: Int, screenY: Int): Boolean = {
    false
  }

  def scrolled(amount: Int): Boolean = {
    false
  }
}
