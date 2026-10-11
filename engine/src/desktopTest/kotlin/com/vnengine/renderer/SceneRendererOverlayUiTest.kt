package com.vnengine.renderer

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import com.vnengine.FakeAudioManager
import com.vnengine.dsl.Position
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SceneRendererOverlayUiTest {
    private fun render(engine: GameEngine, body: ComposeUiTest.() -> Unit) = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { SceneRenderer(engine) }
        mainClock.advanceTimeBy(400)
        waitForIdle()
        body()
    }

    @Test
    fun flashOverlayRenders() = render(
        GameEngine(
            game {
                scene("start") {
                    timeline { flash(0xFF0000, 10.0) }
                    dialogue("n", "hi")
                }
            },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onRoot().assertIsDisplayed()
    }

    @Test
    fun transitionOverlayRenders() = render(
        GameEngine(
            game {
                scene("start") {
                    timeline { fade(10.0) }
                    dialogue("n", "hi")
                }
            },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithText("hi").assertIsDisplayed()
    }

    @Test
    fun rightPositionedSpriteUsesPositiveBias() = render(
        GameEngine(
            game { scene("start") { show("hero", at = Position.Right); dialogue("n", "hi") } },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithContentDescription("hero").assertIsDisplayed()
    }

    @Test
    fun invalidHexBackgroundFallsBack() = render(
        GameEngine(
            game { scene("start") { background("#zzzzzz"); dialogue("n", "hi") } },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithText("hi").assertIsDisplayed()
    }

    @Test
    fun loadingSceneFromTimelineCompletes() = render(
        GameEngine(
            game {
                scene("start") {
                    show("hero")
                    timeline { move("hero", Position.Left, 0.2); zoom("hero", 1.5f, 0.2) }
                    dialogue("n", "hi")
                }
            },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithContentDescription("hero").assertIsDisplayed()
    }
}
