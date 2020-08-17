package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.darkyen.paragrowth.render.Shader.Companion.NULL_SHADER
import com.darkyen.paragrowth.render.Shader.Companion.NULL_VAO

/** Holds info needed to draw something. */
class RenderModel : Comparable<RenderModel> {

    /** The primitive type, OpenGL constant e.g: [GL20.GL_TRIANGLES], [GL20.GL_POINTS], [GL20.GL_LINES],
     * [GL20.GL_LINE_STRIP], [GL20.GL_TRIANGLE_STRIP]  */
    var primitiveType: Int = 0

    /** The offset in the [.mesh] to this part. If the mesh is indexed ([Mesh.getNumIndices] > 0), this is the offset
     * in the indices array, otherwise it is the offset in the vertices array.  */
    var offset: Int = 0

    /** Value to be added to indices to find the vertex to draw */
    var baseVertex:Int = 0

    /** The size (in total number of vertices) of this part in the [.mesh]. When the mesh is indexed (
     * [Mesh.getNumIndices] > 0), this is the number of indices, otherwise it is the number of vertices.  */
    var count: Int = 0

    /** Amount of instances to draw. 0 to disable instancing. */
    var instances:Int = 0

    /** Holds bound buffers to render. */
    var vao:GlVertexArrayObject = NULL_VAO

    /** The [Shader] to be used to render this [RenderModel] using a [RenderBatch]. */
    var shader:Shader = NULL_SHADER

    /** Attributes to pass on to shaders for this object. Backed by attributes of the [RenderBatch]. */
    val attributes = Attributes(ModelAttributeLayer)

    /** After sorting by shader, items are ordered by this. Smaller values are sorted earlier.
     * Can be anything, typically it is a distance to the camera. */
    var order:Float = 0f

    fun set(model:Model) {
        primitiveType = model.primitive
        offset = model.indexOffset
        baseVertex = model.baseVertex
        count = model.indexCount
        vao = model.vao
    }

    fun reset() {
        primitiveType = 0
        offset = 0
        baseVertex = 0
        count = 0
        instances = 0
        vao = NULL_VAO
        shader = NULL_SHADER
        attributes.clear()
        order = 0f
    }

    override fun compareTo(other: RenderModel): Int {
        val byShader = shader.order.compareTo(other.shader.order)
        if (byShader != 0) {
            return byShader
        }

        return order.compareTo(other.order)
    }
}