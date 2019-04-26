package com.darkyen.paragrowth.doodad

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.darkyen.paragrowth.WorldCharacteristics
import com.darkyen.paragrowth.render.ModelBuilder
import com.darkyen.paragrowth.util.Color
import com.darkyen.paragrowth.util.VectorUtils
import com.darkyen.paragrowth.util.fudge

import java.util.Random

/**
 * Warning: Mess inside.
 */
internal class DoodadInstance(val template: Doodad, rootWidth: Float, sides: Int, private val trunkColor: Float) {

    val rootWidth: Float
    val sides: Int
    val position = Vector3()
    var root: TrunkInstance? = null

    var blendVerticesFrom:Short = -1
    var blendVerticesTo:Short = -1
    var blendVerticesHeight:Float = 0f

    init {
        this.rootWidth = Math.max(rootWidth, 0.1f)
        this.sides = Math.max(sides, 2)
    }

    internal class TrunkInstance(val tag: String, val length: Float, val endWidth: Float) {
        val end = Vector3()
        val direction = Vector3()
        val trunkChildren = Array<TrunkInstance>(false, 8, TrunkInstance::class.java)
        val leafChildren = Array<LeafInstance>(false, 8, LeafInstance::class.java)
    }

    internal interface LeafInstance {
        fun build(builder: ModelBuilder, trunk: TrunkInstance, random: Random, characteristics: WorldCharacteristics)
    }

    internal class HullLeafInstance(val tag: String, val sides: Int, val ringsPre: Int, val ringsPost: Int, val widest: Float, val width: Float, val color: Float) : LeafInstance {
        val end = Vector3()

        private fun widthAt(progress: Float): Float {
            if (progress <= 0f || progress >= 1f) {
                return progress * width
            }
            return if (progress < widest) {
                Interpolation.circleOut.apply(progress / widest) * width
            } else {
                Interpolation.circleOut.apply(1f - (progress - widest) / (1f - widest)) * width
            }
        }

        override fun build(builder: ModelBuilder, trunk: TrunkInstance, random: Random, characteristics: WorldCharacteristics) {
            // 0 - start cap
            // ringsPre * start rings
            // widest - mid ring
            // ringsPost * end rings
            // 1 - end cap

            val pos = Vector3()
            val stepPercentPre = widest / (ringsPre + 1)
            val stepPercentPost = (1f - widest) / (ringsPost + 1)

            val startCap = createCap(builder, trunk.end, trunk.direction, 0f, random, color, characteristics.coherence)
            var ring: Short = -1

            var progress = 0f
            for (i in 0 until ringsPre) {
                progress += stepPercentPre
                val newRing = createRing(builder, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence)
                if (i == 0) {
                    joinRingCap(builder, newRing, startCap, sides)
                } else {
                    joinRings(builder, ring, newRing, sides)
                }
                ring = newRing
            }

            // Mid ring
            progress = widest // It is assumed that it already approximately is there
            val midRing = createRing(builder, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence)
            if (ringsPre == 0) {
                joinRingCap(builder, midRing, startCap, sides)
            } else {
                joinRings(builder, ring, midRing, sides)
            }
            ring = midRing

            for (i in 0 until ringsPost) {
                progress += stepPercentPost
                val newRing = createRing(builder, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence)
                joinRings(builder, ring, newRing, sides)
                ring = newRing
            }

            val endCap = createCap(builder, end, trunk.direction, 0f, random, color, characteristics.coherence)
            joinRingCap(builder, ring, endCap, sides)
        }
    }

    private fun build(builder: ModelBuilder, trunk: TrunkInstance, trunkColor: Float, baseRing: Short, random: Random, characteristics: WorldCharacteristics) {
        if (trunk.trunkChildren.size == 0) {
            var capBaseRing = baseRing
            if (trunk.endWidth > Doodad.MIN_WIDTH) {
                // Too wide, lets end with standard ring and cap
                val endRing = createRing(builder, sides, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence)
                joinRings(builder, baseRing, endRing, sides)
                capBaseRing = endRing
            }
            // Create top cap
            val cap = createCap(builder, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence)
            joinRingCap(builder, capBaseRing, cap, sides)
        } else {
            val endRing = createRing(builder, sides, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence)
            joinRings(builder, baseRing, endRing, sides)

            for (child in trunk.trunkChildren) {
                build(builder, child, trunkColor, endRing, random, characteristics)
            }
        }

        for (leaf in trunk.leafChildren) {
            leaf.build(builder, trunk, random, characteristics)
        }
    }

    fun build(builder: ModelBuilder, random: Random, characteristics: WorldCharacteristics) {
        // Create bottom cap
        val baseCap = createCap(builder, position, root!!.direction, -rootWidth, random, trunkColor, characteristics.coherence)
        // Create bottom ring
        val baseRing = createRing(builder, sides, position, root!!.direction, rootWidth, random, trunkColor, characteristics.coherence)
        joinRingCap(builder, baseRing, baseCap, sides)

        build(builder, root!!, trunkColor, baseRing, random, characteristics)
    }

    companion object {

        fun ModelBuilder.vertex(xyz:Vector3, color:Color):Short {
            return vertex(xyz.x, xyz.y, xyz.z, color)
        }

        private fun createRing(builder: ModelBuilder, sides: Int, position: Vector3, normal: Vector3, radius: Float, random: Random, color: Float, coherence: Float): Short {
            val tangent = VectorUtils.generateTangent(normal).scl(radius)
            val rot = Matrix3().setToRotation(normal, 360f / sides)

            var vColor = color.fudge(random, coherence, 0.3f)
            val vPos = Vector3().set(position).add(tangent)
            val resultIndex = builder.vertex(vPos, vColor)

            for (i in 1 until sides) {
                tangent.mul(rot)

                vColor = color.fudge(random, coherence, 0.3f)
                vPos.set(position).add(tangent)
                val v = builder.vertex(vPos, vColor)
                assert(v.toInt() == resultIndex + i)
            }

            return resultIndex
        }

        private fun createCap(builder: ModelBuilder, position: Vector3, normal: Vector3, radius: Float, random: Random, color: Float, coherence: Float): Short {
            return builder.vertex(Vector3().set(position).mulAdd(normal, radius), color.fudge(random, coherence, 0.3f))
        }

        private fun joinRings(builder: ModelBuilder, first: Short, second: Short, sides: Int) {
            for (i in 0 until sides) {
                // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
                val i1 = if (i == sides - 1) 0 else i + 1
                builder.index((first + i).toShort(), (first + i1).toShort(), (second + i).toShort())
                builder.index((first + i1).toShort(), (second + i1).toShort(), (second + i).toShort())
            }
        }

        private fun joinRingCap(builder: ModelBuilder, ring: Short, cap: Short, sides: Int) {
            for (i in 0 until sides) {
                // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
                val i1 = if (i == sides - 1) 0 else i + 1
                builder.index((ring + i).toShort(), (ring + i1).toShort(), cap)
            }
        }
    }
}
