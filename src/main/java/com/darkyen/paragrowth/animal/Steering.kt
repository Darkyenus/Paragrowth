package com.darkyen.paragrowth.animal

import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Vector2
import com.darkyen.paragrowth.util.addRotated
import com.darkyen.paragrowth.util.angleRad
import java.lang.Math.abs
import kotlin.math.sqrt

/**
 * Movement attributes for an agent.
 *
 * ## Heft
 * Agent has a pool of "heft", with size 1. It determines, how fast can it move while it turns.
 * For example, if heft value of speed and turning is both 1, the actor can't turn at max speed at all and has to slow down.
 * When it slows down to 70% of max speed, it can then turn with 30% of max turn speed, etc.
 */
open class AgentAttributes {
    /** Units per second per second */
    var maxAcceleration:Float = 1f
    /** Units per second per second */
    var maxDeceleration:Float = 1f
    /** [0..1]: How much does haste affect acceleration?
     * 0 = completely linearly, agent uses N% of acceleration to reach N% of max speed
     * 1 = not at all, agent uses max acceleration even if it is trying to reach low speed */
    var agility:Float = 0.5f
    /** Units per second */
    var maxSpeed:Float = 1f
    /** [0..1]: how much does max speed occupy the agent to prevent steering? */
    var speedHeft:Float = 0.75f
    /** Radians per second */
    var maxTurnSpeed:Float = 1f
    /** [0..1]: how much does max turn speed occupy the agent to prevent linear movement? */
    var turnHeft:Float = 0.75f
}

interface AgentAttributesMethods<A:AgentAttributes> {
    fun A.setToLerp(a0:A, a1:A, progress:Float)
    fun A.set(to:A)
}

inline fun <reified A:AgentAttributes> createAgentAttributesMethods():AgentAttributesMethods<A> {
    val fields = A::class.java.fields

    return object : AgentAttributesMethods<A> {
        override fun A.setToLerp(a0: A, a1: A, progress: Float) {
            for (field in fields) {
                field.setFloat(this, lerp(field.getFloat(a0), field.getFloat(a1), progress))
            }
        }

        override fun A.set(to: A) {
            for (field in fields) {
                field.setFloat(this, field.getFloat(to))
            }
        }

    }
}

class MovementAgent(private val timestep:Float = 1f / 120f) {

    private var timeAccumulator = 0f
    private var blend = 0f

    private val oldPosition = Vector2()
    private val newPosition = Vector2()
    private var oldHeading = 0f
    private var newHeading = 0f
    private var oldVelocity = 0f
    private var newVelocity = 0f

    val x:Float
        get() = lerp(oldPosition.x, newPosition.x, blend)

    val y:Float
        get() = lerp(oldPosition.y, newPosition.y, blend)

    var heading:Float
        get() = lerp(oldHeading, newHeading, blend)
        set(value) {
            oldHeading = value
            newHeading = value
        }

    var velocity:Float
        get() = lerp(oldVelocity, newVelocity, blend)
        set(value) {
            oldVelocity = value
            newVelocity = value
        }

    fun setPosition(x:Float, y:Float) {
        oldPosition.set(newPosition.set(x, y))
    }

    private inline fun withTimestep(delta:Float, action:()->Unit) {
        timeAccumulator += delta
        if (timeAccumulator >= timestep) {
            do {
                oldPosition.set(newPosition)
                oldHeading = newHeading
                oldVelocity = newVelocity
                action()
                timeAccumulator -= timestep
            } while (timeAccumulator > timestep)
            blend = timeAccumulator / timestep
        }
    }

    /** Arrive at [target], with zero velocity. */
    fun moveTo(target:Vector2, haste:Float, delta:Float, attributes:AgentAttributes, targetVelocity:Float = 0f) {
        withTimestep(delta) {
            doMoveTo(target, haste, attributes, targetVelocity)
        }
    }

    private fun doMoveTo(target: Vector2, haste: Float, attributes: AgentAttributes, targetVelocity: Float) {
        /*
        We have some preferred deceleration to achieve ... comfortableDeceleration
        If we are too close, we can decelerate more    ... maxDeceleration

        Cases:
        1. Deceleration that would be used now is smaller than comfortable,
           so it is fine to just move in the general direction.
           There might be a time, when the timestep, when we should start decelerating.
        2. Deceleration needed to stop is higher than comfortable, but less than max.
           Decelerate with larger deceleration until full stop.
        3. Deceleration needed is higher than max possible deceleration.
           We will overshoot, otherwise use logic from 2.
         */
        val moveChangeX = target.x - newPosition.x
        val moveChangeY = target.y - newPosition.y
        val distance = Vector2.len(moveChangeX, moveChangeY)
        val targetHeading = angleRad(moveChangeX, moveChangeY)

        val comfortableDeceleration = attributes.maxDeceleration * haste
        val maxDeceleration = attributes.maxDeceleration

        val currentRequiredDeceleration = (1f - targetVelocity) * 0.5f * velocity * velocity / distance
        when {
            currentRequiredDeceleration >= maxDeceleration ->
                doMove(targetHeading, haste, attributes, -maxDeceleration, target)
            currentRequiredDeceleration >= comfortableDeceleration ->
                doMove(targetHeading, haste, attributes, -currentRequiredDeceleration, target)
            else ->
                doMove(targetHeading, haste, attributes, turnTarget = target)
        }
    }

    fun move(targetHeading:Float, haste:Float, delta:Float, attributes:AgentAttributes) {
        withTimestep(delta) {
            doMove(targetHeading, haste, attributes)
        }
    }

    private fun doMove(targetHeading:Float, haste:Float, attributes:AgentAttributes, maxAcceleration:Float = Float.POSITIVE_INFINITY, turnTarget:Vector2? = null) {
        val requiredHeadingChange = angleDelta(newHeading, targetHeading)
        var requiredVelocity = attributes.maxSpeed * haste

        var turnSpeed = 0f
        var turnChangeInSec = Float.POSITIVE_INFINITY
        if (!isZero(requiredHeadingChange)) {
            // Need to turn
            val availableTurnHeft = clamp((1f - (newVelocity / attributes.maxSpeed) * attributes.speedHeft) / attributes.turnHeft, 0f, 1f)
            // At full haste, require full turning heft if 180deg away
            val idealTurnHeft = abs(requiredHeadingChange / PI) * haste

            if (idealTurnHeft > availableTurnHeft) {
                // Slow down to get better turning heft
                val requiredVelocityForTurning = (1f - idealTurnHeft) / attributes.speedHeft
                requiredVelocity = minOf(requiredVelocity, requiredVelocityForTurning)
            }

            // TODO(jp): Try to prevent circling around target

            turnSpeed = Math.copySign(attributes.maxTurnSpeed * minOf(availableTurnHeft, lerp(availableTurnHeft, haste, attributes.agility)), requiredHeadingChange)
            turnChangeInSec = requiredHeadingChange / turnSpeed
        }

        val requiredVelocityChange = requiredVelocity - newVelocity
        var acceleration = minOf(0f, maxAcceleration)
        var accelerationChangeInSec = Float.POSITIVE_INFINITY
        if (!isZero(requiredVelocityChange)) {
            //val currentSpeedHeft = MathUtils.clamp((1f - (turnSpeed / attributes.maxTurnSpeed) * attributes.turnHeft) / attributes.speedHeft, 0f, 1f)
            val velocityChangeFactor = lerp(haste , 1f, attributes.agility)
            acceleration = if (requiredVelocityChange < 0f) {
                // Need to slow down
                -velocityChangeFactor * attributes.maxDeceleration
            } else {
                // Need to speed up
                velocityChangeFactor * attributes.maxAcceleration
            }
            acceleration = minOf(acceleration, maxAcceleration)
            accelerationChangeInSec = requiredVelocityChange / acceleration
        }

        if (turnChangeInSec < timestep && accelerationChangeInSec < timestep) {
            if (turnChangeInSec < accelerationChangeInSec) {
                // 1. Speed + Turn
                doMove(acceleration, turnSpeed, turnChangeInSec)
                // 2. Turn
                doMove(0f, turnSpeed, accelerationChangeInSec - turnChangeInSec)
                // 3. -
                doMove(0f, 0f, timestep - accelerationChangeInSec)
            } else {
                // 1. Speed + Turn
                doMove(acceleration, turnSpeed, accelerationChangeInSec)
                // 2. Speed
                doMove(acceleration, 0f, turnChangeInSec - accelerationChangeInSec)
                // 3. -
                doMove(0f, 0f, timestep - turnChangeInSec)
            }
        } else if (turnChangeInSec < timestep) {
            // 1. Turn
            doMove(acceleration, turnSpeed, turnChangeInSec)
            // 2. -
            doMove(acceleration, 0f, timestep - turnChangeInSec)
        } else if (accelerationChangeInSec < timestep) {
            // 1. Speed
            doMove(acceleration, turnSpeed, accelerationChangeInSec)
            // 2. -
            doMove(0f, turnSpeed, timestep - accelerationChangeInSec)
        } else {
            // 1. -
            doMove(acceleration, turnSpeed, timestep)
        }
    }

    private fun doMove(acceleration:Float, turnSpeed:Float, delta:Float) {
        // s = v * t + 0.5*a*t^2
        val distance = newVelocity * delta + 0.5f * acceleration * delta * delta
        newVelocity = maxOf(newVelocity + delta * acceleration, 0f)
        newHeading += (turnSpeed * delta) % PI2

        var movementX:Float
        var movementY:Float
        if (isZero(turnSpeed)) {
            // Simple movement equation
            movementX = distance
            movementY = 0f
        } else {
            // Moving on a circle
            val angleMoved = delta * turnSpeed
            //val circumference = distance / (angleMoved / PI2)
            //val circleRadius = circumference * (1f / PI2)
            val circleRadius = distance / angleMoved
            movementX = Math.sin(angleMoved.toDouble()).toFloat() * circleRadius
            movementY = -(Math.cos(angleMoved.toDouble()).toFloat() - 1f) * circleRadius

            // There may be some precision issues with very small deltas
            if (delta < 0.1f * timestep) {
                val len2 = Vector2.len2(movementX, movementY)
                if (len2 > distance * distance) {
                    val invLen = (1.0 / sqrt(len2)).toFloat()
                    movementX *= invLen
                    movementY *= invLen
                }
            }
        }

        newPosition.addRotated(movementX, movementY, newHeading)
    }

}

/** Smallest angle to add to [fromRad] to reach [toRad] orientation.
 * @return value in range [-PI..PI] */
private fun angleDelta(fromRad:Float, toRad:Float):Float {
    return ((toRad - fromRad + PI2 + PI) % PI2) - PI
}
