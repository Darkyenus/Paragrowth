package com.darkyen.paragrowth.doodad

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.util.WORLD_BLEND_ATTRIBUTE
import com.darkyen.paragrowth.util.setupBlendWallUniforms

val VA_BLEND_OFFSET = VertexAttribute("a_blend_offset", GL30.GL_FLOAT, 1)

val DOODAD_ATTRIBUTES = VertexAttributes(
        VA_POSITION3,
        VA_COLOR1,
        VA_BLEND_OFFSET
)

val DOODAD_SHADER_BLEND_IN = DoodadShader(true)
val DOODAD_SHADER_BLEND_OUT = DoodadShader(false)

/**
 *
 */
class DoodadShader(blendIn:Boolean) : Shader(DOODADS, "doodad", DOODAD_ATTRIBUTES) {

    init {
        globalUniform("u_projViewTrans") { uniform, camera, _ ->
            uniform.set(camera.combined)
        }

        globalUniform("u_blend") { uniform, _, attributes ->
            uniform.set(attributes[WORLD_BLEND_ATTRIBUTE][0])
        }

        globalUniform("u_blendIn") { uniform, camera, attributes ->
            uniform.set(if(blendIn) 1 else 0)
        }

        setupBlendWallUniforms()
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_NONE)// TODO(jp): Enable, after fixing winding
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}