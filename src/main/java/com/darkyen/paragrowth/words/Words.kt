package com.darkyen.paragrowth.words

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20.*
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.math.*
import com.badlogic.gdx.utils.Align
import com.darkyen.paragrowth.TextAnalyzer
import com.darkyen.paragrowth.font.FontLoader
import com.darkyen.paragrowth.font.GlyphLayout
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.util.*
import org.lwjgl.opengl.GL15.GL_WRITE_ONLY
import kotlin.math.roundToInt

private val WORD_ATTRIBUTES = VertexAttributes(
        VertexAttribute("a_letter_xywh", GL30.GL_FLOAT, 4, instancingDivisor = 1),
        VertexAttribute("a_letter_uvu2v2", GL30.GL_FLOAT, 4, instancingDivisor = 1),
        VertexAttribute("a_position_xyz", GL30.GL_FLOAT, 3, instancingDivisor = 1),
        VertexAttribute("a_color", GL30.GL_UNSIGNED_BYTE, 4, normalized = true, instancingDivisor = 1)
)

private object AvailableWords {
    val positiveWords = GdxArray<String>()
    val negativeWords = GdxArray<String>()
    val colorWords = GdxArray<String>()
    val colors = GdxFloatArray()
    val maxWordLength:Int

    init {
        TextAnalyzer.get().apply {
            exportPositive(positiveWords)
            exportNegative(negativeWords)
            export(colorWords, colors)
        }
        var maxLength = 0
        for (word in positiveWords) {
            maxLength = maxOf(maxLength, word.length)
        }
        for (word in negativeWords) {
            maxLength = maxOf(maxLength, word.length)
        }
        for (word in colorWords) {
            maxLength = maxOf(maxLength, word.length)
        }
        maxWordLength = maxLength
    }
}

/**
 *
 */
class Words(private val onCollectedTextChange:(Words, String) -> Unit) {

    private val worldText = StringBuilder()

    private val maxPlacedWords = 128
    val placedWords = GdxArray<WorldWord>(maxPlacedWords)
    var enabled = true

    private val indices = GlBuffer(GL_STATIC_DRAW).apply {
        setData(shortArrayOf(
                0, 1, 2,
                2, 3, 0))
    }
    private val vertices = GlBuffer(GL_STREAM_DRAW).apply {
        val bytes = WORD_ATTRIBUTES.getByteSize(4) * AvailableWords.maxWordLength * maxPlacedWords
        reserve(bytes / 4, GL_FLOAT)
    }

    private val vao = run {
        val stride = WORD_ATTRIBUTES.getByteSize(4)/4

        GlVertexArrayObject(indices, WORD_ATTRIBUTES,
            GlVertexArrayObject.Binding(vertices, stride, 0),
            GlVertexArrayObject.Binding(vertices, stride, 4),
            GlVertexArrayObject.Binding(vertices, stride, 8),
            GlVertexArrayObject.Binding(vertices, stride, 11)
        )
    }

    private fun generateWords(amount:Int, area: Rectangle, avoid: Vector2, height:(x:Float, y:Float) -> Float) {
        placedWords.ensureCapacity(amount)
        val avoidDistance = 50f
        val avoidDistance2 = avoidDistance * avoidDistance

        // May generate less words if there is no suitable place for them
        var remaining = amount
        for (j in 0 until 10 * amount) {
            val x = area.x + area.width * MathUtils.random()
            val y = area.y + area.height * MathUtils.random()
            val z = height(x, y)

            if (z < 0f || avoid.dst2(x, y) < avoidDistance2) {
                continue
            }


            val word:String
            val color:Color
            when (MathUtils.random.nextInt(3)) {
                0 -> {
                    val wordIndex = MathUtils.random.nextInt(AvailableWords.positiveWords.size)
                    word = AvailableWords.positiveWords[wordIndex]
                    color = rgb(1f, 1f, 1f, 1f)
                }
                1 -> {
                    val wordIndex = MathUtils.random.nextInt(AvailableWords.negativeWords.size)
                    word = AvailableWords.negativeWords[wordIndex]
                    color = rgb(0f, 0f, 0f, 1f)
                }
                else -> {
                    val wordIndex = MathUtils.random.nextInt(AvailableWords.colorWords.size)
                    word = AvailableWords.colorWords[wordIndex]
                    color = AvailableWords.colors[wordIndex]
                }
            }

            val ww = WorldWord(word, color)
            ww.position.set(x, y, z)
            placedWords.add(ww)

            if (--remaining <= 0) {
                break
            }
        }

        //if (remaining > 0) println("Remaining to make: $remaining")
    }

    fun render(batch:RenderBatch) {
        var letters = 0

        vertices.accessMapped(GL_WRITE_ONLY) { byteVerts ->

            val verts = byteVerts.asFloatBuffer()
            val vertDrawDelegate = object : GlyphLayout.DrawDelegate {

                var alpha = 1f

                var selectedColor = 0f
                override fun setColor(color: Float) {
                    this.selectedColor = color
                }

                var positionX:Float = 0f
                var positionY:Float = 0f
                var positionZ:Float = 0f

                var i = 0
                override fun draw(page: Int, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float) {
                    // TODO(jp): Optimize
                    var i = this.i
                    verts.put(i++, x)
                    verts.put(i++, y)
                    verts.put(i++, width)
                    verts.put(i++, height)

                    verts.put(i++, u)
                    verts.put(i++, v)
                    verts.put(i++, u2)
                    verts.put(i++, v2)

                    verts.put(i++, positionX)
                    verts.put(i++, positionY)
                    verts.put(i++, positionZ)

                    verts.put(i++, selectedColor.withAlpha(alpha))
                    this.i = i
                    letters++
                }
            }


            for (word in placedWords) {
                if (word.fade < 0f) {
                    continue
                }

                vertDrawDelegate.alpha = if (word.collected || !enabled) Interpolation.circleIn.apply(word.fade) else Interpolation.swingOut.apply(word.fade)
                vertDrawDelegate.positionX = word.position.x
                vertDrawDelegate.positionY = word.position.y
                vertDrawDelegate.positionZ = if (word.collected || !enabled) word.position.z else word.position.z + Interpolation.circleIn.apply(word.fade) * 2f

                word.glyphLayout.draw(vertDrawDelegate, -word.glyphLayout.width * 0.5f, word.glyphLayout.height)
            }
        }

        if (letters > 0) {
            batch.render().apply {
                primitiveType = GL_TRIANGLES
                count = 6
                vao = this@Words.vao
                instances = letters
                shader = WordShader
            }
        }
    }

    fun update(delta:Float, worldArea:Rectangle, playerPosition:Vector2, getHeight:(x:Float, y:Float) -> Float) {
        val fadeChange = if (enabled) delta / 25f else delta
        val collectedFadeChange = delta * 2f

        placedWords.removeAll { ww ->
            if (ww.fadeIn && ww.fade < 1f) {
                ww.fade += fadeChange
                if (ww.fade >= 1f) {
                    ww.fadeIn = false
                    ww.fade = 2f - ww.fade
                }
            } else if (!ww.fadeIn) {
                ww.fade -= if (ww.collected) collectedFadeChange else fadeChange
                if (ww.fade < 0f) {
                    return@removeAll true // -> remove
                }
            }

            if (enabled && ww.fade > 0.5f && !ww.collected && playerPosition.dst2(ww.position.x, ww.position.y) < 9f) {
                ww.collected = true
                ww.fade = 1f
                ww.fadeIn = false
                worldText.append(' ').append(ww.word)
                onCollectedTextChange(this, worldText.toString())
            }

            false // -> keep
        }

        if (enabled) {
            val targetWordAmount = MathUtils.clamp((worldArea.area() / (100f * 100f)).roundToInt(), 3, maxPlacedWords)
            if (placedWords.size < targetWordAmount) {
                generateWords(minOf(targetWordAmount - placedWords.size, 5), worldArea, playerPosition, getHeight)
            }
        }
    }

    class WorldWord(val word:String, color:Color) {
        val position = Vector3()

        var fade = MathUtils.random(-10f, 0f)
        var fadeIn = true
        var collected = false

        val glyphLayout = GlyphLayout(WORD_FONT, false).apply {
            setText(word, color, 0f, Align.left)
        }
    }

}

private val WORD_FONT = FontLoader.loadFont(Gdx.files.local("Avara.ttf"), 50, 4f)

object WordShader : Shader(DOODADS, "word", WORD_ATTRIBUTES) {

    init {
        globalUniform("u_projViewTrans") { uniform, camera, _ ->
            uniform.set(camera.combined)
        }

        val fontTextureDescriptor = TextureDescriptor(WORD_FONT.pages[0], Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)

        globalUniform("u_font_texture") { uniform, _, _ ->
            uniform.set(fontTextureDescriptor)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL_NONE)
        context.setDepthTest(GL_LESS)
        context.setDepthMask(true)
    }
}