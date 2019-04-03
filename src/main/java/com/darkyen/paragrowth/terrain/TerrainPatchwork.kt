package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.terrain.generator.TerrainProvider

/**
 * A collection of terrain patches.
 */
class TerrainPatchwork(terrainProvider: TerrainProvider) : Renderable, Disposable {

    private val patchAmountX: Int = MathUtils.ceilPositive(terrainProvider.sizeX / PATCH_WIDTH)
    private val patchAmountY: Int = MathUtils.ceilPositive(terrainProvider.sizeY / PATCH_HEIGHT)
    private val patches: Array<TerrainPatch>
    private val seaPatch: TerrainPatch

    private val vertexBuffer:GlBuffer
    private val indexBuffer:GlBuffer
    private val vao:GlVertexArrayObject

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
        for (y in 0 until patchAmountY) {
            for (x in 0 until patchAmountX) {
                val xOffset = x * PATCH_WIDTH
                val yOffset = y * PATCH_HEIGHT
                val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE)
                generateTerrainPatchVertices(xOffset, yOffset, terrainProvider, vertexArray, heightMap)
                vertexBuffer.setSubData(vertexBufferFilled, vertexArray)
                vertexBufferFilled += vertexArray.size
                val model = Model(vao, TERRAIN_PATCH_INDEX_COUNT, 0, baseVertex)
                baseVertex += TERRAIN_PATCH_VERTEX_COUNT

                patches[i++] = TerrainPatch(xOffset, yOffset, heightMap, model)
            }
        }


        val heightMap = FloatArray(PATCH_SIZE * PATCH_SIZE)
        generateTerrainPatchVertices(0f, 0f, object : TerrainProvider {
            override fun getSizeX(): Float = terrainProvider.sizeX

            override fun getSizeY(): Float = terrainProvider.sizeY

            override fun getHeight(x: Float, y: Float): Float = -1f

            override fun getColor(x: Float, y: Float): Float = terrainProvider.getColor(x - PATCH_WIDTH, y - PATCH_HEIGHT)
        }, vertexArray, heightMap)
        vertexBuffer.setSubData(vertexBufferFilled, vertexArray)
        val model = Model(vao, TERRAIN_PATCH_INDEX_COUNT, 0, baseVertex)
        seaPatch = TerrainPatch(0f, 0f, heightMap, model)
    }

    private fun heightAtVertex(x: Int, y: Int): Float {
        val patchX = Math.floorDiv(x, PATCH_UNIT_SIZE)
        if (patchX < 0 || patchX >= patchAmountX) {
            return -1f
        }
        val patchY = Math.floorDiv(y, PATCH_UNIT_SIZE)
        if (patchY < 0 || patchY >= patchAmountY) {
            return -1f
        }

        val inPatchX = Math.floorMod(x, PATCH_UNIT_SIZE)
        val inPatchY = Math.floorMod(y, PATCH_UNIT_SIZE)
        return patches[patchY * patchAmountX + patchX].heightMap[inPatchY * PATCH_SIZE + inPatchX]
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

                if (x >= 0 && y >= 0 && x < patchAmountX && y < patchAmountY) {
                    val patch = patches[patchAmountX * y + x]
                    if (frustum.boundsInFrustum(patch.boundingBox)) {
                        val model = batch.render()
                        model.set(patch.model)
                        model.shader = TERRAIN_SHADER
                        model.order = cameraPosition.dst2(x * PATCH_WIDTH + PATCH_WIDTH * 0.5f, y * PATCH_HEIGHT + PATCH_HEIGHT * 0.5f, 0f)
                        /*if (model.order > lodDistance2) {
                            model.offset = TERRAIN_PATCH_INDEX_COUNT
                            model.count = TERRAIN_PATCH_LOD1_INDEX_COUNT
                        }*/
                    }
                } else {
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
                        model.shader = TERRAIN_OCEAN_SHADER
                        model.attributes[TERRAIN_OCEAN_OFFSET_ATTRIBUTE].set(xOff, yOff)
                        model.order = cameraPosition.dst2(xOff + PATCH_WIDTH * 0.5f, yOff + PATCH_HEIGHT * 0.5f, 0f)
                        if (model.order > lodDistance2) {
                            model.offset = TERRAIN_PATCH_INDEX_COUNT
                            model.count = TERRAIN_PATCH_LOD1_INDEX_COUNT
                        }
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