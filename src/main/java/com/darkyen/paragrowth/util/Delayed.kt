package com.darkyen.paragrowth.util

import com.darkyen.paragrowth.ParagrowthMain
import java.util.concurrent.Callable

/**
 * Value, which is not immediately available, for some reason, but it can be polled for.
 */
interface Delayed<T : Any> {

    /** Get the delayed item, or null if not done yet. */
    fun get():T?
}

/** Produce [Delayed], which will perform [op] on the result of this [Delayed]. */
inline fun <T:Any, T2:Any> Delayed<T>.then(crossinline op:(T)->Delayed<T2>):Delayed<T2> {
    return object : Delayed<T2> {

        var then:Delayed<T2>? = null

        override fun get(): T2? {
            if (then == null) {
                val first = this@then.get() ?: return null
                then = op(first)
            }
            return then?.get()
        }
    }
}

/** Offload [op] to a background thread. */
inline fun <T:Any> offload(crossinline op:()->T):Delayed<T> {
    val task = ParagrowthMain.WORKER_POOL.submit( Callable {
        return@Callable op()
    } )

    return object : Delayed<T> {
        override fun get(): T? {
            return if (task.isDone) {
                task.get()
            } else {
                null
            }
        }
    }
}