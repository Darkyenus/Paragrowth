package com.darkyen.paragrowth.render

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/** Each new instance is globally registered and serves as a key for [Attributes] */
@kotlin.Suppress("unused") // Type T
class AttributeKey<T : Any>(private val name:String, layer:AttributeLayer, internal val new:()->T, internal val clear:(T) -> T?) {

    val id = layer.register(this)

    override fun toString(): String = "$id:$name"
}

class AttributeLayer {
    private var nextId = 0
    private val registeredKeys = arrayOfNulls<AttributeKey<*>>(64)

    internal fun register(key:AttributeKey<*>):Int {
        val id = nextId++
        registeredKeys[id] = key
        return id
    }

    internal fun assertRegistered(key:AttributeKey<*>) {
        assert(registeredKeys[key.id] === key)
    }

    internal val keys:Array<AttributeKey<*>?>
        get() = registeredKeys

    internal val size:Int
        get() = nextId
}

val GlobalAttributeLayer = AttributeLayer()
val ModelAttributeLayer = AttributeLayer()

fun attributeKeyVector2(name:String, layer:AttributeLayer):AttributeKey<Vector2> {
    return AttributeKey(name, layer, { Vector2() }) { it.setZero() }
}

fun attributeKeyVector3(name:String, layer:AttributeLayer):AttributeKey<Vector3> {
    return AttributeKey(name, layer, { Vector3() }) { it.setZero() }
}

fun attributeKeyFloat(name:String, layer:AttributeLayer):AttributeKey<FloatArray> {
    return AttributeKey(name, layer, { FloatArray(1) }) { it.apply { it[0] = 0f } }
}

fun attributeKeyMatrix4(name:String, layer:AttributeLayer):AttributeKey<Matrix4> {
    return AttributeKey(name, layer, { Matrix4() }, { it.idt() })
}

private val NO_VALUES:Array<Any?> = emptyArray()

/** Key-value map for [AttributeKey] keys and [AttributeKey].T values.
 * Can be layered, where keys are retrieved from the whole stack (with top having a priority)
 * and values are set only to current instance. */
class Attributes(private val layer:AttributeLayer) {

    private var values:Array<Any?> = NO_VALUES

    operator fun <T : Any> get(key:AttributeKey<T>):T {
        layer.assertRegistered(key)

        var values = values
        if (key.id >= values.size) {
            values = values.copyOf(layer.size)
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
        layer.assertRegistered(key)

        var values = values
        if (key.id >= values.size) {
            values = values.copyOf(layer.size)
            this.values = values
        }
        values[key.id] = value
    }

    /** Clear this container's attributes. */
    fun clear() {
        val values = values
        val keys = layer.keys
        for (i in values.indices) {
            val value = values[i]
            if (value != null) {
                @Suppress("UNCHECKED_CAST")
                values[i] = (keys[i] as AttributeKey<Any>).clear(value)
            }
        }
    }
}