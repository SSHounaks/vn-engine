package com.vnengine

import com.vnengine.dsl.demoStory
import com.vnengine.engine.GameEngine
import kotlin.test.Test
import kotlin.test.assertTrue

class DemoGameTest {
    @Test
    fun demoStoryRunsToEnd() {
        val engine = GameEngine(demoStory)
        engine.start()
        var guard = 0
        while (!engine.finished.value && guard++ < 50) engine.advance()
        assertTrue(engine.finished.value)
    }
}
