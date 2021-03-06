package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.render.GlBuffer.Companion.glSizeOf
import com.badlogic.gdx.utils.Array as GdxArray

/**
 * @param vertexAttributes for which this VAO should be built
 * @param bindings matching array of concrete bindings for individual [vertexAttributes] attributes
 */
class GlVertexArrayObject(val indices:GlBuffer?, val vertexAttributes:VertexAttributes, vararg val bindings: Binding) : Disposable {

    init {
        assert(vertexAttributes.attributes.size == bindings.size) { "Incompatible vertex attributes and their bindings" }
    }

    val handle = run {
        val buf = IntArray(1)
        Gdx.gl30.glGenVertexArrays(1, buf, 0)
        val handle = buf[0]
        Gdx.gl30.apply {
            glBindVertexArray(handle)
            for ((i, binding) in bindings.withIndex()) {
                glEnableVertexAttribArray(i)
                glBindBuffer(GL20.GL_ARRAY_BUFFER, binding.buffer.handle)
                val attr = vertexAttributes.attributes[i]
                if (attr.normalized != null) {
                    glVertexAttribPointer(i, attr.numComponents, attr.type, attr.normalized, binding.stride * binding.elementSize, binding.offset  * binding.elementSize)
                } else {
                    glVertexAttribIPointer(i, attr.numComponents, attr.type, binding.stride * binding.elementSize, binding.offset * binding.elementSize)
                }
                glVertexAttribDivisor(i, attr.instancingDivisor)
            }
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
            // Bind indices, if any
            glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, indices?.handle ?: 0)
            glBindVertexArray(0)
        }
        handle
    }

    override fun dispose() {
        Gdx.gl30.glDeleteVertexArrays(1, intArrayOf(handle), 0)
    }

    /**
     * @param buffer with the data
     * @param stride in bytes
     * @param offset in bytes
     * See https://www.khronos.org/opengl/wiki/GLAPI/glVertexAttribPointer
     */
    class Binding @JvmOverloads constructor(val buffer: GlBuffer, val stride:Int, val offset:Int, val elementSize:Int = glSizeOf(buffer.currentType))
}