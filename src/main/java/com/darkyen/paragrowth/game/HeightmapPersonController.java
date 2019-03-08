package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.input.GameInput;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;

import static com.badlogic.gdx.Input.Keys;
import static com.darkyen.paragrowth.input.GameInput.Binding.bindKeyboard;
import static com.darkyen.paragrowth.input.GameInput.Binding.bindMouseButton;

/**
 * @author Darkyen
 */
public final class HeightmapPersonController {

    private final GameInput.BoundFunction FORWARD = GameInput.function("Forward", bindKeyboard(Keys.W), bindKeyboard(Keys.UP));
    private final GameInput.BoundFunction BACKWARD = GameInput.function("Backward", bindKeyboard(Keys.S), bindKeyboard(Keys.DOWN));
    private final GameInput.BoundFunction STRAFE_LEFT = GameInput.function("Left", bindKeyboard(Keys.A), bindKeyboard(Keys.LEFT));
    private final GameInput.BoundFunction STRAFE_RIGHT = GameInput.function("Right", bindKeyboard(Keys.D), bindKeyboard(Keys.RIGHT));
    private final GameInput.BoundFunction SPRINT = GameInput.function("Sprint", bindKeyboard(Keys.SHIFT_LEFT));

    private final GameInput.BoundFunction FREE_MOUSE = GameInput.toggleFunction("Free Mouse", bindMouseButton(Input.Buttons.RIGHT))
            .listen((times, pressed) -> {
                Gdx.input.setCursorCatched(!pressed);
                return true;
            });
    public final GameInput.BoundFunction GENERAL_DEBUG = GameInput.toggleFunction("General Debug", bindKeyboard(Input.Keys.F3));
    private final GameInput.BoundFunction MOVEMENT_DEBUG = GameInput.toggleFunction("Movement Debug", bindKeyboard(Input.Keys.F4));
    public final GameInput.BoundFunction PATCHWORK_DEBUG = GameInput.toggleFunction("Patchwork Debug", bindKeyboard(Input.Keys.F5));

    private final GameInput.BoundFunction TO_WRITE_MODE = GameInput.function("To Write Mode", bindKeyboard(Keys.ESCAPE))
            .listen((times, pressed) -> {
                if (pressed) {
                    final Screen oldScreen = ParagrowthMain.INSTANCE.getScreen();
                    ParagrowthMain.INSTANCE.setScreen(new WriteState());
                    if (oldScreen instanceof WanderState) {
                        oldScreen.dispose();
                    }
                }
                return false;
            });

    final GameInput.BoundFunction[] INPUT = {
            FORWARD,
            BACKWARD,
            STRAFE_LEFT,
            STRAFE_RIGHT,
            SPRINT,

            FREE_MOUSE,
            GENERAL_DEBUG,
            MOVEMENT_DEBUG,
            PATCHWORK_DEBUG,
            TO_WRITE_MODE
    };

    private Camera camera;
    private TerrainPatchwork map;

    public HeightmapPersonController(Camera camera, TerrainPatchwork map) {
        this.camera = camera;
        this.map = map;
    }

    /** Velocity in units per second for moving forward, backward and strafing left/right. */
    public final float velocity = 4.5f;
    /** Sets how many degrees to rotate per pixel the mouse moved. */
    public final float degreesPerPixel = 0.5f;
    /** Height of camera when standing */
    public final float height = 1.75f;

    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();

    private float yaw = 0f;
    private float pitch = 0f;

    public void update(float deltaTime){
        if (!FREE_MOUSE.isPressed()) {
            pitch -= Gdx.input.getDeltaX() * degreesPerPixel;
            yaw -= Gdx.input.getDeltaY() * degreesPerPixel;

            if(yaw > 89f){
                yaw = 89f;
            }else if(yaw < -89f){
                yaw = -89f;
            }

            camera.direction.set(1f,0f,0f);
            camera.direction.rotate(camera.up, pitch);
            tmp.set(camera.direction).crs(camera.up).nor();
            camera.direction.rotate(tmp, yaw);
        }

        tmp2.set(camera.direction);
        tmp2.z = 0f;
        tmp2.nor();

        final float height;
        final float velocity;
        if (MOVEMENT_DEBUG.isPressed()) {
            if (SPRINT.isPressed()) {
                velocity = this.velocity * 16f;
                height = 8f;
            } else {
                velocity = this.velocity * 0.03f;
                height = 0.02f;
            }
        } else {
            height = this.height;
            if (!SPRINT.isPressed()) {
                velocity = this.velocity * 2f;
            } else {
                velocity = this.velocity;
            }
        }

        if (FORWARD.isPressed()) {
            tmp.set(tmp2).scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (BACKWARD.isPressed()) {
            tmp.set(tmp2).scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (STRAFE_LEFT.isPressed()) {
            tmp.set(tmp2).crs(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (STRAFE_RIGHT.isPressed()) {
            tmp.set(tmp2).crs(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }


        camera.position.z = Math.max(map.heightAt(camera.position.x,camera.position.y), -0.5f * height) + height;

        camera.update(true);
    }
}
