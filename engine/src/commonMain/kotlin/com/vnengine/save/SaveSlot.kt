package com.vnengine.save

/**
 * A logical save location. The engine uses a fixed layout: numbered manual
 * slots plus a quick-save and an autosave slot. [key] is what [SaveStorage]
 * persists under, so it must be stable and filesystem-safe.
 */
sealed class SaveSlot {
    abstract val key: String
    abstract val displayName: String

    data class Manual(val index: Int) : SaveSlot() {
        override val key: String get() = "manual-$index"
        override val displayName: String get() = "Slot $index"
    }

    object Quick : SaveSlot() {
        override val key: String get() = "quick"
        override val displayName: String get() = "Quick Save"
    }

    object Auto : SaveSlot() {
        override val key: String get() = "auto"
        override val displayName: String get() = "Auto Save"
    }
}

/** Metadata describing a non-empty slot, cheap to compute without loading the full state. */
data class SlotInfo(
    val slot: SaveSlot,
    val savedAt: Long,
    val scene: String,
    val dialogueIndex: Int,
    val preview: String?,
    val steps: Int,
    val version: Int
)
