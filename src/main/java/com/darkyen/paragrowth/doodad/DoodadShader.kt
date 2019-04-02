package com.darkyen.paragrowth.doodad

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.darkyen.paragrowth.render.POS3_COL1_ATTRS
import com.darkyen.paragrowth.render.Shader

/**
 *
 */
object DoodadShader : Shader(DOODADS, "doodad", POS3_COL1_ATTRS) {

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