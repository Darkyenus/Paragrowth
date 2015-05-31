package darkyenus.lowscape.world.skybox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import darkyenus.lowscape.world.LowscapeRenderableSorter;

/**
 * @author Darkyen
 */
public class SkyboxRenderable extends Renderable {
    public SkyboxRenderable() {
        //Mesh
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(VertexAttributes.Usage.Position,GL20.GL_TRIANGLES);
        /*meshBuilder.rect(
                -0.9f,-0.9f,-1f,
                1f,-1f,-1f,
                1f,1f,-1f,
                -1f, 1f,-1f,
                0f,0f,1f);*/
        meshBuilder.box(2f,2f,2f);
        mesh = meshBuilder.end();

        //Material
        material = new Material(IntAttribute.createCullFace(GL20.GL_NONE), new DepthTestAttribute(0, false));

        //Shader
        DefaultShader.Config config = new DefaultShader.Config();
        config.fragmentShader = Gdx.files.local("sky.frag").readString();
        config.vertexShader = Gdx.files.local("sky.vert").readString();
        shader = new DefaultShader(this, config);
        shader.init();

        //Misc
        userData = LowscapeRenderableSorter.SKYBOX;
    }
}
