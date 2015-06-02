package darkyenus.lowscape.world.terrain;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.RandomXS128;
import darkyenus.lowscape.world.doodad.DoodadLibrary;
import darkyenus.lowscape.world.doodad.DoodadWorld;

import java.util.Random;

/**
 * @author Darkyen
 */
public class WorldGenerator {

    public static class World {
        public final DoodadWorld doodadWorld;
        public final TerrainPatchwork terrain;

        public World(DoodadWorld doodadWorld, TerrainPatchwork terrain) {
            this.doodadWorld = doodadWorld;
            this.terrain = terrain;
        }
    }

    public static World generate(Camera camera, int size, long seed, DoodadLibrary doodadLibrary){
        final Random random = new RandomXS128(seed);
        final int patchSize = 256;
        final int patchTextureSize = 2048;
        final int patchTextureRatio = patchTextureSize/patchSize;

        final DoodadWorld doodadWorld = new DoodadWorld(camera);
        final Pixmap[][] terrainTextures = new Pixmap[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Pixmap pixmap = new Pixmap(patchTextureSize, patchTextureSize, Pixmap.Format.RGB888);
                pixmap.setColor(0.1f,0.95f, 0.1f, 1.0f);
                pixmap.fill();
                for (int i = 0; i < 5000; i++) {
                    int featX = random.nextInt(patchTextureSize);
                    int featY = random.nextInt(patchTextureSize);
                    pixmap.setColor(random.nextInt() & 0xFFFFFF);
                    pixmap.drawCircle(featX,featY,8);
                    Matrix4 doodadPosition = doodadWorld.addDoodad(doodadLibrary.DOODADS[random.nextInt(doodadLibrary.DOODADS.length)]);
                    doodadPosition.setToTranslation(x * size + featX/patchTextureRatio, y * size + featY/patchTextureRatio, 0f);//TODO Correct height
                }
                terrainTextures[x][y] = pixmap;
            }
        }

        final TerrainPatchwork terrainPatchwork = new TerrainPatchwork(size, patchSize, camera, (x,y) -> new Texture(terrainTextures[x][y], false));

        return new World(doodadWorld, terrainPatchwork);
    }
}
