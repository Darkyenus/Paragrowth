package darkyenus.lowscape.world.doodad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.math.MathUtils;
import darkyenus.lowscape.LowscapeMain;

import static com.badlogic.gdx.math.MathUtils.cos;
import static com.badlogic.gdx.math.MathUtils.sin;

/**
 * @author Darkyen
 */
public class DoodadFactory {
    private final TextureAtlas worldAtlas = LowscapeMain.assetManager.get("World.atlas");
    private final MeshBuilder meshBuilder = new MeshBuilder();
    private final DefaultShaderProvider shaderProvider;
    {
        DefaultShader.Config config = new DefaultShader.Config();
        config.fragmentShader = Gdx.files.local("doodad.frag").readString();
        config.defaultCullFace = 0;
        config.numBones = 0;
        config.numDirectionalLights = 0;
        config.numPointLights = 0;
        config.numSpotLights = 0;
        shaderProvider = new DefaultShaderProvider(config);
    }

    private final VertexInfo vBl = new VertexInfo();
    private final VertexInfo vBr = new VertexInfo();
    private final VertexInfo vTl = new VertexInfo();
    private final VertexInfo vTr = new VertexInfo();

    {
        vBl.hasPosition = true;
        vBr.hasPosition = true;
        vTl.hasPosition = true;
        vTr.hasPosition = true;
        vBl.hasUV = true;
        vBr.hasUV = true;
        vTl.hasUV = true;
        vTr.hasUV = true;
    }

    public Doodad createPaperModel(float scale, String...textureRegionName){
        TextureRegion[] regions = new TextureRegion[textureRegionName.length];
        for (int i = 0; i < textureRegionName.length; i++) {
            regions[i] = worldAtlas.findRegion(textureRegionName[i]);
        }
        return createPaperModel(scale, regions);
    }

    public Doodad createPaperModel(float scale, TextureRegion...regions){
        assert regions.length >= 1;

        meshBuilder.begin(Usage.Position | Usage.TextureCoordinates /*| Usage.ColorPacked*/, GL20.GL_TRIANGLES);
        float rotationStep = MathUtils.PI2 / regions.length;
        float rotation = 0f;

        Texture texture = null;
        for(TextureRegion region:regions){
            vBr.position.set(region.getRegionWidth()/2 * scale * cos(rotation),region.getRegionWidth()/2 * scale * sin(rotation),0f);
            vBl.position.set(vBr.position).scl(-1f);

            vTl.position.set(vBl.position);
            vTl.position.z = region.getRegionHeight() * scale;
            vTr.position.set(vBr.position);
            vTr.position.z = vTl.position.z;

            vTl.uv.set(region.getU(),region.getV());
            vTr.uv.set(region.getU2(),region.getV());
            vBl.uv.set(region.getU(),region.getV2());
            vBr.uv.set(region.getU2(),region.getV2());

            meshBuilder.rect(vTl,vTr,vBr,vBl);
            rotation += rotationStep;
            if(texture == null){
                texture = region.getTexture();
            }else{
                assert texture == region.getTexture();
            }
        }
        final Mesh mesh = meshBuilder.end();
        final Material material = new Material(
                TextureAttribute.createDiffuse(texture),
                IntAttribute.createCullFace(0),
                FloatAttribute.createAlphaTest(0.5f),
                new BlendingAttribute());
        return new Doodad(mesh, material);
    }
}
