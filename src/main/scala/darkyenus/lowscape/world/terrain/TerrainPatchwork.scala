package darkyenus.lowscape.world.terrain

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.{Renderable, RenderableProvider, Shader}
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.{MathUtils, Vector3}
import com.badlogic.gdx.utils.{Array => GArray, Pool}

/**
 * Private property.
 * User: Darkyen
 * Date: 08/12/14
 * Time: 20:13
 */
final class TerrainPatchwork(val patchAmount:Int,val patchSize:Int, val camera:Camera, patchMinHeight:Float = -100f, patchMaxHeight:Float = 1000f) extends RenderableProvider {

  val heights = Array.ofDim[Float](patchAmount * patchSize - patchAmount + 1,patchAmount * patchSize - patchAmount + 1)

  val terrainShader:Shader = null

  val patches = Array.tabulate[TerrainPatch](patchAmount * patchAmount)((i) => {
    val x = i % patchAmount
    val y = i / patchAmount
    val boundingBox = new BoundingBox(
      new Vector3(x * patchSize, y * patchSize, patchMinHeight),
      new Vector3(x * patchSize + patchSize, y * patchSize + patchSize, patchMaxHeight))

    val patch = new TerrainPatch(heights,patchSize,x * patchSize - x,y * patchSize - y, boundingBox = boundingBox, shader = terrainShader)
    patch.updateRenderables()
    patch
  })

  def generateMesh(tabulator:(Int,Int) => Float): Unit ={
    var x = 0
    var y = 0
    while(x < heights.length){
      while(y < heights.length){
        heights(x)(y) = tabulator(x,y)
        y += 1
      }
      y = 0
      x += 1
    }
  }

  def updateMesh(): Unit ={
    var i = 0
    while(i < patches.length){
      patches(i).updateMesh()
      i += 1
    }
  }

  updateMesh()

  override def getRenderables(renderables: GArray[Renderable], pool: Pool[Renderable]): Unit = {
    var i = 0
    val frustum = camera.frustum
    while(i < patches.length){
      val patch = patches(i)
      if(frustum.boundsInFrustum(patch.boundingBox)){
        patch.getRenderables(renderables,pool)
      }
      i += 1
    }
  }

  private val worldSizeLimit = patchAmount * patchSize - patchAmount
  def heightAt(x:Float,y:Float):Float = {
    val lowX = x.toInt
    val lowY = y.toInt
    if(lowX < 0 || lowY < 0 || lowX >= worldSizeLimit || lowY >= worldSizeLimit){
      return 0
    }
    val bottomX = MathUtils.lerp(heights(lowX)(lowY),heights(lowX + 1)(lowY),x - lowX)
    val topX = MathUtils.lerp(heights(lowX)(lowY + 1),heights(lowX + 1)(lowY+1),x - lowX)
    MathUtils.lerp(bottomX,topX,y - lowY)
  }
}
