package com.darkyen.paragrowth.animal

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.math.collision.BoundingBox
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.render.*
import com.darkyen.paragrowth.terrain.TERRAIN_TIME_ATTRIBUTE
import com.darkyen.paragrowth.terrain.WorldQuery
import com.darkyen.paragrowth.util.*
import kotlin.math.sqrt

private val ANIMAL_TRANSFORM_ATTRIBUTE = attributeKeyMatrix4("animal_transform", ModelAttributeLayer)
private val ANIMAL_CENTER_ATTRIBUTE = attributeKeyVector3("animal_center", ModelAttributeLayer)
private val ANIMAL_SUBMERGE_ATTRIBUTE = attributeKeyFloat("animal_submerge", ModelAttributeLayer)

/**
 *
 */
class AnimalWorld(private val world:WorldQuery) : Renderable {

    private val animals = GdxArray<Animal>()

    fun update(delta:Float, playerPosition:Vector2) {
        val worldDimensions = world.getDimensions()
        for (animal in animals) {
            animal.update(worldDimensions, world, playerPosition, delta)
        }
    }

    private val render_quaternion = Quaternion()
    override fun render(batch: RenderBatch, camera: Camera) {
        val quaternion = render_quaternion

        for (animal in animals) {
            batch.render().apply {
                set(animal.model)
                shader = AnimalShader
                val animalCenter = attributes[ANIMAL_CENTER_ATTRIBUTE]
                animalCenter.set(animal.movement.x, animal.movement.y, animal.positionZ)
                attributes[ANIMAL_TRANSFORM_ATTRIBUTE].apply {
                    translate(animalCenter)
                    rotate(quaternion.setFromAxisRad(Vector3.Z, animal.movement.heading))
                    rotate(quaternion.setFromAxisRad(Vector3.Y, animal.pitch))
                    rotate(quaternion.setFromAxisRad(Vector3.X, animal.roll))
                }
                attributes[ANIMAL_SUBMERGE_ATTRIBUTE][0] = animal.waterSubmerge

                // TODO(jp): Order
            }
        }
    }

    private val models = GdxArray<Model>()

    fun populateWithDucks(worldDimensions:Rectangle) {
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

        for (i in 0 until 20) {
            val female = MathUtils.randomBoolean()

            val animal = Animal(if (female) duckFemale else duckMale, adultSubmerge, DUCK_WATER_MOVEMENT, DUCK_LAND_MOVEMENT, duckBehavior)
            animal.movement.setPosition(worldDimensions.x + worldDimensions.width * MathUtils.random(), worldDimensions.y + worldDimensions.height * MathUtils.random())
            animal.positionZ = world.getHeightAt(animal.movement.x, animal.movement.y)
            animals.add(animal)

            var childNumber = 0
            while ((female && MathUtils.randomBoolean(0.5f)) || MathUtils.randomBoolean(0.1f)) {
                val baby = Animal(duckBaby, babySubmerge, DUCK_WATER_MOVEMENT, DUCK_LAND_MOVEMENT, duckBehavior)
                val position = Vector2(animal.movement.x, animal.movement.y).add(MathUtils.random(-10f, 10f), MathUtils.random(-10f, 10f))
                baby.movement.setPosition(position.x, position.y)
                baby.parent = animal
                baby.childNumber = childNumber++
                animals.add(baby)
            }
        }

        for (i in 0 until 5) {
            val deerAnimal = Animal(deer, 2.4f, DEER_WATER_MOVEMENT, DEER_LAND_MOVEMENT, deerBehavior)
            deerAnimal.movement.setPosition(worldDimensions.x + worldDimensions.width * MathUtils.random(), worldDimensions.y + worldDimensions.height * MathUtils.random())
            deerAnimal.positionZ = world.getHeightAt(deerAnimal.movement.x, deerAnimal.movement.y)
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
        val box = BoundingBox()
        val pos = Vector3()

        for (animal in animals) {
            val color = rgb(0f, 0f, 1f)

            pos.set(animal.movement.x, animal.movement.y, animal.positionZ)
            box.inf().ext(pos, 2f)
            box.forEdges { x1, y1, z1, x2, y2, z2 ->
                renderer.color(color)
                renderer.vertex(x1, y1, z1)
                renderer.color(color)
                renderer.vertex(x2, y2, z2)
            }
        }
    }

    private fun BehaviorBuilder.moveIntoArea(haste:Float, animalKey:Key<Animal>, areaKey:Key<Rectangle>, delta:FloatKey) = none {
        // Run towards world
        val animal = animalKey()
        var targetX = animal.movement.x
        var targetY = animal.movement.x
        val area = areaKey()

        if (area.contains(targetX, targetY)) {
            return@none true
        }

        targetX = MathUtils.clamp(targetX, area.x, area.x + area.width)
        targetY = MathUtils.clamp(targetY, area.y, area.y + area.height)

        animal.movement.moveTo(Vector2(targetX, targetY), haste, delta(), animal.movementAttributes, 1f)
        return@none null
    }

    private fun BehaviorBuilder.avoidPoint(distance:Float, haste:Float, animalKey:Key<Animal>, pointKey:Key<Vector2>, delta:FloatKey) = none {
        val animal = animalKey()
        val point = pointKey()

        val animalPosX = animal.movement.x
        val animalPosY = animal.movement.y

        if (point.dst2(animalPosX, animalPosY) >= distance * distance) {
            return@none true
        }

        animal.movement.move(angleRad(animalPosX - point.x, animalPosY - point.y), haste, delta(), animal.movementAttributes)
        return@none null
    }

    private fun BehaviorBuilder.createParentFollowFormationPoint(formationWidth:Int, rankOffset:Float, columnOffset:Float, animalKey:Key<Animal>, outPointKey:Key<Vector2>) = none {
        val animal = animalKey()
        val parent = animal.parent ?: return@none false

        val rank = animal.childNumber / maxOf(formationWidth, 1)
        val column = animal.childNumber % maxOf(formationWidth, 1)
        val offsetX = -(rank + 1f) * rankOffset
        val rankWidth = maxOf(formationWidth - 1, 0) * columnOffset
        val offsetY = column * columnOffset - rankWidth * 0.5f

        outPointKey().set(parent.movement.x, parent.movement.y).addRotated(offsetX, offsetY, parent.movement.heading)
        true
    }

    private fun BehaviorBuilder.pickRandomPointInArea(areaKey:Key<Rectangle>, outPointKey:Key<Vector2>) = none {
        val area = areaKey()
        outPointKey().set(area.width, area.height).scl(MathUtils.random(), MathUtils.random()).add(area.x, area.y)
        true
    }

    private fun BehaviorBuilder.pickRandomPointInAreaNearAnimal(areaKey:Key<Rectangle>, outPointKey:Key<Vector2>, near:Key<Animal>, minDistance:Float, maxDistance:Float) = none {
        val distance = lerp(minDistance, maxDistance, sqrt(MathUtils.random()))
        val angle = MathUtils.random(MathUtils.PI2)

        val nearAnimal = near()
        outPointKey()
                .set(nearAnimal.movement.x, nearAnimal.movement.y)
                .addRotated(distance, 0f, angle)
                .clampTo(areaKey())

        true
    }

    private fun BehaviorBuilder.adjustPointToHeightRange(adjustedPointKey:Key<Vector2>, minHeight:Float, maxHeight:Float) = none {
        val point = adjustedPointKey()
        world.adjustPointToHeightRange(point, minHeight, maxHeight)
        true
    }

    private fun BehaviorBuilder.moveToPoint(distance:Float, haste:Float, animalKey:Key<Animal>, pointKey:Key<Vector2>, delta:FloatKey) = none {
        val animal = animalKey()
        val point = pointKey()

        val animalPosX = animal.movement.x
        val animalPosY = animal.movement.y

        if (point.dst2(animalPosX, animalPosY) <= distance * distance) {
            return@none true
        }

        animal.movement.moveTo(point, haste, delta(), animal.movementAttributes)
        return@none null
    }

    private fun BehaviorBuilder.waitForAWhile(center:Float, spread:Float, delta:FloatKey) {

        val countdown = register(-1f)

        return none {
            var cd = countdown()

            if (cd < 0f) {
                // Initial fill
                cd = center + MathUtils.random(-spread, spread)
            }

            cd -= delta()
            if (cd > 0f) {
                // Ticking down...
                countdown(cd)
                return@none null
            }

            cd = center + MathUtils.random(-spread, spread)
            countdown(cd)
            return@none true
        }
    }

    private val duckBehavior = behaviorTree {
        val animal = register<Animal>()
        val worldDimensions = register<Rectangle>()
        val playerPosition = register<Vector2>()
        val delta = register(0f)

        repeatUntil(null)() {
            hotSequence(Sequence.AND)() {
                moveIntoArea(1f, animal, worldDimensions, delta)

                avoidPoint(15f, 1f, animal, playerPosition, delta)

                enterIf(true) { animal().parent != null }() {
                    hotSequence(Sequence.AND)() {
                        val childFollowPoint = register { Vector2() }
                        createParentFollowFormationPoint(2, 3f, 3f, animal, childFollowPoint)
                        moveToPoint(0.1f, 0.7f, animal, childFollowPoint, delta)
                    }
                }

                enterIf(true) { animal().parent == null }() {
                    sequence(Sequence.AND)() {
                        val targetPoint = register { Vector2() }
                        pickRandomPointInAreaNearAnimal(worldDimensions, targetPoint, animal, 5f, 40f)
                        enterIf(true){ MathUtils.randomBoolean(0.3f) }() {
                            adjustPointToHeightRange(targetPoint, Float.NEGATIVE_INFINITY, 0.5f)
                        }
                        moveToPoint(0.1f, 0.5f, animal, targetPoint, delta)
                        waitForAWhile(7f, 5f, delta)
                    }
                }
            }
        }
    }

    private val deerBehavior = behaviorTree {
        val animal = register<Animal>()
        val worldDimensions = register<Rectangle>()
        val playerPosition = register<Vector2>()
        val delta = register(0f)

        repeatUntil(null)() {
            hotSequence(Sequence.AND)() {
                moveIntoArea(1f, animal, worldDimensions, delta)

                avoidPoint(20f, 0.9f, animal, playerPosition, delta)

                sequence(Sequence.AND)() {
                    val targetPoint = register { Vector2() }
                    pickRandomPointInAreaNearAnimal(worldDimensions, targetPoint, animal, 20f, 80f)
                    adjustPointToHeightRange(targetPoint, -0.5f, Float.POSITIVE_INFINITY)
                    moveToPoint(0.1f, 0.5f, animal, targetPoint, delta)
                    waitForAWhile(7f, 5f, delta)
                }
            }
        }
    }
}

class AnimalAttributes : AgentAttributes() {
    /** How much should the animal sway from right to left while walking */
    var waddle:Float = 0.1f
    /** How much should the animal sway from front to back while walking */
    var steps:Float = 0f
}
private val ANIMAL_ATTRIBUTES_METHODS = createAgentAttributesMethods<AnimalAttributes>()

val DUCK_WATER_MOVEMENT = AnimalAttributes().apply {
    maxAcceleration = 1f
    maxDeceleration = 1f
    agility = 0.5f
    maxSpeed = 4f
    speedHeft = 0.7f
    maxTurnSpeed = 2f
    turnHeft = 0.8f
    waddle = 0.05f
    steps = 0f
}

val DUCK_LAND_MOVEMENT = AnimalAttributes().apply {
    maxAcceleration = 1f
    maxDeceleration = 1f
    agility = 0.5f
    maxSpeed = 2f
    speedHeft = 0.7f
    maxTurnSpeed = 2f
    turnHeft = 0.8f
    waddle = 0.2f
    steps = 0f
}

val DEER_WATER_MOVEMENT = AnimalAttributes().apply {
    maxAcceleration = 1f
    maxDeceleration = 1f
    agility = 0.5f
    maxSpeed = 4f
    speedHeft = 0.7f
    maxTurnSpeed = 2f
    turnHeft = 0.8f
    waddle = 0.01f
    steps = 0.01f
}

val DEER_LAND_MOVEMENT = AnimalAttributes().apply {
    maxAcceleration = 1f
    maxDeceleration = 1f
    agility = 0.5f
    maxSpeed = 8f
    speedHeft = 0.7f
    maxTurnSpeed = 2f
    turnHeft = 0.8f
    waddle = 0.05f
    steps = 0.1f
}

class Animal(val model: Model, val waterSubmerge:Float,
             private val waterMovement:AnimalAttributes,
             private val landMovement:AnimalAttributes,
             behaviorTemplate:BehaviorTreeTemplate) {

    private val behavior = BehaviorTree(behaviorTemplate, this, Rectangle(), Vector2())

    val movementAttributes = AnimalAttributes().apply {
        ANIMAL_ATTRIBUTES_METHODS.apply { set(waterMovement) }
    }

    val movement = MovementAgent()
    var positionZ = 0f

    /** Banking, waddling, etc. */
    var roll = 0f
    /** Looking up/down */
    var pitch = 0f

    private var animationTime = 0f

    var parent:Animal? = null
    var childNumber = -1

    fun update(worldDimensions:Rectangle, world: WorldQuery, playerPosition:Vector2, delta:Float) {
        animationTime += delta
        if (animationTime > MathUtils.PI*1000f) {
            animationTime -= MathUtils.PI*1000f
        }

        behavior.floatStorage[0] = delta
        (behavior.storage[1] as Rectangle).set(worldDimensions)
        (behavior.storage[2] as Vector2).set(playerPosition)

        behavior.act()

        val howMuchInWater = MathUtils.clamp(map(world.getHeightAt(movement.x, movement.y), -1f, 0f, 1f, 0f), 0f, 1f)
        ANIMAL_ATTRIBUTES_METHODS.apply { movementAttributes.setToLerp(landMovement, waterMovement, howMuchInWater) }

        positionZ = world.getHeightAt(movement.x, movement.y)

        // Waddle
        roll = Math.sin(animationTime.toDouble() * 8f).toFloat() * movementAttributes.waddle

        // Steps
        pitch = Math.sin(animationTime.toDouble() * 4f).toFloat() * movementAttributes.steps
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