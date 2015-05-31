package darkyenus.lowscape.world;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter;

/**
 * @author Darkyen
 */
public class LowscapeRenderableSorter extends DefaultRenderableSorter {

    public static final Object SKYBOX = new Object();

    @Override
    public int compare(Renderable o1, Renderable o2) {
        if(o1.userData == SKYBOX)return -1;
        else if(o2.userData == SKYBOX)return 1;
        else return super.compare(o1, o2);
    }
}
