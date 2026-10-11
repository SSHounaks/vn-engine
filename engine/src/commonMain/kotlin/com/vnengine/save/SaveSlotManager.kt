package com.vnengine.save

import com.vnengine.engine.GameEngine
import com.vnengine.platform.currentTimeMillis

/**
 * Manages the game's fixed save layout: [manualSlotCount] numbered manual slots
 * plus a [SaveSlot.Quick] and [SaveSlot.Auto] slot, all persisted through a
 * [SaveStorage]. Operations return false (rather than throwing) when a slot is
 * out of range, empty, or the backend fails.
 */
class SaveSlotManager(
    private val engine: GameEngine,
    private val saveSystem: SaveSystem = SaveSystem(),
    private val storage: SaveStorage = InMemorySaveStorage(),
    val manualSlotCount: Int = 6,
    private val clock: () -> Long = { currentTimeMillis() }
) {
    fun save(slot: SaveSlot): Boolean {
        if (!isValid(slot)) return false
        val json = saveSystem.save(
            state = engine.state.value,
            savedAt = clock(),
            preview = engine.frame.value.dialogue?.text
        )
        return runCatching { storage.write(slot.key, json) }.isSuccess
    }

    fun load(slot: SaveSlot): Boolean {
        if (!isValid(slot)) return false
        val json = storage.read(slot.key) ?: return false
        val state = saveSystem.load(json) ?: return false
        engine.loadState(state)
        return true
    }

    fun delete(slot: SaveSlot): Boolean {
        if (!isValid(slot)) return false
        return runCatching { storage.delete(slot.key) }.getOrDefault(false)
    }

    fun hasSave(slot: SaveSlot): Boolean = isValid(slot) && storage.read(slot.key) != null

    /** Metadata for a slot, or null when the slot is invalid or empty/corrupt. */
    fun info(slot: SaveSlot): SlotInfo? {
        if (!isValid(slot)) return null
        val json = storage.read(slot.key) ?: return null
        val data = saveSystem.parse(json) ?: return null
        return SlotInfo(
            slot = slot,
            savedAt = data.savedAt,
            scene = data.state.currentScene,
            dialogueIndex = data.state.dialogueIndex,
            preview = data.preview,
            steps = data.steps,
            version = data.version
        )
    }

    fun manualSlots(): List<Int> = (1..manualSlotCount).toList()

    /** All slots in display order: manual slots first, then quick, then auto. */
    fun allSlots(): List<SaveSlot> =
        manualSlots().map { SaveSlot.Manual(it) } + listOf(SaveSlot.Quick, SaveSlot.Auto)

    /** Metadata for every slot in [allSlots]; null entries are empty slots. */
    fun infos(): List<SlotInfo?> = allSlots().map { info(it) }

    fun quickSave(): Boolean = save(SaveSlot.Quick)
    fun quickLoad(): Boolean = load(SaveSlot.Quick)
    fun autoSave(): Boolean = save(SaveSlot.Auto)
    fun autoLoad(): Boolean = load(SaveSlot.Auto)

    fun saveToSlot(index: Int): Boolean = save(SaveSlot.Manual(index))
    fun loadFromSlot(index: Int): Boolean = load(SaveSlot.Manual(index))

    private fun isValid(slot: SaveSlot): Boolean = when (slot) {
        is SaveSlot.Manual -> slot.index in 1..manualSlotCount
        SaveSlot.Quick, SaveSlot.Auto -> true
    }
}
