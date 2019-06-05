package com.darkyen.paragrowth.animal

import com.darkyen.paragrowth.util.GdxArray
import com.darkyen.paragrowth.util.GdxFloatArray
import com.darkyen.paragrowth.util.GdxIntArray

/**
 *
 */
class BehaviorTree (template:BehaviorTreeTemplate) {

    constructor(template:BehaviorTreeTemplate, vararg storage:Any):this(template) {
        for ((i, any) in storage.withIndex()) {
            this.storage[i] = any
        }
    }

    val storage = template.keyDefaults.copyOf().apply {
        for (i in this.indices) {
            val v = this[i]
            if (v is DefaultCreator) {
                this[i] = v.create()
            }
        }
    }
    val intStorage = template.intKeyDefaults.copyOf()
    val floatStorage = template.floatKeyDefaults.copyOf()
    private val rootNode = template.rootNode

    operator fun <V> Key<V>.invoke():V {
        @Suppress("UNCHECKED_CAST")
        return storage[this@invoke.id] as V
    }

    operator fun <V> Key<V>.invoke(value:V) {
        storage[this@invoke.id] = value
    }

    operator fun IntKey.invoke():Int {
        return intStorage[id]
    }

    operator fun IntKey.invoke(newValue:Int) {
        intStorage[id] = newValue
    }

    operator fun FloatKey.invoke():Float {
        return floatStorage[id]
    }

    operator fun FloatKey.invoke(newValue:Float) {
        floatStorage[id] = newValue
    }

    fun BehaviorNode.act():Boolean? {
        return this.behavior(this@BehaviorTree, children)
    }

    fun act():Boolean? {
        return rootNode.act()
    }
}

typealias Behavior = BehaviorTree.(children:Array<BehaviorNode>) -> Boolean?
typealias BehaviorOne = BehaviorTree.(child:BehaviorNode) -> Boolean?
typealias BehaviorNone = BehaviorTree.() -> Boolean?

class BehaviorNode internal constructor(internal val behavior:Behavior, internal val children:Array<BehaviorNode>)

private val NO_CHILDREN = emptyArray<BehaviorNode>()

class Key<V>(internal val id:Int)
class IntKey(internal val id:Int)
class FloatKey(internal val id:Int)

private class DefaultCreator(val create:() -> Any)

class BehaviorTreeTemplate internal constructor(
        internal val keyDefaults:Array<Any?>,
        internal val intKeyDefaults:IntArray,
        internal val floatKeyDefaults:FloatArray,
        internal val rootNode:BehaviorNode)

class BehaviorBuilder internal constructor() {

    private val keyDefaults = GdxArray<Any?>()
    private val intKeyDefaults = GdxIntArray()
    private val floatKeyDefaults = GdxFloatArray()

    private val beginStack = GdxArray<Behavior>()
    private val doneStack = GdxArray<BehaviorNode>(BehaviorNode::class.java)
    private val doneStackIndices = GdxIntArray()

    fun <V>register(default:V? = null):Key<V> {
        val id = keyDefaults.size
        keyDefaults.add(default)
        return Key(id)
    }

    fun <V:Any>register(default:() -> V):Key<V> {
        val id = keyDefaults.size
        keyDefaults.add(DefaultCreator(default))
        return Key(id)
    }

    fun register(default:Int):IntKey {
        val id = intKeyDefaults.size
        intKeyDefaults.add(default)
        return IntKey(id)
    }

    fun register(default:Float):FloatKey {
        val id = floatKeyDefaults.size
        floatKeyDefaults.add(default)
        return FloatKey(id)
    }

    @PublishedApi
    internal fun begin(act:Behavior) {
        beginStack.add(act)
        doneStackIndices.add(doneStack.size)
    }

    @PublishedApi
    internal fun end():Int {
        val children = doneStack.run {
            val doneFrom = doneStackIndices.pop()
            if (doneFrom < size) {
                val c = items.copyOfRange(doneFrom, size)
                size = doneFrom
                c
            } else {
                assert(doneFrom == size)
                NO_CHILDREN
            }
        }

        doneStack.add(BehaviorNode(beginStack.pop(), children))
        return children.size
    }

    inline fun none(crossinline behavior:BehaviorNone) {
        begin { behavior.invoke(this) }
        end()
    }

    inline fun one(noinline behavior:BehaviorOne):BehaviorOne {
        return behavior
    }

    inline fun many(noinline behavior:Behavior):Behavior {
        return behavior
    }

    @JvmName("invokeOne")
    inline operator fun BehaviorOne.invoke(build:BehaviorBuilder.() -> Unit) {
        begin { children -> this@invoke.invoke(this, children[0]) }
        build(this@BehaviorBuilder)
        if (end() != 1) {
            throw IllegalArgumentException("Behavior ${this@invoke} can only have one argument")
        }
    }

    @JvmName("invokeMany")
    inline operator fun Behavior.invoke(build:BehaviorBuilder.() -> Unit) {
        begin(this@invoke)
        build(this@BehaviorBuilder)
        end()
    }

    internal fun buildTemplate():BehaviorTreeTemplate {
        assert(doneStackIndices.size == 0 && beginStack.size == 0)
        val doneStack = doneStack
        val rootNode = when (doneStack.size) {
            0 -> BehaviorNode({ true }, NO_CHILDREN)
            1 -> doneStack[0]
            else -> BehaviorNode({ true }, NO_CHILDREN)
        }

        if (doneStack.size == 1) {
            doneStack[0]
        } else {

            doneStack[0]
        }
        return BehaviorTreeTemplate(keyDefaults.toArray(), intKeyDefaults.toArray(), floatKeyDefaults.toArray(), rootNode)
    }
}

fun behaviorTree(build:BehaviorBuilder.() -> Unit):BehaviorTreeTemplate {
    val builder = BehaviorBuilder()
    builder.build()
    return builder.buildTemplate()
}

fun BehaviorBuilder.alwaysTrue() = none { true }

fun BehaviorBuilder.invert() = one { child ->
    when (child.act()) {
        true -> false
        false -> true
        null -> null
    }
}

fun BehaviorBuilder.enterIf(elseB:Boolean = false, condition:BehaviorTree.() -> Boolean):BehaviorOne {
    // 1 = entered, 0 = not entered
    val state = register(0)
    return one behavior@{ child ->
        if (state() == 0 && !condition()) {
            // Do not enter, fail immediately
            return@behavior elseB
        }
        // Already entered or should enter
        state(1)
        val result = child.act() ?: return@behavior null
        state(0)
        result
    }
}

enum class Sequence {
    AND,
    OR
}

fun BehaviorBuilder.parallel(type:Sequence):Behavior {
    val doneKey = register(0)
    return many behavior@{ children ->
        assert(children.size <= 32)
        var childrenBitmask = doneKey()

        children@for ((index, child) in children.withIndex()) {
            val mask = 1 shl index
            if (childrenBitmask and mask != 0) {
                continue
            }
            val result = child.act() ?: continue
            if ((type == Sequence.AND && result) || (type == Sequence.OR && !result)) {
                childrenBitmask = childrenBitmask or mask
            } else {
                // cleanup
                doneKey(0)
                return@behavior result
            }
        }

        if (Integer.bitCount(childrenBitmask) == children.size) {
            // Done completely
            doneKey(0)
            type == Sequence.AND
        } else {
            null
        }
    }
}

fun BehaviorBuilder.sequence(type:Sequence):Behavior {
    val nextKey = register(0)
    return many behavior@{ children: Array<BehaviorNode> ->
        var next = nextKey()
        while (true) {
            if (next >= children.size) {
                // Done, reset, return
                nextKey(0)
                return@behavior type == Sequence.AND
            }

            val result = children[next].act() ?: break
            if ((type == Sequence.AND) == result) {
                // Keep on trying
                next++
            } else {
                // Done, reset, return
                nextKey(0)
                return@behavior result
            }
        }

        nextKey(next)
        null
    }
}

/** Like [sequence], but always evaluates all possibilities. */
fun BehaviorBuilder.hotSequence(type:Sequence) = many behavior@{ children ->
    for (child in children) {
        when (child.act()) {
            null -> return@behavior null
            true ->
                if (type == Sequence.OR) {
                    return@behavior true
                }
            false ->
                if (type == Sequence.AND) {
                    return@behavior false
                }
        }
    }
    return@behavior type == Sequence.AND
}

fun BehaviorBuilder.repeatUntil(until:Boolean?) = one { child ->
    val result = child.act()
    if (result == null || until == null) {
        null
    } else if (result == until) {
        true
    } else {
        null
    }
}
