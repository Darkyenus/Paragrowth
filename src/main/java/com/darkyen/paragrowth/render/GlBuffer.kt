package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.util.stack
import org.lwjgl.opengl.GL15
import org.lwjgl.system.MemoryUtil
import java.lang.Float
import java.lang.Short
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 *
 * See See http://docs.gl/gl3/glBufferData
 */
class GlBuffer(
        /** One of GL_STREAM_DRAW, GL_STREAM_READ, GL_STREAM_COPY, GL_STATIC_DRAW, GL_STATIC_READ, GL_STATIC_COPY, GL_DYNAMIC_DRAW, GL_DYNAMIC_READ, or GL_DYNAMIC_COPY */
        val usage:Int) : Disposable {

    val handle:Int = Gdx.gl.glGenBuffer()

    var currentType:Int = -1
        private set
    var currentLengthBytes:Int = 0
        private set

    fun setData(buffer: Buffer, glType:Int) {
        currentType = glType
        currentLengthBytes = buffer.limit() * glSizeOf(glType)

        Gdx.gl20.apply {
            glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)
            glBufferData(GL20.GL_ARRAY_BUFFER, buffer.limit(), buffer, usage)
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
        }
    }

    fun setData(data:FloatArray, offset:Int = 0, length:Int = data.size) {
        val buffer = floatBufferFor(length)
        buffer.put(data, offset, length)
        buffer.flip()
        setData(buffer, GL30.GL_FLOAT)
    }

    fun setData(data:ShortArray, offset:Int = 0, length:Int = data.size) {
        val buffer = shortBufferFor(length)
        buffer.put(data, offset, length)
        buffer.flip()
        setData(buffer, GL30.GL_UNSIGNED_SHORT)
    }

    fun getDataDebug():ByteBuffer {
        return Gdx.gl20.run {
            glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)

            val size = stack {
                val int = callocInt(1)
                glGetBufferParameteriv(GL20.GL_ARRAY_BUFFER, GL20.GL_BUFFER_SIZE, int)
                int[0]
            }
            val data = MemoryUtil.memCalloc(size)
            GL15.glGetBufferSubData(GL20.GL_ARRAY_BUFFER, 0, data)
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
            data
        }
    }

    override fun dispose() {
        Gdx.gl20.apply {
            glDeleteBuffer(handle)
        }
    }

    companion object {
        private var buffer: ByteBuffer? = null
        private var shortBuffer: ShortBuffer? = null
        private var floatBuffer: FloatBuffer? = null

        private fun bufferFor(bytes:Int):ByteBuffer {
            var buffer = buffer
            if (buffer != null && bytes <= buffer.capacity()) {
                buffer.clear()
                return buffer
            }
            val newSize = MathUtils.nextPowerOfTwo(bytes)
            buffer = BufferUtils.newUnsafeByteBuffer(newSize)
            Companion.buffer = buffer
            shortBuffer = null
            floatBuffer = null
            return buffer
        }

        private fun shortBufferFor(shorts:Int):ShortBuffer {
            var shortBuffer = shortBuffer
            if (shortBuffer != null && shortBuffer.capacity() >= shorts) {
                shortBuffer.clear()
                return shortBuffer
            }
            shortBuffer = bufferFor(shorts * Short.BYTES).asShortBuffer()
            Companion.shortBuffer = shortBuffer
            return shortBuffer
        }

        private fun floatBufferFor(floats:Int):FloatBuffer {
            var floatBuffer = floatBuffer
            if (floatBuffer != null && floatBuffer.capacity() >= floats) {
                floatBuffer.clear()
                return floatBuffer
            }
            floatBuffer = bufferFor(floats * Float.BYTES).asFloatBuffer()
            Companion.floatBuffer = floatBuffer
            return floatBuffer
        }

        fun glSizeOf(glType:Int):Int {
            return when (glType) {
                GL20.GL_UNSIGNED_BYTE, GL20.GL_BYTE -> 1
                GL20.GL_UNSIGNED_SHORT, GL20.GL_SHORT, GL30.GL_HALF_FLOAT -> 2
                GL30.GL_UNSIGNED_INT, GL30.GL_INT, GL20.GL_FLOAT, GL20.GL_FIXED,
                GL30.GL_INT_2_10_10_10_REV, GL30.GL_UNSIGNED_INT_2_10_10_10_REV, GL30.GL_UNSIGNED_INT_10F_11F_11F_REV -> 4
                else -> {
                    assert(false) { "Unknown type $glType" }
                    -1
                }
            }
        }

        fun glNameOfType(glType:Int):String {
            return when (glType) {
                GL30.GL_BYTE -> "GL_BYTE"
                GL30.GL_UNSIGNED_BYTE -> "GL_UNSIGNED_BYTE"
                GL30.GL_SHORT -> "GL_SHORT"
                GL30.GL_UNSIGNED_SHORT -> "GL_UNSIGNED_SHORT"
                GL30.GL_INT -> "GL_INT"
                GL30.GL_UNSIGNED_INT -> "GL_UNSIGNED_INT"
                GL30.GL_HALF_FLOAT -> "GL_HALF_FLOAT"
                GL30.GL_FLOAT -> "GL_FLOAT"
                GL30.GL_FIXED -> "GL_FIXED"
                GL30.GL_INT_2_10_10_10_REV -> "GL_INT_2_10_10_10_REV"
                GL30.GL_UNSIGNED_INT_2_10_10_10_REV -> "GL_UNSIGNED_INT_2_10_10_10_REV"
                GL30.GL_UNSIGNED_INT_10F_11F_11F_REV -> "GL_UNSIGNED_INT_10F_11F_11F_REV"
                else -> "UNKNOWN($glType)"
            }
        }
    }
}