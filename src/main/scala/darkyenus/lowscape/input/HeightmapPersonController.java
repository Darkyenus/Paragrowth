package darkyenus.lowscape.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import darkyenus.lowscape.world.terrain.TerrainPatchwork;

import static com.badlogic.gdx.Input.Keys;

/**
 * @author Darkyen
 */
public final class HeightmapPersonController extends InputAdapter {
    private Camera camera;
    private TerrainPatchwork map;

    public HeightmapPersonController(Camera camera, TerrainPatchwork map) {
        this.camera = camera;
        this.map = map;
    }

    private final IntIntMap keys = new IntIntMap();
    private static final int STRAFE_LEFT = Keys.A;
    private static final int STRAFE_RIGHT = Keys.D;
    private static final int FORWARD = Keys.W;
    private static final int BACKWARD = Keys.S;
    private static final int JUMP = Keys.SPACE;

    /** Velocity in units per second for moving forward, backward and strafing left/right. */
    public final float velocity = 4.5f;
    /** Sets how many degrees to rotate per pixel the mouse moved. */
    public final float degreesPerPixel = 0.5f;
    /** Height of camera when standing */
    public final float height = 1.75f;
    /** World gravity in this world */
    public final float gravity = 3.3f;
    /** Initial jump speed */
    public final float jumpPower = 0.43f;

    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return true;
    }

    private float yaw = 0f;
    private float pitch = 0f;
    private boolean standing = true;
    private final Vector3 jumpSpeed = new Vector3();

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
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
        return true;
    }

    public void update(float deltaTime){
        tmp2.set(camera.direction);
        tmp2.z = 0f;
        tmp2.nor();

        if (keys.containsKey(FORWARD)) {
            tmp.set(tmp2).scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(BACKWARD)) {
            tmp.set(tmp2).scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_LEFT)) {
            tmp.set(tmp2).crs(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(STRAFE_RIGHT)) {
            tmp.set(tmp2).crs(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }

        float standingHeight = map.heightAt(camera.position.x,camera.position.y) + height;
        if(standing){
            if(keys.containsKey(JUMP)){
                jumpSpeed.set(tmp2.x,tmp2.y,jumpPower);
                standing = false;
            }
            camera.position.z = standingHeight;
        }else{
            jumpSpeed.z -= gravity * deltaTime;
            camera.position.z += jumpSpeed.z;
            if(camera.position.z <= standingHeight){
                camera.position.z = standingHeight;
                standing = true;
            }
        }

        camera.update(true);
    }
}
