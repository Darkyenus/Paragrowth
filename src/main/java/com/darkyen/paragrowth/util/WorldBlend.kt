package com.darkyen.paragrowth.util

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.darkyen.paragrowth.render.*

/**
 *
 */
val WORLD_BLEND_LEFT_ATTR = attributeKeyVector3("blend_left", GlobalAttributeLayer)
val WORLD_BLEND_RIGHT_ATTR = attributeKeyVector3("blend_right", GlobalAttributeLayer)
val WORLD_BLEND_ATTRIBUTE = attributeKeyFloat("world_blend", GlobalAttributeLayer)

/** Blend wall which rises (distance from) to the right. */
private fun Attributes.setBlendWall(attr: AttributeKey<Vector3>, center:Vector2, direction: Vector2) {
    // g(x,y)=y*Dot(LN, (-1,0)) + x*Dot(LN, (0,1)) - Dot(C, (0,-1))*Dot(LN, (1,0))- Dot(C, (1,0))*Dot(LN, (0,1))

    // Dot(LN, (0,1))
    val xCoff = direction.y
    // Dot(LN, (-1,0))
    val yCoff = -direction.x
    // C.y*LN.x - C.x*LN.y
    val zCoff = center.y * direction.x - center.x * direction.y
    this[attr].set(xCoff, yCoff, zCoff)
}

fun Shader.setupBlendWallUniforms() {
    globalUniform("u_blendEdgeLeft") { uniform, _, attributes ->
        uniform.set(attributes[WORLD_BLEND_LEFT_ATTR])
    }
    globalUniform("u_blendEdgeRight") { uniform, _, attributes ->
        uniform.set(attributes[WORLD_BLEND_RIGHT_ATTR])
    }
}

private fun Attributes.disableBlendWalls() {
    this[WORLD_BLEND_RIGHT_ATTR].set(0f, 0f, 0.5f)
    this[WORLD_BLEND_LEFT_ATTR].set(0f, 0f, 0.5f)
}

fun Attributes.setBlendWalls(camera: PerspectiveCamera) {
    if (false) {
        disableBlendWalls()
        return
    }

    val center = Vector2(camera.position.x, camera.position.y)
    val leg = Vector2(camera.direction.x,  camera.direction.y).nor()
    val fov = camera.fieldOfView * (camera.viewportWidth / camera.viewportHeight)
    leg.rotate(-fov * 0.5f)
    setBlendWall(WORLD_BLEND_RIGHT_ATTR, center, leg)

    leg.rotate(fov).scl(-1f)
    setBlendWall(WORLD_BLEND_LEFT_ATTR, center, leg)
}

fun Attributes.setBlend(value:Float) {
    val edge = 0.25f

    val alpha:Float =
        when {
            value < edge -> (value / edge) * 0.5f
            value > 1f - edge -> 0.5f + (value - (1f - edge)) / edge * 0.5f
            else -> 0.5f
        }

    this[WORLD_BLEND_ATTRIBUTE][0] = alpha
}

private fun dot(x:Float, y:Float, z:Float, vec:Vector3):Float {
    return x * vec.x + y * vec.y + z * vec.z
}

fun Attributes.getBlendAt(x:Float, y:Float):Float {
    val u_blend:Float = this[WORLD_BLEND_ATTRIBUTE][0]
    val u_blendEdgeLeft = this[WORLD_BLEND_LEFT_ATTR]
    val u_blendEdgeRight = this[WORLD_BLEND_RIGHT_ATTR]
    val MAX_EDGE_DIST_INV = 1f / 50f


    if (u_blend < 0.0f) {
        return 0.0f
    }
    val leftDist:Float = dot(x, y, 1f, u_blendEdgeLeft)
    val rightDist:Float = dot(x, y, 1f, u_blendEdgeRight)
    val maxDist:Float = maxOf(leftDist, rightDist)

    // Goes [0, 1] as u_blend goes [0, 0.5]
    val outerBlend:Float = MathUtils.clamp(u_blend * 2.0f, 0.0f, 1.0f)

    // Blend value for inside of the wedge
    // Goes [0, 1] as u_blend goes [0.5, 1]
    val wedgeBlend:Float = MathUtils.clamp((u_blend - 0.5f) * 2.0f, 0.0f, 1.0f)

    // 0 inside the wedge, 1 completely outside wedge, coming closer to 0 when near the wedge
    val outWedge:Float = MathUtils.clamp(maxDist * MAX_EDGE_DIST_INV, 0f, 1f)
    return MathUtils.lerp(wedgeBlend, outerBlend, outWedge)
}
