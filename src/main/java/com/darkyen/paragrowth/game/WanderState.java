package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.WorldGenerator;
import com.darkyen.paragrowth.doodad.DoodadWorld;
import com.darkyen.paragrowth.input.GameInput;
import com.darkyen.paragrowth.skybox.SkyboxRenderable;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;
import com.darkyen.paragrowth.util.PrioritizedShader;
import org.lwjgl.opengl.GL32;

import static org.lwjgl.opengl.GL32.GL_DEPTH_CLAMP;

/**
 * @author Darkyen
 */
public final class WanderState extends ScreenAdapter {

    private final WorldCharacteristics worldCharacteristics;

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
    private final DoodadWorld doodads;

    //Input
    private final GameInput gameInput;
    private final HeightmapPersonController cameraController;

    public WanderState(WorldCharacteristics worldCharacteristics) {
        this.worldCharacteristics = worldCharacteristics;
        System.out.println(worldCharacteristics);
        modelBatch = new ModelBatch(PrioritizedShader.SORTER);
        worldCam = new PerspectiveCamera(90f,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldView = new ScreenViewport(worldCam);
        hudView = new ScreenViewport();
        hudStage = new Stage(hudView, ParagrowthMain.batch());

        hudStage.getRoot().setTransform(false);
        skyboxRenderable = new SkyboxRenderable();

        environment = new Environment();

        worldCam.up.set(0,0,1f);
        worldCam.near = 0.4f;
        worldCam.far = 700f;

        worldCam.position.set(1f, 1f, 10f);
        worldCam.direction.set(1,0,0);
        final Table hudTable = new Table(ParagrowthMain.skin());
        statsLabel = new Label("Stats!", ParagrowthMain.skin(),"font-ui", Color.WHITE);

        hudTable.setFillParent(true);
        hudTable.top().left();
        hudTable.add(statsLabel).top().left();
        hudStage.addActor(hudTable);

        //Terrain generation
        final WorldGenerator generator = new WorldGenerator(worldCam, worldCharacteristics);
        terrain = generator.terrainPatchwork;
        doodads = generator.doodadWorld;

        cameraController = new HeightmapPersonController(worldCam, terrain);
        gameInput = new GameInput(cameraController.INPUT);
        gameInput.build();

        generator.setupInitialPosition(worldCam.position);
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

        Gdx.gl.glEnable(GL_DEPTH_CLAMP);
        modelBatch.begin(worldCam);

        modelBatch.render(skyboxRenderable);
        modelBatch.render(terrain, environment);
        modelBatch.render(doodads, environment);

        modelBatch.end();
        Gdx.gl.glDisable(GL_DEPTH_CLAMP);

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
                +"\nZ: "+worldCam.position.z
                +"\n"+extraStats);

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
