package com.vnengine

import com.vnengine.anim.AnimDef
import com.vnengine.anim.Animator
import com.vnengine.anim.FrameDef
import com.vnengine.anim.Pack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimatorTest {
    private val pack = Pack(
        name = "t",
        animations = mapOf(
            "idle" to AnimDef(loop = true, frames = listOf(FrameDef("a", 100), FrameDef("b", 100))),
            "once" to AnimDef(loop = false, frames = listOf(FrameDef("x", 100), FrameDef("y", 100)))
        )
    )

    @Test
    fun loopsBackToStart() {
        val anim = Animator(pack)
        assertTrue(anim.play("idle"))
        assertEquals("a", anim.currentSrc)
        anim.advance(100)
        assertEquals("b", anim.currentSrc)
        anim.advance(100)
        assertEquals("a", anim.currentSrc)
        assertFalse(anim.finished)
    }

    @Test
    fun nonLoopingStopsOnLastFrame() {
        val anim = Animator(pack)
        assertTrue(anim.play("once"))
        anim.advance(100)
        assertEquals("y", anim.currentSrc)
        anim.advance(100)
        assertEquals("y", anim.currentSrc)
        assertTrue(anim.finished)
    }

    @Test
    fun unknownGroupFailsToPlay() {
        val anim = Animator(pack)
        assertFalse(anim.play("missing"))
        assertTrue(anim.play("idle"))
    }

    @Test
    fun loopOverrideRespected() {
        val anim = Animator(pack)
        anim.play("idle", loop = false)
        anim.advance(100)
        anim.advance(100)
        assertTrue(anim.finished)
    }
}
