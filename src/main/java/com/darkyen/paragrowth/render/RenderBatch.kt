package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.darkyen.paragrowth.render.ParaShader.Companion.NULL_SHADER
import com.darkyen.paragrowth.render.ParaShader.Companion.NULL_VAO

/** Collects [RenderModel]s, sorts them and renders them. */
class RenderBatch(context: RenderContext? = null) {

    /** Backing field for [defaultAttributes] */
    private val attributes = Attributes(null)

    /** Fallback attributes used for all [RenderModel]s drawn by this [RenderBatch].
     * Do not use between [begin] and [end]. */
    val defaultAttributes:Attributes
        get() {
            assert(camera == null) { "Used while batch is running" }
            return attributes
        }

    /** Whether [renderContext] is owned and managed by this [RenderBatch].
     * When it isn't, caller is responsible for calling the [RenderContext.begin] and
     * [RenderContext.end], as well as disposing the RenderContext. */
    val ownContext: Boolean = context == null

    /** Current camera being used in the batch, or null if called outside [begin] and [end].
     * It is not allowed to modify this when not inside a batch. */
    private var camera: Camera? = null

    /** [RenderContext] used by this ModelBatch. */
    private val renderContext: RenderContext = context ?: RenderContext(DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1))

    private val renderablesPool = object : Pool<RenderModel>() {
        override fun newObject(): RenderModel = RenderModel(attributes)

        override fun reset(rm: RenderModel) {
            rm.apply {
                primitiveType = 0
                offset = 0
                baseVertex = 0
                count = 0
                vao = NULL_VAO
                shader = NULL_SHADER
                worldTransform.idt()
                attributes.clear()
                order = 0f
            }
        }
    }

    /** list of Renderables to be rendered in the current batch  */
    private val renderables = Array<RenderModel>(RenderModel::class.java)

    /** Start accepting [RenderModel]s to be rendered, through [render].
     * Must be followed by a call to [end]. The OpenGL context must not be altered until then.
     * @param cam The [Camera] to be used when rendering
     */
    fun begin(cam: Camera) {
        if (camera != null) throw GdxRuntimeException("Call end() first")
        camera = cam
        if (ownContext) renderContext.begin()
    }

    private var maxDrawCalls = 0

    /** Flushes the batch, causing all [Renderable]s in the batch to be rendered.
     * Can only be called after the call to [begin] and before the call to [end]. */
    fun flush() {
        val renderablesSize = renderables.size
        if (renderablesSize == 0)
            return

        renderables.sort()

        val renderableItems = renderables.items
        val camera = camera!!
        val context = renderContext

        val firstItem = renderableItems[0]
        var currentShader: ParaShader = firstItem.shader
        currentShader.begin(camera, context)
        currentShader.render(firstItem)

        for (i in 1 until renderablesSize) {
            val renderable = renderableItems[i]
            val renderableShader = renderable.shader
            if (currentShader !== renderableShader) {
                currentShader.end()
                currentShader = renderableShader
                currentShader.begin(camera, context)
            }
            currentShader.render(renderable)
        }
        currentShader.end()

        val drawCalls = renderables.size
        if (maxDrawCalls < drawCalls) {
            maxDrawCalls = drawCalls
            Gdx.app.log("RenderBatch", "Draw call top mark $drawCalls")
        }

        renderablesPool.freeAll(renderables)
        renderables.size = 0
    }

    /** Close this batch for further [RenderModel]s and [flush] all which were added so far.
     * Must be called after a call to [begin]. After a call to this method the OpenGL context can be altered again. */
    fun end() {
        flush()
        if (ownContext) renderContext.end()
        camera = null
    }

    /** Add a single [RenderModel] to the batch and return it, so that it may be set up.
     * Can only be called after a call to [begin] and before a call to [end]. */
    fun render():RenderModel {
        val model = renderablesPool.obtain()
        renderables.add(model)
        return model
    }

    /** Render given [renderable] to this batch.
     *  Can only be called after a call to [begin] and before a call to [end]. */
    fun render(renderable:Renderable) {
        renderable.render(this, camera!!)
    }
}