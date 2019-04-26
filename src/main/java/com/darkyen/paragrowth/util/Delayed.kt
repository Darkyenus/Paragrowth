package com.darkyen.paragrowth.util

import com.darkyen.paragrowth.ParagrowthMain
import java.util.concurrent.Callable

/**
 * Value, which is not immediately available, for some reason, but it can be polled for.
 */
interface Delayed<T : Any> {

    /** Get the delayed item, or null if not done yet. */
    fun poll():T?

    /** Get the delayed item now, even if that means blocking. */
    fun get():T
}

/** Produce [Delayed], which will perform [op] on the result of this [Delayed]. */
inline fun <T:Any, T2:Any> Delayed<T>.then(crossinline op:(T)->Delayed<T2>):Delayed<T2> {
    return object : Delayed<T2> {

        var then:Delayed<T2>? = null

        override fun poll(): T2? {
            if (then == null) {
                val first = this@then.poll() ?: return null
                then = op(first)
            }
            return then?.poll()
        }

        override fun get(): T2 {
            val then = this.then ?: run {
                val first = this@then.get()
                val newThen = op(first)
                this.then = newThen
                newThen
            }

            return then.get()
        }
    }
}

/** After this computation completes, map its result with [op].
 * [op] is guaranteed to be called only once. */
inline fun <T:Any, T2:Any> Delayed<T>.map(crossinline op:(T) -> T2):Delayed<T2> {
    return object : Delayed<T2> {

        var result:T2? = null

        override fun poll(): T2? {
            var result = result
            if (result != null) {
                return result
            }
            val first = this@map.poll() ?: return null
            result = op(first)
            this.result = result
            return result
        }

        override fun get(): T2 {
            return result ?: run {
                val first = this@map.get()
                val newResult = op(first)
                this.result = newResult
                newResult
            }
        }
    }
}

/** Allow this [Delayed] to return only after [other] does too. */
fun <T:Any> Delayed<T>.andWaitFor(other:Delayed<*>):Delayed<T> {
    return object : Delayed<T> {
        override fun poll(): T? {
            if (other.poll() != null) {
                return this@andWaitFor.poll()
            }
            return null
        }

        override fun get(): T {
            other.get()
            return this@andWaitFor.get()
        }

    }
}

fun <T1:Any, T2:Any> Delayed<T1>.pairWith(other:Delayed<T2>):Delayed<Pair<T1, T2>> {
    return object : Delayed<Pair<T1, T2>> {
        override fun poll(): Pair<T1, T2>? {
            val v1 = this@pairWith.poll() ?: return null
            val v2 = other.poll() ?: return null
            return v1 to v2
        }

        override fun get(): Pair<T1, T2> {
            val v1 = this@pairWith.get()
            val v2 = other.get()
            return v1 to v2
        }

    }
}

/** Offload [op] to a background thread. */
inline fun <T:Any> offload(crossinline op:()->T):Delayed<T> {
    val task = ParagrowthMain.WORKER_POOL.submit( Callable {
        return@Callable op()
    } )

    return object : Delayed<T> {

        override fun get(): T {
            return task.get()
        }

        override fun poll(): T? {
            return if (task.isDone) {
                task.get()
            } else {
                null
            }
        }
    }
}

/** Call [op] immediately and return it. */
inline fun <T:Any> immediate(crossinline op:()->T):Delayed<T> {
    val result = op()

    return object : Delayed<T> {
        override fun get(): T = result
        override fun poll(): T? = result
    }
}