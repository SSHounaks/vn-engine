package com.vnengine

import com.vnengine.anim.AnimDef
import com.vnengine.anim.Animator
import com.vnengine.anim.FrameDef
import com.vnengine.anim.Pack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimatorEdgeCaseTest {
    private val pack = Pack(
        name = "t",
        animations = mapOf(
            "idle" to AnimDef(loop = true, frames = listOf(FrameDef("a", 100), FrameDef("b", 100))),
            "empty" to AnimDef(loop = true, frames = emptyList()),
            "solo" to AnimDef(loop = false, frames = listOf(FrameDef("only", 100)))
        )
    )

    @Test
    fun playingGroupWithNoFramesFails() {
        val anim = Animator(pack)
        assertFalse(anim.play("empty"))
        assertEquals("", anim.currentSrc)
    }

    @Test
    fun unknownGroupFails() {
        assertFalse(Animator(pack).play("missing"))
    }

    @Test
    fun replayingSameGroupAfterFinishRestarts() {
        val anim = Animator(pack)
        anim.play("solo")
        anim.advance(100)
        assertEquals("only", anim.currentSrc)
        assertTrue(anim.finished)
        assertTrue(anim.play("solo"))
        assertFalse(anim.finished)
        assertEquals("only", anim.currentSrc)
    }

    @Test
    fun advancingWithoutPlayingIsNoOp() {
        val anim = Animator(pack)
        anim.advance(500)
        assertEquals("", anim.currentSrc)
        assertFalse(anim.finished)
    }

    @Test
    fun advanceAfterFinishIsIgnored() {
        val anim = Animator(pack)
        anim.play("solo")
        anim.advance(100)
        anim.advance(1000)
        assertEquals("only", anim.currentSrc)
    }

    @Test
    fun loopOverrideWinsOverDefinition() {
        val anim = Animator(pack)
        anim.play("idle", loop = false)
        assertFalse(anim.currentLoop)
        anim.play("solo", loop = true)
        assertTrue(anim.currentLoop)
    }
}
