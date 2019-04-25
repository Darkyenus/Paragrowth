package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL30
import com.darkyen.paragrowth.render.GlBuffer.Companion.glNameOfType

/**
 * Represents a single vertex attribute.
 * @param name as which it appears in the shader program source
 * @param numComponents of the vector: 1, 2, 3 or 4
 * @param normalized `true` = convert the full range of the value to `0..1` or `-1..1` range, `false` = convert to float directly, `null` = no conversion, shader type is integer
 * @param arraySize amount of vectors in array, matrix or combination of thereof
 */
class VertexAttribute(val name:String, val type:Int, val numComponents:Int, val normalized:Boolean? = false, val arraySize:Int = 1) {
    fun withName(name:String):VertexAttribute {
        return VertexAttribute(name, type, numComponents, normalized, arraySize)
    }
}

val VA_POSITION3 = VertexAttribute("a_position", GL30.GL_FLOAT, 3)
val VA_COLOR1 = VertexAttribute("a_color", GL30.GL_UNSIGNED_BYTE, 3, normalized = true)

class VertexAttributes(vararg val attributes:VertexAttribute) {

    @JvmField
    val locations = run {
        var sum = 0
        IntArray(attributes.size) {
            val result = sum
            sum += attributes[it].arraySize
            result
        }
    }

    override fun hashCode(): Int {
        val attributes = attributes
        var hash = attributes.size
        for (element in attributes)
            hash = 31 * hash + element.type + element.numComponents + element.arraySize
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VertexAttributes) return false
        val size = attributes.size
        if (size != other.attributes.size) return false
        for (i in 0 until size) {
            val our = attributes[i]
            val their = other.attributes[i]
            if (our.type != their.type || our.numComponents != their.numComponents || our.arraySize != their.arraySize)
                return false
        }
        return true
    }

    override fun toString(): String {
        val sb = StringBuilder()
        attributes.joinTo(sb, ", ", "{", "}") {
            sb.append(glNameOfType(it.type)).append(it.numComponents).append(' ').append(it.name)
            if (it.arraySize > 1) {
                sb.append('[').append(it.arraySize).append(']')
            }
            ""
        }
        return sb.toString()
    }
}