@file:JvmName("MeshBuilding")
package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.util.GdxFloatArray
import com.darkyen.paragrowth.util.GdxShortArray
import com.badlogic.gdx.graphics.VertexAttribute as GdxVertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes as GdxVertexAttributes

/**
 * Shared Mesh Builder instance.
 */
@JvmField
val MESH_BUILDER = MeshBuilder()

/** 3D Position & color attributes  */
@JvmField
val POSITION3_COLOR1_ATTRIBUTES = GdxVertexAttributes(
        GdxVertexAttribute.Position(), //3
        GdxVertexAttribute.ColorPacked()//1
)


inline fun buildMesh(attributes:GdxVertexAttributes, build:MeshBuilder.() -> Unit): Mesh {
    val builder = MESH_BUILDER
    builder.begin(attributes)
    builder.build()
    return builder.end()
}

class ModelBuilder(val vertexFloats:Int) {

    val indices = GdxShortArray()
    val vertices = GdxFloatArray()
    var nextIndex:Short = 0
        private set

    fun vertex(vararg v:Float):Short {
        assert(v.size == vertexFloats)
        vertices.addAll(*v)
        return nextIndex++
    }

    fun index(vararg i:Short) {
        indices.addAll(*i)
    }

    fun triangleRect(corner00: Short, corner10: Short, corner11: Short, corner01: Short) {
        index(corner00, corner10, corner11, corner11, corner01, corner00)
    }

    fun createVertexBuffer(static:Boolean = true):GlBuffer {
        val buffer = GlBuffer(if (static) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW)
        buffer.setData(vertices.items, 0, vertices.size)
        return buffer
    }

    fun createIndexBuffer(static:Boolean = true):GlBuffer {
        val buffer = GlBuffer(if (static) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW)
        buffer.setData(indices.items, 0, indices.size)
        return buffer
    }

    fun computeBoundingBox3D(offset:Int, stride:Int, out:BoundingBox) {
        val vertices = vertices.items
        for (i in 0 until nextIndex) {
            val off = i * stride + offset
            val x = vertices[off]
            val y = vertices[off + 1]
            val z = vertices[off + 2]

            out.ext(x, y, z)
        }
    }

    fun computeMax(offset:Int, stride:Int, count:Int):Float {
        var max = Float.NEGATIVE_INFINITY

        val vertices = vertices.items
        var i = offset
        for (y in 0 until count) {
            max = maxOf(max, vertices[i])
            i += stride
        }

        return max
    }
}