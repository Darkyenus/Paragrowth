package com.darkyen.paragrowth.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.darkyen.paragrowth.ParagrowthMain;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.WorldGenerator;
import com.darkyen.paragrowth.doodad.DoodadWorld;
import com.darkyen.paragrowth.input.GameInput;
import com.darkyen.paragrowth.render.RenderBatch;
import com.darkyen.paragrowth.skybox.Skybox;
import com.darkyen.paragrowth.terrain.TerrainPatchwork;
import com.darkyen.paragrowth.util.DebugRenderKt;
import org.lwjgl.opengl.GL32;

import static com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST;
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
    private final RenderBatch modelBatch;
    private final PerspectiveCamera worldCam;
    private final ScreenViewport worldView;

    private final ImmediateModeRenderer debugRenderer;

    //3D Objects
    private final Skybox skyboxRenderable;
    private final TerrainPatchwork terrain;
    private final DoodadWorld doodads;

    //Input
    private final GameInput gameInput;
    private final HeightmapPersonController cameraController;

    public WanderState(WorldCharacteristics worldCharacteristics) {
        this.worldCharacteristics = worldCharacteristics;
        System.out.println(worldCharacteristics);
        modelBatch = new RenderBatch();
        worldCam = new PerspectiveCamera(90f,Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        worldView = new ScreenViewport(worldCam);
        hudView = new ScreenViewport();
        hudStage = new Stage(hudView, ParagrowthMain.batch());

        hudStage.getRoot().setTransform(false);
        skyboxRenderable = new Skybox();

        debugRenderer = new ImmediateModeRenderer20(5000, false, true, 0, DebugRenderKt.getDebugShader());

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
        Gdx.input.setCursorCatched(true);
        worldCam.update();
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        GL32.glProvokingVertex(GL32.GL_FIRST_VERTEX_CONVENTION);// Needed for heightmap

        updateWorld(delta);
        hudStage.act(delta);

        // Used for skybox and for objects too close to camera
        // (they won't get proper depth-testing, but skybox won't show through)
        Gdx.gl.glEnable(GL_DEPTH_CLAMP);
        modelBatch.begin(worldCam);

        modelBatch.render(skyboxRenderable);
        modelBatch.render(terrain);
        //modelBatch.render(doodads); // TODO(jp): Uncomment

        modelBatch.end();
        Gdx.gl.glDisable(GL_DEPTH_CLAMP);

        if (cameraController.PATCHWORK_DEBUG.isPressed()) {
            Gdx.gl.glDisable(GL_DEPTH_TEST);
            debugRenderer.begin(worldCam.combined, GL20.GL_LINES);

            doodads.renderDebug(debugRenderer);

            debugRenderer.end();
            Gdx.gl.glEnable(GL_DEPTH_TEST);
        }


        if (cameraController.GENERAL_DEBUG.isPressed()) {
            hudStage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width,height);
        hudView.update(width,height,true);
    }

    private static final StringBuilder extraStats = new StringBuilder();

    private void updateWorld(float delta) {
        cameraController.update(delta);

        extraStats.setLength(0);
        extraStats.append("FPS: ").append(Gdx.graphics.getFramesPerSecond())
                .append("\nX: ").append(worldCam.position.x)
                .append("\nY: ").append(worldCam.position.y)
                .append("\nZ: ").append(worldCam.position.z)
                .append("\n");

        statsLabel.setText(extraStats);
    }

    @Override
    public void dispose() {
        hudStage.dispose();
        skyboxRenderable.dispose();
        terrain.dispose();
    }
}
