package com.darkyen.paragrowth.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.utils.NumberUtils
import java.util.*

val White = Color.WHITE.toFloatBits()

/**
 * Create RGB Color
 */
fun rgb(red:Float, green:Float = red, blue:Float = green, alpha:Float = 1f):Float =
        Color.toFloatBits(
                clamp(red, 0f, 1f),
                clamp(green, 0f, 1f),
                clamp(blue, 0f, 1f),
                clamp(alpha, 0f, 1f))

val HueRed = 0/6f
val HueOrange = 1/6f
val HueYellow = 2/6f
val HueGreen = 3/6f
val HueBlue = 4/6f
val HuePurple = 5/6f

fun hsb(hue: Float, saturation: Float, brightness: Float, alpha:Float = 1f): Float {
    val satu = clamp(saturation, 0f, 1f)
    val brig = clamp(brightness, 0f, 1f)

    var r = 0f
    var g = 0f
    var b = 0f
    if (satu == 0f) {
        b = brig
        g = brig
        r = brig
    } else {
        val h = (hue - Math.floor(hue.toDouble()).toFloat()) * 6.0f
        val f = h - Math.floor(h.toDouble()).toFloat()
        val p = brig * (1.0f - satu)
        val q = brig * (1.0f - satu * f)
        val t = brig * (1.0f - satu * (1.0f - f))
        when (h.toInt()) {
            0 -> {
                r = brig
                g = t
                b = p
            }
            1 -> {
                r = q
                g = brig
                b = p
            }
            2 -> {
                r = p
                g = brig
                b = t
            }
            3 -> {
                r = p
                g = q
                b = brig
            }
            4 -> {
                r = t
                g = p
                b = brig
            }
            5 -> {
                r = brig
                g = p
                b = q
            }
        }
    }
    return Color.toFloatBits(r, g, b, clamp(alpha, 0f, 1f))
}

fun Color.fromHsb(hue: Float, saturation: Float, brightness: Float, alpha:Float = 1f) {
    val satu = clamp(saturation, 0f, 1f)
    val brig = clamp(brightness, 0f, 1f)

    var r = 0f
    var g = 0f
    var b = 0f
    if (satu == 0f) {
        b = brig
        g = brig
        r = brig
    } else {
        val h = (hue - Math.floor(hue.toDouble()).toFloat()) * 6.0f
        val f = h - Math.floor(h.toDouble()).toFloat()
        val p = brig * (1.0f - satu)
        val q = brig * (1.0f - satu * f)
        val t = brig * (1.0f - satu * (1.0f - f))
        when (h.toInt()) {
            0 -> {
                r = brig
                g = t
                b = p
            }
            1 -> {
                r = q
                g = brig
                b = p
            }
            2 -> {
                r = p
                g = brig
                b = t
            }
            3 -> {
                r = p
                g = q
                b = brig
            }
            4 -> {
                r = t
                g = p
                b = brig
            }
            5 -> {
                r = brig
                g = p
                b = q
            }
        }
    }

    this.r = r
    this.g = g
    this.b = b
    this.a = clamp(alpha, 0f, 1f)
}

fun randomColor(): Float {
    return java.lang.Float.intBitsToFloat(MathUtils.random.nextInt() or -0x2000000)
}

val Float.red:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*0)) and 0xFF) / 0xFF.toFloat()

val Float.green:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*1)) and 0xFF) / 0xFF.toFloat()

val Float.blue:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*2)) and 0xFF) / 0xFF.toFloat()

val Float.alpha:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*3)) and 0xFF) / 0xFF.toFloat()

fun lerpRGB(from:Float, to:Float, progress:Float) =
        rgb(
                lerp(from.red, to.red, progress),
                lerp(from.green, to.green, progress),
                lerp(from.blue, to.blue, progress),
                lerp(from.alpha, to.alpha, progress))

fun lerpHSB(from:Float, to:Float, progress:Float) =
        hsb(
                MathUtils.lerpAngleDeg(from.hue * 360f, to.hue * 360f, progress) / 360f,
                lerp(from.saturation, to.saturation, progress),
                lerp(from.brightness, to.brightness, progress),
                lerp(from.alpha, to.alpha, progress))

fun lerpHSB(from:FloatArray, progress:Float):Float {
    val fullProgress = progress * (from.size - 1)
    val firstIndex = fullProgress.toInt()
    if (firstIndex == from.lastIndex) {
        return from.last()
    }
    return lerpHSB(from[firstIndex], from[firstIndex + 1], fullProgress % 1f)
}

val Float.hue:Float
    get() {
        val r:Float = this.red
        val g:Float = this.green
        val b:Float = this.blue

        val cMax = maxOf(r,g, b)
        val cMin = minOf(r, g, b)

        var hue: Float
        val saturation: Float = if (cMax != 0f) {
            (cMax - cMin) / cMax
        } else {
            0f
        }

        if (saturation == 0f)
            hue = 0f
        else {
            val redC = (cMax - r) / (cMax - cMin)
            val greenC = (cMax - g) / (cMax - cMin)
            val blueC = (cMax - b) / (cMax - cMin)
            hue = when {
                r == cMax -> blueC - greenC
                g == cMax -> 2.0f + redC - blueC
                else -> 4.0f + greenC - redC
            }
            hue /= 6.0f
            if (hue < 0)
                hue += 1.0f
        }

        return hue
    }

val Float.saturation:Float
    get() {
        val r:Float = this.red
        val g:Float = this.green
        val b:Float = this.blue

        val cMax = maxOf(r,g, b)
        val cMin = minOf(r, g, b)

        return if (cMax != 0f) {
            (cMax - cMin) / cMax
        } else {
            0f
        }
    }

val Float.brightness:Float
    get() {
        val r:Float = this.red
        val g:Float = this.green
        val b:Float = this.blue

        return maxOf(r,g, b)
    }

val Color.hue:Float
    get() {
        val cMax = maxOf(r,g, b)
        val cMin = minOf(r, g, b)

        var hue: Float
        val saturation: Float = if (cMax != 0f) {
            (cMax - cMin) / cMax
        } else {
            0f
        }

        if (saturation == 0f)
            hue = 0f
        else {
            val redC = (cMax - r) / (cMax - cMin)
            val greenC = (cMax - g) / (cMax - cMin)
            val blueC = (cMax - b) / (cMax - cMin)
            hue = when {
                r == cMax -> blueC - greenC
                g == cMax -> 2.0f + redC - blueC
                else -> 4.0f + greenC - redC
            }
            hue /= 6.0f
            if (hue < 0)
                hue += 1.0f
        }

        return hue
    }

val Color.saturation:Float
    get() {
        val cMax = maxOf(r,g, b)
        val cMin = minOf(r, g, b)

        return if (cMax != 0f) {
            (cMax - cMin) / cMax
        } else {
            0f
        }
    }

val Color.brightness:Float
    get() {
        return maxOf(r,g, b)
    }

private fun smoothClamp(value:Float): Float {
    return Math.tanh((value + value).toDouble()).toFloat()
}

fun Color.set(color:Float):Color {
    this.r = color.red
    this.g = color.green
    this.b = color.blue
    this.a = color.alpha
    return this
}

private fun Random.fudgeAmount(coherence: Float, amount:Float):Float {
    val variance = 1f - coherence
    val base = variance * variance * (nextFloat() - 0.5f)
    val offsetBase = Math.copySign(Math.sqrt(Math.abs(base).toDouble()).toFloat(), base)
    return (offsetBase + (nextFloat() - 0.5f) * 0.2f) * amount
}

fun Float.fudge(random: Random, coherence:Float, amount:Float = 1f):Float {
    var hue = this.hue
    val saturation = this.saturation
    if (saturation < 0.001f) {
        hue = random.nextFloat()
    }

    val h = hue + random.fudgeAmount(coherence, amount * 0.2f)
    val s = smoothClamp(saturation + random.fudgeAmount(coherence, amount * 0.5f))
    val b = smoothClamp(this.brightness + random.fudgeAmount(coherence, amount * 0.5f))
    return hsb(h,s,b)
}

fun Color.fudge(random: Random, coherence:Float, amount:Float = 1f):Color {
    var hue = this.hue
    val saturation = this.saturation
    if (saturation < 0.001f) {
        hue = random.nextFloat()
    }

    val h = hue + random.fudgeAmount(coherence, amount * 0.2f)
    val s = smoothClamp(saturation + random.fudgeAmount(coherence, amount * 0.5f))
    val b = smoothClamp(this.brightness + random.fudgeAmount(coherence, amount * 0.5f))
    this.fromHsb(h,s,b)
    return this
}