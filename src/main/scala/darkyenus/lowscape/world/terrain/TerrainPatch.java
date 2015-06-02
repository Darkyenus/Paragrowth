package darkyenus.lowscape.world.terrain;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 *
 * Due to how are indices handled, size can be 256 at most
 *
 * @author Darkyen
 */
public class TerrainPatch implements RenderableProvider {
    private final float[][] heights;
    private final int size, xOffset, yOffset;
    private final Matrix4 transform = new Matrix4();
    protected final BoundingBox boundingBox;
    private final Shader shader;

    private final int vertexSize;
    private final float[] vertices;

    private final Mesh mesh;

    private final Material terrainMaterial;

    private final Renderable[] stripRenderables;

    public TerrainPatch(float[][] heights, int size, int xOffset, int yOffset, BoundingBox boundingBox, Shader shader, Texture terrainTexture) {
        assert size > 0 && size <= 256 : "Terrain size must be positive and at most 256";
        this.heights = heights;
        this.size = size;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.boundingBox = boundingBox;
        this.shader = shader;

        vertexSize = 3+3+2;
        vertices = new float[size * size * vertexSize];
        mesh = new Mesh(false,true,size*size,size * 2 * (size - 1),new VertexAttributes(
                VertexAttribute.Position(),//3
                VertexAttribute.Normal(),//3
                VertexAttribute.TexCoords(0)//2
        ));

        //Generate indices
        final short[] indices = new short[size * 2 * (size - 1)];
        int index = 0;
        for (int strip = 0; strip < size - 1; strip++) {
            for (int x = 0; x < size; x++) {
                indices[index++] = (short)(strip * size + x);
                int s = strip * size + size + x;
                indices[index++] = (short)s;
            }
        }
        int strip = 0;

        while(strip < size - 1){

            strip += 1;
        }

        mesh.setIndices(indices);
        terrainMaterial = new Material(
                //ColorAttribute.createDiffuse(0.1f, 0.9f, 0.2f, 1f),
                TextureAttribute.createDiffuse(terrainTexture)
        );
        stripRenderables = new Renderable[this.size - 1];
        for (int i = 0; i < stripRenderables.length; i++) {
            stripRenderables[i] = new Renderable();
        }
    }

    private final Vector3 va = new Vector3();
    private final Vector3 vb = new Vector3();
    private final Vector3 vc = new Vector3();
    private final Vector3 vd = new Vector3();
    private final Vector3 vn = new Vector3();
    private final Vector3 vt = new Vector3();

    public void updateMesh(){
        float uvStep = 1f/size;

        int x = xOffset;
        int i = 0;
        while(x < xOffset + size){
            int y = yOffset;
            while(y < yOffset + size){
                vertices[i] = x;
                vertices[i + 1] = y;
                vertices[i + 2] = heights[x][y];

                if(x == 0 || y == 0 || x == heights.length - 1 || y == heights[x].length - 1){
                    vertices[i + 3] = 0f;
                    vertices[i + 4] = 0f;
                    vertices[i + 5] = 1f; //Simple upward pointing normal since not enough sampling points
                }else{
                    //http://forum.unity3d.com/threads/calculate-vertex-normals-in-shader-from-heightmap.169871/
                    final float hA = heights[x][y - 1];
                    final float hB = heights[x + 1][y];
                    final float hC = heights[x][y + 1];
                    final float hD = heights[x - 1][y];
                    final float hN = heights[x][y];
                    va.set(0,1,hA - hN);
                    vb.set(1,0,hB - hN);
                    vc.set(0,-1,hC - hN);
                    vd.set(-1,0,hD - hN);

                    vt.set(va).crs(vb);
                    vn.set(vt);
                    vt.set(vb).crs(vc);
                    vn.add(vt);
                    vt.set(vc).crs(vd);
                    vn.add(vt);
                    vt.set(vd).crs(va);
                    vn.add(vt);

                    vn.scl(1f/ -4f).nor();

                    vertices[i + 3] = vn.x;
                    vertices[i + 4] = vn.y;
                    vertices[i + 5] = vn.z;
                }

                //Normal
                vertices[i + 6] = x * uvStep;
                vertices[i + 7] = y * uvStep;

                i += vertexSize;
                y += 1;
            }
            x += 1;
        }
        mesh.setVertices(vertices);
    }

    public void updateRenderables(){
        final int size2 = size * 2;

        for (int strip = 0; strip < stripRenderables.length; strip++) {
            Renderable terrain = stripRenderables[strip];
            terrain.mesh = mesh;
            terrain.meshPartOffset = strip * size2;
            terrain.meshPartSize = size2;
            terrain.primitiveType = GL20.GL_TRIANGLE_STRIP;
            terrain.material = terrainMaterial;
            terrain.worldTransform.set(transform);
            terrain.shader = shader;
        }
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        renderables.addAll(stripRenderables,0,stripRenderables.length);
    }
}
