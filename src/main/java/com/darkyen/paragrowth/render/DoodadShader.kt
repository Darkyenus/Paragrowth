package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Matrix4
import com.darkyen.paragrowth.util.PrioritizedShader.DOODADS

/**
 *
 */
class DoodadShader : ParaShader(DOODADS, "doodad", POSITION3_COLOR1_ATTRIBUTES) {

    init {
        val tmpMat4 = Matrix4()
        localUniform("u_projViewWorldTrans") { uniform, camera, renderable ->
            uniform.set(tmpMat4.set(camera.combined).mul(renderable.worldTransform))
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_NONE)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}