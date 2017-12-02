package com.darkyen.paragrowth.util;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

/**
 * For shaders, used for sorting.
 */
public interface PrioritizedShader {

    int LAST = Integer.MAX_VALUE;
    int SKYBOX = -1000;
    int TERRAIN = -100;
    int DOODADS = -10;
    int DEFAULT = 0;
    int FIRST = Integer.MIN_VALUE;

    /**
     * @return small value to be drawn first, large value to be drawn later
     */
    int priority();

    PrioritizedSorter SORTER = new PrioritizedSorter();

    class PrioritizedSorter implements RenderableSorter, Comparator<Renderable> {

        private PrioritizedSorter() {
        }

        private int weight(Shader shader) {
            if (shader instanceof PrioritizedShader) {
                return ((PrioritizedShader) shader).priority();
            }
            return PrioritizedShader.DEFAULT;
        }

        @Override
        public int compare(Renderable o1, Renderable o2) {
            final int weight1 = weight(o1.shader);
            final int weight2 = weight(o2.shader);
            if (weight1 < weight2) {
                return -1;
            } else if (weight1 > weight2) {
                return 1;
            }

            final Vector3 pos1 = getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1);
            final Vector3 pos2 = getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2);
            final float dst = (int)(1000f * camera.position.dst2(pos1)) - (int)(1000f * camera.position.dst2(pos2));
            return dst < 0 ? -1 : (dst > 0 ? 1 : 0);
        }

        private Camera camera;
        private final Vector3 tmpV1 = new Vector3();
        private final Vector3 tmpV2 = new Vector3();

        private Vector3 getTranslation (Matrix4 worldTransform, Vector3 center, Vector3 output) {
            if (center.isZero())
                worldTransform.getTranslation(output);
            else if (!worldTransform.hasRotationOrScaling())
                worldTransform.getTranslation(output).add(center);
            else
                output.set(center).mul(worldTransform);
            return output;
        }

        @Override
        public void sort(Camera camera, Array<Renderable> renderables) {
            this.camera = camera;
            renderables.sort(this);
        }
    }
}
