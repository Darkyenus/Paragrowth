package com.darkyen.paragrowth.render

/** Each new instance is globally registered and serves as a key for [Attributes] */
@kotlin.Suppress("unused") // Type T
class AttributeKey<T>(private val name:String) {

    val id = nextId++

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

private var nextId:Int = 0
private val NO_VALUES:Array<Any?> = emptyArray()

/** Key-value map for [AttributeKey] keys and [AttributeKey].T values.
 * Can be layered, where keys are retrieved from the whole stack (with top having a priority)
 * and values are set only to current instance. */
class Attributes(private val parent:Attributes?) {

    private var values:Array<Any?> = NO_VALUES

    operator fun <T> get(key:AttributeKey<T>):T? {
        @Suppress("UNCHECKED_CAST")
        if (key.id < values.size) {
            return values[key.id] as T?
        }
        return parent?.get(key)
    }

    operator fun <T> set(key:AttributeKey<T>, value:T) {
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
        values.fill(null)
    }
}