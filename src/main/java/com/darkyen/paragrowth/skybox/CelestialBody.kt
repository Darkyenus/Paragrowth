package com.darkyen.paragrowth.skybox

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool
import com.darkyen.paragrowth.util.Color
import com.darkyen.paragrowth.render.POSITION3_COLOR1_ATTRIBUTES
import com.darkyen.paragrowth.render.buildMesh

/**
 * Represents sun, moons, stars...
 */
class CelestialBody(private val mesh: Mesh) : RenderableProvider {

    /** Unit vector pointing at the position of the body on the sky.  */
    val position = Vector3(0f, 0f, 1f)

    override fun getRenderables(renderables: Array<Renderable>, pool: Pool<Renderable>) {
        val renderable = pool.obtain()
        renderable.meshPart.set("celestial-body", mesh, 0, mesh.numIndices, GL20.GL_TRIANGLES)

        //renderable.worldTransform.setToLookAt()

        renderables.add(renderable)
    }
}

/** Mesh with center point, from which triangles which form outer edge grow.
 * Outer edge may not be smooth, but form a sawtooth, where innerRadius is the inner radius
 * and spokeRadius is the outer radius.
 * @param spokes at least 3 */
fun createSun(spokes: Int, innerRadius: Float, spokeRadius: Float, color: Color): Mesh {
    assert(spokes >= 3)

    return buildMesh(POSITION3_COLOR1_ATTRIBUTES) {
        val center = this.vertex(0f, 0f, 0f, color)
        var angle = MathUtils.random(MathUtils.PI2)
        val angleStep = MathUtils.PI2 / spokes

        for (spoke in 0 until spokes) {
            val x = MathUtils.sin(angle)
            val y = MathUtils.cos(angle)

            val outer = vertex(x * spokeRadius, y * spokeRadius, 0f, color)
            val inner = vertex(x * innerRadius, y * innerRadius, 0f, color)
            triangle(center, outer, ((inner + 2) % spokes * 2).toShort())

            angle += angleStep
        }
    }
}
