package com.darkyen.paragrowth.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.darkyen.paragrowth.render.Shader.Companion.NULL_SHADER
import com.darkyen.paragrowth.render.Shader.Companion.NULL_VAO
import com.darkyen.paragrowth.util.GdxArray
import com.darkyen.paragrowth.util.stack
import org.lwjgl.opengl.GL32

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
    private val renderables = GdxArray<RenderModel>(RenderModel::class.java)

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

        val camera = camera!!
        val context = renderContext

        var drawCalls = 0

        var currentShader:Shader? = null
        var currentVao:GlVertexArrayObject? = null

        renderables.forSimilarRenderables { items, from, to ->
            val first = items[from]
            // Bind shader and VAO
            val shader = first.shader
            if (shader != currentShader) {
                currentShader?.end()
                shader.begin(camera, context)
                currentShader = shader
            }
            val vao = first.vao
            if (vao != currentVao) {
                Gdx.gl30.glBindVertexArray(vao.handle)
                currentVao = vao
            }

            val primitiveType = first.primitiveType
            // Check if we can do any optimizations
            if (vao.indices == null) {
                // No indices
                // TODO(jp): Optimize when needed
                for (i in from until to) {
                    val rm = items[i]
                    assert(rm.baseVertex == 0) { "Can't use baseVertex without indices" }
                    drawCalls++
                    Gdx.gl30.glDrawArrays(primitiveType, rm.offset, rm.count)
                }
            } else {
                // With indices
                val indicesType = vao.indices.currentType
                val offsetSize = when (indicesType) {
                    GL30.GL_UNSIGNED_BYTE -> 1
                    GL30.GL_UNSIGNED_SHORT -> 2
                    GL30.GL_UNSIGNED_INT -> 4
                    else -> {
                        assert(vao.indices.currentLengthBytes == 0) { "Indices are not empty, but have an unsupported type: $indicesType" }
                        return@forSimilarRenderables
                    }
                }

                val drawCount = to - from

                // Different draw methods when uniforms are set and when not
                if (shader.hasLocalUniforms || drawCount <= 1 /* This is faster when we deal with only one item */) {
                    // Must do the slow path
                    for (i in from until to) {
                        val rm = items[i]

                        val count = rm.count
                        val offsetBytes = rm.offset * offsetSize

                        assert(count * offsetSize + offsetBytes <= vao.indices.currentLengthBytes) {
                            "Mesh attempting to access memory outside of the index buffer (count: $count, offset: ${rm.offset}, max: ${vao.indices.currentLengthBytes / offsetSize})"
                        }

                        shader.updateLocalUniforms(rm)

                        drawCalls++
                        GL32.glDrawElementsBaseVertex(primitiveType, count, indicesType, offsetBytes.toLong(), rm.baseVertex)
                    }
                } else stack {
                    // Can merge everything into common
                    val countArr = IntArray(drawCount)
                    val offsetBuf = mallocPointer(drawCount)
                    val baseVertexArr = IntArray(drawCount)

                    for ((bufI, i) in (from until to).withIndex()) {
                        val rm = items[i]

                        countArr[bufI] = rm.count
                        offsetBuf.put(bufI, (rm.offset * offsetSize).toLong())
                        baseVertexArr[bufI] = rm.baseVertex
                    }

                    drawCalls++
                    GL32.glMultiDrawElementsBaseVertex(primitiveType, countArr, indicesType, offsetBuf, baseVertexArr)

                    // Fun https://www.reddit.com/r/opengl/comments/3m9u36/how_to_render_using_glmultidrawarraysindirect/
                }

                // TODO(jp): Instancing!
            }
        }

        if (maxDrawCalls < drawCalls) {
            maxDrawCalls = drawCalls
            Gdx.app.log("RenderBatch", "Draw call top mark $drawCalls")
        }

        renderablesPool.freeAll(renderables)
        renderables.size = 0
    }

    private inline fun GdxArray<RenderModel>.forSimilarRenderables(action:(items:Array<RenderModel>, from:Int, to:Int) -> Unit) {
        val renderableItems = this.items
        val renderablesSize = renderables.size

        var first:RenderModel? = null
        var firstIndex = -1
        for (i in 0 until renderablesSize) {
            val nextRenderable = renderableItems[i]
            assert(nextRenderable.vao.vertexAttributes == nextRenderable.shader.vertexAttributes) { "VAO and shader vertex attributes don't match (${nextRenderable.shader})" }

            if (first == null) {
                first = nextRenderable
                firstIndex = i
            } else if (first.vao !== nextRenderable.vao
                    || first.shader !== nextRenderable.shader
                    || first.primitiveType != nextRenderable.primitiveType) {
                // Flush!
                action(renderableItems, firstIndex, i)
                first = nextRenderable
                firstIndex = i
            }// else has common vao, keep it for the action
        }

        if (first != null) {
            // Final flush
            action(renderableItems, firstIndex, renderablesSize)
        }
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