package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.terrain.generator.TerrainProvider

/*
triangles = (size-1)^2*2
indices = (size-1)^2*2*3
max indices = 2^16
=> size <= 105.512, patch size must be odd
=> size = 105
 */
/** Total amount of vertices along each side */
const val PATCH_SIZE = 105
/** Total amount of triangles along each side (not couning those, which touch only by single point) */
const val PATCH_UNIT_SIZE = PATCH_SIZE - 1

const val X_STEP = 1f
const val X_STAGGER = 0.5f
const val Y_STEP = 0.866025404f // sqrt(3.0) / 2.0

/** Total size of the patch in world space, X coordinate */
const val PATCH_WIDTH = PATCH_UNIT_SIZE * X_STEP
/** Total size of the patch in world space, Y coordinate */
const val PATCH_HEIGHT = PATCH_UNIT_SIZE * Y_STEP

/** Total amount of triangles per patch */
private const val TRIANGLE_COUNT = PATCH_UNIT_SIZE * PATCH_UNIT_SIZE * 2
/** Amount of indices needed to draw the whole patch */
const val TERRAIN_PATCH_INDEX_COUNT = TRIANGLE_COUNT * 3
/** Total amount of vertices needed to draw the whole patch.
 * Some triangles must overlap, because there is more triangles than vertices and we need an unique provoking
 * vertex for each one. For EVEN rows, the provoking vertex is the top-left one and top one.
 * For ODD rows, the provoking vertex is the top one and the top-right one. */
const val TERRAIN_PATCH_VERTEX_COUNT = PATCH_SIZE * PATCH_SIZE + PATCH_UNIT_SIZE * PATCH_UNIT_SIZE
const val TERRAIN_PATCH_VERTEX_SIZE = /* XYZ */3 + /* Color */1 + /* Normal */3

/*
Arrangement:
0 \/\/\/\/\/\/\ even
1 /\/\/\/\/\/\/ odd
2 \/\/\/\/\/\/\
3 /\/\/\/\/\/\/
4

X step: 1
X stagger: 0.5
Y step: sqrt(3)/2
*/

/** Generate vertices with coordinates, color and triangle normal.
 * @param outVertices size should be TERRAIN_PATCH_VERTEX_COUNT * VERTEX_SIZE
 * @param outHeightMap size should be PATCH_SIZE * PATCH_SIZE */
fun generateTerrainPatchVertices(xOffset:Float, yOffset:Float, generator: TerrainProvider, outVertices:FloatArray, outHeightMap:FloatArray) {
    val X_HALF_STEP = X_STEP * 0.5f
    val Y_HALF_STEP = Y_STEP * 0.5f

    val normal = Vector3()

    var v = 0
    var yPos = yOffset
    var h = 0
    // Stepping through hourglass middles
    var y = 1
    while (y < PATCH_SIZE) {
        var xPos = xOffset
        // Do a line of top X that makes the first row

        // Top of even row
        var height = generator.getHeight(xPos, yPos)
        outHeightMap[h++] = height
        for (x in 0 until PATCH_UNIT_SIZE) {
            run {
                val xTPos = xPos + X_HALF_STEP
                val yTPos = yPos + Y_HALF_STEP

                // Top left of red
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = generator.getColor(xTPos, yTPos)
                generator.getNormal(normal, xTPos, yTPos)
                outVertices[v++] = normal.x
                outVertices[v++] = normal.y
                outVertices[v++] = normal.z
            }

            xPos += X_STEP
            height = generator.getHeight(xPos, yPos)
            outHeightMap[h++] = height

            run {
                val xTPos = xPos
                val yTPos = yPos + Y_HALF_STEP

                // Top of green
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = generator.getColor(xTPos, yTPos)
                generator.getNormal(normal, xTPos, yTPos)
                outVertices[v++] = normal.x
                outVertices[v++] = normal.y
                outVertices[v++] = normal.z
            }
        }

        yPos += Y_STEP
        xPos = xOffset + X_STAGGER
        height = generator.getHeight(xPos, yPos)
        outHeightMap[h++] = height

        // Top of odd row
        for (x in 0 until PATCH_UNIT_SIZE) {
            run {
                val xTPos = xPos
                val yTPos = yPos + Y_HALF_STEP

                // Top of dark red
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = generator.getColor(xTPos, yTPos)
                generator.getNormal(normal, xTPos, yTPos)
                outVertices[v++] = normal.x
                outVertices[v++] = normal.y
                outVertices[v++] = normal.z
            }

            xPos += X_STEP
            height = generator.getHeight(xPos, yPos)
            outHeightMap[h++] = height

            run {
                val xTPos = xPos - X_HALF_STEP
                val yTPos = yPos + Y_HALF_STEP

                // Top right of dark green
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = generator.getColor(xTPos, yTPos)
                generator.getNormal(normal, xTPos, yTPos)
                outVertices[v++] = normal.x
                outVertices[v++] = normal.y
                outVertices[v++] = normal.z
            }
        }

        yPos += Y_STEP
        y += 2
    }

    // Do one more bottom row, without colors
    val NO_COLOR = Color.MAGENTA.toFloatBits()
    var xPos = xOffset
    var height = generator.getHeight(xPos, yPos)
    outHeightMap[h++] = height
    for (x in 0 until PATCH_UNIT_SIZE) {
        // Top left of red
        outVertices[v++] = xPos
        outVertices[v++] = yPos
        outVertices[v++] = height
        outVertices[v++] = NO_COLOR
        outVertices[v++] = 0f
        outVertices[v++] = 0f
        outVertices[v++] = 1f

        xPos += X_STEP
        height = generator.getHeight(xPos, yPos)
        outHeightMap[h++] = height

        // Top of green
        outVertices[v++] = xPos
        outVertices[v++] = yPos
        outVertices[v++] = height
        outVertices[v++] = NO_COLOR
        outVertices[v++] = 0f
        outVertices[v++] = 0f
        outVertices[v++] = 1f
    }
}

/** Generate indices for the terrain mesh */
fun generateTerrainPatchIndices():ShortArray {
    val ROW_AMOUNT = PATCH_SIZE + PATCH_SIZE - 2
    val indices = ShortArray(TERRAIN_PATCH_INDEX_COUNT)
    var i = 0

    // Do all of the double-strips
    var y = 0
    while (y < PATCH_UNIT_SIZE) {
        // First Red
        indices[i++] = (y * ROW_AMOUNT).toShort()
        indices[i++] = (y * ROW_AMOUNT + 1).toShort()
        indices[i++] = (y * ROW_AMOUNT + ROW_AMOUNT).toShort()

        // Other Red
        for (x in 1 until PATCH_UNIT_SIZE) {
            indices[i++] = (x * 2 + y * ROW_AMOUNT).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + 1).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT - 1).toShort()
        }

        // All Green
        for (x in 0 until PATCH_UNIT_SIZE) {
            indices[i++] = (x * 2 + 1 + y * ROW_AMOUNT).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT + 1).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT).toShort()
        }

        // All Dark Red
        for (x in 0 until PATCH_UNIT_SIZE) {
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT).toShort()
        }

        // All Dark Green
        for (x in 0 until PATCH_UNIT_SIZE) {
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT + 1).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1).toShort()
            indices[i++] = (x * 2 + y * ROW_AMOUNT + ROW_AMOUNT).toShort()
        }
        y += 2
    }
    return indices
}

val VA_POSITION = VertexAttribute("a_position", GL30.GL_FLOAT, 3)

val TERRAIN_PATCH_ATTRIBUTES = VertexAttributes(
        VA_POSITION,
        VA_COLOR1,
        VA_NORMAL3
)

class TerrainPatch(
        private val xOffset:Float,
        private val yOffset:Float,
        val heightMap:FloatArray,
        private val model:Model) {

    init {
        assert(heightMap.size == PATCH_SIZE * PATCH_SIZE)
    }

    val boundingBox = BoundingBox().apply {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        for (h in heightMap) {
            min = minOf(min, h)
            max = maxOf(max, h)
        }
        this.min.set(xOffset, yOffset, min)
        this.max.set(xOffset + PATCH_WIDTH, yOffset + PATCH_HEIGHT, max)
    }


    fun fillRenderModel(model: RenderModel) {
        model.set(this.model)
        model.shader = TerrainShader
    }
}

object TerrainShader : ParaShader(TERRAIN, "terrain", TERRAIN_PATCH_ATTRIBUTES) {

    init {
        localUniform("u_worldTrans") { uniform, _, renderable ->
            uniform.set(renderable.worldTransform)
        }
        globalUniform("u_projViewTrans") { uniform, camera ->
            uniform.set(camera.combined)
        }

        val startTime = System.currentTimeMillis()
        globalUniform("u_time") { uniform, _ ->
            val time = (System.currentTimeMillis() - startTime) / 1000f
            uniform.set(time)
        }

        ParagrowthMain.assetManager.load("Water_001_DISP.png", Texture::class.java)
        ParagrowthMain.assetManager.load("Water_001_NORM.jpg", Texture::class.java)
        ParagrowthMain.assetManager.finishLoading()
        val displacement = TextureDescriptor(ParagrowthMain.assetManager.get("Water_001_DISP.png", Texture::class.java), Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        val normal = TextureDescriptor(ParagrowthMain.assetManager.get("Water_001_NORM.jpg", Texture::class.java), Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)

        globalUniform("u_displacement") { uniform, _ ->
            uniform.set(displacement)
        }

        globalUniform("u_normal") { uniform, _ ->
            uniform.set(normal)
        }

        globalUniform("u_position") { uniform, camera ->
            uniform.set(camera.position)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}