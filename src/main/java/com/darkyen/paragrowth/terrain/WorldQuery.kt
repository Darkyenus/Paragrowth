package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

/**
 * Used to query world properties.
 */
interface WorldQuery {
    fun getHeightAt(x:Float, y:Float):Float

    fun getDimensions(): Rectangle

    fun adjustPointToHeightRange(point: Vector2, minHeight:Float, maxHeight:Float):Boolean
}