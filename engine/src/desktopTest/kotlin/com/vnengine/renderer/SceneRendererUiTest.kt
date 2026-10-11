package com.vnengine.renderer

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vnengine.FakeAudioManager
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for the click-to-complete typewriter behavior: a tap while a
 * line is still typing reveals the whole line; the *next* tap advances.
 */
@OptIn(ExperimentalTestApi::class)
class SceneRendererUiTest {
    @Test
    fun tapCompletesLineThenAdvances() = runComposeUiTest {
        val script = game {
            scene("start") {
                dialogue("n", "Hello world")
                dialogue("n", "Second line")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()

        mainClock.autoAdvance = false
        setContent { SceneRenderer(engine) }
        mainClock.advanceTimeBy(32)

        // Let the typewriter reveal a few characters, but not all of them.
        mainClock.advanceTimeBy(60)
        waitForIdle()

        // First tap: complete the current line.
        onRoot().performClick()
        mainClock.advanceTimeBy(32)
        waitForIdle()
        onNodeWithText("Hello world").assertExists()
        assertEquals("Hello world", engine.frame.value.dialogue?.text)

        // Second tap: advance to the next line (no choices present).
        onRoot().performClick()
        mainClock.advanceTimeBy(32)
        waitForIdle()
        assertEquals("Second line", engine.frame.value.dialogue?.text)
    }

    @Test
    fun finishedScreenTapRestarts() = runComposeUiTest {
        val script = game {
            scene("start") { dialogue("n", "The only line") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()

        mainClock.autoAdvance = false
        setContent { SceneRenderer(engine) }
        mainClock.advanceTimeBy(32)
        waitForIdle()

        onRoot().performClick()
        mainClock.advanceTimeBy(32)
        waitForIdle()
        assertEquals("start", engine.state.value.currentScene)
    }
}
