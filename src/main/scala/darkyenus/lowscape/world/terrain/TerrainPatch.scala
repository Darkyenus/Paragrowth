package darkyenus.lowscape.world.terrain

import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.{Shader, Material, Renderable, RenderableProvider}
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.{GL20, Mesh, VertexAttribute, VertexAttributes}
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.{Matrix4, Vector3}
import com.badlogic.gdx.utils.{Array => GArray, Pool}

/**
 *
 * Due to how are indices handled, size can be 256 at most
 *
 * Private property.
 * User: Darkyen
 * Date: 07/12/14
 * Time: 22:18
 */
final class TerrainPatch(heights:Array[Array[Float]], val size:Int, val xOffset:Int, val yOffset:Int, val transform:Matrix4 = new Matrix4(), val boundingBox: BoundingBox, shader:Shader) extends RenderableProvider {

  assume(size > 0 && size <= 256,"Terrain size must be positive and at most 256")

  private val vertices = new Array[Float](size * size * (3 + 3))

  private val mesh = new Mesh(false,true,size*size,size * 2 * (size - 1),new VertexAttributes(
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)
  ))

  {
    //Generate indices
    val indices:Array[Short] = new Array[Short](size * 2 * (size - 1))
    var strip = 0
    var x = 0
    var i = 0
    while(strip < size - 1){
      while(x < size){
        indices(i) = (strip * size + x).toShort
        i += 1
        val s = strip * size + size + x
        indices(i) = s.toShort
        i += 1
        x += 1
      }
      x = 0
      strip += 1
    }

    mesh.setIndices(indices)
  }

  private val va = new Vector3
  private val vb = new Vector3
  private val vc = new Vector3
  private val vd = new Vector3
  private val vn = new Vector3
  private val vt = new Vector3

  def updateMesh(): Unit ={
    var x = xOffset
    var i = 0
    while(x < xOffset + size){
      var y = yOffset
      while(y < yOffset + size){
        vertices(i + 0) = x
        vertices(i + 1) = y
        vertices(i + 2) = heights(x)(y)

        if(x == 0 || y == 0 || x == heights.length - 1 || y == heights(x).length - 1){
          vertices(i + 3) = 0f
          vertices(i + 4) = 0f
          vertices(i + 5) = 1f //Simple upward pointing normal since not enough sampling points
        }else{
          //http://forum.unity3d.com/threads/calculate-vertex-normals-in-shader-from-heightmap.169871/
          val hA = heights(x)(y - 1)
          val hB = heights(x + 1)(y)
          val hC = heights(x)(y + 1)
          val hD = heights(x - 1)(y)
          val hN = heights(x)(y)
          va.set(0,1,hA - hN)
          vb.set(1,0,hB - hN)
          vc.set(0,-1,hC - hN)
          vd.set(-1,0,hD - hN)

          vt.set(va).crs(vb)
          vn.set(vt)
          vt.set(vb).crs(vc)
          vn.add(vt)
          vt.set(vc).crs(vd)
          vn.add(vt)
          vt.set(vd).crs(va)
          vn.add(vt)

          vn.scl(1f/ -4f).nor()

          vertices(i + 3) = vn.x
          vertices(i + 4) = vn.y
          vertices(i + 5) = vn.z
          /*float3 va = { 0, 1, (h_A - h_N)*heightScale };
          float3 vb = { 1, 0, (h_B - h_N)*heightScale };
          float3 vc = { 0, -1, (h_C - h_N)*heightScale };
          float3 vd = { -1, 0, (h_D - h_N)*heightScale };
          //cross products of each vector yields the normal of each tri - return the average normal of all 4 tris
          float3 average_n = ( cross(va, vb) + cross(vb, vc) + cross(vc, vd) + cross(vd, va) ) / -4;
          return normalize( average_n );*/
        }

        i += 6
        y += 1
      }
      x += 1
    }
    mesh.setVertices(vertices)
  }

  val terrainMaterial = new Material(ColorAttribute.createDiffuse(0.1f,0.9f,0.2f,1f))

  private val stripRenderables:Array[Renderable] = Array.fill(size - 1)(new Renderable)

  def updateRenderables(){
    var strip = 0
    val size2 = size * 2

    while(strip < stripRenderables.length){
      val terrain = stripRenderables(strip)
      terrain.mesh = mesh
      terrain.meshPartOffset = strip * size2
      terrain.meshPartSize = size2
      terrain.primitiveType = GL20.GL_TRIANGLE_STRIP
      terrain.material = terrainMaterial
      terrain.worldTransform.set(transform)
      terrain.shader = shader
      strip += 1
    }
  }

  override def getRenderables(renderables: GArray[Renderable], pool: Pool[Renderable]): Unit = {
    renderables.addAll(stripRenderables,0,stripRenderables.length)
  }
}
