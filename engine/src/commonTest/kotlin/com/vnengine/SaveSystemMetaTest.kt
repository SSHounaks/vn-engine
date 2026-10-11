package com.vnengine

import com.vnengine.engine.GameState
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SaveSystemMetaTest {
    @Test
    fun saveRecordsMetadataAndParseReadsItBack() {
        val state = GameState(
            currentScene = "chapter2",
            dialogueIndex = 3,
            history = listOf("a", "b", "c")
        )
        val json = SaveSystem().save(state, savedAt = 99L, preview = "Hi there")

        val data = assertNotNull(SaveSystem().parse(json))
        assertEquals(2, data.version)
        assertEquals(99L, data.savedAt)
        assertEquals("Hi there", data.preview)
        assertEquals(3, data.steps)
        assertEquals("chapter2", data.state.currentScene)
        assertEquals(listOf("a", "b", "c"), data.recentEvents)
    }

    @Test
    fun stepsDefaultToHistorySize() {
        val state = GameState(history = listOf("s1", "s2", "s3", "s4"))
        val data = assertNotNull(SaveSystem().parse(SaveSystem().save(state)))
        assertEquals(4, data.steps)
    }

    @Test
    fun parseMalformedReturnsNull() {
        assertNull(SaveSystem().parse("{ not json"))
    }

    @Test
    fun legacySaveWithoutMetadataStillDecodes() {
        val legacy = """{"version":1,"timestamp":5,"state":""" +
            """{"currentScene":"start","dialogueIndex":0,"visibleSprites":[],"variables":{},"history":[]}}"""
        val data = assertNotNull(SaveSystem().parse(legacy))
        assertEquals(1, data.version)
        assertEquals(0L, data.savedAt)
        assertNull(data.preview)
        assertNotNull(SaveSystem().load(legacy))
    }
}
