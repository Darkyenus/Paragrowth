package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.GL20

/**
 *
 */
class Model(val vao:GlVertexArrayObject,
            val indexCount:Int,
            val indexOffset:Int = 0,
            val baseVertex:Int = 0,
            val primitive:Int = GL20.GL_TRIANGLES)