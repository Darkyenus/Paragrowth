package com.darkyen.paragrowth.skybox

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.util.Color
import com.darkyen.paragrowth.util.WORLD_BLEND_ATTRIBUTE
import com.darkyen.paragrowth.util.lerpHSB
import com.darkyen.paragrowth.util.rgb

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

        builder.box { x, y, z ->
            vertex(x, y, z)
        }

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

    var lowColor:Color = rgb(0f, 1f, 1f)
    var highColor:Color = rgb(0f, 1f, 1f)

    var lowColorBlend:Color = 0f
    var highColorBlend:Color = 0f

    override fun render(batch: RenderBatch, camera: Camera) {
        val colorBlend = batch.attributes[WORLD_BLEND_ATTRIBUTE][0]
        batch.render().apply {
            attributes[LOW_COLOR_ATTRIBUTE][0] = lerpHSB(lowColor, lowColorBlend, colorBlend)
            attributes[HIGH_COLOR_ATTRIBUTE][0] = lerpHSB(highColor, highColorBlend, colorBlend)
            primitiveType = GL20.GL_TRIANGLES
            count = this@Skybox.count
            this.vao = this@Skybox.vao
            shader = SkyboxShader
        }
    }
}

private val LOW_COLOR_ATTRIBUTE = attributeKeyFloat("lowColor")
private val HIGH_COLOR_ATTRIBUTE = attributeKeyFloat("highColor")

object SkyboxShader : Shader(SKYBOX, "sky", SKYBOX_ATTRIBUTES) {

    init {
        globalUniform("u_cameraUp") { uniform, camera, _ ->
            uniform.set(camera.up)
        }

        val resultCombined = Matrix4()
        val tmp = Matrix4()
        globalUniform("u_viewTurnMat") { uniform, camera, _ ->
            val origCam = camera as PerspectiveCamera
            //Sets it to origCam.combined but without the translation part
            uniform.set(resultCombined.set(origCam.projection).mul(tmp.setToLookAt(origCam.direction, origCam.up)))
        }

        localUniform("u_low_color") { uniform, _, renderable ->
            uniform.setColor(renderable.attributes[LOW_COLOR_ATTRIBUTE][0])
        }

        localUniform("u_high_color") { uniform, _, renderable ->
            uniform.setColor(renderable.attributes[HIGH_COLOR_ATTRIBUTE][0])
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_NONE)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(false)
    }
}