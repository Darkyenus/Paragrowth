package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.darkyen.paragrowth.WorldCharacteristics;
import com.darkyen.paragrowth.util.ColorKt;
import com.darkyen.paragrowth.util.VectorUtils;

import java.util.Random;

/**
 * Warning: Mess inside.
 */
class DoodadInstance {

    final Doodad template;

    final float rootWidth;
    final int sides;
    final Vector3 position = new Vector3();
    private final float trunkColor;
    TrunkInstance root;

    DoodadInstance(Doodad template, float rootWidth, int sides, float trunkColor) {
        this.template = template;
        this.rootWidth = Math.max(rootWidth, 0.1f);
        this.sides = Math.max(sides, 2);
        this.trunkColor = trunkColor;
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
    private short createRing(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, Vector3 position, Vector3 normal, float radius, Random random, float color, float coherence) {
        final Vector3 tangent = VectorUtils.generateTangent(normal).scl(radius);
        final Matrix3 rot = createRing_rot.setToRotation(normal, 360f / sides);

        ColorKt.fudge(ColorKt.set(baseVertex.color, color), random, coherence, 0.3f);

        baseVertex.position.set(position).add(tangent);
        final short resultIndex = builder.vertex(baseVertex);
        for (int i = 1; i < sides; i++) {
            tangent.mul(rot);

            ColorKt.fudge(ColorKt.set(baseVertex.color, color), random, coherence, 0.3f);
            baseVertex.position.set(position).add(tangent);
            final short v = builder.vertex(baseVertex);
            assert v == resultIndex + i;
        }

        return resultIndex;
    }

    private short createCap(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, Vector3 position, Vector3 normal, float radius, Random random, float color, float coherence) {
        ColorKt.fudge(ColorKt.set(baseVertex.color, color), random, coherence, 0.3f);
        baseVertex.position.set(position).mulAdd(normal, radius);

        return builder.vertex(baseVertex);
    }

    private void joinRings(MeshBuilder builder, short first, short second) {
        for (int i = 0; i < sides; i++) {
            // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
            final int i1 = i == sides - 1 ? 0 : i + 1;
            builder.index((short)(first + i), (short) (first + i1), (short) (second + i));
            builder.index((short)(first + i1), (short) (second + i1), (short) (second + i));
        }
    }

    private void joinRingCap(MeshBuilder builder, short ring, short cap) {
        for (int i = 0; i < sides; i++) {
            // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
            final int i1 = i == sides - 1 ? 0 : i + 1;
            builder.index((short)(ring + i), (short) (ring + i1), cap);
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

    private void build(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, TrunkInstance trunk, float trunkColor, short baseRing, Random random, WorldCharacteristics characteristics) {
        if (trunk.children.size == 0) {
            short capBaseRing = baseRing;
            if (trunk.endWidth > Doodad.MIN_WIDTH) {
                // Too wide, lets end with standard ring and cap
                final short endRing = createRing(builder, baseVertex, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
                joinRings(builder, baseRing, endRing);
                capBaseRing = endRing;
            }
            // Create top cap
            final short cap = createCap(builder, baseVertex, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
            joinRingCap(builder, capBaseRing, cap);
        } else {
            final short endRing = createRing(builder, baseVertex, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
            joinRings(builder, baseRing, endRing);

            for (TrunkInstance child : trunk.children) {
                build(builder, baseVertex, child, trunkColor, endRing, random, characteristics);
            }
        }

    }

    public void build(MeshBuilder builder, Random random, WorldCharacteristics characteristics) {
        final MeshPartBuilder.VertexInfo baseVertex = DoodadInstance.build_baseVertex;

        // Create bottom cap
        final short baseCap = createCap(builder, baseVertex, position, root.direction, -rootWidth, random, trunkColor, characteristics.coherence);
        // Create bottom ring
        final short baseRing = createRing(builder, baseVertex, position, root.direction, rootWidth, random, trunkColor, characteristics.coherence);
        joinRingCap(builder, baseRing, baseCap);

        build(builder, baseVertex, root, trunkColor, baseRing, random, characteristics);
    }
}
