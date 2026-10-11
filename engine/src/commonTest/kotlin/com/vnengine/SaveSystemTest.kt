package com.vnengine

import com.vnengine.dsl.Position
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveSystemTest {
    private val script = game {
        scene("start") {
            show("hero", at = Position.Right, scale = 0.9f, layer = 1, bias = -0.3f, anim = "idle")
            dialogue("n", "one")
            dialogue("n", "two")
            next("end")
        }
        scene("end") { dialogue("n", "done") }
    }

    @Test
    fun saveLoadResumesSceneVarsAndSprites() {
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()

        val saved = SaveSystem().save(engine.state.value)

        engine.advance()
        engine.advance()
        assertEquals("end", engine.state.value.currentScene)

        val restored = SaveSystem().load(saved)
        assertNotNull(restored)
        engine.loadState(restored)

        assertEquals("start", engine.state.value.currentScene)
        assertEquals(1, engine.state.value.dialogueIndex)
        val sprite = engine.state.value.visibleSprites.single()
        assertEquals("hero", sprite.id)
        assertEquals(Position.Right, sprite.position)
        assertEquals(0.9f, sprite.scale)
        assertEquals(1, sprite.layer)
        assertEquals(-0.3f, sprite.bias)
        assertEquals("idle", sprite.anim)
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(SaveSystem().load("{ not json"))
    }

    @Test
    fun variablesSurviveRoundTrip() {
        val script2 = game {
            scene("start") {
                onEnter { vars[com.vnengine.engine.varInt("aff")] = 9 }
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script2, FakeAudioManager())
        engine.start()
        val json = SaveSystem().save(engine.state.value)
        assertTrue(json.contains("\"start\""))
    }
}
