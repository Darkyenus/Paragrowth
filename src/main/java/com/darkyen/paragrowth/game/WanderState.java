package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.WorldGenerator;
import com.darkyen.paragrowth.input.GameInput;
import com.darkyen.paragrowth.skybox.SkyboxRenderable;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;
import org.lwjgl.opengl.GL32;

/**
 * @author Darkyen
 */
public final class WanderState extends ScreenAdapter {

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
    private final GameInput gameInput;
    private final HeightmapPersonController cameraController;

    //Doodads

    public WanderState(WorldCharacteristics worldCharacteristics) {
        modelBatch = new ModelBatch();
        worldCam = new PerspectiveCamera(90f,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldView = new ScreenViewport(worldCam);
        hudView = new ScreenViewport();
        hudStage = new Stage(hudView, ParagrowthMain.batch());

        hudStage.getRoot().setTransform(false);
        skyboxRenderable = new SkyboxRenderable();

        environment = new Environment();

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.4f, 0.6f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.7f, 0.9f, -1f, -0.8f, -0.2f));

        worldCam.up.set(0,0,1f);
        worldCam.near = 0.01f;
        worldCam.far = 1000f;

        worldCam.position.set(1f, 1f, 10f);
        worldCam.direction.set(1,0,0);
        final Table hudTable = new Table(ParagrowthMain.skin());
        statsLabel = new Label("Stats!", ParagrowthMain.skin(),"font-ui", Color.WHITE);

        hudTable.setFillParent(true);
        hudTable.top().left();
        hudTable.add(statsLabel).top().left();
        hudStage.addActor(hudTable);

        //Terrain generation
        final WorldGenerator generator = new WorldGenerator(worldCharacteristics);
        terrain = new TerrainPatchwork(worldCam, generator);
        //doodadWorld = world.doodadWorld;

        cameraController = new HeightmapPersonController(worldCam,terrain);
        gameInput = new GameInput(cameraController.INPUT);
        gameInput.build();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(gameInput);
        worldCam.update();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        GL32.glProvokingVertex(GL32.GL_FIRST_VERTEX_CONVENTION);// Needed for heightmap
        updateWorld(delta);
        hudStage.act(delta);

        modelBatch.begin(worldCam);

        modelBatch.render(skyboxRenderable);
        modelBatch.render(terrain,environment);
        //modelBatch.render(doodadWorld, environment);

        modelBatch.end();

        hudStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width,height);
        hudView.update(width,height,true);
    }

    public static final StringBuilder extraStats = new StringBuilder();

    private void updateWorld(float delta) {
        cameraController.update(delta);

        statsLabel.setText("FPS: "+Gdx.graphics.getFramesPerSecond()
                +"\nX: "+worldCam.position.x
                +"\nY: "+worldCam.position.y
                +"\nZ: "+worldCam.position.z+"\n"+extraStats);

        extraStats.setLength(0);
    }

    @Override
    public void dispose() {
        hudStage.dispose();
        modelBatch.dispose();
        skyboxRenderable.dispose();
        terrain.dispose();
    }
}
