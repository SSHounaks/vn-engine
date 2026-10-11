package com.vnengine

import com.vnengine.dsl.Position
import com.vnengine.dsl.SpriteRef
import com.vnengine.dsl.TimelineEvent
import com.vnengine.dsl.TransitionType
import com.vnengine.effect.EffectKind
import com.vnengine.effect.EffectRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimelineTest {
    private val sprites = listOf(SpriteRef("hero", Position.Center))

    @Test
    fun schedulesSequentiallyAndCompletes() {
        val runner = EffectRunner()
        runner.build(
            sprites,
            listOf(TimelineEvent.Zoom("hero", 2f, 1000), TimelineEvent.Wait(500))
        )
        assertEquals(2, runner.scheduled.size)
        assertEquals(0, runner.scheduled[0].startMs)
        assertEquals(1000, runner.scheduled[1].startMs)

        assertFalse(runner.isComplete())
        runner.tick(1500)
        assertTrue(runner.isComplete())
        assertEquals(2f, runner.computeVisuals(sprites).sprites.first().scale)
    }

    @Test
    fun moveInterpolatesOffset() {
        val runner = EffectRunner()
        runner.build(sprites, listOf(TimelineEvent.Move("hero", Position.Right, 1000)))

        runner.tick(0)
        val start = runner.computeVisuals(sprites).sprites.first().xOffset
        runner.tick(1000)
        val end = runner.computeVisuals(sprites).sprites.first().xOffset

        assertEquals(0f, start)
        assertTrue(end > 0f)
    }

    @Test
    fun parallelWaitsForLongestChild() {
        val runner = EffectRunner()
        runner.build(
            sprites,
            listOf(
                TimelineEvent.Parallel(
                    listOf(TimelineEvent.Wait(1000), TimelineEvent.Wait(400))
                ),
                TimelineEvent.Wait(500)
            )
        )
        assertEquals(3, runner.scheduled.size)
        assertEquals(1000, runner.scheduled.last().startMs)
        runner.tick(1500)
        assertTrue(runner.isComplete())
    }

    @Test
    fun flashExposesAlpha() {
        val runner = EffectRunner()
        runner.build(emptyList(), listOf(TimelineEvent.Flash(0xFF0000, 200)))
        runner.tick(50)
        assertTrue(runner.computeVisuals(emptyList()).flashAlpha > 0f)
    }

    @Test
    fun shakeProducesOffsetAndRotation() {
        val runner = EffectRunner()
        runner.build(sprites, listOf(TimelineEvent.Shake("hero", 20f, 1000)))
        runner.tick(300)
        val v = runner.computeVisuals(sprites).sprites.first()
        assertTrue(v.rotation != 0f || v.yOffset != 0f)
    }

    @Test
    fun zoomEasesScaleAtMidpoint() {
        val runner = EffectRunner()
        runner.build(sprites, listOf(TimelineEvent.Zoom("hero", 2f, 1000)))
        runner.tick(500)
        val mid = runner.computeVisuals(sprites).sprites.first().scale
        assertTrue(mid > 1f && mid < 2f)
    }

    @Test
    fun animEventScheduledButLeavesVisualUntouched() {
        val runner = EffectRunner()
        runner.build(sprites, listOf(TimelineEvent.Anim("hero", "walk", 500, loop = true)))
        val kind = runner.scheduled.single().kind
        assertTrue(kind is EffectKind.Anim)
        assertEquals("walk", (kind as EffectKind.Anim).group)
        runner.tick(250)
        assertEquals(1f, runner.computeVisuals(sprites).sprites.first().scale)
    }

    @Test
    fun transitionExposesAlphaAndSlideIsTransparent() {
        val fade = EffectRunner()
        fade.build(emptyList(), listOf(TimelineEvent.Transition(TransitionType.Fade, 0.5)))
        fade.tick(0)
        assertTrue(fade.computeVisuals(emptyList()).transitionAlpha >= 0f)

        val slide = EffectRunner()
        slide.build(emptyList(), listOf(TimelineEvent.Transition(TransitionType.Slide, 0.5)))
        slide.tick(50)
        assertEquals(0f, slide.computeVisuals(emptyList()).transitionAlpha)
    }

    @Test
    fun resetAndZeroDurationEffects() {
        val runner = EffectRunner()
        runner.build(sprites, listOf(TimelineEvent.Flash(0x00FF00, 0)))
        assertTrue(runner.isComplete())
        runner.reset(100)
        assertEquals(0f, runner.computeVisuals(sprites).flashAlpha)
    }
}
