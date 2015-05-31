package darkyenus.lowscape.world.doodad

import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.math.{MathUtils, Vector3}
import com.badlogic.gdx.utils.{Array => GArray, Pool}
import darkyenus.lowscape.world.doodad.StaticDoodadPart.StaticDoodadPart

/**
 * Private property.
 * User: Darkyen
 * Date: 15/12/14
 * Time: 17:24
 */
class LowscapeDoodadWorld extends RenderableProvider {

  val staticDoodads:GArray[StaticDoodad] = new GArray[StaticDoodad](false,128,classOf[StaticDoodad])

  override def getRenderables(renderables: GArray[Renderable], pool: Pool[Renderable]): Unit = {
    var i = 0
    while(i < staticDoodads.size){
      val renderable = pool.obtain()
      val staticDoodad = staticDoodads.items(i)
      renderable.mesh = staticDoodad.graphics
      renderable.meshPartOffset = 0
      renderable.meshPartSize = renderable.mesh.getNumIndices
      renderable.primitiveType = GL20.GL_TRIANGLES
      renderable.worldTransform.idt().setToTranslation(staticDoodad.position)
      renderable.material = Doodad.doodadMaterial
      renderables.add(renderable)
      i += 1
    }
  }
}

private object Doodad {
  val doodadAttributes = new VertexAttributes(VertexAttribute.Position(),VertexAttribute.TexCoords(0))
  val doodadMaterial = new Material(ColorAttribute.createAmbient(0.1f,0.1f,0.1f,1f))
  val meshBuilder:MeshBuilder = new utils.MeshBuilder()

  val vBl = new Vector3
  val vBr = new Vector3
  val vTl = new Vector3
  val vTr = new Vector3
}

class StaticDoodad(val graphics:StaticDoodadPart,val position:Vector3 = new Vector3)

object StaticDoodadPart {
  type StaticDoodadPart = Mesh
  import Doodad._

  def create(scale:Float,from:TextureRegion*):StaticDoodadPart = {
    assert(from.nonEmpty,"Cannot create StaticDoodadPart from empty TextureRegion list.")

    val mB = meshBuilder

    def setVertices(tr:TextureRegion,rotation:Float): Unit = {
      import com.badlogic.gdx.math.MathUtils._
      vBr.set(tr.getRegionWidth/2 * scale * cos(rotation),tr.getRegionWidth/2 * scale * sin(rotation),0f)
      vBl.set(vBr).scl(-1f)

      vTl.set(vBl)
      vTl.z = tr.getRegionHeight * scale
      vTr.set(vBr)
      vTr.z = vTl.z
    }

    val rotationPart = MathUtils.PI2 / from.length
    mB.begin(doodadAttributes,GL20.GL_TRIANGLES)
    var rotation = 0f
    for(reg <- from){
      setVertices(reg,rotation)
      rotation += rotationPart

      mB.setUVRange(reg)
      mB.rect(vBl,vBr,vTr,vTl,null)
    }
    val mesh = mB.end()

    mesh
  }
}
