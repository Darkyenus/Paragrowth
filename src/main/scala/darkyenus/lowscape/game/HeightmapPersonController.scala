package darkyenus.lowscape.game

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.IntIntMap
import com.badlogic.gdx.{Gdx, InputAdapter}
import darkyenus.lowscape.world.terrain.TerrainPatchwork

/**
 * Private property.
 * User: Darkyen
 * Date: 11/12/14
 * Time: 14:37
 */
class HeightmapPersonController(camera: Camera, map:TerrainPatchwork) extends InputAdapter {
  private val keys: IntIntMap = new IntIntMap
  private val STRAFE_LEFT: Int = Keys.A
  private val STRAFE_RIGHT: Int = Keys.D
  private val FORWARD: Int = Keys.W
  private val BACKWARD: Int = Keys.S
  private val JUMP: Int = Keys.SPACE

  /** Velocity in units per second for moving forward, backward and strafing left/right. */
  var velocity: Float = 1.1f
  /** Sets how many degrees to rotate per pixel the mouse moved. */
  var degreesPerPixel: Float = 0.5f
  /** Height of camera when standing */
  var height:Float = 1.75f
  /** World gravity in this world */
  var gravity:Float = 9.81f
  /** Initial jump speed */
  var jumpPower:Float = 1.43f

  private val tmp: Vector3 = new Vector3
  private val tmp2: Vector3 = new Vector3

  override def keyDown(keycode: Int): Boolean = {
    keys.put(keycode, keycode)
    true
  }

  override def keyUp(keycode: Int): Boolean = {
    keys.remove(keycode, 0)
    true
  }

  private var yaw = 0f
  private var pitch = 0f
  private var standing = true
  private val jumpSpeed = new Vector3()

  override def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = {
    pitch -= Gdx.input.getDeltaX * degreesPerPixel
    yaw -= Gdx.input.getDeltaY * degreesPerPixel

    if(yaw > 89f){
      yaw = 89f
    }else if(yaw < -89f){
      yaw = -89f
    }

    camera.direction.set(1f,0f,0f)
    camera.direction.rotate(camera.up, pitch)
    tmp.set(camera.direction).crs(camera.up).nor
    camera.direction.rotate(tmp, yaw)
    true
  }

  def update(deltaTime: Float) {
    tmp2.set(camera.direction)
    tmp2.z = 0f
    tmp2.nor()

    if (keys.containsKey(FORWARD)) {
      tmp.set(tmp2).scl(deltaTime * velocity)
      camera.position.add(tmp)
    }
    if (keys.containsKey(BACKWARD)) {
      tmp.set(tmp2).scl(-deltaTime * velocity)
      camera.position.add(tmp)
    }
    if (keys.containsKey(STRAFE_LEFT)) {
      tmp.set(tmp2).crs(camera.up).nor.scl(-deltaTime * velocity)
      camera.position.add(tmp)
    }
    if (keys.containsKey(STRAFE_RIGHT)) {
      tmp.set(tmp2).crs(camera.up).nor.scl(deltaTime * velocity)
      camera.position.add(tmp)
    }

    val standingHeight = map.heightAt(camera.position.x,camera.position.y) + height
    if(standing){
      if(keys.containsKey(JUMP)){
        jumpSpeed.set(tmp2.x,tmp2.y,jumpPower)
        standing = false
      }
      camera.position.z = standingHeight
    }else{
      jumpSpeed.z -= gravity * deltaTime
      camera.position.z += jumpSpeed.z
      if(camera.position.z <= standingHeight){
        camera.position.z = standingHeight
        standing = true
      }
    }

    camera.update(true)
  }
}
