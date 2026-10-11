package com.vnengine

import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.save.InMemorySaveStorage
import com.vnengine.save.SaveSlot
import com.vnengine.save.SaveSlotManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveSlotManagerTest {
    private val script = game {
        scene("start") {
            dialogue("n", "one")
            dialogue("n", "two")
            dialogue("n", "three")
        }
    }

    private fun newManager(
        manualSlotCount: Int = 6,
        clock: () -> Long = { 0L }
    ): Pair<SaveSlotManager, GameEngine> {
        val engine = GameEngine(script, FakeAudioManager())
        val manager = SaveSlotManager(engine, storage = InMemorySaveStorage(), manualSlotCount = manualSlotCount, clock = clock)
        return manager to engine
    }

    @Test
    fun quickSaveAndLoadRestoreProgress() {
        val (manager, engine) = newManager()
        engine.start()
        engine.advance()

        assertFalse(manager.hasSave(SaveSlot.Quick))
        assertTrue(manager.quickSave())
        assertTrue(manager.hasSave(SaveSlot.Quick))

        engine.advance()
        assertEquals(2, engine.state.value.dialogueIndex)

        assertTrue(manager.quickLoad())
        assertEquals(1, engine.state.value.dialogueIndex)
    }

    @Test
    fun loadWithoutSaveReturnsFalse() {
        val (manager, _) = newManager()
        assertFalse(manager.quickLoad())
        assertFalse(manager.autoLoad())
        assertFalse(manager.load(SaveSlot.Manual(1)))
    }

    @Test
    fun manualSlotsAreIndependent() {
        val (manager, engine) = newManager()
        engine.start()
        assertTrue(manager.saveToSlot(1))
        engine.advance()
        assertTrue(manager.saveToSlot(2))

        engine.advance()
        assertEquals(2, engine.state.value.dialogueIndex)

        assertTrue(manager.loadFromSlot(1))
        assertEquals(0, engine.state.value.dialogueIndex)
        assertTrue(manager.loadFromSlot(2))
        assertEquals(1, engine.state.value.dialogueIndex)
    }

    @Test
    fun quickAutoAndManualAreSeparate() {
        val (manager, engine) = newManager()
        engine.start()
        manager.quickSave()
        engine.advance()
        manager.autoSave()
        engine.advance()
        manager.saveToSlot(1)

        assertTrue(manager.loadFromSlot(1))
        assertEquals(2, engine.state.value.dialogueIndex)

        assertTrue(manager.autoLoad())
        assertEquals(1, engine.state.value.dialogueIndex)

        assertTrue(manager.quickLoad())
        assertEquals(0, engine.state.value.dialogueIndex)
    }

    @Test
    fun outOfRangeManualSlotsAreRejected() {
        val (manager, _) = newManager(manualSlotCount = 3)
        assertFalse(manager.saveToSlot(0))
        assertFalse(manager.saveToSlot(4))
        assertTrue(manager.saveToSlot(3))
        assertNull(manager.info(SaveSlot.Manual(0)))
        assertNull(manager.info(SaveSlot.Manual(99)))
    }

    @Test
    fun overwriteReplacesPreviousSave() {
        val (manager, engine) = newManager()
        engine.start()
        manager.saveToSlot(1)
        engine.advance()
        engine.advance()
        manager.saveToSlot(1)

        engine.start()
        assertTrue(manager.loadFromSlot(1))
        assertEquals(2, engine.state.value.dialogueIndex)
    }

    @Test
    fun deleteClearsSlot() {
        val (manager, engine) = newManager()
        engine.start()
        manager.quickSave()
        assertTrue(manager.hasSave(SaveSlot.Quick))
        assertTrue(manager.delete(SaveSlot.Quick))
        assertFalse(manager.hasSave(SaveSlot.Quick))
        assertFalse(manager.delete(SaveSlot.Quick))
    }

    @Test
    fun infoDescribesSavedSlot() {
        val (manager, engine) = newManager(clock = { 1234L })
        engine.start()
        engine.advance()
        manager.saveToSlot(2)

        val info = assertNotNull(manager.info(SaveSlot.Manual(2)))
        assertEquals(SaveSlot.Manual(2), info.slot)
        assertEquals(1234L, info.savedAt)
        assertEquals("start", info.scene)
        assertEquals(1, info.dialogueIndex)
        assertEquals("two", info.preview)
        assertEquals(2, info.version)
    }

    @Test
    fun infosListsEverySlotInOrderAndNullForEmpty() {
        val (manager, engine) = newManager(manualSlotCount = 2)
        engine.start()
        manager.saveToSlot(1)

        val infos = manager.infos()
        assertEquals(4, infos.size)
        assertEquals(SaveSlot.Manual(1), infos[0]?.slot)
        assertNull(infos[1])
        assertNull(infos[2])
        assertNull(infos[3])

        assertEquals(
            listOf(SaveSlot.Manual(1), SaveSlot.Manual(2), SaveSlot.Quick, SaveSlot.Auto),
            manager.allSlots()
        )
    }

    @Test
    fun corruptStoredSaveYieldsNullInfoAndFalseLoad() {
        val engine = GameEngine(script, FakeAudioManager())
        val storage = InMemorySaveStorage()
        storage.write(SaveSlot.Quick.key, "{ not json")
        val manager = SaveSlotManager(engine, storage = storage)
        assertNull(manager.info(SaveSlot.Quick))
        assertFalse(manager.quickLoad())
    }
}
