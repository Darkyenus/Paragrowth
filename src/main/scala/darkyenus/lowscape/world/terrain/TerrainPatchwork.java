package darkyenus.lowscape.world.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * @author Darkyen
 */
public class TerrainPatchwork implements RenderableProvider {

    private final Camera camera;
    private final float[][] heights;
    private final TerrainPatch[] patches;
    private final int worldSizeLimit;

    public TerrainPatchwork(int patchAmount, int patchSize, Camera camera) {
        this.camera = camera;

        final float patchMinHeight = -100f, patchMaxHeight = 1000f;
        final Shader terrainShader = null;

        heights = new float[patchAmount * patchSize - patchAmount + 1][patchAmount * patchSize - patchAmount + 1];

        patches = new TerrainPatch[patchAmount * patchAmount];
        for (int i = 0; i < patches.length; i++) {
            final int x = i % patchAmount;
            final int y = i / patchAmount;
            final BoundingBox boundingBox = new BoundingBox(
                    new Vector3(x * patchSize, y * patchSize, patchMinHeight),
                    new Vector3(x * patchSize + patchSize, y * patchSize + patchSize, patchMaxHeight));

            TerrainPatch patch = new TerrainPatch(heights,patchSize,x * patchSize - x,y * patchSize - y, boundingBox, terrainShader);
            patch.updateRenderables();
            patches[i] = patch;
        }

        worldSizeLimit = patchAmount * patchSize - patchAmount;

        updateMesh();
    }

    public void generateMesh(TerrainGenerator generator){
        for (int x = 0, h = heights.length; x < h; x++) {
            for (int y = 0; y < h; y++) {
                heights[x][y] = generator.getHeight(x,y);
            }
        }
    }

    @FunctionalInterface
    public interface TerrainGenerator {
        float getHeight(int x,int y);
    }

    public void updateMesh(){
        for(TerrainPatch patch:patches){
            patch.updateMesh();
        }
    }

    public final float heightAt(float x, float y) {
        final int lowX = (int) x;
        final int lowY = (int) y;
        if(lowX < 0 || lowY < 0 || lowX >= worldSizeLimit || lowY >= worldSizeLimit){
            return 0;
        }
        final float bottomX = MathUtils.lerp(heights[lowX][lowY],heights[lowX + 1][lowY],x - lowX);
        final float topX = MathUtils.lerp(heights[lowX][lowY + 1],heights[lowX + 1][lowY+1],x - lowX);
        return MathUtils.lerp(bottomX, topX, y - lowY);
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        final Frustum frustum = camera.frustum;
        for(TerrainPatch patch: patches){
            if(frustum.boundsInFrustum(patch.boundingBox)){
                patch.getRenderables(renderables,pool);
            }
        }
    }
}
