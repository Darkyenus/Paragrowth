package com.darkyen.paragrowth.terrain

import com.badlogic.gdx.math.Rectangle

/**
 * Used to query world properties.
 */
interface WorldQuery {
    fun getHeightAt(x:Float, y:Float):Float

    fun getDimensions(): Rectangle
}