package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.darkyen.paragrowth.util.PrioritizedShader.TERRAIN

/**
 *
 */
class TerrainShader : ParaShader(TERRAIN, "terrain", TERRAIN_PATCH_ATTRIBUTES) {

    init {
        localUniform("u_worldTrans") { uniform, _, renderable ->
            uniform.set(renderable.worldTransform)
        }
        globalUniform("u_projViewTrans") { uniform, camera ->
            uniform.set(camera.combined)
        }

        val startTime = System.currentTimeMillis()
        globalUniform("u_time") { uniform, _ ->
            val time = (System.currentTimeMillis() - startTime) / 1000f
            uniform.set(time)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}