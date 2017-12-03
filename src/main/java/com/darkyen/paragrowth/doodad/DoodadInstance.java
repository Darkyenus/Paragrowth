package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.darkyen.paragrowth.util.VectorUtils;

/**
 *
 */
class DoodadInstance {

    final Doodad template;

    final float rootWidth;
    final int sides;
    final Vector3 position = new Vector3();
    TrunkInstance root;

    DoodadInstance(Doodad template, float rootWidth, int sides) {
        this.template = template;
        this.rootWidth = Math.max(rootWidth, 0.1f);
        this.sides = Math.max(sides, 2);
    }

    static class TrunkInstance {

        final Vector3 end = new Vector3();
        final Vector3 direction = new Vector3();
        final float endWidth;
        final Array<TrunkInstance> children = new Array<>(false, 8, TrunkInstance.class);

        TrunkInstance(float endWidth) {
            this.endWidth = endWidth;
        }
    }

    private static final Matrix3 createRing_rot = new Matrix3();
    private short createRing(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, Vector3 position, Vector3 normal, float radius) {
        final Vector3 tangent = VectorUtils.generateTangent(normal).scl(radius);
        final Matrix3 rot = createRing_rot.setToRotation(normal, 360f / sides);

        baseVertex.color.r = MathUtils.random();
        baseVertex.color.g = MathUtils.random();
        baseVertex.color.b = MathUtils.random();

        baseVertex.position.set(position).add(tangent);
        final short resultIndex = builder.vertex(baseVertex);
        for (int i = 1; i < sides; i++) {
            tangent.mul(rot);

            baseVertex.color.r = MathUtils.random();
            baseVertex.color.g = MathUtils.random();
            baseVertex.color.b = MathUtils.random();
            baseVertex.position.set(position).add(tangent);
            final short v = builder.vertex(baseVertex);
            assert v == resultIndex + i;
        }

        return resultIndex;
    }

    private void joinRings(MeshBuilder builder, short first, short second) {
        for (int i = 0; i < sides; i++) {
            // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
            final int i1 = i == sides - 1 ? 0 : i + 1;
            builder.index((short)(first + i), (short) (first + i1), (short) (second + i));
            builder.index((short)(first + i1), (short) (second + i1), (short) (second + i));
        }
    }

    private static final MeshPartBuilder.VertexInfo build_baseVertex = new MeshPartBuilder.VertexInfo();
    static {
        final MeshPartBuilder.VertexInfo baseVertex = DoodadInstance.build_baseVertex;
        baseVertex.hasPosition = true;
        baseVertex.hasColor = true;
        baseVertex.hasNormal = false;
        baseVertex.hasUV = false;
        baseVertex.color.a = 1f;
    }

    private void build(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, TrunkInstance trunk, short baseRing) {
        final short endRing = createRing(builder, baseVertex, trunk.end, trunk.direction, trunk.endWidth);
        joinRings(builder, baseRing, endRing);

        if (trunk.children.size == 0) {
            // Create top cap
            //TODO
        } else {
            for (TrunkInstance child : trunk.children) {
                build(builder, baseVertex, child, endRing);
            }
        }

    }

    public void build(MeshBuilder builder) {
        final MeshPartBuilder.VertexInfo baseVertex = DoodadInstance.build_baseVertex;

        //Create bottom cap
        //TODO

        final short baseRing = createRing(builder, baseVertex, position, root.direction, rootWidth);
        build(builder, baseVertex, root, baseRing);
    }
}
