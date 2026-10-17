package com.vnengine.live2d

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MotionCurveTest {
    @Test
    fun linearInterpolatesAndClamps() {
        val curve = MotionCurve(listOf(0.0, 0.0, 0.0, 1.0, 1.0))
        assertEquals(0.0, curve.sample(0.0), 1e-6)
        assertEquals(0.5, curve.sample(0.5), 1e-6)
        assertEquals(1.0, curve.sample(1.0), 1e-6)
        assertEquals(1.0, curve.sample(5.0), 1e-6)
    }

    @Test
    fun multiSegmentLinear() {
        val curve = MotionCurve(listOf(0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 2.0, 2.0))
        assertEquals(0.5, curve.sample(0.5), 1e-6)
        assertEquals(1.5, curve.sample(1.5), 1e-6)
    }

    @Test
    fun steppedHoldsUntilNextPoint() {
        val curve = MotionCurve(listOf(0.0, 0.0, 2.0, 1.0, 1.0))
        assertEquals(0.0, curve.sample(0.5), 1e-6)
        assertEquals(1.0, curve.sample(2.0), 1e-6)
    }

    @Test
    fun inverseSteppedJumpsAtStart() {
        val curve = MotionCurve(listOf(0.0, 0.0, 3.0, 1.0, 1.0))
        assertEquals(1.0, curve.sample(0.5), 1e-6)
        assertEquals(1.0, curve.sample(1.5), 1e-6)
    }

    @Test
    fun bezierIsMonotonicAndMidpoint() {
        val curve = MotionCurve(listOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0))
        assertEquals(0.5, curve.sample(0.5), 1e-3)
        assertTrue(curve.sample(0.25) < curve.sample(0.75))
    }

    @Test
    fun emptyCurveReturnsZero() {
        assertEquals(0.0, MotionCurve(emptyList()).sample(1.0), 1e-6)
    }
}

class MotionPlayerTest {
    private fun motion(segments: List<Double>, duration: Double = 1.0, loop: Boolean = false) =
        CubismMotion(
            meta = MotionMeta(duration = duration, loop = loop),
            curves = listOf(MotionCurveDef(id = "ParamAngleX", segments = segments))
        )

    @Test
    fun playsCurveAndHoldsLastFrame() {
        val player = MotionPlayer()
        player.play(motion(listOf(0.0, 0.0, 0.0, 1.0, 10.0)), fadeInMs = 0.0)
        assertEquals(5.0, player.update(500.0).parameters["ParamAngleX"]!!, 1e-3)
        assertEquals(10.0, player.update(1000.0).parameters["ParamAngleX"]!!, 1e-3)
    }

    @Test
    fun blendsTwoActiveMotions() {
        val player = MotionPlayer()
        player.play(motion(listOf(0.0, 0.0, 0.0, 1.0, 10.0)), fadeInMs = 0.0, exclusive = false)
        player.play(motion(listOf(0.0, 0.0, 0.0, 1.0, 20.0)), fadeInMs = 0.0, exclusive = false)
        assertEquals(15.0, player.update(1000.0).parameters["ParamAngleX"]!!, 1e-3)
    }

    @Test
    fun fadeWeightsBlendBetweenMotions() {
        // Stepped segments hold a constant value: A = 10, B = 20.
        val player = MotionPlayer()
        player.play(motion(listOf(0.0, 10.0, 2.0, 1.0, 10.0)), fadeInMs = 0.0, exclusive = false)
        player.play(motion(listOf(0.0, 20.0, 2.0, 1.0, 20.0)), fadeInMs = 1000.0, exclusive = false)
        // A weight 1.0, B weight 0.5 => (10 + 10) / 1.5 = 13.333...
        assertEquals(40.0 / 3.0, player.update(500.0).parameters["ParamAngleX"]!!, 1e-3)
    }

    @Test
    fun separatesPartOpacityCurvesFromParameterCurves() {
        val player = MotionPlayer()
        val m = CubismMotion(
            meta = MotionMeta(duration = 1.0),
            curves = listOf(
                MotionCurveDef(target = "Parameter", id = "ParamAngleX", segments = listOf(0.0, 0.0, 0.0, 1.0, 10.0)),
                MotionCurveDef(target = "PartOpacity", id = "Part01ArmRB001", segments = listOf(0.0, 0.0, 0.0, 1.0, 0.0))
            )
        )
        player.play(m, fadeInMs = 0.0)
        val frame = player.update(1000.0)
        assertEquals(10.0, frame.parameters.getValue("ParamAngleX"), 1e-3)
        assertNull(frame.parameters["Part01ArmRB001"])
        assertEquals(0.0, frame.partOpacities.getValue("Part01ArmRB001"), 1e-3)
        assertNull(frame.partOpacities["ParamAngleX"])
    }
}

class ParameterBlendTest {
    @Test
    fun overwriteAddMultiply() {
        assertEquals(1.0, ParameterBlend.apply(0.0, "Overwrite", 1.0, 1.0), 1e-6)
        assertEquals(3.0, ParameterBlend.apply(1.0, "Add", 2.0, 1.0), 1e-6)
        assertEquals(2.0, ParameterBlend.apply(1.0, "Multiply", 2.0, 1.0), 1e-6)
    }

    @Test
    fun weightInterpolatesTowardTarget() {
        assertEquals(0.5, ParameterBlend.apply(0.0, "Overwrite", 1.0, 0.5), 1e-6)
    }
}

class ExpressionPlayerTest {
    @Test
    fun rampsWeightAndExposesParameters() {
        val player = ExpressionPlayer()
        val expr = CubismExpression(
            fadeInTime = 1.0,
            parameters = listOf(ExpressionParameter(id = "P", value = 1.0))
        )
        player.set(expr, fadeInSeconds = 1.0)
        player.update(0.5)
        assertEquals(0.5, player.blendWeight, 1e-6)
        assertEquals(1, player.parameters()!!.size)
        player.update(1.0)
        assertEquals(1.0, player.blendWeight, 1e-6)
    }

    @Test
    fun nothingActiveInitially() {
        assertNull(ExpressionPlayer().parameters())
    }
}
