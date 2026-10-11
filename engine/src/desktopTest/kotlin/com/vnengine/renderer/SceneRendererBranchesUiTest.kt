package com.vnengine.renderer

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import com.vnengine.FakeAudioManager
import com.vnengine.anim.AnimDef
import com.vnengine.anim.FrameDef
import com.vnengine.anim.Pack
import com.vnengine.dsl.Position
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SceneRendererBranchesUiTest {
    private fun render(engine: GameEngine, body: ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent { SceneRenderer(engine) }
            mainClock.advanceTimeBy(400)
            waitForIdle()
            body()
        }

    @Test
    fun staticSpriteRendersAsImage() = render(
        GameEngine(
            game { scene("start") { show("hero", at = Position.Left); dialogue("n", "hi") } },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithContentDescription("hero").assertIsDisplayed()
    }

    @Test
    fun animatedSpriteRendersFrameImage() = render(
        GameEngine(
            game { scene("start") { show("hero", anim = "idle"); dialogue("n", "hi") } },
            FakeAudioManager(),
            Pack(
                name = "t",
                animations = mapOf(
                    "idle" to AnimDef(loop = true, frames = listOf(FrameDef("frame1.png", 100)))
                )
            )
        ).also { it.start() }
    ) {
        onNodeWithContentDescription("hero").assertIsDisplayed()
    }

    @Test
    fun missingSpriteFallsBackToPlaceholder() = render(
        GameEngine(
            game { scene("start") { show("ghost"); dialogue("n", "hi") } },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithText("ghost").assertIsDisplayed()
    }

    @Test
    fun choiceButtonsAreClickable() {
        val engine = GameEngine(
            game {
                scene("start") {
                    dialogue("n", "pick")
                    choice { option("Warm", "warm"); option("Cold", "cold") }
                }
                scene("warm") { dialogue("n", "warm") }
                scene("cold") { dialogue("n", "cold") }
            },
            FakeAudioManager()
        ).also { it.start() }
        engine.advance()

        render(engine) {
            onNodeWithText("Warm").assertIsDisplayed()
            onNodeWithText("Warm").performClick()
            waitForIdle()
            assertEquals("warm", engine.state.value.currentScene)
        }
    }

    @Test
    fun endScreenShowsAndRestartsOnTap() {
        val engine = GameEngine(
            game { scene("start") { dialogue("n", "only") } },
            FakeAudioManager()
        ).also { it.start(); it.advance() }

        render(engine) {
            onNodeWithText("The End").assertIsDisplayed()
            onRoot().performClick()
            waitForIdle()
            assertTrue(!engine.finished.value)
            assertEquals("start", engine.state.value.currentScene)
        }
    }

    @Test
    fun realBackgroundAndFallbackBackgroundsRender() = render(
        GameEngine(
            game {
                scene("start") {
                    background("sky")
                    dialogue("n", "hi")
                    choice { option("go", "hex") }
                }
                scene("hex") { background("#123456"); dialogue("n", "hex") }
            },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithText("hi").assertIsDisplayed()
    }

    @Test
    fun namedAndInvalidBackgroundsRender() = render(
        GameEngine(
            game { scene("start") { background("night_sky"); dialogue("n", "hi") } },
            FakeAudioManager()
        ).also { it.start() }
    ) {
        onNodeWithText("hi").assertIsDisplayed()
    }
}
