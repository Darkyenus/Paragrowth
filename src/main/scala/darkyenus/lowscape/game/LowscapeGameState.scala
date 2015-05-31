package darkyenus.lowscape.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.{VertexAttributes, Color}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.{DirectionalLight, PointLight}
import com.badlogic.gdx.graphics.g3d.utils.{FirstPersonCameraController, ModelBuilder}
import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.ui.{Label, Table}
import darkyenus.lowscape.world._
import darkyenus.lowscape.world.terrain.TerrainPatchwork
import darkyenus.lowscape.{LowscapeMain, LowscapeState}

/**
 * Private property.
 * User: Darkyen
 * Date: 08/12/14
 * Time: 16:53
 */
final class LowscapeGameState extends LowscapeState {

  private val worldCam = worldView.getCamera
  val terrain = new TerrainPatchwork(patchAmount = 1,patchSize = 256,worldCam)
  val cameraController = new HeightmapPersonController(worldCam,terrain)
  val environment = new Environment
  environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.4f, 0.6f, 1f))
  environment.add(new DirectionalLight().set(0.8f, 0.7f, 0.9f, -1f, -0.8f, -0.2f))
  val flashlight = new PointLight().set(1f,1f,0f,0f,1f,0f,15f)
  environment.add(flashlight)

  worldCam.up.set(0,0,1f)
  worldCam.near = 0.1f
  worldCam.far = 1000f

  worldCam.position.set(0, 0, 10f)
  worldCam.direction.set(1,0,0)

  val statsLabel = new Label("Stats!",LowscapeMain.skin,"font-ui-small",Color.WHITE)
  val hudTable = new Table()
  hudTable.setFillParent(true)
  hudTable.top().left()
  hudTable.add(statsLabel).top().left()
  hudStage.addActor(hudTable)

    //val loader = new DoodadLoader
    //val pineRegion = LowscapeMain.worldAtlas.findRegion("pine")
    //val model = loader.loadPaperModel(0.5f,pineRegion,pineRegion,pineRegion,pineRegion,pineRegion,pineRegion)

    //val instance = new DoodadModelInstance(model)
    //instance.position.setToTranslation(5f,1f,0f)

    //loader.refreshMeshes()

    /*for(i <- 0 until 10){
      world.staticDoodads.add(new StaticDoodad(model,new Vector3(5f*i,1f,0f)))
    }*/


  override def show(): Unit = {
    Gdx.input.setInputProcessor(cameraController)
    worldCam.update()

    terrain.generateMesh((x, y) => 25f * MathUtils.sinDeg(time*2f + (x * 180f) / 32) * MathUtils.sinDeg(time*2f - (y * 180f) / 32))
    terrain.updateMesh()
  }

  var time = 0f
  override def updateWorld(delta: Float): Unit = {
    time += delta
    //terrain.generateMesh((x, y) => 25f * MathUtils.sinDeg(time*18f + (x * 180f) / 128) * MathUtils.sinDeg(time*18f - (y * 180f) / 128))
    //terrain.updateMesh()

    cameraController.update(delta)

    flashlight.position.set(worldCam.position).mulAdd(worldCam.direction,2f)

    statsLabel.setText("FPS: "+Gdx.graphics.getFramesPerSecond
      +"\nX: "+worldCam.position.x
      +"\nY: "+worldCam.position.y
      +"\nZ: "+worldCam.position.z)
  }

  override def renderWorld(modelBatch: ModelBatch): Unit = {
    modelBatch.render(terrain,environment)
    //modelBatch.render(instance,environment)
  }
}
