package com.darkyen.paragrowth.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.collision.BoundingBox

/**
 *
 */
fun BoundingBox.forEdges(action:(x1:Float, y1:Float, z1:Float, x2:Float, y2:Float, z2:Float) -> Unit) {
    // Bottom ring
    action(min.x, min.y, min.z, max.x, min.y, min.z)
    action(max.x, min.y, min.z, max.x, max.y, min.z)
    action(max.x, max.y, min.z, min.x, max.y, min.z)
    action(min.x, max.y, min.z, min.x, min.y, min.z)

    // Top ring
    action(min.x, min.y, max.z, max.x, min.y, max.z)
    action(max.x, min.y, max.z, max.x, max.y, max.z)
    action(max.x, max.y, max.z, min.x, max.y, max.z)
    action(min.x, max.y, max.z, min.x, min.y, max.z)

    // Struts
    action(min.x, min.y, min.z, min.x, min.y, max.z)
    action(max.x, min.y, min.z, max.x, min.y, max.z)
    action(max.x, max.y, min.z, max.x, max.y, max.z)
    action(min.x, max.y, min.z, min.x, max.y, max.z)
}

val DebugShader by lazy {
    AutoReloadShaderProgram(Gdx.files.local("debug_vert.glsl"), Gdx.files.local("debug_frag.glsl"))
}
