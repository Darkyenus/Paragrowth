package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.WorldSpecifics
import com.darkyen.paragrowth.render.*

/**
 * A collection of terrain patches.
 */
class TerrainPatchwork(val worldSpec: WorldSpecifics) : Renderable, Disposable {

    // inclusive min
    private val minPatchX = MathUtils.floor(worldSpec.offsetX / PATCH_WIDTH)
    private val minPatchY = MathUtils.floor(worldSpec.offsetY / PATCH_HEIGHT)
    // exclusive max
    private val maxPatchX = MathUtils.ceil((worldSpec.offsetX + worldSpec.noise.sizeX) / PATCH_WIDTH)
    private val maxPatchY = MathUtils.ceil((worldSpec.offsetY + worldSpec.noise.sizeY) / PATCH_HEIGHT)

    private val patchAmountX: Int = maxPatchX - minPatchX
    private val patchAmountY: Int = maxPatchY - minPatchY
    private val patches: Array<TerrainPatch>
    private val seaPatch: TerrainPatch

    private val vertexBuffer:GlBuffer
    private val indexBuffer:GlBuffer
    private val vao:GlVertexArrayObject

    private var blendingTo:TerrainPatchwork? = null
    private var blendVao:Array<GlVertexArrayObject>? = null
    var blendProgress:Float = 0f

    init {
        @Suppress("UNCHECKED_CAST")
        this.patches = arrayOfNulls<TerrainPatch>(patchAmountX * patchAmountY) as Array<TerrainPatch>

        val indexBuffer = GlBuffer(GL20.GL_STATIC_DRAW)
        val normalIndices = generateTerrainPatchIndices()
        val lod1Indices = generateTerrainPatchIndicesLoD1()
        indexBuffer.reserve(normalIndices.size + lod1Indices.size, GL20.GL_UNSIGNED_SHORT)
        indexBuffer.setSubData(0, normalIndices)
        indexBuffer.setSubData(normalIndices.size, lod1Indices)
        this.indexBuffer = indexBuffer

        val vertexBuffer = GlBuffer(GL20.GL_STATIC_DRAW)
        vertexBuffer.reserve((patchAmountX * patchAmountY + 1 /* ocean */) * TERRAIN_PATCH_VERTEX_COUNT * TERRAIN_PATCH_VERTEX_SIZE, GL30.GL_FLOAT)
        this.vertexBuffer = vertexBuffer
        var vertexBufferFilled = 0
        var baseVertex = 0

        val vao = GlVertexArrayObject(indexBuffer, TERRAIN_PATCH_ATTRIBUTES,
                GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 0), // xyz
                GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 3), // color
                GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 4) // normal
        )
        this.vao = vao

        val vertexArray = FloatArray(TERRAIN_PATCH_VERTEX_COUNT * TERRAIN_PATCH_VERTEX_SIZE)

        var i = 0
        for (y in minPatchY until maxPatchY) {
            for (x in minPatchX until maxPatchX) {
                val xOffset = x * PATCH_WIDTH
                val yOffset = y * PATCH_HEIGHT
                val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE)
                val colorQuery = worldSpec.queryColors()
                generateTerrainPatchVertices(xOffset, yOffset, worldSpec::getHeight, colorQuery::getColor, worldSpec::getNormal, vertexArray, heightMap)
                vertexBuffer.setSubData(vertexBufferFilled, vertexArray)
                vertexBufferFilled += vertexArray.size
                val model = Model(vao, TERRAIN_PATCH_INDEX_COUNT, 0, baseVertex)
                baseVertex += TERRAIN_PATCH_VERTEX_COUNT

                patches[i++] = TerrainPatch(xOffset, yOffset, heightMap, model)
            }
        }

        val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE)
        generateTerrainPatchVertices(0f, 0f, { _, _ -> -1f }, { _, _ -> worldSpec.waterColor}, { _, _, _ -> }, vertexArray, heightMap)
        vertexBuffer.setSubData(vertexBufferFilled, vertexArray)
        val model = Model(vao, TERRAIN_PATCH_INDEX_COUNT, 0, baseVertex)
        seaPatch = TerrainPatch(0f, 0f, heightMap, model)
    }

    fun blendTo(tp:TerrainPatchwork) {
        blendingTo = tp
        blendProgress = 0f
        // Now we have a problem, because we need a separate VAO for each overlapping row
        val minOverlapY = maxOf(minPatchY, tp.minPatchY)
        val maxOverlapY = minOf(maxPatchY, tp.maxPatchY)

        blendVao = Array(maxOf(maxOverlapY - minOverlapY, 0)) { i ->
            val thisRowStart = minOverlapY + i - minPatchY
            val blendRowStart = minOverlapY + i - tp.minPatchY
            val offset = TERRAIN_PATCH_VERTEX_COUNT * (blendRowStart - thisRowStart)
            // TODO(jp): This will probably explode, at least for negative offsets
            print(offset)

            GlVertexArrayObject(indexBuffer, TERRAIN_PATCH_BLEND_ATTRIBUTES,
                    GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 0), // xyz
                    GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 3), // color
                    GlVertexArrayObject.Binding(vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, 4), // normal

                    GlVertexArrayObject.Binding(tp.vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, offset + 0), // xyz
                    GlVertexArrayObject.Binding(tp.vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, offset + 3), // color
                    GlVertexArrayObject.Binding(tp.vertexBuffer, TERRAIN_PATCH_VERTEX_SIZE, offset + 4) // normal
            )
        }
    }

    private fun heightAtVertex(x: Int, y: Int): Float {
        val patchX = Math.floorDiv(x, PATCH_UNIT_SIZE)
        if (patchX < minPatchX || patchX >= maxPatchX) {
            return -1f
        }
        val patchY = Math.floorDiv(y, PATCH_UNIT_SIZE)
        if (patchY < minPatchY || patchY >= maxPatchY) {
            return -1f
        }

        val inPatchX = Math.floorMod(x, PATCH_UNIT_SIZE)
        val inPatchY = Math.floorMod(y, PATCH_UNIT_SIZE)
        return patches[(patchY - minPatchY) * patchAmountX + (patchX - minPatchX)].heightMap[inPatchY * PATCH_SIZE + inPatchX]
    }

    // For easier to debug math
    fun heightAt(x: Float, y: Float): Float {
        val gridY = y / Y_STEP
        val baseY = Math.floor(gridY.toDouble()).toInt()
        val yFrac = gridY - baseY

        val odd = baseY and 1 == 0
        val gridX = x / X_STEP
        // Skew the grid to look like rectangles with alternating diagonals
        val skewedX = gridX - if (odd) yFrac * X_STAGGER else (1f - yFrac) * X_STAGGER

        val baseX = Math.floor(skewedX.toDouble()).toInt()
        val xFrac = skewedX - baseX

        /*
        We are somewhere in a triangle, with straight on top or on bottom.
        We want to find "global coordinates" of vertices of this triangle and interpolation factors.

        Also, we need interpolation factor for the base and point.
         */

        val trigBaseYOff: Int
        val trigPointXOff: Int
        val trigPointYOff: Int

        if (odd) {
            if (yFrac < 1f - xFrac) {
                trigBaseYOff = 0
                trigPointXOff = 0
                trigPointYOff = 1
            } else {
                trigBaseYOff = 1
                trigPointXOff = 1
                trigPointYOff = 0
            }
        } else {
            if (yFrac < xFrac) {
                trigBaseYOff = 0
                trigPointXOff = 1
                trigPointYOff = 1
            } else {
                trigBaseYOff = 1
                trigPointXOff = 0
                trigPointYOff = 0
            }
        }

        // Convert to barycentric
        // https://en.wikipedia.org/wiki/Barycentric_coordinate_system
        // P1 = base left, P2 = base right, P3 = point
        val x1 = 0
        val x2 = 1

        val detT = ((trigBaseYOff - trigPointYOff) * (x1 - trigPointXOff) + (trigPointXOff - x2) * (trigBaseYOff - trigPointYOff)).toFloat()
        val a1 = ((trigBaseYOff - trigPointYOff) * (xFrac - trigPointXOff) + (trigPointXOff - x2) * (yFrac - trigPointYOff)) / detT
        val a2 = ((trigPointYOff - trigBaseYOff) * (xFrac - trigPointXOff) + (x1 - trigPointXOff) * (yFrac - trigPointYOff)) / detT
        val a3 = 1f - a1 - a2

        val hBaseLeft = heightAtVertex(baseX, baseY + trigBaseYOff)
        val hBaseRight = heightAtVertex(baseX + 1, baseY + trigBaseYOff)
        val hPoint = heightAtVertex(baseX + trigPointXOff, baseY + trigPointYOff)

        return hBaseLeft * a1 + hBaseRight * a2 + hPoint * a3
    }

    fun setupGlobalAttributes(batch:RenderBatch) {
        batch.attributes[TERRAIN_WATER_COLOR_FROM_ATTRIBUTE][0] = worldSpec.waterColor
        batch.attributes[TERRAIN_WATER_COLOR_TO_ATTRIBUTE][0] = (blendingTo ?: this).worldSpec.waterColor
        batch.attributes[TERRAIN_BLEND_ATTRIBUTE][0] = blendProgress
    }

    private val render_bounds = BoundingBox()
    private val render_boundsSea = BoundingBox()

    override fun render(batch: RenderBatch, camera: Camera) {
        val frustum = camera.frustum
        val cameraPosition = camera.position
        val lodDistance = 200
        val lodDistance2 = lodDistance * lodDistance
        val bounds = this.render_bounds.set(frustum.planePoints)

        val lowX = Math.floor(((bounds.min.x - X_STEP) / PATCH_WIDTH).toDouble()).toInt()
        val highX = Math.ceil(((bounds.max.x + X_STEP) / PATCH_WIDTH).toDouble()).toInt()

        val lowY = Math.floor(((bounds.min.y - Y_STEP) / PATCH_HEIGHT).toDouble()).toInt()
        val highY = Math.ceil(((bounds.max.y + Y_STEP) / PATCH_HEIGHT).toDouble()).toInt()

        for (y in lowY..highY) {
            for (x in lowX..highX) {
                /*
                Possible modes:
                w Water
                w Water -> Water (color!)
                l Land
                l Land -> Water
                l Water -> Land
                b Land -> Land

                l = Land <-> Water
                b = Land <-> Land
                w = Water <-> Water
                 */
                val baseLand = x >= minPatchX && y >= minPatchY && x < maxPatchX && y < maxPatchY
                val blendToLand = blendingTo?.run { x >= minPatchX && y >= minPatchY && x < maxPatchX && y < maxPatchY }

                // w
                if ((!baseLand && blendToLand == null) || (!baseLand && blendToLand == false)) {
                    val patch = seaPatch
                    val xOff = x * PATCH_WIDTH
                    val yOff = y * PATCH_HEIGHT

                    val box = render_boundsSea.set(patch.boundingBox)
                    box.min.add(xOff, yOff, 0f)
                    box.max.add(xOff, yOff, 0f)
                    // Not updating cnt because it is private, but it does not matter, frustum check does not use it

                    if (frustum.boundsInFrustum(box)) {
                        val model = batch.render()
                        model.set(patch.model)
                        model.shader = TERRAIN_SHADER_W_W
                        model.attributes[TERRAIN_W_W_OCEAN_OFFSET_ATTRIBUTE].set(xOff, yOff)
                        model.order = cameraPosition.dst2(xOff + PATCH_WIDTH * 0.5f, yOff + PATCH_HEIGHT * 0.5f, 0f)
                        if (model.order > lodDistance2) {
                            model.offset = TERRAIN_PATCH_INDEX_COUNT
                            model.count = TERRAIN_PATCH_LOD1_INDEX_COUNT
                        }
                    }
                } else if /* l */ ((baseLand && blendToLand == null /* Land */)
                        || (!baseLand && blendToLand == true /* Water -> Land */)
                        || (baseLand && blendToLand == false /* Land -> Water */)) {

                    val patch = if (blendToLand != true)
                        patches[patchAmountX * (y - minPatchY) + (x - minPatchX)]
                    else {
                        val b = blendingTo!!
                        b.patches[b.patchAmountX * (y - b.minPatchY) + (x - b.minPatchX)]
                    }


                    if (frustum.boundsInFrustum(patch.boundingBox)) {
                        val model = batch.render()
                        model.set(patch.model)
                        if (blendToLand != null) {
                            model.shader = if (blendToLand) TERRAIN_SHADER_W_L else TERRAIN_SHADER_L_W
                        } else {
                            model.shader = TERRAIN_SHADER_L_W
                        }
                        model.order = cameraPosition.dst2(x * PATCH_WIDTH + PATCH_WIDTH * 0.5f, y * PATCH_HEIGHT + PATCH_HEIGHT * 0.5f, 0f)


                        // TODO Investigate re-enabling this after having better lod indices
                        /*if (model.order > lodDistance2) {
                            model.offset = TERRAIN_PATCH_INDEX_COUNT
                            model.count = TERRAIN_PATCH_LOD1_INDEX_COUNT
                        }*/
                    }
                } else /* Land -> Land */ {
                    assert(baseLand && blendToLand == true)

                    val patch = patches[patchAmountX * (y - minPatchY) + (x - minPatchX)]

                    if (frustum.boundsInFrustum(patch.boundingBox)) {
                        val model = batch.render()
                        model.set(patch.model)
                        model.shader = TERRAIN_SHADER_L_L
                        model.order = cameraPosition.dst2(x * PATCH_WIDTH + PATCH_WIDTH * 0.5f, y * PATCH_HEIGHT + PATCH_HEIGHT * 0.5f, 0f)


                        // TODO Investigate re-enabling this after having better lod indices
                        /*if (model.order > lodDistance2) {
                            model.offset = TERRAIN_PATCH_INDEX_COUNT
                            model.count = TERRAIN_PATCH_LOD1_INDEX_COUNT
                        }*/
                    }
                }
            }
        }
    }

    override fun dispose() {
        indexBuffer.dispose()
        vertexBuffer.dispose()
        vao.dispose()
    }
}