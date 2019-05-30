package com.darkyen.paragrowth.animal

import com.badlogic.gdx.Gdx
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
private val ANIMAL_SUBMERGE_ATTRIBUTE = attributeKeyFloat("animal_submerge")

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
                attributes[ANIMAL_CENTER_ATTRIBUTE].set(animal.position)
                attributes[ANIMAL_TRANSFORM_ATTRIBUTE].apply {
                    translate(animal.position)
                    rotate(quaternion.setFromAxisRad(Vector3.Z, animal.yaw))
                    rotate(quaternion.setFromAxisRad(Vector3.Y, animal.pitch))
                    rotate(quaternion.setFromAxisRad(Vector3.X, animal.roll))
                }
                attributes[ANIMAL_SUBMERGE_ATTRIBUTE][0] = animal.waterSubmerge

                // TODO(jp): Order
            }
        }
    }

    private val models = GdxArray<Model>()

    fun populateWithDucks() {
        // No ducks given, all custom.

        val models = run {
            // has to face towards positive X
            val builder = ModelBuilder(ANIMAL_ATTRIBUTES)

            for (model in arrayOf(
                    "duck.obj",
                    "duck_female.obj",
                    "duck_baby.obj",
                    "deer.obj"
            )) {
                builder.loadObjModel(Gdx.files.local(model)) { x, y, z, material ->
                    vertex(x, y, z, material.diffuse.toFloatBits())
                }
                builder.modelEnd()
            }

            builder.generateModels(ANIMAL_ATTRIBUTES)
        }

        this.models.addAll(*models)

        val (duckMale, duckFemale, duckBaby, deer) = models
        val adultSubmerge = 0.4f
        val babySubmerge = 0.2f

        val world = getWorldDimensions()
        for (i in 0 until 20) {
            val female = MathUtils.randomBoolean()

            val animal = Animal(if (female) duckFemale else duckMale, adultSubmerge)
            animal.position.set(world.x + world.width * MathUtils.random(), world.y + world.height * MathUtils.random(), 0f)
            animal.position.z = getWorldHeight(animal.position.x, animal.position.y)
            animals.add(animal)

            while ((female && MathUtils.randomBoolean(0.5f)) || MathUtils.randomBoolean(0.1f)) {
                val baby = Animal(duckBaby, babySubmerge)
                baby.position.set(animal.position).add(MathUtils.random(-10f, 10f), MathUtils.random(-10f, 10f), 0f)
                baby.parent = animal
                animals.add(baby)
            }
        }

        for (i in 0 until 5) {
            val deerAnimal = Animal(deer, 2.4f, 2f, 4f, 0.05f, 0.05f, 0.01f, 0.1f)
            deerAnimal.position.set(world.x + world.width * MathUtils.random(), world.y + world.height * MathUtils.random(), 0f)
            deerAnimal.position.z = getWorldHeight(deerAnimal.position.x, deerAnimal.position.y)
            animals.add(deerAnimal)
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

            val box = BoundingBox().inf().ext(animal.position, 2f)
            box.forEdges { x1, y1, z1, x2, y2, z2 ->
                renderer.color(color)
                renderer.vertex(x1, y1, z1)
                renderer.color(color)
                renderer.vertex(x2, y2, z2)
            }
        }
    }
}

class Animal(val model: Model, val waterSubmerge:Float,
             val waterSpeed:Float = 2f, val landSpeed:Float = 1f,
             val waterWaddle:Float = 0.05f, val landWaddle:Float = 0.2f,
             val waterSteps:Float = 0f, val landSteps:Float = 0f) {

    val position = Vector3()
    /** World heading (north, south, etc.) */
    var yaw = 0f
    /** Banking, waddling, etc. */
    var roll = 0f
    /** Looking up/down */
    var pitch = 0f

    private val target = Vector2()
    private var animationTime = 0f

    var parent:Animal? = null

    fun update(worldDimensions:Rectangle, getWorldHeight:(x:Float, y:Float) -> Float, delta:Float) {
        animationTime += delta
        if (animationTime > MathUtils.PI*1000f) {
            animationTime -= MathUtils.PI*1000f
        }

        val parent = parent
        if (parent == null) {
            // Check if target is correct
            target.x = MathUtils.clamp(target.x, worldDimensions.x, worldDimensions.x + worldDimensions.width)
            target.y = MathUtils.clamp(target.y, worldDimensions.y, worldDimensions.y + worldDimensions.height)

            // Find new target if this one has been reached
            if (target.epsilonEquals(position.x, position.y, 1f) || MathUtils.randomBoolean(0.0001f)) {
                // Reached the target, find new one
                target.set(worldDimensions.width, worldDimensions.height).scl(MathUtils.random.nextFloat(), MathUtils.random.nextFloat()).add(worldDimensions.x, worldDimensions.y)
            }
        } else {
            target.set(parent.position.x, parent.position.y)
        }

        // Move towards target
        val direction = Vector2(target).sub(position.x, position.y)
        yaw = direction.angleRad()

        val howMuchInWater = MathUtils.clamp(VectorUtils.map(getWorldHeight(position.x, position.y), -1f, 0f, 1f, 0f), 0f, 1f)
        val speed = MathUtils.lerp(landSpeed, waterSpeed, howMuchInWater)

        var distanceToTravel = speed * delta
        val pathLength = direction.len()

        if (parent == null) {
            if (distanceToTravel < pathLength) {
                direction.scl(distanceToTravel / pathLength)
            }
        } else {
            // Baby behavior
            if (pathLength < 5f) {
                // Too close to mama, wait
                direction.setZero()
            } else {
                if (pathLength > 10f) {
                    // Too far, speed up!
                    distanceToTravel *= 2f
                }

                if (distanceToTravel < pathLength) {
                    direction.scl(distanceToTravel / pathLength)
                }
            }
        }

        position.x += direction.x
        position.y += direction.y
        position.z = getWorldHeight(position.x, position.y)

        // Waddle
        roll = Math.sin(animationTime.toDouble() * 8f).toFloat() * MathUtils.lerp(landWaddle, waterWaddle, howMuchInWater)

        // Steps
        pitch = Math.sin(animationTime.toDouble() * 4f).toFloat() * MathUtils.lerp(landSteps, waterSteps, howMuchInWater)
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

        localUniform("u_animalSubmerge") { uniform, _, renderable ->
            uniform.set(renderable.attributes[ANIMAL_SUBMERGE_ATTRIBUTE][0])
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