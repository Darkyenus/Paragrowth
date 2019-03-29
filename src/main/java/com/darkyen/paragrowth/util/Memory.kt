package com.darkyen.paragrowth.util

import org.lwjgl.system.MemoryStack

/**
 * Perform operation [op] on [MemoryStack] which is subsequently freed.
 */
inline fun <R> stack(op: MemoryStack.()->R):R {
    val stack = MemoryStack.stackGet()
    val stackPointer = stack.pointer
    try {
        return stack.op()
    } finally {
        stack.pointer = stackPointer
    }
}