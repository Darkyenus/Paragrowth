package com.darkyen.paragrowth.animal

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.terrain.TERRAIN_TIME_ATTRIBUTE
import com.darkyen.paragrowth.util.*

private val ANIMAL_TRANSFORM_ATTRIBUTE = attributeKeyMatrix4("animal_transform")
private val ANIMAL_CENTER_ATTRIBUTE = attributeKeyVector3("animal_center")

/**
 *
 */
class AnimalWorld(private val getWorldHeight:(x:Float, y:Float) -> Float, private val getWorldDimensions: () -> Rectangle) : Renderable {

    val animals = GdxArray<Animal>()

    fun update(delta:Float) {
        val worldDimensions = getWorldDimensions()
        for (animal in animals) {
            animal.update(worldDimensions, getWorldHeight, delta)
        }
    }

    override fun render(batch: RenderBatch, camera: Camera) {
        val quaternion = Quaternion()

        for (animal in animals) {
            batch.render().apply {
                set(animal.model)
                shader = AnimalShader
                quaternion.setEulerAnglesRad(animal.rotation.x, animal.rotation.y,  animal.rotation.z)
                attributes[ANIMAL_CENTER_ATTRIBUTE].set(animal.position)
                attributes[ANIMAL_TRANSFORM_ATTRIBUTE].translate(animal.position).rotate(quaternion)

                // TODO(jp): Order
            }
        }
    }

    private val models = GdxArray<Model>()

    fun populateWithDucks() {
        // No ducks given, all generated.

        val model = run {
            // has to face towards positive X
            val builder = ModelBuilder(4)

            builder.box { x, y, z ->
                vertex(x, y * 0.5f, z, White)
            }

            val vertices = builder.createVertexBuffer()
            val indices = builder.createIndexBuffer()
            val vao = GlVertexArrayObject(indices, ANIMAL_ATTRIBUTES,
                    GlVertexArrayObject.Binding(vertices, 4, 0),
                    GlVertexArrayObject.Binding(vertices, 4, 3)
            )
            Model(vao, builder.indices.size)
        }

        models.add(model)

        val world = getWorldDimensions()
        for (i in 0 until 20) {
            val animal = Animal(model)
            animal.position.set(world.x + world.width * MathUtils.random(), world.y + world.height * MathUtils.random(), 0f)
            animal.position.z = getWorldHeight(animal.position.x, animal.position.y)
            animals.add(animal)
        }
    }

    fun dispose() {
        for (model in models) {
            model.vao.dispose()
            model.vao.indices?.dispose()
            model.vao.bindings.forEach { it.buffer.dispose() }
        }
    }

    fun renderDebug(renderer: ImmediateModeRenderer) {
        for (animal in animals) {
            val color = rgb(0f, 0f, 1f)

            val box = BoundingBox().inf().ext(animal.position, 5f)
            box.forEdges { x1, y1, z1, x2, y2, z2 ->
                renderer.color(color)
                renderer.vertex(x1, y1, z1)
                renderer.color(color)
                renderer.vertex(x2, y2, z2)
            }
        }
    }
}

class Animal(val model: Model, val waterSpeed:Float = 2f, val landSpeed:Float = 1f) {

    val position = Vector3()
    val rotation = Vector3()

    private val target = Vector2()
    private var animationTime = 0f

    fun update(worldDimensions:Rectangle, getWorldHeight:(x:Float, y:Float) -> Float, delta:Float) {
        animationTime += delta
        if (animationTime > MathUtils.PI*1000f) {
            animationTime -= MathUtils.PI*1000f
        }

        // Check if target is correct
        target.x = MathUtils.clamp(target.x, worldDimensions.x, worldDimensions.x + worldDimensions.width)
        target.y = MathUtils.clamp(target.y, worldDimensions.y, worldDimensions.y + worldDimensions.height)

        // Find new target if this one has been reached
        if (target.epsilonEquals(position.x, position.y, 1f)) {
            // Reached the target, find new one
            target.set(worldDimensions.width, worldDimensions.height).scl(MathUtils.random.nextFloat(), MathUtils.random.nextFloat()).add(worldDimensions.x, worldDimensions.y)
        }

        // Move towards target
        val direction = Vector2(target).sub(position.x, position.y)
        rotation.z = direction.angleRad()

        val howMuchInWater = MathUtils.clamp(VectorUtils.map(getWorldHeight(position.x, position.y), -1f, 0f, 1f, 0f), 0f, 1f)
        val speed = MathUtils.lerp(landSpeed, waterSpeed, howMuchInWater)

        val distanceToTravel = speed * delta
        val pathLength = direction.len()
        if (distanceToTravel < pathLength) {
            direction.scl(distanceToTravel / pathLength)
        }

        position.x += direction.x
        position.y += direction.y
        position.z = getWorldHeight(position.x, position.y)

        // Waddle
        //rotation.x = Math.sin(animationTime.toDouble()).toFloat() * MathUtils.lerp(0.6f, 0.1f, howMuchInWater)
    }
}

val ANIMAL_ATTRIBUTES = VertexAttributes(
        VA_POSITION3,
        VA_COLOR1
)


object AnimalShader: Shader(ANIMALS, "animal", ANIMAL_ATTRIBUTES) {

    init {
        globalUniform("u_projViewTrans") { uniform, camera, _ ->
            uniform.set(camera.combined)
        }

        localUniform("u_modelTrans") { uniform, _, renderable ->
            uniform.set(renderable.attributes[ANIMAL_TRANSFORM_ATTRIBUTE])
        }

        localUniform("u_animalCenter") { uniform, _, renderable ->
            uniform.set(renderable.attributes[ANIMAL_CENTER_ATTRIBUTE])
        }

        globalUniform("u_time") { uniform, _, attributes ->
            uniform.set(attributes[TERRAIN_TIME_ATTRIBUTE][0])
        }

        ParagrowthMain.assetManager.load("Water_001_DISP.png", Texture::class.java)
        ParagrowthMain.assetManager.finishLoading()
        val displacement = TextureDescriptor(ParagrowthMain.assetManager.get("Water_001_DISP.png", Texture::class.java), Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)

        globalUniform("u_displacement_texture") { uniform, _, _ ->
            uniform.set(displacement)
        }
    }

    override fun adjustContext(context: RenderContext) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        context.setCullFace(GL20.GL_BACK)
        context.setDepthTest(GL20.GL_LESS)
        context.setDepthMask(true)
    }
}