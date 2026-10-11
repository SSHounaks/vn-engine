package com.vnengine.save

import com.vnengine.engine.GameEngine
import com.vnengine.engine.GameState

class SaveSlotManager(private val engine: GameEngine, private val saveSystem: SaveSystem) {
    private val slots = mutableMapOf<Int, String>()

    fun quickSave() {
        val state = engine.state.value
        slots[0] = saveSystem.save(state, 0)
    }

    fun quickLoad(): Boolean {
        val save = slots[0] ?: return false
        val state = saveSystem.load(save) ?: return false
        engine.loadState(state)
        return true
    }

    fun saveToSlot(slot: Int) {
        val state = engine.state.value
        slots[slot] = saveSystem.save(state, slot)
    }

    fun loadFromSlot(slot: Int): Boolean {
        val save = slots[slot] ?: return false
        val state = saveSystem.load(save) ?: return false
        engine.loadState(state)
        return true
    }

    fun hasSave(slot: Int): Boolean = slots.containsKey(slot)

    fun getSlotInfo(slot: Int): String? = slots[slot]
}
