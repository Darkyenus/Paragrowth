package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.darkyen.paragrowth.util.PrioritizedShader;
import com.darkyen.paragrowth.util.AutoReloadShaderProgram;

/**
 *
 */
class DoodadShader extends BaseShader implements PrioritizedShader {

    private DoodadShader() {
        register(DefaultShader.Inputs.projViewWorldTrans, DefaultShader.Setters.projViewWorldTrans);
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Do not init me directly!");
    }

    @Override
    public int compareTo(Shader other) {
        if (other == null) return -1;
        if (other == this) return 0;
        return 0;
    }

    @Override
    public boolean canRender(Renderable instance) {
        return true;
    }

    @Override
    public void render (Renderable renderable, Attributes combinedAttributes) {
        context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        context.setCullFace(GL20.GL_NONE);
        context.setDepthTest(GL20.GL_LESS);
        context.setDepthMask(true);

        super.render(renderable, combinedAttributes);
    }

    private static DoodadShader INSTANCE = null;
    static DoodadShader get(Renderable renderable) {
        DoodadShader instance = INSTANCE;
        if (instance != null) {
            return instance;
        }

        instance = new DoodadShader();
        instance.init(new AutoReloadShaderProgram(Gdx.files.local("doodad_vert.glsl"), Gdx.files.local("doodad_frag.glsl")), renderable);

        INSTANCE = instance;
        return instance;
    }

    @Override
    public int priority() {
        return DOODADS;
    }
}
