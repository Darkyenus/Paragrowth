package com.darkyen.paragrowth.doodad;

import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Interpolation;
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

        final String tag;
        final float length;
        final Vector3 end = new Vector3();
        final Vector3 direction = new Vector3();
        final float endWidth;
        final Array<TrunkInstance> trunkChildren = new Array<>(false, 8, TrunkInstance.class);
        final Array<LeafInstance> leafChildren = new Array<>(false, 8, LeafInstance.class);

        TrunkInstance(String tag, float length, float endWidth) {
            this.tag = tag;
            this.length = length;
            this.endWidth = endWidth;
        }
    }

    interface LeafInstance {
        void build(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, TrunkInstance trunk, Random random, WorldCharacteristics characteristics);
    }

    static class HullLeafInstance implements LeafInstance {
        final String tag;
        final int sides;
        final int ringsPre;
        final int ringsPost;
        final Vector3 end = new Vector3();
        final float widest;
        final float width;
        final float color;

        HullLeafInstance(String tag, int sides, int ringsPre, int ringsPost, float widest, float width, float color) {
            this.tag = tag;
            this.sides = sides;
            this.ringsPre = ringsPre;
            this.ringsPost = ringsPost;
            this.widest = widest;
            this.width = width;
            this.color = color;
        }

        private float widthAt(float progress) {
            if (progress <= 0f || progress >= 1f) {
                return progress * width;
            }
            if (progress < widest) {
                return Interpolation.circleOut.apply(progress / widest) * width;
            } else {
                return Interpolation.circleOut.apply(1f - (progress - widest) / (1f - widest)) * width;
            }
        }

        private static final Vector3 build_pos = new Vector3();

        @Override
        public void build(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, TrunkInstance trunk, Random random, WorldCharacteristics characteristics) {
            // 0 - start cap
            // ringsPre * start rings
            // widest - mid ring
            // ringsPost * end rings
            // 1 - end cap

            final Vector3 pos = HullLeafInstance.build_pos;
            final float stepPercentPre = widest / (ringsPre+1);
            final float stepPercentPost = (1f - widest) / (ringsPost+1);

            final short startCap = createCap(builder, baseVertex, trunk.end, trunk.direction, 0f, random, color, characteristics.coherence);
            short ring = -1;

            float progress = 0f;
            for (int i = 0; i < ringsPre; i++) {
                progress += stepPercentPre;
                final short newRing = createRing(builder, baseVertex, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence);
                if (i == 0) {
                    joinRingCap(builder, newRing, startCap, sides);
                } else {
                    joinRings(builder, ring, newRing, sides);
                }
                ring = newRing;
            }

            // Mid ring
            progress = widest; // It is assumed that it already approximately is there
            final short midRing = createRing(builder, baseVertex, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence);
            if (ringsPre == 0) {
                joinRingCap(builder, midRing, startCap, sides);
            } else {
                joinRings(builder, ring, midRing, sides);
            }
            ring = midRing;

            for (int i = 0; i < ringsPost; i++) {
                progress += stepPercentPost;
                final short newRing = createRing(builder, baseVertex, sides, pos.set(trunk.end).lerp(end, progress), trunk.direction, widthAt(progress), random, color, characteristics.coherence);
                joinRings(builder, ring, newRing, sides);
                ring = newRing;
            }

            final short endCap = createCap(builder, baseVertex, end, trunk.direction, 0f, random, color, characteristics.coherence);
            joinRingCap(builder, ring, endCap, sides);
        }
    }

    private static final Matrix3 createRing_rot = new Matrix3();
    private static short createRing(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, int sides, Vector3 position, Vector3 normal, float radius, Random random, float color, float coherence) {
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

    private static short createCap(MeshBuilder builder, MeshPartBuilder.VertexInfo baseVertex, Vector3 position, Vector3 normal, float radius, Random random, float color, float coherence) {
        ColorKt.fudge(ColorKt.set(baseVertex.color, color), random, coherence, 0.3f);
        baseVertex.position.set(position).mulAdd(normal, radius);

        return builder.vertex(baseVertex);
    }

    private static void joinRings(MeshBuilder builder, short first, short second, int sides) {
        for (int i = 0; i < sides; i++) {
            // TODO Winding? Probably don't care, but maybe we care about provoking vertex...
            final int i1 = i == sides - 1 ? 0 : i + 1;
            builder.index((short)(first + i), (short) (first + i1), (short) (second + i));
            builder.index((short)(first + i1), (short) (second + i1), (short) (second + i));
        }
    }

    private static void joinRingCap(MeshBuilder builder, short ring, short cap, int sides) {
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
        if (trunk.trunkChildren.size == 0) {
            short capBaseRing = baseRing;
            if (trunk.endWidth > Doodad.MIN_WIDTH) {
                // Too wide, lets end with standard ring and cap
                final short endRing = createRing(builder, baseVertex, sides, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
                joinRings(builder, baseRing, endRing, sides);
                capBaseRing = endRing;
            }
            // Create top cap
            final short cap = createCap(builder, baseVertex, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
            joinRingCap(builder, capBaseRing, cap, sides);
        } else {
            final short endRing = createRing(builder, baseVertex, sides, trunk.end, trunk.direction, trunk.endWidth, random, trunkColor, characteristics.coherence);
            joinRings(builder, baseRing, endRing, sides);

            for (TrunkInstance child : trunk.trunkChildren) {
                build(builder, baseVertex, child, trunkColor, endRing, random, characteristics);
            }
        }

        for (LeafInstance leaf : trunk.leafChildren) {
            leaf.build(builder, baseVertex, trunk, random, characteristics);
        }
    }

    public void build(MeshBuilder builder, Random random, WorldCharacteristics characteristics) {
        final MeshPartBuilder.VertexInfo baseVertex = DoodadInstance.build_baseVertex;

        // Create bottom cap
        final short baseCap = createCap(builder, baseVertex, position, root.direction, -rootWidth, random, trunkColor, characteristics.coherence);
        // Create bottom ring
        final short baseRing = createRing(builder, baseVertex, sides, position, root.direction, rootWidth, random, trunkColor, characteristics.coherence);
        joinRingCap(builder, baseRing, baseCap, sides);

        build(builder, baseVertex, root, trunkColor, baseRing, random, characteristics);
    }
}
