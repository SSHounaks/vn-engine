package com.vnengine

import com.vnengine.engine.DialogueController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogueControllerTest {
    @Test
    fun revealsCharactersOverTime() {
        val c = DialogueController(charIntervalMs = 10)
        c.setLine("abc")
        assertEquals("", c.visibleText)
        assertFalse(c.complete)

        assertTrue(c.tick(10))
        assertEquals("a", c.visibleText)
        c.tick(10)
        assertEquals("ab", c.visibleText)
        c.tick(10)
        assertEquals("abc", c.visibleText)
        assertTrue(c.complete)
        assertEquals(3 to 3, c.progress)
    }

    @Test
    fun completesImmediatelyOnSkip() {
        val c = DialogueController(charIntervalMs = 10)
        c.setLine("hello")
        c.completeNow()
        assertEquals("hello", c.visibleText)
        assertTrue(c.complete)
        assertEquals(5 to 5, c.progress)
        assertFalse(c.tick(1000))
    }

    @Test
    fun emptyLineIsComplete() {
        val c = DialogueController()
        c.setLine("")
        assertTrue(c.complete)
        assertEquals("", c.visibleText)
    }

    @Test
    fun largeDeltaRevealsToEnd() {
        val c = DialogueController(charIntervalMs = 10)
        c.setLine("abcde")
        c.tick(1000)
        assertEquals("abcde", c.visibleText)
        assertTrue(c.complete)
    }
}
