package com.darkyen.paragrowth.util

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.utils.NumberUtils
import java.util.*

typealias GdxColor = com.badlogic.gdx.graphics.Color

typealias Color = Float

val Color.red:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*0)) and 0xFF) / 0xFF.toFloat()

val Color.green:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*1)) and 0xFF) / 0xFF.toFloat()

val Color.blue:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*2)) and 0xFF) / 0xFF.toFloat()

val Color.alpha:Float
    get() = ((NumberUtils.floatToIntColor(this) ushr (8*3)) and 0xFF) / 0xFF.toFloat()

val Color.hue:Float
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

val Color.saturation:Float
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

val Color.brightness:Float
    get() {
        val r:Float = this.red
        val g:Float = this.green
        val b:Float = this.blue

        return maxOf(r,g, b)
    }

inline fun Color.getHSB(returnHue:(Float)->Unit, returnSaturation:(Float)->Unit, returnBrightness:(Float)->Unit) {
    val r:Float = this.red
    val g:Float = this.green
    val b:Float = this.blue

    val cMax = maxOf(r, g, b)
    val cMin = minOf(r, g, b)

    returnBrightness(cMax)

    val saturation: Float = if (cMax != 0f) {
        (cMax - cMin) / cMax
    } else {
        0f
    }

    returnSaturation(saturation)

    var hue: Float
    if (saturation == 0f) {
        hue = 0f
    } else {
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

    returnHue(hue)
}

val GdxColor.hue:Float
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

val GdxColor.saturation:Float
    get() {
        val cMax = maxOf(r,g, b)
        val cMin = minOf(r, g, b)

        return if (cMax != 0f) {
            (cMax - cMin) / cMax
        } else {
            0f
        }
    }

val GdxColor.brightness:Float
    get() {
        return maxOf(r,g, b)
    }

val White = GdxColor.WHITE.toFloatBits()

val HueRed = 0/6f
val HueOrange = 1/6f
val HueYellow = 2/6f
val HueGreen = 3/6f
val HueBlue = 4/6f
val HuePurple = 5/6f

/**
 * Create RGB Color
 */
fun rgb(red:Float, green:Float = red, blue:Float = green, alpha:Float = 1f):Color =
        GdxColor.toFloatBits(
                clamp(red, 0f, 1f),
                clamp(green, 0f, 1f),
                clamp(blue, 0f, 1f),
                clamp(alpha, 0f, 1f))

fun hsb(hue: Float, saturation: Float, brightness: Float, alpha:Float = 1f): Color {
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
    return GdxColor.toFloatBits(r, g, b, clamp(alpha, 0f, 1f))
}

fun GdxColor.fromHsb(hue: Float, saturation: Float, brightness: Float, alpha:Float = 1f) {
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

fun randomColor(): Color {
    return java.lang.Float.intBitsToFloat(MathUtils.random.nextInt() or -0x2000000)
}

fun lerpRGB(from:Color, to:Color, progress:Float) =
        rgb(
                lerp(from.red, to.red, progress),
                lerp(from.green, to.green, progress),
                lerp(from.blue, to.blue, progress),
                lerp(from.alpha, to.alpha, progress))

fun lerpHSB(from:Color, to:Color, progress:Float) =
        hsb(
                lerpHue(from.hue, to.hue, progress),
                lerp(from.saturation, to.saturation, progress),
                lerp(from.brightness, to.brightness, progress),
                lerp(from.alpha, to.alpha, progress))

fun lerpHSB(from:FloatArray, progress:Float):Color {
    val fullProgress = progress * (from.size - 1)
    val firstIndex = fullProgress.toInt()
    if (firstIndex == from.lastIndex) {
        return from.last()
    }
    return lerpHSB(from[firstIndex], from[firstIndex + 1], fullProgress % 1f)
}

/** In modulo 1 lerp in the shorter direction */
private fun lerpHue(from: Float, to: Float, progress: Float): Float {
    val delta = (to - from + 1.5f) % 1f - 0.5f
    return (from + delta * progress + 1f) % 1f
}

private fun fastHsb(hue:Float, saturation:Float, brightness:Float):Color {
    // This is the fastest variant. Modulo, roundtrip to int, etc. is all slower
    val h = (hue - Math.floor(hue.toDouble()).toFloat()) * 6.0f
    val f = h - Math.floor(h.toDouble()).toFloat()

    val p = brightness * (1.0f - saturation)
    val q = brightness * (1.0f - saturation * f)
    val t = brightness * (1.0f - saturation * (1.0f - f))

    val r: Float
    val g: Float
    val b: Float
    when (h.toInt()) {
        0 -> {
            r = brightness
            g = t
            b = p
        }
        1 -> {
            r = q
            g = brightness
            b = p
        }
        2 -> {
            r = p
            g = brightness
            b = t
        }
        3 -> {
            r = p
            g = q
            b = brightness
        }
        4 -> {
            r = t
            g = p
            b = brightness
        }
        else -> {
            r = brightness
            g = p
            b = q
        }
    }

    return GdxColor.toFloatBits(r, g, b, 1f)
}

/** A very optimized combination of HSB lerp and fudge. */
fun lerpHSBAndFudge(from:FloatArray, progress:Float, random: Random, coherence:Float, amount:Float = 1f):Color {
    val fullProgress = progress * (from.size - 1)
    val firstIndex = fullProgress.toInt()
    if (firstIndex == from.lastIndex) {
        // TODO(jp): Forgot about fudge
        return from.last()
    }

    var fromHue = 0f
    var fromSaturation = 0f
    var fromBrightness = 0f
    from[firstIndex].getHSB({fromHue = it}, {fromSaturation = it}, {fromBrightness = it})

    var toHue = 0f
    var toSaturation = 0f
    var toBrightness = 0f
    from[firstIndex + 1].getHSB({toHue = it}, {toSaturation = it}, {toBrightness = it})

    val blend = fullProgress % 1f
    var hue = lerpHue(fromHue, toHue, blend)
    var saturation = lerp(fromSaturation, toSaturation, blend)
    var brightness = lerp(fromBrightness, toBrightness, blend)

    if (saturation < 0.001f) {
        hue = random.nextFloat()
    }

    hue += random.fudgeAmount(coherence, amount * 0.2f)
    saturation += random.fudgeAmount(coherence, amount * 0.5f)
    brightness += random.fudgeAmount(coherence, amount * 0.5f)

    saturation = clamp01(saturation)
    brightness = clamp01(brightness)

    return fastHsb(hue, saturation, brightness)
}

/** Smoothly clamp [value] to -1..1 range */
private fun smoothClamp11(value:Float): Float {
    //WARNING: VERY SLOW
    return Math.tanh((value*2f).toDouble()).toFloat()
}

/** Smoothly clamp [value] to 0..1 range */
private fun smoothClamp01(value:Float):Float {
    // WARNING: VERY SLOW
    return Math.tanh((value*4f - 2f).toDouble()).toFloat() * 0.5f + 0.5f
}

private fun clamp01(value:Float):Float {
    return if (value <= 0f) 0f else if (value >= 1f) 1f else value
}

fun GdxColor.set(color:Color):GdxColor {
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

fun Color.fudge(random: Random, coherence:Float, amount:Float = 1f):Color {
    var hue = this.hue
    val saturation = this.saturation
    if (saturation < 0.001f) {
        hue = random.nextFloat()
    }

    val h = hue + random.fudgeAmount(coherence, amount * 0.2f)
    val s = clamp01(saturation + random.fudgeAmount(coherence, amount * 0.5f))
    val b = clamp01(this.brightness + random.fudgeAmount(coherence, amount * 0.5f))
    return hsb(h,s,b)
}

fun GdxColor.fudge(random: Random, coherence:Float, amount:Float = 1f):GdxColor {
    var hue = this.hue
    val saturation = this.saturation
    if (saturation < 0.001f) {
        hue = random.nextFloat()
    }

    val h = hue + random.fudgeAmount(coherence, amount * 0.2f)
    val s = clamp01(saturation + random.fudgeAmount(coherence, amount * 0.5f))
    val b = clamp01(this.brightness + random.fudgeAmount(coherence, amount * 0.5f))
    this.fromHsb(h,s,b)
    return this
}