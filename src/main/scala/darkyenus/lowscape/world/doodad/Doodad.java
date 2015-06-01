package darkyenus.lowscape.world.doodad;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Pool;

/**
 * @author Darkyen
 */
public class Doodad {

    private final Mesh mesh;
    private final Material material;

    public Doodad(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Renderable getRenderable(Pool<Renderable> pool){
        Renderable renderable = pool.obtain();
        renderable.mesh = mesh;
        renderable.meshPartOffset = 0;
        renderable.meshPartSize = mesh.getNumIndices();
        renderable.material = material;
        renderable.primitiveType = GL20.GL_TRIANGLES;
        return renderable;
    }
}
