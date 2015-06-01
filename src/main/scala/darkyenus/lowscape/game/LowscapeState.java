package darkyenus.lowscape.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import darkyenus.lowscape.graphics.LowscapeRenderableSorter;
import darkyenus.lowscape.graphics.skybox.SkyboxRenderable;
import darkyenus.lowscape.input.HeightmapPersonController;
import darkyenus.lowscape.world.doodad.Doodad;
import darkyenus.lowscape.world.doodad.DoodadFactory;
import darkyenus.lowscape.world.doodad.DoodadWorld;
import darkyenus.lowscape.world.terrain.TerrainPatchwork;

/**
 * @author Darkyen
 */
public final class LowscapeState extends ScreenAdapter {

    //2D
    private final ScreenViewport hudView;
    private final Stage hudStage;
    private final Label statsLabel;

    //3D
    private final ModelBatch modelBatch;
    private final PerspectiveCamera worldCam;
    private final ScreenViewport worldView;
    private final Environment environment;

    //3D Objects
    private final SkyboxRenderable skyboxRenderable;
    private final TerrainPatchwork terrain;

    //Input
    private final HeightmapPersonController cameraController;

    public LowscapeState(Batch batch, Skin skin) {
        modelBatch = new ModelBatch(new LowscapeRenderableSorter());
        worldCam = new PerspectiveCamera(90f,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldView = new ScreenViewport(worldCam);
        hudView = new ScreenViewport();
        hudStage = new Stage(hudView, batch);

        hudStage.getRoot().setTransform(false);
        skyboxRenderable = new SkyboxRenderable();
        terrain = new TerrainPatchwork(1,256,worldCam);
        cameraController = new HeightmapPersonController(worldCam,terrain);
        environment = new Environment();

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.4f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.7f, 0.9f, -1f, -0.8f, -0.2f));

        worldCam.up.set(0,0,1f);
        worldCam.near = 0.1f;
        worldCam.far = 1000f;

        worldCam.position.set(128f, 128f, 10f);
        worldCam.direction.set(1,0,0);
        final Table hudTable = new Table(skin);
        statsLabel = new Label("Stats!", skin,"font-ui-small", Color.WHITE);

        hudTable.setFillParent(true);
        hudTable.top().left();
        hudTable.add(statsLabel).top().left();
        hudStage.addActor(hudTable);

        doodadWorld = new DoodadWorld(worldCam);
    }

    private final DoodadFactory doodadFactory = new DoodadFactory();
    private final DoodadWorld doodadWorld;
    private final Doodad pine = doodadFactory.createPaperModel(1f, "pine", "pine", "pine");


    @Override
    public void show() {
        Gdx.input.setInputProcessor(cameraController);
        worldCam.update();


        doodadWorld.addDoodad(pine).setToTranslation(130f,130f, 0f);
        doodadWorld.addDoodad(pine).setToTranslation(131f,131f, 1f);
        doodadWorld.addDoodad(pine).setToTranslation(132f,132f, 2f);
        doodadWorld.addDoodad(pine).setToTranslation(133f,133f, 3f);

        //terrain.generateMesh((x, y) => 25f * MathUtils.sinDeg(time*2f + (x * 180f) / 32) * MathUtils.sinDeg(time*2f - (y * 180f) / 32))
        terrain.updateMesh();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        updateWorld(delta);
        hudStage.act(delta);

        modelBatch.begin(worldCam);

        modelBatch.render(skyboxRenderable);
        modelBatch.render(terrain,environment);
        modelBatch.render(doodadWorld, environment);

        modelBatch.end();

        hudStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width,height);
        hudView.update(width,height,true);
    }

    private void updateWorld(float delta) {
        //terrain.generateMesh((x, y) => 25f * MathUtils.sinDeg(time*18f + (x * 180f) / 128) * MathUtils.sinDeg(time*18f - (y * 180f) / 128))
        //terrain.updateMesh()
        {
            float pineX = MathUtils.random(256f);
            float pineY = MathUtils.random(256f);
            doodadWorld.addDoodad(pine).setToTranslation(pineX,pineY, terrain.heightAt(pineX,pineY));
        }

        cameraController.update(delta);

        statsLabel.setText("FPS: "+Gdx.graphics.getFramesPerSecond()
                +"\nX: "+worldCam.position.x
                +"\nY: "+worldCam.position.y
                +"\nZ: "+worldCam.position.z);
    }

}
