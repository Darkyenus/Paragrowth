package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.math.Matrix4
import com.darkyen.paragrowth.util.PrioritizedShader.SKYBOX

/**
 *
 */
class SkyboxShader : ParaShader(SKYBOX, "sky", POSITION3_ATTRIBUTES) {

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