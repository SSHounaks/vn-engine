package com.vnengine

import com.vnengine.save.InMemorySaveStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemorySaveStorageTest {
    @Test
    fun writeReadDeleteAndKeys() {
        val storage = InMemorySaveStorage()
        assertNull(storage.read("a"))
        assertTrue(storage.keys().isEmpty())

        storage.write("a", "one")
        storage.write("b", "two")
        assertEquals("one", storage.read("a"))
        assertEquals(setOf("a", "b"), storage.keys())

        storage.write("a", "updated")
        assertEquals("updated", storage.read("a"))

        assertTrue(storage.delete("a"))
        assertFalse(storage.delete("a"))
        assertEquals(setOf("b"), storage.keys())
    }
}
