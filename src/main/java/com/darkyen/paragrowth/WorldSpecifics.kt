package com.darkyen.paragrowth

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.math.Vector3
import com.darkyen.paragrowth.terrain.generator.Noise
import com.darkyen.paragrowth.terrain.generator.OpenSimplexNoise
import com.darkyen.paragrowth.util.*

/**
 * Stateless thread-safe characterization of a world, specific realization of [WorldCharacteristics].
 */
class WorldSpecifics(val characteristics: WorldCharacteristics, centerX: Float, centerY: Float, centerForSpawn: Boolean) {

    val offsetX: Float
    val offsetY: Float
    val noise: Noise

    val waterColor: Float
    private val beachColor: FloatArray
    private val terrainColor: FloatArray

    private val colorNoise = OpenSimplexNoise().apply {
        initialize(characteristics.seed)
    }

    init {
        val random = RandomXS128(characteristics.seed)
        waterColor = characteristics.getRandomFudgedColor(random, WorldColors.WATER)
        beachColor = floatArrayOf(characteristics.getRandomFudgedColor(random, WorldColors.BEACH), characteristics.getRandomFudgedColor(random, WorldColors.BEACH))

        terrainColor = floatArrayOf(characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN), characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN), characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN), characteristics.getRandomFudgedColor(random, WorldColors.TERRAIN))
    }

    init {
        val worldSize = (MathUtils.clamp(Math.sqrt(characteristics.size.toDouble()).toFloat(), 1f, 30f) * 100f).toInt()

        noise = Noise.generateSimplexNoise(worldSize, worldSize,
                characteristics.seed, 1f,
                1f / 200f, 2f, 5, 40f, 0.5f, -1f)
        noise.islandize(1f, -1f)
        noise.max(-1f)

        if (centerForSpawn) {
            val v = Vector3()
            val RNG = setupInitialPosition_RNG
            RNG.setSeed(characteristics.seed)
            noise.findRandomPositionInHeightRange(v, RNG, 0.1f, java.lang.Float.POSITIVE_INFINITY)
            this.offsetX = centerX - v.x
            this.offsetY = centerY - v.y
        } else {
            this.offsetX = centerX - worldSize * 0.5f
            this.offsetY = centerY - worldSize * 0.5f
        }
    }

    fun sizeX(): Int {
        return noise.sizeX
    }

    fun sizeY(): Int {
        return noise.sizeY
    }

    fun getHeight(x: Float, y: Float): Float {
        return noise.getHeight(x - offsetX, y - offsetY)
    }

    fun getNormal(to: Vector3, x: Float, y: Float) {
        noise.getNormal(to, x - offsetX, y - offsetY)
    }

    fun findInitialPosition(pos: Vector3): Boolean {
        val RNG = setupInitialPosition_RNG
        RNG.setSeed(characteristics.seed)

        val success = noise.findRandomPositionInHeightRange(pos, RNG, 0.1f, java.lang.Float.POSITIVE_INFINITY)
        pos.add(offsetX, offsetY, 0f)
        return success
    }

    fun queryColors(): WorldColorQuery {
        return WorldColorQuery()
    }

    /**
     * Stateful, non-thread safe query for world's colors.
     */
    inner class WorldColorQuery {
        private val randomForColors = RandomXS128(characteristics.seed)

        fun getColor(x: Float, y: Float): Float {
            val height = noise.getHeight(x - offsetX, y - offsetY)
            if (height <= 0f) {
                return waterColor
            }
            val colorBase: FloatArray
            val colorNoiseScale: Float
            if (height < 1f) {
                colorBase = beachColor
                colorNoiseScale = COLOR_NOISE_SCALE_BEACH
            } else {
                colorBase = terrainColor
                colorNoiseScale = COLOR_NOISE_SCALE_TERRAIN
            }

            val alpha = colorNoise.evaluatePositive(x * colorNoiseScale, y * colorNoiseScale, height * colorNoiseScale)
            return lerpHSBAndFudge(colorBase, alpha, randomForColors, characteristics.coherence, 0.6f)
        }
    }

    companion object {
        private const val COLOR_NOISE_SCALE_BEACH = 0.2f
        private const val COLOR_NOISE_SCALE_TERRAIN = 0.02f

        private val setupInitialPosition_RNG = RandomXS128()
    }


}
