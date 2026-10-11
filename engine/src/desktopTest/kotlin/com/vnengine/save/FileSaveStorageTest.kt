package com.vnengine.save

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSaveStorageTest {
    private fun tempDir(): File = Files.createTempDirectory("vn-saves-test").toFile().apply { deleteOnExit() }

    @Test
    fun writesReadsListsAndDeletes() {
        val storage = FileSaveStorage(tempDir())
        assertNull(storage.read("manual-1"))
        assertTrue(storage.keys().isEmpty())

        storage.write("manual-1", "hello")
        storage.write("quick", "world")
        assertEquals("hello", storage.read("manual-1"))
        assertEquals(setOf("manual-1", "quick"), storage.keys())

        assertTrue(storage.delete("manual-1"))
        assertFalse(storage.delete("manual-1"))
        assertEquals(setOf("quick"), storage.keys())
    }

    @Test
    fun persistsAcrossInstances() {
        val dir = tempDir()
        FileSaveStorage(dir).write("auto", "persisted")

        val reopened = FileSaveStorage(dir)
        assertEquals("persisted", reopened.read("auto"))
        assertTrue(reopened.keys().contains("auto"))
    }

    @Test
    fun creatingStorageMakesMissingDirectory() {
        val dir = File(File(tempDir(), "nested"), "saves")
        assertFalse(dir.exists())
        FileSaveStorage(dir)
        assertTrue(dir.isDirectory)
    }

    @Test
    fun defaultDirectoryLivesUnderUserHome() {
        val dir = FileSaveStorage.defaultDirectory()
        assertTrue(dir.path.contains(".vn-engine"))
        assertTrue(dir.path.contains("saves"))
    }
}
