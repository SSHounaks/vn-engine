package com.vnengine

import com.vnengine.anim.AnimDef
import com.vnengine.anim.FrameDef
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.engine.GameVars
import com.vnengine.save.SaveSlotManager
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Marker

class PackStatsTest {
    @Test
    fun totalDurationSumsFrameDurations() {
        val anim = AnimDef(loop = true, frames = listOf(FrameDef("a", 100), FrameDef("b", 150)))
        assertEquals(250L, anim.totalDurationMs)
    }

    @Test
    fun emptyAnimHasZeroDuration() {
        assertEquals(0L, AnimDef(frames = emptyList()).totalDurationMs)
    }
}

class SaveSlotInfoTest {
    @Test
    fun getSlotInfoReturnsJsonAfterSaveAndNullOtherwise() {
        val engine = GameEngine(game { scene("start") { dialogue("n", "hi") } }, FakeAudioManager())
        val manager = SaveSlotManager(engine, SaveSystem())
        engine.start()
        assertNull(manager.getSlotInfo(0))
        manager.quickSave()
        assertNotNull(manager.getSlotInfo(0))
    }
}

class GameVarsNonScalarTest {
    @Test
    fun nonScalarValuesAreObservedAndStringEncoded() {
        val seen = mutableListOf<String>()
        val vars = GameVars { name, type -> seen += "$name:$type" }
        vars.storeSet("marker", Marker())
        assertTrue(seen.contains("marker:Marker"), "seen=$seen")
        assertTrue(vars.toSaveMap().getValue("marker").startsWith("s:"))
    }
}
