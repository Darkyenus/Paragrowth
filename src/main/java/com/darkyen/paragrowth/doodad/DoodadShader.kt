package com.darkyen.paragrowth.doodad

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.darkyen.paragrowth.render.*

val DOODAD_ATTRIBUTES = VertexAttributes(
        VA_POSITION3,
        VA_COLOR1
)

/**
 *
 */
object DoodadShader : Shader(DOODADS, "doodad", DOODAD_ATTRIBUTES) {

    init {
        globalUniform("u_projViewTrans") { uniform, camera, _ ->
            uniform.set(camera.combined)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_NONE)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}