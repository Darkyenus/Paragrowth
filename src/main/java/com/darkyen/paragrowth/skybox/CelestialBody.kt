package com.darkyen.paragrowth.skybox

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.util.Color

/** 3D Position & color attributes  */
val CELESTIAL_BODY_ATTRIBUTES = VertexAttributes(
        VA_POSITION3, //3
        VA_COLOR1 //1
)

/**
 * Represents sun, moons, stars...
 */
class CelestialBody(builder: ModelBuilder) : Renderable {

    /** Unit vector pointing at the position of the body on the sky.  */
    val position = Vector3(0f, 0f, 1f)

    override fun render(batch: RenderBatch, camera: Camera) {
        val model = batch.render()

        TODO("not implemented")
    }
}

/** Mesh with center point, from which triangles which form outer edge grow.
 * Outer edge may not be smooth, but form a sawtooth, where innerRadius is the inner radius
 * and spokeRadius is the outer radius.
 * @param spokes at least 3 */
fun createSun(spokes: Int, innerRadius: Float, spokeRadius: Float, color: Color): ModelBuilder {
    assert(spokes >= 3)

    val builder = ModelBuilder(4)
    builder.vertex()

    val center = builder.vertex(0f, 0f, 0f, color)
    var angle = MathUtils.random(MathUtils.PI2)
    val angleStep = MathUtils.PI2 / spokes

    for (spoke in 0 until spokes) {
        val x = MathUtils.sin(angle)
        val y = MathUtils.cos(angle)

        val outer = builder.vertex(x * spokeRadius, y * spokeRadius, 0f, color)
        val inner = builder.vertex(x * innerRadius, y * innerRadius, 0f, color)
        builder.index(center, outer, ((inner + 2) % spokes * 2).toShort())

        angle += angleStep
    }

    return builder
}
