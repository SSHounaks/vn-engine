package com.vnengine

import com.vnengine.dsl.Position
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.engine.GameVars
import com.vnengine.engine.VarKey
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveSystemEdgeCaseTest {
    private val script = game {
        scene("start") {
            show("hero", at = Position.Center)
            dialogue("n", "one")
        }
    }

    @Test
    fun invalidPositionStringLoadsNull() {
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val json = SaveSystem().save(engine.state.value).replace("\"Center\"", "\"Diagonal\"")
        assertNull(SaveSystem().load(json))
    }

    @Test
    fun saveCarriesVersionAndSavedAt() {
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val json = SaveSystem().save(engine.state.value, savedAt = 2L)
        assertTrue(json.contains("\"version\":2"))
        assertTrue(json.contains("\"savedAt\""))
    }

    @Test
    fun longHistoryRoundTrips() {
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val state = engine.state.value.copy(history = List(15) { "scene_$it" })
        val restored = assertNotNull(SaveSystem().load(SaveSystem().save(state)))
        assertEquals(15, restored.history.size)
        assertEquals("scene_14", restored.history.last())
    }

    @Test
    fun typedVariablesSurviveStateRoundTrip() {
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val vars = GameVars()
        vars[VarKey("long", 1L)] = 5L
        vars[VarKey("double", 1.0)] = 3.5
        val state = engine.state.value.copy(variables = vars.toSaveMap())

        val restored = assertNotNull(SaveSystem().load(SaveSystem().save(state)))
        val view = GameVars()
        view.loadFrom(restored.variables)
        assertEquals(5L, view[VarKey("long", 0L)])
        assertEquals(3.5, view[VarKey("double", 0.0)])
    }
}
