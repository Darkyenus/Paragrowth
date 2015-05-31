package darkyenus.lowscape.world.doodad

import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.{Material, Renderable, RenderableProvider, Shader}
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math
import com.badlogic.gdx.math.{MathUtils, Matrix4, Vector2, Vector3}
import com.badlogic.gdx.utils.{Array => GArray, Pool}

/**
 * Private property.
 * User: Darkyen
 * Date: 18/12/14
 * Time: 19:22
 */
class DoodadLoader {
  private val meshAttributes = new VertexAttributes(
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
    new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"))

  private class VertexInfo(val position:Vector3 = new Vector3,val uv:Vector2 = new Vector2)

  /** @param size amount of quads this can hold. This does not have to hold only quads. */
  private class GrowingMesh(size:Int) {
    val maxVertices = size*4
    val maxIndices = size*6
    val vertices = new Array[Float](maxVertices)//TODO Probably wrong
    val indices = new Array[Short](maxIndices)
    val mesh = new Mesh(true, maxVertices, maxIndices, meshAttributes)
    private var vertexPosition = 0
    var indexPosition = 0
    private var dirty = false

    def willFit(vertices:Int,indices:Int):Boolean = {
      vertexPosition + vertices < maxVertices && indexPosition + indices < maxIndices
    }

    @inline
    private def vertex(v:VertexInfo):Short = {
      val result:Short = vertexPosition.toShort
      val vertexBaseIndex = vertexPosition * meshAttributes.vertexSize
      vertices(vertexBaseIndex) = v.position.x
      vertices(vertexBaseIndex + 1) = v.position.y
      vertices(vertexBaseIndex + 2) = v.position.z
      vertices(vertexBaseIndex + 3) = v.uv.x
      vertices(vertexBaseIndex + 4) = v.uv.y
      vertexPosition += 1
      result
    }

    @inline
    private def index(i:Short): Unit ={
      indices(indexPosition) = i
      indexPosition += 1
    }

    /**
     * Adds a rectangle into this mesh. Does NOT check if it fits.
     * @param vBl Bottom Left corner
     * @param vBr Bottom Right corner
     * @param vTr Top Right corner
     * @param vTl Top Left corner
     */
    def addRectangle(vBl:VertexInfo,vBr:VertexInfo,vTr:VertexInfo,vTl:VertexInfo): Unit = {
      dirty = true
      val bl = vertex(vBl)
      val br = vertex(vBr)
      val tr = vertex(vTr)
      val tl = vertex(vTl)
      index(bl)
      index(br)
      index(tr)
      index(tr)
      index(tl)
      index(bl)
    }

    def refresh(): Unit ={
      if(dirty){
        dirty = false
        mesh.setVertices(vertices)
        mesh.setIndices(indices)
      }
    }
  }

  private val meshes = new GArray[GrowingMesh](false,2,classOf[GrowingMesh])

  def refreshMeshes(): Unit ={
    var i = 0
    while(i < meshes.size){
      meshes.items(i).refresh()
      i += 1
    }
  }

  @inline
  private def activeMesh:GrowingMesh = {
    meshes.items(meshes.size - 1)
  }

  private def ensureMeshSpace(vertices:Int,indices:Int): Unit ={
    if(meshes.size == 0 || !activeMesh.willFit(vertices,indices)){
      //5460 <- From SpriteBatch, magic number, lazy to compute, will be probably right
      meshes.add(new GrowingMesh(5460))
    }
  }

  private val doodadMaterial = new Material(ColorAttribute.createAmbient(Color.WHITE))

  private val viTmp1 = new VertexInfo()
  private val viTmp2 = new VertexInfo()
  private val viTmp3 = new VertexInfo()
  private val viTmp4 = new VertexInfo()

  def loadPaperModel(scale:Float,regions:TextureRegion*):DoodadModel = {
    val vertices = regions.size * 4
    val indices = regions.size * 6
    ensureMeshSpace(vertices,indices)
    val mesh = activeMesh
    val offset = mesh.indexPosition

    val vBl:VertexInfo = viTmp1
    val vBr:VertexInfo = viTmp2
    val vTr:VertexInfo = viTmp3
    val vTl:VertexInfo = viTmp4

    def setVertices(tr:TextureRegion,rotation:Float): Unit = {
      import com.badlogic.gdx.math.MathUtils._
      vBr.position.set(tr.getRegionWidth/2 * scale * cos(rotation),tr.getRegionWidth/2 * scale * sin(rotation),0f)
      vBl.position.set(vBr.position).scl(-1f)

      vTl.position.set(vBl.position)
      vTl.position.z = tr.getRegionHeight * scale
      vTr.position.set(vBr.position)
      vTr.position.z = vTl.position.z

      vTl.uv.set(tr.getU,tr.getV)
      vTr.uv.set(tr.getU2,tr.getV)
      vBl.uv.set(tr.getU,tr.getV2)
      vBr.uv.set(tr.getU2,tr.getV2)
    }

    val rotationPart = MathUtils.PI2 / regions.size
    var rotation = 0f
    for(reg <- regions){
      setVertices(reg,rotation)
      mesh.addRectangle(vBl,vBr,vTr,vTl)
      rotation += rotationPart
    }

    new DoodadModel(mesh.mesh,offset,indices,doodadMaterial,null)
  }
}

class DoodadModel(val mesh: Mesh, val offset:Int, val length:Int, val material: Material, val shader:Shader)

class DoodadModelInstance(val model: DoodadModel) extends RenderableProvider {
  
  val position:Matrix4 = new math.Matrix4()
  
  override def getRenderables(renderables: GArray[Renderable], pool: Pool[Renderable]): Unit = {
    val r = pool.obtain()
    val m = model
    r.mesh = m.mesh
    r.meshPartOffset = m.offset
    r.meshPartSize = m.length
    r.primitiveType = GL20.GL_TRIANGLES
    r.worldTransform.set(position)
    r.material = m.material
    r.shader = m.shader

    renderables.add(r)
  }
}
