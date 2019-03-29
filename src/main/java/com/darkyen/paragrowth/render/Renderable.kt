package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.Camera

/** Something with can be drawn on the screen through [RenderBatch]. */
interface Renderable {

    /** Do the frustum culling through [camera] and then [RenderBatch.render] to [batch]. */
    fun render(batch:RenderBatch, camera: Camera)

}