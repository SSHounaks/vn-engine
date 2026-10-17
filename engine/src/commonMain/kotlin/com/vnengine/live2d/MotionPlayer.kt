package com.vnengine.live2d

/** Parameter blend modes, matching Cubism's Expression/motion blend kinds. */
object ParameterBlend {
    fun apply(current: Double, blend: String, value: Double, weight: Double): Double {
        val target = when (blend.lowercase()) {
            "add" -> current + value
            "multiply" -> current * value
            else -> value
        }
        val w = weight.coerceIn(0.0, 1.0)
        return current + (target - current) * w
    }
}

/** One frame of motion output: parameter values plus raw part opacities. */
class MotionFrame(
    val parameters: Map<String, Double>,
    val partOpacities: Map<String, Double>
)

/**
 * Plays Cubism motions and expressions, producing blended parameter values each
 * frame. Mirrors the parts of the Cubism MotionManager that Ren'Py reimplements.
 */
class MotionPlayer {
    private class Slot(
        val motion: CubismMotion,
        val loop: Boolean,
        val fadeInMs: Double
    ) {
        var elapsedMs: Double = 0.0
        val durationMs: Double get() = motion.meta.duration * 1000.0
    }

    private class CurveSet(val parameters: Map<String, MotionCurve>, val partOpacities: Map<String, MotionCurve>)

    private val slots = mutableListOf<Slot>()
    private val curves = mutableMapOf<CubismMotion, CurveSet>()

    val isFinished: Boolean get() = slots.isEmpty()

    fun play(
        motion: CubismMotion,
        loop: Boolean = motion.meta.loop,
        fadeInMs: Double = 500.0,
        exclusive: Boolean = true
    ) {
        if (exclusive) slots.clear()
        slots.add(Slot(motion, loop, fadeInMs))
    }

    fun update(deltaMs: Double): MotionFrame {
        if (slots.isEmpty()) return MotionFrame(emptyMap(), emptyMap())
        val parameterWeighted = mutableMapOf<String, Double>()
        val parameterWeight = mutableMapOf<String, Double>()
        val partWeighted = mutableMapOf<String, Double>()
        val partWeight = mutableMapOf<String, Double>()
        slots.forEach { slot ->
            slot.elapsedMs += deltaMs
            val weight = weightOf(slot)
            if (weight <= 0.0) return@forEach
            val seconds = if (slot.loop && slot.motion.meta.duration > 0.0) {
                (slot.elapsedMs / 1000.0) % slot.motion.meta.duration
            } else {
                (slot.elapsedMs / 1000.0).coerceAtMost(slot.motion.meta.duration)
            }
            val set = curvesFor(slot.motion)
            set.parameters.forEach { (id, curve) ->
                parameterWeighted[id] = (parameterWeighted[id] ?: 0.0) + weight * curve.sample(seconds)
                parameterWeight[id] = (parameterWeight[id] ?: 0.0) + weight
            }
            set.partOpacities.forEach { (id, curve) ->
                partWeighted[id] = (partWeighted[id] ?: 0.0) + weight * curve.sample(seconds)
                partWeight[id] = (partWeight[id] ?: 0.0) + weight
            }
        }
        return MotionFrame(
            parameters = parameterWeighted.mapValues { (id, sum) -> sum / (parameterWeight[id] ?: 1.0) },
            partOpacities = partWeighted.mapValues { (id, sum) -> sum / (partWeight[id] ?: 1.0) }
        )
    }

    private fun weightOf(slot: Slot): Double =
        if (slot.fadeInMs <= 0.0) 1.0
        else (slot.elapsedMs / slot.fadeInMs).coerceIn(0.0, 1.0)

    private fun curvesFor(motion: CubismMotion): CurveSet =
        curves.getOrPut(motion) {
            val parameters = LinkedHashMap<String, MotionCurve>()
            val partOpacities = LinkedHashMap<String, MotionCurve>()
            motion.curves.forEach { curve ->
                if (curve.target.equals("PartOpacity", ignoreCase = true)) {
                    partOpacities[curve.id] = MotionCurve(curve.segments)
                } else {
                    parameters[curve.id] = MotionCurve(curve.segments)
                }
            }
            CurveSet(parameters, partOpacities)
        }
}

/** Applies an expression's parameter overrides, ramping in over its fade time. */
class ExpressionPlayer {
    private var expression: CubismExpression? = null
    private var weight = 0.0
    private var target = 0.0
    private var fadeStep = 0.0

    fun set(expression: CubismExpression?, fadeInSeconds: Double = expression?.fadeInTime ?: 0.5) {
        this.expression = expression
        this.target = if (expression == null) 0.0 else 1.0
        this.fadeStep = if (fadeInSeconds <= 0.0) Double.MAX_VALUE else 1.0 / fadeInSeconds
    }

    fun update(deltaSeconds: Double) {
        weight = if (weight < target) {
            (weight + fadeStep * deltaSeconds).coerceAtMost(target)
        } else {
            (weight - fadeStep * deltaSeconds).coerceAtLeast(target)
        }
    }

    fun parameters(): List<ExpressionParameter>? {
        val e = expression ?: return null
        if (weight <= 0.0) return null
        return e.parameters
    }

    val blendWeight: Double get() = weight
}

