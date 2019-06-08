@file:JvmName("VectorUtils")
package com.darkyen.paragrowth.util

import com.badlogic.gdx.math.*
import kotlin.math.cos
import kotlin.math.sin

fun generateTangent(normal: Vector3): Vector3 {
    // https://stackoverflow.com/questions/5255806/how-to-calculate-tangent-and-binormal
    val c1 = Vector3().set(0f, 1f, 0f).crs(normal)
    val c2 = Vector3().set(0f, 0f, 1f).crs(normal)
    return if (c1.len2() > c2.len2()) {
        c1
    } else {
        c2
    }
}

fun toNormalSpace(vector: Vector3, normal: Vector3) {
    val tangent = generateTangent(normal)
    val biTangent = Vector3().set(normal).crs(tangent)
    val mat = Matrix3()
    mat.`val`[Matrix3.M00] = tangent.x
    mat.`val`[Matrix3.M01] = tangent.y
    mat.`val`[Matrix3.M02] = tangent.z
    mat.`val`[Matrix3.M10] = biTangent.x
    mat.`val`[Matrix3.M11] = biTangent.y
    mat.`val`[Matrix3.M12] = biTangent.z
    mat.`val`[Matrix3.M20] = normal.x
    mat.`val`[Matrix3.M21] = normal.y
    mat.`val`[Matrix3.M22] = normal.z

    vector.mul(mat)
}


/**
 * Returns a re-mapped float value from inRange to outRange.
 */
fun map(value: Float, inRangeStart: Float, inRangeEnd: Float, outRangeStart: Float, outRangeEnd: Float): Float {
    return outRangeStart + (outRangeEnd - outRangeStart) * ((value - inRangeStart) / (inRangeEnd - inRangeStart))
}

/** See [Vector2.angleRad] */
fun angleRad(x:Float, y:Float):Float = Math.atan2(y.toDouble(), x.toDouble()).toFloat()

fun Vector2.addRotated(x:Float, y:Float, radians:Float):Vector2 {
    val cos = cos(radians)
    val sin = sin(radians)

    val newX = x * cos - y * sin
    val newY = x * sin + y * cos
    return add(newX, newY)
}

fun Vector2.clampTo(area: Rectangle):Vector2 {
    x = MathUtils.clamp(x, area.x, area.x + area.width)
    y = MathUtils.clamp(y, area.y, area.y + area.height)
    return this
}