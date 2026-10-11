package com.vnengine.harness

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaveSlotDriverTest {
    @Test
    fun driverQuickSaveAndLoadRoundTrip() {
        val driver = GameDriver(TestScripts.linear()).start()
        driver.advance()
        assertTrue(driver.quickSave())

        driver.advanceDialogue()
        assertEquals("end", driver.scene)

        assertTrue(driver.quickLoad())
        assertEquals(1, driver.dialogueIndex)
    }

    @Test
    fun driverManualSlotsAndInfo() {
        val driver = GameDriver(TestScripts.branching()).start()
        driver.advance()
        driver.choose("Warm")
        assertTrue(driver.saveToSlot(1))

        val info = assertNotNull(driver.slotInfo(1))
        assertEquals("warm", info.scene)
        assertEquals(2, driver.intValue(TestScripts.affection))

        assertTrue(driver.loadFromSlot(1))
        assertFalse(driver.loadFromSlot(4))
    }
}
