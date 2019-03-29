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

    fun reserve(elementCount:Int, glType:Int) {
        currentType = glType
        val byteSize = elementCount * glSizeOf(glType)
        currentLengthBytes = byteSize
        Gdx.gl20.apply {
            glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)
            glBufferData(GL20.GL_ARRAY_BUFFER, byteSize, null, usage)
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
        }
    }

    fun setData(buffer: ByteBuffer, glType:Int) {
        val byteLength = buffer.remaining()
        currentType = glType
        currentLengthBytes = byteLength

        Gdx.gl20.apply {
            glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)
            glBufferData(GL20.GL_ARRAY_BUFFER, byteLength, buffer, usage)
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
        }
    }

    fun setSubData(subElementOffset:Int, buffer:ByteBuffer) {
        val byteOffset = glSizeOf(currentType) * subElementOffset
        val byteSize = buffer.remaining()

        assert(byteOffset + byteSize <= currentLengthBytes) { "$byteOffset + $byteSize <= $currentLengthBytes" }

        Gdx.gl20.apply {
            glBindBuffer(GL20.GL_ARRAY_BUFFER, handle)
            glBufferSubData(GL20.GL_ARRAY_BUFFER, byteOffset, byteSize, buffer)
            glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)
        }
    }

    fun setData(data:FloatArray, offset:Int = 0, length:Int = data.size) {
        setData(bufferAsFloatFor(length) {
            it.put(data, offset, length)
            it.flip()
        }, GL30.GL_FLOAT)
    }

    fun setSubData(subElementOffset:Int, data:FloatArray, offset:Int = 0, length:Int = data.size) {
        setSubData(subElementOffset, bufferAsFloatFor(length) {
            it.put(data, offset, length)
            it.flip()
        })
    }

    fun setData(data:ShortArray, offset:Int = 0, length:Int = data.size) {
        setData(bufferAsShortFor(length) {
            it.put(data, offset, length)
            it.flip()
        }, GL30.GL_UNSIGNED_SHORT)
    }

    fun setSubData(subElementOffset:Int, data:ShortArray, offset:Int = 0, length:Int = data.size) {
        setSubData(subElementOffset, bufferAsShortFor(length) {
            it.put(data, offset, length)
            it.flip()
        })
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

    fun getDataDebugFloat():FloatArray {
        val buffer = getDataDebug().asFloatBuffer()
        val result = FloatArray(buffer.remaining())
        buffer.get(result)
        return result
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

        private inline fun bufferAsShortFor(shorts:Int, use:(ShortBuffer)->Unit):ByteBuffer {
            var buffer = buffer
            var shortBuffer = shortBuffer
            if (buffer != null && shortBuffer != null && shortBuffer.capacity() >= shorts) {
                shortBuffer.clear()
            } else {
                buffer = bufferFor(shorts * Short.BYTES)
                shortBuffer = buffer.asShortBuffer()!!
                Companion.shortBuffer = shortBuffer
            }
            use(shortBuffer)
            buffer.limit(shortBuffer.limit() * Short.BYTES)
            return buffer
        }

        private inline fun bufferAsFloatFor(floats:Int, use:(FloatBuffer)->Unit):ByteBuffer {
            var buffer = buffer
            var floatBuffer = floatBuffer
            if (buffer != null && floatBuffer != null && floatBuffer.capacity() >= floats) {
                floatBuffer.clear()
            } else {
                buffer = bufferFor(floats * Float.BYTES)
                floatBuffer = buffer.asFloatBuffer()!!
                Companion.floatBuffer = floatBuffer
            }
            use(floatBuffer)
            buffer.limit(floatBuffer.limit() * Float.BYTES)
            return buffer
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