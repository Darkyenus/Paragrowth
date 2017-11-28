package com.darkyen.paragrowth.terrain;

import com.badlogic.gdx.math.Vector2;

/**
 * @author Darkyen
 */
public final class Terrain {
    public final int size;

    //Represents the height at this point
    public final float[][] height;
    public final float defaultHeight;
    //Represents the still water level at this point
    // when water < height, water = 0
    public final float[][] water;
    public final float defaultWater;
    //Represents the flowing water level at this point (see note at water)
    public final float[][] river;
    //Represents the amount of downfall at this point
    public final float[][] downfall;
    //Represents the temperature at this point
    public final float[][] temperature;
    public final float defaultTemperature;

    private final float defaultDownfall;

    public final Vector2 wind = new Vector2();

    public Terrain(int size, float initialHeight, float initialWaterLevel, float initialTemperature) {
        this.size = size;
        height = new float[size][size];
        water = new float[size][size];
        river = new float[size][size];
        downfall = new float[size][size];
        temperature = new float[size][size];
        defaultHeight = initialHeight;
        defaultWater = initialWaterLevel;
        defaultTemperature = initialTemperature;

        foreach((x,y) -> setHeight(x,y, initialHeight));
        foreach((x,y) -> setTemperature(x,y, initialTemperature));
        defaultDownfall = Math.max(0f, defaultWater - defaultHeight) * Math.max(0f, defaultTemperature);
    }

    // Utility utility methods
    @FunctionalInterface
    public interface TerrainLambda {
        void apply(int x, int y);
    }

    protected float get(float[][] fromData, int x, int y, float defaultResult){
        if(x < 0 || y < 0 || x >= size || y >= size) return defaultResult;
        else return fromData[x][y];
    }

    protected void set(float[][] data, int x, int y, float value){
        if (x >= 0 && y >= 0 && x < size && y < size) {
            data[x][y] = value;
        }
    }

    public void foreach(TerrainLambda function){
        final int size = this.size;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                function.apply(x,y);
            }
        }
    }

    // Utility methods
    // Those are just accessors, don't do any logic.

    public float getHeight(int x, int y){
        return get(height, x, y, defaultHeight);
    }

    public float getWater(int x, int y){
        return get(water, x, y, defaultWater);
    }

    public float getRiver(int x, int y){
        return get(river, x, y, 0f);//0 - no rivers flowing from outside
    }

    public float getDownfall(int x, int y){
        return get(downfall, x, y, defaultDownfall);
    }

    public float getTemperature(int x, int y){
        return get(temperature, x, y, defaultTemperature);
    }

    public void setHeight(int x, int y, float height){
        set(this.height, x, y, height);
    }

    public void setWater(int x, int y, float water){
        set(this.water, x, y, water);
    }

    public void setRiver(int x, int y, float river){
        set(this.river, x, y, river);
    }

    public void setDownfall(int x, int y, float downfall){
        set(this.downfall, x, y, downfall);
    }

    public void setTemperature(int x, int y, float temperature){
        set(this.temperature, x, y, temperature);
    }

    protected float[][] createBufferArray(){
        return new float[size][size];
    }

    protected void copy(float[][] from, float[][] to){
        final int size = this.size;
        for (int x = 0; x < size; x++) {
            System.arraycopy(from[x], 0, to[x], 0, size);
        }
    }

    // Logic-using getters

    public Biome getBiome(int x, int y){
        final float height = getHeight(x,y);
        final float water = getWater(x,y);
        final float river = getRiver(x,y);
        final float temperature = getTemperature(x,y);
        final float downfall = getDownfall(x,y);

        if (water > height){
            return Biome.SEA;
        }else if(river > 0f){
            return Biome.RIVER;
        }else if(downfall > 1f){
            return Biome.GRASSLAND;
        }else if(temperature > 0.5f){
            return Biome.DESERT;
        }else{
            return Biome.TUNDRA;
        }
    }

    // Calculation methods

    public void settleStillWater(){
        //Reset all water
        foreach((x,y) -> setWater(x,y,0f));

        //Flood fill with water from edges
        //(Array to fool the lambda)
        final boolean[] active = new boolean[]{false};
        do {
            active[0] = false;
            foreach((x,y) -> {
                final float myWater = getWater(x,y);
                float theirWater;
                if((theirWater = getWater(x-1,y)) > myWater){
                    setWater(x,y, theirWater);
                    active[0] = true;
                }else if((theirWater = getWater(x+1,y)) > myWater){
                    setWater(x,y, theirWater);
                    active[0] = true;
                }else if((theirWater = getWater(x,y-1)) > myWater){
                    setWater(x,y, theirWater);
                    active[0] = true;
                }else if((theirWater = getWater(x,y+1)) > myWater){
                    setWater(x,y, theirWater);
                    active[0] = true;
                }
            });
        }while(active[0]);
    }

    /**
     * Regenerates downfall, from the values of water level.
     * settleStillWater first though.
     */
    public void generateDownfall(){
        //Generate base downfall
        foreach((x,y) -> setDownfall(x,y, Math.max(0f, getWater(x, y) - getHeight(x, y)) * Math.max(0f, getTemperature(x,y))));
        //Move the downfall using wind
        final float[][] movedDownfall = createBufferArray();
        final float windMagnitude = wind.len();
        final int windX = (int)(wind.x / windMagnitude + 0.5f);
        final int windY = (int)(wind.y / windMagnitude + 0.5f);
        final float cloudLayer = 4f; //4km
        foreach((x,y) -> {
            int destX = x, destY = y;
            for (int mag = 0; mag < windMagnitude; mag++) {
                if(getHeight(destX + windX, destY + windY) > cloudLayer){
                    break;
                }
                destX += windX;
                destY += windY;
            }
            set(movedDownfall, destX, destY, getDownfall(x,y));
        });
        copy(movedDownfall, downfall);
    }

    public void generateRivers(){
        //Generate uniform layer of river water based on downfall
        copy(downfall, river);
        //Add each river layer to nearest lower neighbor if layer too small
        //TODO
    }
}
