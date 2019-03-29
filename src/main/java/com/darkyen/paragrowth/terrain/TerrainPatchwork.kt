package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.render.GlBuffer
import com.darkyen.paragrowth.render.RenderBatch
import com.darkyen.paragrowth.render.Renderable
import com.darkyen.paragrowth.terrain.generator.TerrainProvider

/**
 * A collection of terrain patches.
 */
class TerrainPatchwork(terrainProvider: TerrainProvider) : Renderable, Disposable {

    private val patchAmountX: Int = MathUtils.ceilPositive(terrainProvider.sizeX / PATCH_WIDTH)
    private val patchAmountY: Int = MathUtils.ceilPositive(terrainProvider.sizeY / PATCH_HEIGHT)
    private val patches: Array<TerrainPatch>
    private val seaPatch: TerrainPatch

    private val xyBuffer:GlBuffer
    private val indicesBuffer:GlBuffer

    init {
        @Suppress("UNCHECKED_CAST")
        this.patches = arrayOfNulls<TerrainPatch>(patchAmountX * patchAmountY) as Array<TerrainPatch>

        val xy = generateTerrainPatchVertexXY()
        val xyBuffer = GlBuffer(GL20.GL_STATIC_DRAW)
        xyBuffer.setData(xy)
        this.xyBuffer = xyBuffer

        val indicesBuffer = GlBuffer(GL20.GL_STATIC_DRAW)
        indicesBuffer.setData(generateTerrainPatchIndices())
        this.indicesBuffer = indicesBuffer

        var i = 0
        for (y in 0 until patchAmountY) {
            for (x in 0 until patchAmountX) {
                patches[i++] = createTerrainPatch(x * PATCH_WIDTH, y * PATCH_HEIGHT, terrainProvider, xy, xyBuffer, indicesBuffer)
            }
        }

        this.seaPatch = createWaterTerrainPatch(xy, xyBuffer, indicesBuffer, terrainProvider)
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
                        patch.fillRenderModel(model)
                        model.order = camera.position.dst2(x * PATCH_WIDTH + PATCH_WIDTH * 0.5f, y * PATCH_HEIGHT + PATCH_HEIGHT * 0.5f, 0f)
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
                        patch.fillRenderModel(model)
                        model.worldTransform.translate(xOff, yOff, 0f)
                        model.order = camera.position.dst2(xOff + PATCH_WIDTH * 0.5f, yOff + PATCH_HEIGHT * 0.5f, 0f)
                    }
                }
            }
        }
    }

    override fun dispose() {
        xyBuffer.dispose()
        indicesBuffer.dispose()
        for (patch in patches) {
            patch.dispose()
        }
        seaPatch.dispose()
    }
}
