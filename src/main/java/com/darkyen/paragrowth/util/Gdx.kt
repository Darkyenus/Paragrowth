package com.darkyen.paragrowth.util

/**
 * Type-aliases to simplify usage of Gdx collections, whose names conflict with Kotlin classes.
 */
typealias GdxArray<T> = com.badlogic.gdx.utils.Array<T>
typealias GdxBoolArray = com.badlogic.gdx.utils.BooleanArray
typealias GdxCharArray = com.badlogic.gdx.utils.CharArray
typealias GdxShortArray = com.badlogic.gdx.utils.ShortArray
typealias GdxFloatArray = com.badlogic.gdx.utils.FloatArray
typealias GdxIntArray = com.badlogic.gdx.utils.IntArray
typealias GdxLongArray = com.badlogic.gdx.utils.LongArray

@Suppress("UNCHECKED_CAST")
inline fun <reified T> arrayOfSize(size: Int): Array<T> = arrayOfNulls<T>(size) as Array<T>

inline fun <T> GdxArray<T>.each(op:(T)->Unit) {
    for (i in 0 until size) {
        op(get(i))
    }
}