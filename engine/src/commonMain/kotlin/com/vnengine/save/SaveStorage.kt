package com.vnengine.save

/**
 * Persistence backend for save slots. Keys are stable slot identifiers
 * (see [SaveSlot.key]); values are the serialized [SaveData] JSON produced by
 * [SaveSystem]. Implementations may use memory, files, or anything else.
 */
interface SaveStorage {
    fun read(key: String): String?

    /** Persist [data] under [key], overwriting any previous value. May throw on IO failure. */
    fun write(key: String, data: String)

    /** Remove the entry for [key]; returns true if something was removed. */
    fun delete(key: String): Boolean

    /** All keys currently holding a save. */
    fun keys(): Set<String>
}

/** Process-lifetime storage used by tests and the harness. */
class InMemorySaveStorage : SaveStorage {
    private val entries = linkedMapOf<String, String>()

    override fun read(key: String): String? = entries[key]

    override fun write(key: String, data: String) {
        entries[key] = data
    }

    override fun delete(key: String): Boolean = entries.remove(key) != null

    override fun keys(): Set<String> = entries.keys.toSet()
}
