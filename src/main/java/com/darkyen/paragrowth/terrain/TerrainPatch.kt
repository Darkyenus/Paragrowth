package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.render.Shader.Companion.TERRAIN
import com.darkyen.paragrowth.render.Shader.Companion.TERRAIN_OCEAN
import com.darkyen.paragrowth.util.*
import java.nio.FloatBuffer

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

/** Amount of indices needed to draw the whole patch */
const val TERRAIN_PATCH_INDEX_COUNT = (PATCH_UNIT_SIZE * PATCH_UNIT_SIZE * 2) * 3
//const val TERRAIN_PATCH_LOD1_INDEX_COUNT = (PATCH_UNIT_SIZE/2 * (1 + 2 + PATCH_UNIT_SIZE - 1)) * 3
var TERRAIN_PATCH_LOD1_INDEX_COUNT = 0 // Filled at generation time
/** Total amount of vertices needed to draw the whole patch.
 * Some triangles must overlap, because there is more triangles than vertices and we need an unique provoking
 * vertex for each one. For EVEN rows, the provoking vertex is the top-left one and top one.
 * For ODD rows, the provoking vertex is the top one and the top-right one. */
const val TERRAIN_PATCH_VERTEX_COUNT = PATCH_SIZE * PATCH_SIZE + PATCH_UNIT_SIZE * PATCH_UNIT_SIZE
const val TERRAIN_PATCH_VERTEX_SIZE = /* XYZ */3 + /* Color (packed) */1 + /* Normal (packed) */1

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

@PublishedApi
internal fun packNormal(normal:Vector3):Float {
    val max =  0b01111_11111
    val mask = 0b11111_11111
    val x = (normal.x * max).toInt() and mask
    val y = (normal.y * max).toInt() and mask
    val z = (normal.z * max).toInt() and mask
    val xyz = x or (y shl 10) or (z shl 20)
    return Float.fromBits(xyz)
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun FloatBuffer.set(index:Int, value:Float) {
    this.put(index, value)
}

@PublishedApi
internal val NO_COLOR = rgb(1f, 0f, 1f)
@PublishedApi
internal val NO_NORMAL = packNormal(Vector3.Z)

/** Generate vertices with coordinates, color and triangle normal.
 * @param outVertices size should be TERRAIN_PATCH_VERTEX_COUNT * VERTEX_SIZE
 * @param outHeightMap size should be PATCH_SIZE * PATCH_SIZE */
inline fun generateTerrainPatchVertices(xOffset:Float, yOffset:Float,
                                        getHeight:(x:Float, y:Float) -> Float, getColor:(x:Float, y:Float) -> Color, getNormal:(out:Vector3, x:Float, y:Float) -> Unit,
                                        outVertices: FloatBuffer, outHeightMap:FloatArray) {
    val X_HALF_STEP = X_STEP * 0.5f
    val Y_HALF_STEP = Y_STEP * 0.5f

    val normal = Vector3(0f, 0f, 1f)

    var v = 0
    var yPos = yOffset
    var h = 0
    // Stepping through hourglass middles
    var y = 1
    while (y < PATCH_SIZE) {
        var xPos = xOffset
        // Do a line of top X that makes the first row

        // Top of even row
        var height = getHeight(xPos, yPos)
        outHeightMap[h++] = height
        for (x in 0 until PATCH_UNIT_SIZE) {
            run {
                val xTPos = xPos + X_HALF_STEP
                val yTPos = yPos + Y_HALF_STEP

                // Top left of red
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = getColor(xTPos, yTPos)
                getNormal(normal, xTPos, yTPos)
                outVertices[v++] = packNormal(normal)
            }

            xPos += X_STEP
            height = getHeight(xPos, yPos)
            outHeightMap[h++] = height

            run {
                val xTPos = xPos
                val yTPos = yPos + Y_HALF_STEP

                // Top of green
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = getColor(xTPos, yTPos)
                getNormal(normal, xTPos, yTPos)
                outVertices[v++] = packNormal(normal)
            }
        }

        yPos += Y_STEP
        xPos = xOffset + X_STAGGER
        height = getHeight(xPos, yPos)
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
                outVertices[v++] = getColor(xTPos, yTPos)
                getNormal(normal, xTPos, yTPos)
                outVertices[v++] = packNormal(normal)
            }

            xPos += X_STEP
            height = getHeight(xPos, yPos)
            outHeightMap[h++] = height

            run {
                val xTPos = xPos - X_HALF_STEP
                val yTPos = yPos + Y_HALF_STEP

                // Top right of dark green
                outVertices[v++] = xPos
                outVertices[v++] = yPos
                outVertices[v++] = height
                outVertices[v++] = getColor(xTPos, yTPos)
                getNormal(normal, xTPos, yTPos)
                outVertices[v++] = packNormal(normal)
            }
        }

        yPos += Y_STEP
        y += 2
    }

    // Do one more bottom row, without colors
    var xPos = xOffset
    yPos = yOffset + PATCH_HEIGHT // By now, a lot of error has accumulated, so a better reset is needed to get rid of a seam
    var height = getHeight(xPos, yPos)
    outHeightMap[h++] = height
    for (x in 0 until PATCH_UNIT_SIZE) {
        // Top left of red
        outVertices[v++] = xPos
        outVertices[v++] = yPos
        outVertices[v++] = height
        outVertices[v++] = NO_COLOR
        outVertices[v++] = NO_NORMAL

        xPos += X_STEP
        height = getHeight(xPos, yPos)
        outHeightMap[h++] = height

        // Top of green
        outVertices[v++] = xPos
        outVertices[v++] = yPos
        outVertices[v++] = height
        outVertices[v++] = NO_COLOR
        outVertices[v++] = NO_NORMAL
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
    assert(i == indices.size)

    return indices
}

/** Generate indices for the terrain mesh, LoD 1 */
fun generateTerrainPatchIndicesLoD1():ShortArray {
    val ROW_AMOUNT = PATCH_SIZE + PATCH_SIZE - 2
    val indices = GdxShortArray()

    // Do all of the double-strips
    for (y in 0 until PATCH_SIZE-1 step 2) {
        // Row head

        // top
        indices.add(y * ROW_AMOUNT)
        indices.add(y * ROW_AMOUNT + 1)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT)

        // bottom
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT)

        // wedge
        indices.add(y * ROW_AMOUNT + 1)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT + ROW_AMOUNT + 1)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT)

        when {
            y == 0 -> { // BEGIN ROW
                for (x in 1 until PATCH_SIZE - 2 step 2) {
                    // left
                    indices.add(y * ROW_AMOUNT + x*2)
                    indices.add(y * ROW_AMOUNT + x*2 + 1)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2)

                    // big
                    indices.add(y * ROW_AMOUNT + x*2 + 1)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 4)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2)

                    // right
                    indices.add(y * ROW_AMOUNT + x*2 + 2)
                    indices.add(y * ROW_AMOUNT + x*2 + 3)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 4)
                }
            }
            y + 3 == PATCH_SIZE -> {// END ROW
                for (x in 1 until PATCH_SIZE - 2 step 2) {
                    // left
                    indices.add(y * ROW_AMOUNT + x*2)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 1)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2)

                    // big
                    indices.add(y * ROW_AMOUNT + x*2)
                    indices.add(y * ROW_AMOUNT + x*2 + 3)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 1)

                    // right
                    indices.add(y * ROW_AMOUNT + x*2 + 3)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 4)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 2)
                }
            }
            (y / 2) % 2 == 0 -> {// BODY A
                // BODY START (half triangle)
                indices.add(y * ROW_AMOUNT + 2)
                indices.add(y * ROW_AMOUNT + 3)
                indices.add(y * ROW_AMOUNT + 2 * ROW_AMOUNT + 2)


                // A-shaped
                for (x in 1 until PATCH_SIZE - 2 step 2) {
                    // big
                    indices.add(y * ROW_AMOUNT + x*2 + 1)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 4)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2)
                }

                for (x in 2 until PATCH_SIZE - 3 step 2) {
                    indices.add(y * ROW_AMOUNT + x*2)
                    indices.add(y * ROW_AMOUNT + x*2 + 3)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 1)
                }

                // BODY END (half triangle)
                indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 4)
                indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 2)
                indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 2)
            }
            else -> {// BODY B (flipped A)
                // BODY START (half triangle)
                indices.add(y * ROW_AMOUNT + 2)
                indices.add(y * ROW_AMOUNT + 2 * ROW_AMOUNT + 3)
                indices.add(y * ROW_AMOUNT + 2 * ROW_AMOUNT + 2)


                // V-shaped
                for (x in 1 until PATCH_SIZE - 2 step 2) {
                    indices.add(y * ROW_AMOUNT + x*2)
                    indices.add(y * ROW_AMOUNT + x*2 + 3)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 1)
                }

                for (x in 2 until PATCH_SIZE - 3 step 2) {
                    indices.add(y * ROW_AMOUNT + x*2 + 1)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2 + 4)
                    indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT + x*2)
                }


                // BODY END (half triangle)
                indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 3)
                indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 2)
                indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 4)
            }
        }

        // Row tail

        // top
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 2)
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 1)
        indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 2)

        // top right
        indices.add(y * ROW_AMOUNT + ROW_AMOUNT - 1)
        indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT - 1)
        indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 2)

        // bottom right
        indices.add(y * ROW_AMOUNT + 2*ROW_AMOUNT - 1)
        indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 1)
        indices.add(y * ROW_AMOUNT + 3*ROW_AMOUNT - 2)
    }

    TERRAIN_PATCH_LOD1_INDEX_COUNT = indices.size
    return indices.toArray()
}

val VA_NORMAL3_TINY = VertexAttribute("a_normal", GL30.GL_INT_2_10_10_10_REV, 4, true)

val TERRAIN_PATCH_ATTRIBUTES = VertexAttributes(
        VA_POSITION3,
        VA_COLOR1,
        VA_NORMAL3_TINY
)

val TERRAIN_PATCH_BLEND_ATTRIBUTES = VertexAttributes(
        VA_POSITION3,
        VA_COLOR1,
        VA_NORMAL3_TINY,
        VA_POSITION3.withName("a_position_blend"),
        VA_COLOR1.withName("a_color_blend"),
        VA_NORMAL3_TINY.withName("a_normal_blend")
)

class TerrainPatch(
        private val xOffset:Float,
        private val yOffset:Float,
        val heightMap:FloatArray,
        val model:Model) {

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
        this.min.set(xOffset, yOffset, min - 1f)
        this.max.set(xOffset + PATCH_WIDTH, yOffset + PATCH_HEIGHT, max + 1f)
    }
}

// TODO Better order for better batching
enum class TerrainShaderType(val order:Int) {
    LAND_LAND(TERRAIN),
    LAND_WATER(TERRAIN),
    WATER_LAND(TERRAIN),
    WATER_WATER(TERRAIN_OCEAN)
}

val TERRAIN_SHADER_L_L = TerrainShader(TerrainShaderType.LAND_LAND)
val TERRAIN_SHADER_L_W = TerrainShader(TerrainShaderType.LAND_WATER)
val TERRAIN_SHADER_W_L = TerrainShader(TerrainShaderType.WATER_LAND)
val TERRAIN_SHADER_W_W = TerrainShader(TerrainShaderType.WATER_WATER)

/** Time for waves */
val TERRAIN_TIME_ATTRIBUTE = attributeKeyFloat("terrain_time", GlobalAttributeLayer)
val TERRAIN_WATER_COLOR_FROM_ATTRIBUTE = attributeKeyFloat("terrain_water_color_from", GlobalAttributeLayer)
val TERRAIN_WATER_COLOR_TO_ATTRIBUTE = attributeKeyFloat("terrain_water_color_to", GlobalAttributeLayer)
/** Location offset in the world */
val TERRAIN_W_W_OCEAN_OFFSET_ATTRIBUTE = attributeKeyVector2("terrain_ocean_offset", ModelAttributeLayer)

class TerrainShader(type:TerrainShaderType) : Shader(
        type.order,
        "terrain",
        if (type == TerrainShaderType.LAND_LAND) TERRAIN_PATCH_BLEND_ATTRIBUTES else TERRAIN_PATCH_ATTRIBUTES,
        fragmentShaderName = "terrain",
        maxInstances = if (type == TerrainShaderType.WATER_WATER) 64 else 0,
        defines = mapOf(type.toString() to "1")) {

    init {
        if (type == TerrainShaderType.WATER_WATER) {
            // Only ocean needs transformation matrix, normal terrain has position baked in
            instancedUniform("u_worldTrans") { uniform, _, renderable ->
                uniform.set(renderable.attributes[TERRAIN_W_W_OCEAN_OFFSET_ATTRIBUTE])
            }
        }

        setupBlendWallUniforms()

        globalUniform("u_eye_position") { uniform, camera, _ ->
            uniform.set(camera.position)
        }

        if (type == TerrainShaderType.WATER_LAND) {
            globalUniform("u_water_color_from") { uniform, _, attributes ->
                uniform.setColor(attributes[TERRAIN_WATER_COLOR_FROM_ATTRIBUTE][0])
            }
        }
        if (type == TerrainShaderType.LAND_WATER || type == TerrainShaderType.WATER_WATER) {
            globalUniform("u_water_color_to") { uniform, _, attributes ->
                uniform.setColor(attributes[TERRAIN_WATER_COLOR_TO_ATTRIBUTE][0])
            }
        }

        globalUniform("u_blend") { uniform, _, attributes ->
            uniform.set(attributes[WORLD_BLEND_ATTRIBUTE][0])
        }

        globalUniform("u_projViewTrans") { uniform, camera, _ ->
            uniform.set(camera.combined)
        }

        globalUniform("u_time") { uniform, _, attributes ->
            uniform.set(attributes[TERRAIN_TIME_ATTRIBUTE][0])
        }

        ParagrowthMain.assetManager.load("Water_001_DISP.png", Texture::class.java)
        ParagrowthMain.assetManager.load("Water_001_NORM.jpg", Texture::class.java)
        ParagrowthMain.assetManager.finishLoading()
        val displacement = TextureDescriptor(ParagrowthMain.assetManager.get("Water_001_DISP.png", Texture::class.java), Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        val normal = TextureDescriptor(ParagrowthMain.assetManager.get("Water_001_NORM.jpg", Texture::class.java), Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)

        globalUniform("u_displacement_texture") { uniform, _, _ ->
            uniform.set(displacement)
        }

        globalUniform("u_normal_texture") { uniform, _, _ ->
            uniform.set(normal)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}