package com.darkyen.paragrowth.skybox

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.render.*

val SKYBOX_ATTRIBUTES = VertexAttributes(VA_POSITION3)

/**
 * @author Darkyen
 */
class Skybox : Renderable, Disposable {

    private val vertices: GlBuffer
    private val indices: GlBuffer
    private val vao: GlVertexArrayObject

    private val count:Int

    init {
        val builder = ModelBuilder(3)

        val i000 = builder.vertex(-0.5f, -0.5f, -0.5f)
        val i100 = builder.vertex( 0.5f, -0.5f, -0.5f)
        val i110 = builder.vertex( 0.5f,  0.5f, -0.5f)
        val i010 = builder.vertex(-0.5f,  0.5f, -0.5f)
        val i001 = builder.vertex(-0.5f, -0.5f,  0.5f)
        val i101 = builder.vertex( 0.5f, -0.5f,  0.5f)
        val i111 = builder.vertex( 0.5f,  0.5f,  0.5f)
        val i011 = builder.vertex(-0.5f,  0.5f,  0.5f)

        builder.triangleRect(i000, i100, i110, i010)
        builder.triangleRect(i101, i001, i011, i111)
        builder.triangleRect(i000, i010, i011, i001)
        builder.triangleRect(i101, i111, i110, i100)
        builder.triangleRect(i101, i100, i000, i001)
        builder.triangleRect(i110, i111, i011, i010)

        vertices = builder.createVertexBuffer()
        indices = builder.createIndexBuffer()
        vao = GlVertexArrayObject(indices, SKYBOX_ATTRIBUTES, GlVertexArrayObject.Binding(vertices, 3, 0))

        count = builder.indices.size
    }

    override fun dispose() {
        vertices.dispose()
        indices.dispose()
        vao.dispose()
    }

    override fun render(batch: RenderBatch, camera: Camera) {
        batch.render().apply {
            primitiveType = GL20.GL_TRIANGLES
            count = this@Skybox.count
            this.vao = this@Skybox.vao
            shader = SkyboxShader
        }
    }
}

object SkyboxShader : ParaShader(SKYBOX, "sky", SKYBOX_ATTRIBUTES) {

    init {
        globalUniform("u_cameraUp") { uniform, camera ->
            uniform.set(camera.up)
        }

        val resultCombined = Matrix4()
        val tmp = Matrix4()
        globalUniform("u_viewTurnMat") { uniform, camera ->
            val origCam = camera as PerspectiveCamera
            //Sets it to origCam.combined but without the translation part
            uniform.set(resultCombined.set(origCam.projection).mul(tmp.setToLookAt(origCam.direction, origCam.up)))
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_NONE)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(false)
    }
}