package darkyenus.lowscape.graphics.skybox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Matrix4;
import darkyenus.lowscape.graphics.LowscapeRenderableSorter;

/**
 * @author Darkyen
 */
public class SkyboxRenderable extends Renderable {
    public SkyboxRenderable() {
        //Mesh
        MeshBuilder meshBuilder = new MeshBuilder();
        meshBuilder.begin(VertexAttributes.Usage.Position,GL20.GL_TRIANGLES);
        meshBuilder.box(1f,1f,1f);
        mesh = meshBuilder.end();

        meshPartSize = mesh.getNumIndices();
        meshPartOffset = 0;
        primitiveType = GL20.GL_TRIANGLES;

        //Material
        material = new Material(IntAttribute.createCullFace(GL20.GL_NONE), new DepthTestAttribute(0, false));

        //Shader
        DefaultShader.Config config = new DefaultShader.Config();
        config.fragmentShader = Gdx.files.local("sky.frag").readString();
        config.vertexShader = Gdx.files.local("sky.vert").readString();

        DefaultShader shader = new DefaultShader(this, config);
        BaseShader.Uniform turnMatrix = new BaseShader.Uniform("u_viewTurnMat");
        BaseShader.GlobalSetter turnMatrixSetter = new BaseShader.GlobalSetter() {

            private Matrix4 resultCombined = new Matrix4();
            private Matrix4 tmp = new Matrix4();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                PerspectiveCamera origCam = (PerspectiveCamera) shader.camera;
                //Sets it to origCam.combined but without the translation part
                shader.set(inputID, resultCombined.set(origCam.projection).mul(tmp.setToLookAt(origCam.direction, origCam.up)));
            }
        };

        shader.register(turnMatrix, turnMatrixSetter);
        this.shader = shader;
        shader.init();

        //Misc
        userData = LowscapeRenderableSorter.SKYBOX;
    }
}
