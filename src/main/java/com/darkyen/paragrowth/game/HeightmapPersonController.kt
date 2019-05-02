package com.darkyen.paragrowth.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.darkyen.paragrowth.ParagrowthMain
import com.darkyen.paragrowth.input.GameInput
import com.darkyen.paragrowth.input.GameInput.Binding.bindKeyboard
import com.darkyen.paragrowth.input.GameInput.Binding.bindMouseButton

/**
 */
@Suppress("PrivatePropertyName", "PropertyName")
class HeightmapPersonController(private val camera: Camera, private val heightmap: (x:Float, y:Float) -> Float) {

    private val FORWARD = GameInput.function("Forward", bindKeyboard(Keys.W), bindKeyboard(Keys.UP))
    private val BACKWARD = GameInput.function("Backward", bindKeyboard(Keys.S), bindKeyboard(Keys.DOWN))
    private val STRAFE_LEFT = GameInput.function("Left", bindKeyboard(Keys.A), bindKeyboard(Keys.LEFT))
    private val STRAFE_RIGHT = GameInput.function("Right", bindKeyboard(Keys.D), bindKeyboard(Keys.RIGHT))
    private val SPRINT = GameInput.function("Sprint", bindKeyboard(Keys.SHIFT_LEFT))

    private val FREE_MOUSE = GameInput.toggleFunction("Free Mouse", bindMouseButton(Input.Buttons.RIGHT))
            .listen { times, pressed ->
                Gdx.input.isCursorCatched = !pressed
                true
            }
    val GENERAL_DEBUG = GameInput.toggleFunction("General Debug", bindKeyboard(Keys.F3))!!
    private val MOVEMENT_DEBUG = GameInput.toggleFunction("Movement Debug", bindKeyboard(Keys.F4))
    val PATCHWORK_DEBUG = GameInput.toggleFunction("Patchwork Debug", bindKeyboard(Keys.F5))!!
    val CYCLE_TERRAIN_DEBUG = GameInput.toggleFunction("Cycle Terrains", bindKeyboard(Keys.F6))!!

    private val TO_WRITE_MODE = GameInput.function("To Write Mode", bindKeyboard(Keys.ESCAPE))
            .listen { _, pressed ->
                if (pressed) {
                    val oldScreen = ParagrowthMain.INSTANCE.screen
                    ParagrowthMain.INSTANCE.screen = WriteState()
                    if (oldScreen is WanderState) {
                        oldScreen.dispose()
                    }
                }
                false
            }

    internal val INPUT = arrayOf(
            FORWARD, BACKWARD, STRAFE_LEFT, STRAFE_RIGHT,
            SPRINT, FREE_MOUSE,
            GENERAL_DEBUG, MOVEMENT_DEBUG, PATCHWORK_DEBUG, CYCLE_TERRAIN_DEBUG,
            TO_WRITE_MODE)

    /** Velocity in units per second for moving forward, backward and strafing left/right.  */
    private val velocity = 4.5f
    /** Sets how many degrees to rotate per pixel the mouse moved.  */
    private val degreesPerPixel = 0.5f
    /** Height of camera when standing  */
    val height = 1.75f

    private val tmp = Vector3()
    private val tmp2 = Vector3()

    private var yaw = 0f
    private var pitch = 0f

    fun update(deltaTime: Float) {
        if (!FREE_MOUSE.isPressed) {
            pitch -= Gdx.input.deltaX * degreesPerPixel
            yaw -= Gdx.input.deltaY * degreesPerPixel

            if (yaw > 89f) {
                yaw = 89f
            } else if (yaw < -89f) {
                yaw = -89f
            }

            camera.direction.set(1f, 0f, 0f)
            camera.direction.rotate(camera.up, pitch)
            tmp.set(camera.direction).crs(camera.up).nor()
            camera.direction.rotate(tmp, yaw)
        }

        tmp2.set(camera.direction)
        tmp2.z = 0f
        tmp2.nor()

        val height: Float
        val velocity: Float
        if (MOVEMENT_DEBUG.isPressed) {
            if (SPRINT.isPressed) {
                velocity = this.velocity * 16f
                height = 8f
            } else {
                velocity = this.velocity * 0.03f
                height = 0.02f
            }
        } else {
            height = this.height
            velocity = if (!SPRINT.isPressed) {
                this.velocity * 2f
            } else {
                this.velocity
            }
        }

        if (FORWARD.isPressed) {
            tmp.set(tmp2).scl(deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (BACKWARD.isPressed) {
            tmp.set(tmp2).scl(-deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (STRAFE_LEFT.isPressed) {
            tmp.set(tmp2).crs(camera.up).nor().scl(-deltaTime * velocity)
            camera.position.add(tmp)
        }
        if (STRAFE_RIGHT.isPressed) {
            tmp.set(tmp2).crs(camera.up).nor().scl(deltaTime * velocity)
            camera.position.add(tmp)
        }


        camera.position.z = Math.max(heightmap(camera.position.x, camera.position.y), -0.5f * height) + height
        if (MOVEMENT_DEBUG.isPressed && Gdx.input.isKeyPressed(Keys.ALT_LEFT)) {
            camera.position.z = 50f
        }

        camera.update(true)
    }
}
