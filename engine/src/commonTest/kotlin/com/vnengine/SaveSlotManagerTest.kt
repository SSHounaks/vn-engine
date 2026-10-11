package com.vnengine

import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.save.SaveSlotManager
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveSlotManagerTest {
    private val script = game {
        scene("start") {
            dialogue("n", "one")
            dialogue("n", "two")
            dialogue("n", "three")
        }
    }

    private fun newManager(): Pair<SaveSlotManager, GameEngine> {
        val engine = GameEngine(script, FakeAudioManager())
        return SaveSlotManager(engine, SaveSystem()) to engine
    }

    @Test
    fun quickSaveAndLoadRestoreProgress() {
        val (manager, engine) = newManager()
        engine.start()
        engine.advance()

        assertFalse(manager.hasSave(0))
        manager.quickSave()
        assertTrue(manager.hasSave(0))

        engine.advance()
        assertEquals(2, engine.state.value.dialogueIndex)

        assertTrue(manager.quickLoad())
        assertEquals(1, engine.state.value.dialogueIndex)
    }

    @Test
    fun loadWithoutSaveReturnsFalse() {
        val (manager, _) = newManager()
        assertFalse(manager.quickLoad())
        assertFalse(manager.loadFromSlot(3))
    }

    @Test
    fun namedSlotsAreIndependent() {
        val (manager, engine) = newManager()
        engine.start()
        manager.saveToSlot(1)
        engine.advance()
        manager.saveToSlot(2)

        engine.advance()
        manager.loadFromSlot(1)
        assertEquals(0, engine.state.value.dialogueIndex)
    }
}
