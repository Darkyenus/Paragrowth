package com.darkyen.paragrowth.render

import com.badlogic.gdx.math.Vector2
import com.darkyen.paragrowth.util.GdxArray

/** Each new instance is globally registered and serves as a key for [Attributes] */
@kotlin.Suppress("unused") // Type T
class AttributeKey<T : Any>(private val name:String, internal val new:()->T, internal val clear:(T) -> T?) {

    val id = nextId++
    init {
        REGISTERED_KEYS.add(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeKey<*>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "$id:$name"
    }
}

fun attributeKeyVector2(name:String):AttributeKey<Vector2> {
    return AttributeKey(name, { Vector2() }) { it.setZero() }
}

fun attributeKeyFloat(name:String):AttributeKey<FloatArray> {
    return AttributeKey(name, { FloatArray(1) }) { it.apply { it[0] = 0f } }
}

private var nextId:Int = 0
private val REGISTERED_KEYS = GdxArray<AttributeKey<*>>(AttributeKey::class.java)
private val NO_VALUES:Array<Any?> = emptyArray()

/** Key-value map for [AttributeKey] keys and [AttributeKey].T values.
 * Can be layered, where keys are retrieved from the whole stack (with top having a priority)
 * and values are set only to current instance. */
class Attributes(private val parent:Attributes?) {

    private var values:Array<Any?> = NO_VALUES

    operator fun <T : Any> get(key:AttributeKey<T>):T {
        var values = values
        if (key.id >= values.size) {
            values = values.copyOf(nextId)
            this.values = values
        }

        var value = values[key.id]
        if (value == null) {
            value = key.new()
            values[key.id] = value
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    operator fun <T : Any> set(key:AttributeKey<T>, value:T) {
        var values = values
        if (key.id >= values.size) {
            values = values.copyOf(nextId)
            this.values = values
        }
        values[key.id] = value
    }

    /** Clear this container for attributes.
     * The parent, if any, is not modified. */
    fun clear() {
        val values = values
        for (i in values.indices) {
            val value = values[i]
            if (value != null) {
                @Suppress("UNCHECKED_CAST")
                values[i] = (REGISTERED_KEYS.items[i] as AttributeKey<Any>).clear(value)
            }
        }
    }
}