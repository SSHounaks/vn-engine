package com.vnengine.save

import java.io.File

/**
 * JVM [SaveStorage] that keeps one JSON file per slot under [directory].
 * Defaults to `<user home>/.vn-engine/saves`.
 */
class FileSaveStorage(private val directory: File = defaultDirectory()) : SaveStorage {
    init {
        if (!directory.exists()) directory.mkdirs()
    }

    private fun fileFor(key: String): File = File(directory, "$key.json")

    override fun read(key: String): String? =
        fileFor(key).takeIf { it.isFile }?.readText()

    override fun write(key: String, data: String) {
        directory.mkdirs()
        fileFor(key).writeText(data)
    }

    override fun delete(key: String): Boolean = fileFor(key).delete()

    override fun keys(): Set<String> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.mapTo(mutableSetOf()) { it.name.removeSuffix(".json") }
            ?: emptySet()

    companion object {
        fun defaultDirectory(): File =
            File(System.getProperty("user.home") ?: ".", ".vn-engine/saves")
    }
}
