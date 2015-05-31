package darkyenus.lowscape

import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.graphics.{GL20, PerspectiveCamera}
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.{Gdx, InputProcessor, Screen}
import darkyenus.lowscape.world.LowscapeRenderableSorter
import darkyenus.lowscape.world.skybox.SkyboxRenderable

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

  val skyboxRenderable = new SkyboxRenderable

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
