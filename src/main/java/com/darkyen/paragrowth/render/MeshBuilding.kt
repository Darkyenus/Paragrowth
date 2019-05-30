@file:JvmName("MeshBuilding")
package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.util.GdxFloatArray
import com.darkyen.paragrowth.util.GdxShortArray
import com.badlogic.gdx.graphics.VertexAttribute as GdxVertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes as GdxVertexAttributes

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

    inline fun box(createVertex: ModelBuilder.(x:Float, y:Float, z:Float) -> Short) {
        val i000 = createVertex(this, -0.5f, -0.5f, -0.5f)
        val i100 = createVertex(this,  0.5f, -0.5f, -0.5f)
        val i110 = createVertex(this,  0.5f,  0.5f, -0.5f)
        val i010 = createVertex(this, -0.5f,  0.5f, -0.5f)
        val i001 = createVertex(this, -0.5f, -0.5f,  0.5f)
        val i101 = createVertex(this,  0.5f, -0.5f,  0.5f)
        val i111 = createVertex(this,  0.5f,  0.5f,  0.5f)
        val i011 = createVertex(this, -0.5f,  0.5f,  0.5f)

        triangleRect(i000, i100, i110, i010)
        triangleRect(i101, i001, i011, i111)
        triangleRect(i000, i010, i011, i001)
        triangleRect(i101, i111, i110, i100)
        triangleRect(i101, i100, i000, i001)
        triangleRect(i110, i111, i011, i010)
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