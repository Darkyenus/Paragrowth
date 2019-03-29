package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
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
private const val INDEX_COUNT = TRIANGLE_COUNT * 3
/** Total amount of vertices needed to draw the whole patch.
 * Some triangles must overlap, because there is more triangles than vertices and we need an unique provoking
 * vertex for each one. For EVEN rows, the provoking vertex is the top-left one and top one.
 * For ODD rows, the provoking vertex is the top one and the top-right one. */
private const val VERTEX_COUNT = PATCH_SIZE * PATCH_SIZE + PATCH_UNIT_SIZE * PATCH_UNIT_SIZE

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

/** Generate vertices with Z coordinate, color and triangle normal */
fun generateTerrainPatchVertexZColorNormal(xOffset:Float, yOffset:Float, xy:FloatArray, heightMap:FloatArray, generator: TerrainProvider): FloatArray {
    val X_HALF_STEP = X_STEP * 0.5f
    val Y_HALF_STEP = Y_STEP * 0.5f

    val vertices = FloatArray(VERTEX_COUNT * (/* Z */1 + /* Color */1 + /* Normal */3))
    val normal = Vector3()

    var offXY = 0
    var v = 0
    var h = 0
    // Stepping through hourglass middles
    var y = 1
    while (y < PATCH_SIZE) {
        // Do a line of top X that makes the first row

        // Top of even row
        var height = heightMap[h++]
        for (x in 0 until PATCH_UNIT_SIZE) {
            run {
                val xPos = xy[offXY++] + xOffset + X_HALF_STEP
                val yPos = xy[offXY++] + yOffset + Y_HALF_STEP

                // Top left of red
                vertices[v++] = height
                vertices[v++] = generator.getColor(xPos, yPos)
                generator.getNormal(normal, xPos, yPos)
                vertices[v++] = normal.x
                vertices[v++] = normal.y
                vertices[v++] = normal.z
            }

            height = heightMap[h++]

            run {
                val xPos = xy[offXY++] + xOffset
                val yPos = xy[offXY++] + yOffset + Y_HALF_STEP

                // Top of green
                vertices[v++] = height
                vertices[v++] = generator.getColor(xPos, yPos)
                generator.getNormal(normal, xPos, yPos)
                vertices[v++] = normal.x
                vertices[v++] = normal.y
                vertices[v++] = normal.z
            }
        }

        height = heightMap[h++]

        // Top of odd row
        for (x in 0 until PATCH_UNIT_SIZE) {
            run {
                val xPos = xy[offXY++] + xOffset
                val yPos = xy[offXY++] + yOffset + Y_HALF_STEP

                // Top of dark red
                vertices[v++] = height
                vertices[v++] = generator.getColor(xPos, yPos)
                generator.getNormal(normal, xPos, yPos)
                vertices[v++] = normal.x
                vertices[v++] = normal.y
                vertices[v++] = normal.z
            }

            height = heightMap[h++]

            run {
                val xPos = xy[offXY++] + xOffset - X_HALF_STEP
                val yPos = xy[offXY++] + yOffset + Y_HALF_STEP

                // Top right of dark green
                vertices[v++] = height
                vertices[v++] = generator.getColor(xPos, yPos)
                generator.getNormal(normal, xPos, yPos)
                vertices[v++] = normal.x
                vertices[v++] = normal.y
                vertices[v++] = normal.z
            }
        }

        y += 2
    }

    // Do one more bottom row, without colors
    val NO_COLOR = Color.MAGENTA.toFloatBits()
    var height = heightMap[h++]
    for (x in 0 until PATCH_UNIT_SIZE) {
        // Top left of red
        vertices[v++] = height
        vertices[v++] = NO_COLOR
        vertices[v++] = 0f
        vertices[v++] = 0f
        vertices[v++] = 1f

        // Top of green
        height = heightMap[h++]
        vertices[v++] = height
        vertices[v++] = NO_COLOR
        vertices[v++] = 0f
        vertices[v++] = 0f
        vertices[v++] = 1f
    }

    return vertices
}

/** Generate height map based on array with xy coordinates. */
fun generateHeightMap(offX:Float, offY:Float, generator: TerrainProvider):FloatArray {
    val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE)
    var h = 0

    var yPos = 0f + offY
    // Stepping through hourglass middles
    var y = 1
    while (y < PATCH_SIZE) {
        var xPos = 0f + offX
        // Do a line of top X that makes the first row

        // Top of even row
        heightMap[h++] = generator.getHeight(xPos, yPos)
        for (x in 0 until PATCH_UNIT_SIZE) {
            xPos += X_STEP
            heightMap[h++] = generator.getHeight(xPos, yPos)
        }

        yPos += Y_STEP
        xPos = X_STAGGER + offX
        heightMap[h++] = generator.getHeight(xPos, yPos)

        // Top of odd row
        for (x in 0 until PATCH_UNIT_SIZE) {
            xPos += X_STEP
            heightMap[h++] = generator.getHeight(xPos, yPos)
        }

        yPos += Y_STEP
        y += 2
    }

    // Do one more bottom row, without colors
    var xPos = 0f + offX
    heightMap[h++] = generator.getHeight(xPos, yPos)
    for (x in 0 until PATCH_UNIT_SIZE) {
        xPos += X_STEP
        heightMap[h++] = generator.getHeight(xPos, yPos)
    }

    return heightMap
}

/** Generate vertices with x and y coordinate. These can be shared by all terrain patches. */
fun generateTerrainPatchVertexXY():FloatArray {
    val vertices = FloatArray(VERTEX_COUNT * 2)

    var v = 0
    var yPos = 0f
    // Stepping through hourglass middles
    var y = 1
    while (y < PATCH_SIZE) {
        var xPos = 0f
        // Do a line of top X that makes the first row

        // Top of even row
        for (x in 0 until PATCH_UNIT_SIZE) {

            // Top left of red
            vertices[v++] = xPos
            vertices[v++] = yPos

            xPos += X_STEP

            // Top of green
            vertices[v++] = xPos
            vertices[v++] = yPos
        }

        yPos += Y_STEP
        xPos = X_STAGGER

        // Top of odd row
        for (x in 0 until PATCH_UNIT_SIZE) {
            // Top of dark red
            vertices[v++] = xPos
            vertices[v++] = yPos

            xPos += X_STEP

            // Top right of dark green
            vertices[v++] = xPos
            vertices[v++] = yPos
        }

        yPos += Y_STEP
        y += 2
    }

    // Do one more bottom row, without colors
    var xPos = 0f
    for (x in 0 until PATCH_UNIT_SIZE) {
        // Top left of red
        vertices[v++] = xPos
        vertices[v++] = yPos

        xPos += X_STEP

        // Top of green
        vertices[v++] = xPos
        vertices[v++] = yPos
    }

    return vertices
}

/** Generate indices for the terrain mesh */
fun generateTerrainPatchIndices():ShortArray {
    val ROW_AMOUNT = PATCH_SIZE + PATCH_SIZE - 2
    val indices = ShortArray(INDEX_COUNT)
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

// TODO(jp): Inline!
fun createTerrainPatch(xOffset:Float, yOffset:Float, generator:TerrainProvider, xy:FloatArray, xyBuffer: GlBuffer, indicesBuffer:GlBuffer):TerrainPatch {
    val heightMap = generateHeightMap(xOffset, yOffset, generator)
    val zColNor = generateTerrainPatchVertexZColorNormal(xOffset, yOffset, xy, heightMap, generator)

    return TerrainPatch(xOffset, yOffset, heightMap, zColNor, xyBuffer, indicesBuffer)
}

fun createWaterTerrainPatch(xy:FloatArray, xyBuffer:GlBuffer, indicesBuffer:GlBuffer, generator:TerrainProvider):TerrainPatch {
    val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE) { -1f }
    val zColNor = generateTerrainPatchVertexZColorNormal(-PATCH_WIDTH, -PATCH_HEIGHT, xy, heightMap, generator)

    return TerrainPatch(0f, 0f, heightMap, zColNor, xyBuffer, indicesBuffer)
}

val VA_POSITION_XY = VertexAttribute("a_position_xy", GL30.GL_FLOAT, 2)
val VA_POSITION_Z = VertexAttribute("a_position_z", GL30.GL_FLOAT, 1)

val TERRAIN_PATCH_ATTRIBUTES = VertexAttributes(
        VA_POSITION_XY,
        VA_POSITION_Z,
        VA_COLOR1,
        VA_NORMAL3
)

class TerrainPatch(
        private val xOffset:Float,
        private val yOffset:Float,
        val heightMap:FloatArray,
        zColNor:FloatArray, xyBuffer:GlBuffer, indicesBuffer:GlBuffer) : Disposable {

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

    private val zColNorBuffer = GlBuffer(GL20.GL_STATIC_DRAW).apply { setData(zColNor) }
    private val vao = GlVertexArrayObject(indicesBuffer, TERRAIN_PATCH_ATTRIBUTES,
            GlVertexArrayObject.Binding(xyBuffer, 2, 0), // xy
            GlVertexArrayObject.Binding(zColNorBuffer, 5, 0), // z
            GlVertexArrayObject.Binding(zColNorBuffer, 5, 1), // color
            GlVertexArrayObject.Binding(zColNorBuffer, 5, 2) // normal
    )


    fun fillRenderModel(model: RenderModel) {
        model.worldTransform.translate(xOffset, yOffset, 0f)
        model.vao = vao
        model.primitiveType = GL20.GL_TRIANGLES
        model.count = INDEX_COUNT
        model.shader = TerrainShader
    }

    override fun dispose() {
        zColNorBuffer.dispose()
        vao.dispose()
    }
}