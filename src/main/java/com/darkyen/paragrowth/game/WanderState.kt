package com.darkyen.paragrowth.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.WorldCharacteristics
import com.darkyen.paragrowth.WorldSpecifics
import com.darkyen.paragrowth.doodad.DoodadWorld
import com.darkyen.paragrowth.input.GameInput
import com.darkyen.paragrowth.render.RenderBatch
import com.darkyen.paragrowth.skybox.Skybox
import com.darkyen.paragrowth.terrain.TERRAIN_TIME_ATTRIBUTE
import com.darkyen.paragrowth.terrain.TerrainPatchwork
import com.darkyen.paragrowth.util.*
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP

/**
 * @author Darkyen
 */
class WanderState(worldCharacteristics: WorldCharacteristics) : ScreenAdapter() {

    //2D
    private val hudView: ScreenViewport
    private val hudStage: Stage
    private val statsLabel: Label

    //3D
    private val modelBatch: RenderBatch
    private val worldCam: PerspectiveCamera
    private val worldView: ScreenViewport

    private val debugRenderer: ImmediateModeRenderer

    //3D Objects
    private val skyboxRenderable: Skybox

    private var nextWorldAlpha = 0f
    private var terrain: TerrainPatchwork
    private var nextTerrain: TerrainPatchwork? = null
    private var doodads: DoodadWorld
    private var nextDoodads: DoodadWorld? = null

    //Input
    private val gameInput: GameInput
    private val cameraController: HeightmapPersonController

    private val startTime = System.currentTimeMillis()

    private var rendered = 0

    init {
        println(worldCharacteristics)
        modelBatch = RenderBatch()
        worldCam = PerspectiveCamera(90f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        worldView = ScreenViewport(worldCam)
        hudView = ScreenViewport()
        hudStage = Stage(hudView, ParagrowthMain.batch())

        hudStage.root.isTransform = false
        skyboxRenderable = Skybox()

        debugRenderer = ImmediateModeRenderer20(5000, false, true, 0, DebugShader)

        worldCam.up.set(0f, 0f, 1f)
        worldCam.near = 0.4f
        worldCam.far = 500f

        worldCam.position.set(1f, 1f, 10f)
        worldCam.direction.set(1f, 0f, 0f)
        val hudTable = Table(ParagrowthMain.skin())
        statsLabel = Label("Stats!", ParagrowthMain.skin(), "font-ui", Color.WHITE)

        hudTable.setFillParent(true)
        hudTable.top().left()
        hudTable.add(statsLabel).top().left()
        hudStage.addActor(hudTable)

        //Terrain generation
        val worldSpecifics = WorldSpecifics(worldCharacteristics, 0f, 0f, false)
        terrain = TerrainPatchwork.build(worldSpecifics).get()
        doodads = DoodadWorld.build(worldCharacteristics.seed, worldSpecifics).get()

        cameraController = HeightmapPersonController(worldCam) { x, y ->
            val base = terrain.heightAt(x, y)
            val blend = nextTerrain?.heightAt(x, y) ?: base
            val alpha = modelBatch.attributes.getBlendAt(x, y)
            MathUtils.lerp(base, blend, alpha)
        }
        gameInput = GameInput(*cameraController.INPUT)
        gameInput.build()

        worldSpecifics.findInitialPosition(worldCam.position)
    }

    override fun show() {
        Gdx.input.inputProcessor = gameInput
        Gdx.input.isCursorCatched = true
        worldCam.update()
    }

    override fun hide() {
        Gdx.input.isCursorCatched = true
    }

    private var developingNextWorld:Delayed<Pair<TerrainPatchwork, DoodadWorld>>? = null

    override fun render(delta: Float) {
        val nextTerrain = nextTerrain
        if (nextTerrain != null) {
            nextWorldAlpha += delta / 10f
            if (nextWorldAlpha > 1f) {
                nextWorldAlpha = 0f

                terrain.dispose()
                terrain = nextTerrain
                this.nextTerrain = null

                doodads.dispose()
                doodads = nextDoodads!!
                nextDoodads = null
            }
        } else if (cameraController.CYCLE_TERRAIN_DEBUG.isPressed) {
            var developingNextWorld = developingNextWorld
            if (developingNextWorld == null) {
                val seed = System.currentTimeMillis()
                val centerX = worldCam.position.x
                val centerY = worldCam.position.y

                val worldCharacteristics = offload {
                    val characteristics = WorldCharacteristics.random(seed)
                    WorldSpecifics(characteristics, centerX, centerY, true)
                }
                val terrainPatchwork = worldCharacteristics.then { TerrainPatchwork.build(it) }
                val blendOut = worldCharacteristics.then { doodads.prepareBlendOut(it) }
                val doodadWorld = worldCharacteristics
                        .then { DoodadWorld.build(it.characteristics.seed, it) }
                        .then { it.prepareBlendIn(terrain.worldSpec) }

                developingNextWorld = terrainPatchwork.pairWith(doodadWorld).andWaitFor(blendOut)
                this.developingNextWorld = developingNextWorld
            }

            val new = developingNextWorld.poll()
            if (new != null) {
                this.developingNextWorld = null
                val (newTerrain, newDoodads) = new

                this.nextTerrain = newTerrain
                this.nextDoodads = newDoodads
                terrain.blendTo(newTerrain)
                modelBatch.attributes.setBlendWalls(worldCam)
                nextWorldAlpha = 0f
            }
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        GL32.glProvokingVertex(GL32.GL_FIRST_VERTEX_CONVENTION)// Needed for heightmap

        // Before camera controller update
        modelBatch.attributes.setBlend(nextWorldAlpha)

        updateWorld(delta)
        hudStage.act(delta)

        // Used for skybox and for objects too close to camera
        // (they won't get proper depth-testing, but skybox won't show through)
        Gdx.gl.glEnable(GL_DEPTH_CLAMP)
        modelBatch.attributes[TERRAIN_TIME_ATTRIBUTE][0] = (System.currentTimeMillis() - startTime) / 1000f
        terrain.setupGlobalAttributes(modelBatch)

        modelBatch.begin(worldCam)

        modelBatch.render(skyboxRenderable)
        modelBatch.render(terrain)

        doodads.render(modelBatch, worldCam, false)
        nextDoodads?.render(modelBatch, worldCam, true)

        rendered = modelBatch.end()
        Gdx.gl.glDisable(GL_DEPTH_CLAMP)

        if (cameraController.PATCHWORK_DEBUG.isPressed) {
            Gdx.gl.glDisable(GL_DEPTH_TEST)
            debugRenderer.begin(worldCam.combined, GL20.GL_LINES)

            doodads.renderDebug(worldCam, debugRenderer)

            debugRenderer.end()
            Gdx.gl.glEnable(GL_DEPTH_TEST)
        }

        if (cameraController.GENERAL_DEBUG.isPressed) {
            hudStage.draw()
        }
    }

    override fun resize(width: Int, height: Int) {
        worldView.update(width, height)
        hudView.update(width, height, true)
    }

    private fun updateWorld(delta: Float) {
        cameraController.update(delta)

        val stats = StringBuilder(128)
        stats.append("FPS: ").append(Gdx.graphics.framesPerSecond)
                .append("\nX: ").append(worldCam.position.x)
                .append("\nY: ").append(worldCam.position.y)
                .append("\nZ: ").append(worldCam.position.z)
                .append("\nRendered: ").append(rendered)
                .append("\n")

        statsLabel.setText(stats)
    }

    override fun dispose() {
        hudStage.dispose()
        skyboxRenderable.dispose()
        terrain.dispose()
        nextTerrain?.dispose()
        doodads.dispose()
    }
}
