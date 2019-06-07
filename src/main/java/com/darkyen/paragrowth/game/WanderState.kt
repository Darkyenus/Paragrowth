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
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.WorldCharacteristics
import com.darkyen.paragrowth.WorldSpecifics
import com.darkyen.paragrowth.animal.AnimalWorld
import com.darkyen.paragrowth.doodad.DoodadWorld
import com.darkyen.paragrowth.input.GameInput
import com.darkyen.paragrowth.render.RenderBatch
import com.darkyen.paragrowth.skybox.Skybox
import com.darkyen.paragrowth.terrain.TERRAIN_TIME_ATTRIBUTE
import com.darkyen.paragrowth.terrain.TerrainPatchwork
import com.darkyen.paragrowth.terrain.WorldQuery
import com.darkyen.paragrowth.util.*
import com.darkyen.paragrowth.words.Words
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

    private val animalWorld: AnimalWorld
    private val words: Words

    private val worldQuery: WorldQuery

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
        skyboxRenderable.lowColor = worldSpecifics.lowSkyboxColor
        skyboxRenderable.highColor = worldSpecifics.highSkyboxColor

        worldQuery = object : WorldQuery {
            override fun getHeightAt(x: Float, y: Float): Float {
                val base = terrain.heightAt(x, y)
                val blend = nextTerrain?.heightAt(x, y) ?: base
                val alpha = modelBatch.attributes.getBlendAt(x, y)
                return MathUtils.lerp(base, blend, alpha)
            }

            override fun getDimensions(): Rectangle {
                val blend = modelBatch.attributes[WORLD_BLEND_ATTRIBUTE][0]

                val x = terrain.worldSpec.offsetX
                val y = terrain.worldSpec.offsetY
                val nextX = nextTerrain?.worldSpec?.offsetX ?: x
                val nextY = nextTerrain?.worldSpec?.offsetY ?: y

                val width = terrain.worldSpec.noise.sizeX.toFloat()
                val height = terrain.worldSpec.noise.sizeY.toFloat()
                val nextWidth = nextTerrain?.worldSpec?.noise?.sizeX?.toFloat() ?: width
                val nextHeight = nextTerrain?.worldSpec?.noise?.sizeY?.toFloat() ?: height

                return Rectangle(MathUtils.lerp(x, nextX, blend),
                        MathUtils.lerp(y, nextY, blend),
                        MathUtils.lerp(width, nextWidth, blend),
                        MathUtils.lerp(height, nextHeight, blend))
            }
        }

        cameraController = HeightmapPersonController(worldCam, worldQuery)
        gameInput = GameInput(*cameraController.INPUT)
        gameInput.build()

        worldSpecifics.findInitialPosition(worldCam.position)

        animalWorld = AnimalWorld(worldQuery)
        animalWorld.populateWithDucks(worldQuery.getDimensions())

        words = Words { words, text ->
            if (developingNextWorld == null) {
                words.enabled = false
                developingNextWorld = startDevelopingNextWorld {
                    WorldCharacteristics.fromText(text)
                }
            }
        }
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

    private fun startDevelopingNextWorld(characteristics:() -> WorldCharacteristics): Delayed<Pair<TerrainPatchwork, DoodadWorld>> {
        val centerX = worldCam.position.x
        val centerY = worldCam.position.y

        val worldCharacteristics = offload {
            WorldSpecifics(characteristics(), centerX, centerY, true)
        }
        val terrainPatchwork = worldCharacteristics.then { TerrainPatchwork.build(it) }
        val blendOut = worldCharacteristics.then { doodads.prepareBlendOut(it) }
        val doodadWorld = worldCharacteristics
                .then { DoodadWorld.build(it.characteristics.seed, it) }
                .then { it.prepareBlendIn(terrain.worldSpec) }

        return terrainPatchwork.pairWith(doodadWorld).andWaitFor(blendOut)
    }


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

                skyboxRenderable.apply {
                    lowColor = lowColorBlend
                    highColor = highColorBlend
                }

                words.enabled = true
            }
        } else if (cameraController.CYCLE_TERRAIN_DEBUG.isPressed) {
            if (developingNextWorld == null) {
                developingNextWorld = startDevelopingNextWorld {
                    WorldCharacteristics.random(System.currentTimeMillis())
                }
            }
        }

        developingNextWorld?.poll()?.let { (newTerrain, newDoodads) ->
            this.developingNextWorld = null
            this.nextTerrain = newTerrain
            this.nextDoodads = newDoodads
            terrain.blendTo(newTerrain)
            modelBatch.attributes.setBlendWalls(worldCam)
            skyboxRenderable.lowColorBlend = newTerrain.worldSpec.lowSkyboxColor
            skyboxRenderable.highColorBlend = newTerrain.worldSpec.highSkyboxColor

            nextWorldAlpha = 0f
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
        animalWorld.render(modelBatch, worldCam)
        words.render(modelBatch)

        rendered = modelBatch.end()
        Gdx.gl.glDisable(GL_DEPTH_CLAMP)

        if (cameraController.PATCHWORK_DEBUG.isPressed) {
            Gdx.gl.glDisable(GL_DEPTH_TEST)
            debugRenderer.begin(worldCam.combined, GL20.GL_LINES)

            doodads.renderDebug(worldCam, debugRenderer)

            animalWorld.renderDebug(debugRenderer)

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
        val playerPosition = Vector2(worldCam.position.x, worldCam.position.y)
        animalWorld.update(delta, playerPosition)
        words.update(delta, playerPosition, worldQuery)

        val stats = StringBuilder(128)
        stats.append("FPS: ").append(Gdx.graphics.framesPerSecond)
                .append("\nX: ").append(worldCam.position.x)
                .append("\nY: ").append(worldCam.position.y)
                .append("\nZ: ").append(worldCam.position.z)
                .append("\nRendered: ").append(rendered)
                .append("\nWords: ").append(words.placedWords.size)

        statsLabel.setText(stats)
    }

    override fun dispose() {
        hudStage.dispose()
        skyboxRenderable.dispose()
        terrain.dispose()
        nextTerrain?.dispose()
        doodads.dispose()
        animalWorld.dispose()
    }
}
