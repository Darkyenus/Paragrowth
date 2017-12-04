package com.darkyen.paragrowth.skybox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Matrix4;
import com.darkyen.paragrowth.util.PrioritizedShader;
import com.darkyen.paragrowth.util.AutoReloadShaderProgram;

/**
 *
 */
public class SkyboxShader extends BaseShader implements PrioritizedShader {

    private static final BaseShader.Uniform turnMatrix = new BaseShader.Uniform("u_viewTurnMat");
    private static final BaseShader.GlobalSetter turnMatrixSetter = new BaseShader.GlobalSetter() {

        private Matrix4 resultCombined = new Matrix4();
        private Matrix4 tmp = new Matrix4();

        @Override
        public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
            PerspectiveCamera origCam = (PerspectiveCamera) shader.camera;
            //Sets it to origCam.combined but without the translation part
            shader.set(inputID, resultCombined.set(origCam.projection).mul(tmp.setToLookAt(origCam.direction, origCam.up)));
        }
    };


    private SkyboxShader() {
        register(DefaultShader.Inputs.cameraUp, DefaultShader.Setters.cameraUp);
        register(turnMatrix, turnMatrixSetter);
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Do not init me directly!");
    }

    @Override
    public int compareTo(Shader other) {
        return -1;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        context.setCullFace(GL20.GL_NONE);
        context.setDepthTest(GL20.GL_LESS);
        context.setDepthMask(false);

        super.render(renderable, combinedAttributes);
    }

    private static SkyboxShader INSTANCE = null;
    static SkyboxShader get(Renderable renderable) {
        SkyboxShader instance = INSTANCE;
        if (instance != null) {
            return instance;
        }

        instance = new SkyboxShader();
        instance.init(new AutoReloadShaderProgram(Gdx.files.local("sky_vert.glsl"), Gdx.files.local("sky_frag.glsl")), renderable);

        INSTANCE = instance;
        return instance;
    }

    @Override
    public int priority() {
        return SKYBOX;
    }
}
