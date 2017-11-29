package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 *
 */
class TerrainShader extends BaseShader {

    private final int u_projViewWorldTrans;

    private TerrainShader() {
        u_projViewWorldTrans = register(DefaultShader.Inputs.projViewWorldTrans, DefaultShader.Setters.projViewWorldTrans);
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
        context.setCullFace(GL20.GL_BACK);
        context.setDepthTest(GL20.GL_LEQUAL, 0f, 1f);
        context.setDepthMask(true);

        super.render(renderable, combinedAttributes);
    }

    private static TerrainShader INSTANCE = null;
    static TerrainShader get(Renderable renderable) {
        TerrainShader instance = INSTANCE;
        if (instance != null) {
            return instance;
        }

        instance = new TerrainShader();
        instance.init(new ShaderProgram(Gdx.files.local("terrain_vert.glsl"), Gdx.files.local("terrain_frag.glsl")), renderable);

        INSTANCE = instance;
        return instance;
    }
}
