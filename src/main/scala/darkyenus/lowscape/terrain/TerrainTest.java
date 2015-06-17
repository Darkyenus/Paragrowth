package darkyenus.lowscape.terrain;

import com.badlogic.gdx.math.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Darkyen
 */
public class TerrainTest {

    private static void updateTerrain(Terrain terrain){
        terrain.settleStillWater();
        terrain.generateDownfall();
    }

    private static void initializeTerrain(Terrain terrain){
        terrain.wind.set(5f,1f);
        terrain.foreach((x,y) -> {
            float fX = ((float)x) / terrain.size * 360f;
            float fY = ((float)y) / terrain.size * 360f;
            terrain.temperature[x][y] = MathUtils.cosDeg(fY) + 0.3f;
            terrain.height[x][y] = -MathUtils.cosDeg(fX) * -MathUtils.cosDeg(fY) + 2f;
        });
    }

    private final static int WINDOW_SIZE = 800;
    public static void main(String[] args){
        final Terrain terrain = new Terrain(1024, 0.5f, 1.0f, 0.8f);
        initializeTerrain(terrain);

        JFrame frame = new JFrame("Terrain Test");
        Canvas canvas = new Canvas(){
            BufferedImage doubleBuffer = null;

            @Override
            public void paint(Graphics g) {
                updateTerrain(terrain);
                if(doubleBuffer != null){
                    g.drawImage(doubleBuffer, 0, 0, WINDOW_SIZE, WINDOW_SIZE, null);
                }
                doubleBuffer = TerrainIO.toImage(terrain);
                repaint();
            }
        };

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_SIZE,WINDOW_SIZE);
        canvas.setSize(WINDOW_SIZE,WINDOW_SIZE);
        frame.setResizable(false);
        frame.add(canvas);

        frame.setVisible(true);
    }
}
