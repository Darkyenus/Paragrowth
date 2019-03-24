@file:JvmName("MeshBuilding")
package com.darkyen.paragrowth.render

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.math.Vector3
import com.darkyen.paragrowth.util.*

/**
 * Shared Mesh Builder instance.
 */
@JvmField
val MESH_BUILDER = MeshBuilder()

/** 3D Position & color attributes  */
@JvmField
val POSITION3_COLOR1_ATTRIBUTES = VertexAttributes(
        VertexAttribute.Position(), //3
        VertexAttribute.ColorPacked()//1
)

@JvmField
val POSITION3_COLOR1_NORMAL3_ATTRIBUTES = VertexAttributes(
        VertexAttribute.Position(), //3
        VertexAttribute.ColorPacked(),//1
        VertexAttribute.Normal()//3
)

@JvmField
val TERRAIN_PATCH_ATTRIBUTES = POSITION3_COLOR1_NORMAL3_ATTRIBUTES

@JvmField
val POSITION3_ATTRIBUTES = VertexAttributes(
        VertexAttribute.Position()
)

inline fun buildMesh(attributes:VertexAttributes, build:MeshBuilder.() -> Unit): Mesh {
    val builder = MESH_BUILDER
    builder.begin(attributes)
    builder.build()
    return builder.end()
}


private val tmpVec3 = Vector3()
private val tmpGdxCol = GdxColor()

fun MeshBuilder.vertex(x:Float, y:Float, z:Float, color: Color):Short {
    return vertex(tmpVec3.set(x, y, z), null, tmpGdxCol.set(color.red, color.green, color.blue, color.alpha), null)
}