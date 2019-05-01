package com.darkyen.paragrowth.doodad

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.WorldCharacteristics
import com.darkyen.paragrowth.WorldSpecifics
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.util.*
import org.lwjgl.opengl.GL15
import java.nio.FloatBuffer

/**
 *
 */
class DoodadWorld private constructor(seed: Long, world: WorldSpecifics) : Disposable {

    private val patches: GdxArray<DoodadPatch>

    private var generatePatchTasks:GdxArray<Delayed<Int>>? = GdxArray()

    init {
        val minPatchX = MathUtils.floor(world.offsetX / PATCH_SIZE)
        val minPatchY = MathUtils.floor(world.offsetY / PATCH_SIZE)
        val maxPatchX = MathUtils.ceil((world.offsetX + world.sizeX()) / PATCH_SIZE)
        val maxPatchY = MathUtils.ceil((world.offsetY + world.sizeY()) / PATCH_SIZE)

        this.patches = GdxArray(false,(maxPatchX - minPatchX) * (maxPatchY - minPatchY), DoodadPatch::class.java)

        val doodadSet = Doodads.createDoodadSet(RandomXS128(seed), world.characteristics)

        for (x in minPatchX until maxPatchX) {
            for (y in minPatchY until maxPatchY) {
                val instances = GdxArray<DoodadInstance>(DoodadInstance::class.java)
                generatePatchTasks!!.add(offload {
                    buildPatch(seed + x + y * (maxPatchX - minPatchY), world, (x * PATCH_SIZE).toFloat(), (y * PATCH_SIZE).toFloat(), doodadSet, instances, world.characteristics)
                }.map { builder ->
                    if (builder.indices.size == 0) {
                        0
                    } else {
                        val patch = completePatch(builder, instances)
                        patches.add(patch)
                        patch.doodads.size
                    }
                })
            }
        }
    }

    private fun tryCompleteInitialization():Boolean {
        val generatePatchTasks = generatePatchTasks ?: return true
        var totalDoodads = 0
        for (task in generatePatchTasks) {
            totalDoodads += task.poll() ?: return false
        }
        this.generatePatchTasks = null
        return true
    }

    private fun completeInitialization():Boolean {
        val generatePatchTasks = generatePatchTasks ?: return true
        var totalDoodads = 0
        for (task in generatePatchTasks) {
            totalDoodads += task.get()
        }
        this.generatePatchTasks = null
        return true
    }

    fun render(batch: RenderBatch, camera: Camera, blendIn:Boolean) {
        val patches = this.patches
        val frustum = camera.frustum

        for (patch in patches) {
            if (!frustum.boundsInFrustum(patch.boundingBox)) {
                continue
            }

            val model = batch.render()
            model.vao = patch.vao
            model.primitiveType = GL20.GL_TRIANGLES
            model.count = patch.count
            model.order = camera.position.dst2(patch.boundingBox.centerX, patch.boundingBox.centerY, patch.boundingBox.centerZ)
            model.shader = if (blendIn) DOODAD_SHADER_BLEND_IN else DOODAD_SHADER_BLEND_OUT
        }
    }

    fun prepareBlendIn(from: WorldSpecifics):Delayed<DoodadWorld> {
        for (patch in patches) {
            patch.blendsMappedData = patch.blends.beginMappedAccess(GL15.GL_WRITE_ONLY).asFloatBuffer()
        }

        return offload {
            for (patch in patches) {
                val blends = patch.blendsMappedData!!
                for (doodad in patch.doodads) {
                    val underZ = from.getHeight(doodad.position.x, doodad.position.y)
                    //val shift = underZ - doodad.position.z - doodad.blendVerticesHeight - 0.5f
                    val shift = underZ - doodad.position.z
                    for (i in doodad.blendVerticesFrom until doodad.blendVerticesTo) {
                        blends.put(i, shift)
                    }
                }
                patch.blendsMappedData = null
            }
        }.map {
            for (patch in patches) {
                patch.blends.endMappedAccess()
            }
            this
        }
    }

    fun prepareBlendOut(to:WorldSpecifics): Delayed<DoodadWorld> {
        for (patch in patches) {
            patch.blendsMappedData = patch.blends.beginMappedAccess(GL15.GL_WRITE_ONLY).asFloatBuffer()
        }

        return immediate {
            for (patch in patches) {
                val blends = patch.blendsMappedData!!
                for (doodad in patch.doodads) {
                    val newZ = to.getHeight(doodad.position.x, doodad.position.y)
                    val shift = newZ - doodad.position.z - doodad.blendVerticesHeight - 0.5f

                    for (i in doodad.blendVerticesFrom until doodad.blendVerticesTo) {
                        blends.put(i, shift)
                    }
                }
                patch.blendsMappedData = null
            }
        }.map {
            for (patch in patches) {
                patch.blends.endMappedAccess()
            }
            this
        }
    }

    fun renderDebug(camera:Camera, renderer: ImmediateModeRenderer) {
        val frustum = camera.frustum

        for (patch in patches) {
            val box = patch.boundingBox
            val shown = frustum.boundsInFrustum(box)

            box.forEdges { x1, y1, z1, x2, y2, z2 ->
                renderer.color(if (shown) Color.GREEN else Color.RED)
                renderer.vertex(x1, y1, z1)
                renderer.color(if (shown) Color.GREEN else Color.RED)
                renderer.vertex(x2, y2, z2)
            }
        }
    }

    override fun dispose() {
        for (patch in patches) {
            patch.indices.dispose()
            patch.vertices.dispose()
            patch.blends.dispose()
            patch.vao.dispose()
        }
        patches.clear()
    }

    private class DoodadPatch(
            val indices:GlBuffer,
            val vertices:GlBuffer,
            val blends:GlBuffer,
            val vao:GlVertexArrayObject,
            val count:Int,
            val doodads:GdxArray<DoodadInstance>) {

        val boundingBox = BoundingBox()

        var blendsMappedData: FloatBuffer? = null
    }

    companion object {
        private const val PATCH_SIZE = 256
        private const val DOODADS_PER_PATCH = 256

        private fun buildPatch(seed: Long, world: WorldSpecifics, baseX: Float, baseY: Float, doodadSet: GdxArray<Doodad>, instances:GdxArray<DoodadInstance>, characteristics: WorldCharacteristics): ModelBuilder {
            val random = RandomXS128(seed)
            val builder = ModelBuilder(3 + 1)

            for (i in 0 until DOODADS_PER_PATCH) {
                val x = baseX + random.nextFloat() * PATCH_SIZE
                val y = baseY + random.nextFloat() * PATCH_SIZE

                val z = world.getHeight(x, y)
                // Cull doodads in water or (TODO:) too close
                if (z <= 0.1f) {
                    continue
                }

                val instanceVerticesFrom = builder.nextIndex
                val instanceVertexCountBefore = builder.vertices.size

                val instance = doodadSet.get(random.nextInt(doodadSet.size)).instantiate(random, x, y, z, world.characteristics)
                instance.build(builder, random, characteristics)

                val instanceVerticesTo = builder.nextIndex
                instance.blendVerticesFrom = instanceVerticesFrom
                instance.blendVerticesTo = instanceVerticesTo
                instance.blendVerticesHeight = maxOf(builder.computeMax(instanceVertexCountBefore + 2, 4, instanceVerticesTo - instanceVerticesFrom) - z, 0f)
                instances.add(instance)
            }

            return builder

        }

        private fun completePatch(builder:ModelBuilder, instances:GdxArray<DoodadInstance>):DoodadPatch {
            val vertices = builder.createVertexBuffer(true)
            val indices = builder.createIndexBuffer(true)

            val blends = GlBuffer(GL20.GL_STATIC_DRAW)
            blends.reserve(builder.nextIndex.toInt(), GL20.GL_FLOAT)

            val vao = GlVertexArrayObject(indices, DOODAD_ATTRIBUTES,
                    GlVertexArrayObject.Binding(vertices, 4, 0),
                    GlVertexArrayObject.Binding(vertices, 4, 3),
                    GlVertexArrayObject.Binding(blends, 1, 0)
            )
            val patch = DoodadPatch(indices, vertices, blends, vao, builder.indices.size, instances)
            builder.computeBoundingBox3D(0, 4, patch.boundingBox)
            return patch
        }

        fun build(seed: Long, world: WorldSpecifics):Delayed<DoodadWorld> {
            val dw = DoodadWorld(seed, world)
            return object : Delayed<DoodadWorld> {
                override fun poll(): DoodadWorld? {
                    return if (dw.tryCompleteInitialization()) {
                        dw
                    } else {
                        null
                    }
                }

                override fun get(): DoodadWorld {
                    dw.completeInitialization()
                    return dw
                }
            }
        }
    }
}
