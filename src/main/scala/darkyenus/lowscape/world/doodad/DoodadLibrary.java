package darkyenus.lowscape.world.doodad;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * @author Darkyen
 */
public class DoodadLibrary {

    public final Doodad PINE;
    public final Doodad GRASS1;
    public final Doodad GRASS2;
    public final Doodad GRASS3;
    public final Doodad FLOWER1;
    public final Doodad FLOWER2;

    public final Doodad[] DOODADS;

    public DoodadLibrary(DoodadFactory factory) {
        final TextureAtlas ta = factory.worldAtlas;
        final TextureRegion
                pine        = ta.findRegion("pine"),
                grass1      = ta.findRegion("grass1"),
                grass2      = ta.findRegion("grass2"),
                flower1     = ta.findRegion("flower1"),
                flower2     = ta.findRegion("flower2");

        PINE = factory.createPaperModel(0.1f, pine, pine, pine);
        GRASS1 = factory.createPaperModel(0.02f, grass1, grass1, grass1);
        GRASS2 = factory.createPaperModel(0.02f, grass2, grass2, grass2);
        GRASS3 = factory.createPaperModel(0.02f, grass1, grass2, grass1);
        FLOWER1 = factory.createPaperModel(0.02f, flower1, flower1);
        FLOWER2 = factory.createPaperModel(0.02f, flower2, flower2);

        DOODADS = new Doodad[]{PINE, GRASS1, GRASS2, GRASS3, FLOWER1, FLOWER2};
    }



}
