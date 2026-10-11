package com.vnengine

import com.vnengine.dsl.Position
import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.game
import com.vnengine.dsl.intGe
import com.vnengine.engine.GameEngine
import com.vnengine.engine.GameVars
import com.vnengine.engine.VarKey
import com.vnengine.engine.varInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {
    private fun varsOf(engine: GameEngine): GameVars {
        val view = GameVars()
        view.loadFrom(engine.state.value.variables)
        return view
    }

    @Test
    fun advanceWalksDialogueThenTransitions() {
        val script = game {
            scene("start") {
                dialogue("n", "one")
                dialogue("n", "two")
                next("end")
            }
            scene("end") { dialogue("n", "done") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        assertEquals("one", engine.frame.value.dialogue?.text)
        engine.advance()
        assertEquals("two", engine.frame.value.dialogue?.text)
        engine.advance()
        assertEquals("end", engine.state.value.currentScene)
    }

    @Test
    fun sceneBlockRunsExactlyOnce() {
        var runs = 0
        val script = game {
            scene("start") {
                runs++
                dialogue("n", "a")
                dialogue("n", "b")
                next("end")
            }
            scene("end") { dialogue("n", "z") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        assertEquals(1, runs)
        engine.advance()
        engine.advance()
        engine.tick(16)
        engine.tick(16)
        assertEquals(1, runs)
    }

    @Test
    fun chooseRequiresExhaustedDialogueAndIsOnceOnly() {
        val affection = varInt("affection")
        val script = game {
            scene("start") {
                dialogue("n", "pick")
                choice {
                    option("A", "a") { add(affection, 1) }
                    option("B", "b") { add(affection, 5) }
                }
            }
            scene("a") {
                dialogue("n", "A path.")
                choice { option("again", "b") { add(affection, 100) } }
            }
            scene("b") { dialogue("n", "B path.") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()

        engine.choose(0)
        assertEquals("start", engine.state.value.currentScene)

        engine.advance()
        engine.choose(0)
        assertEquals("a", engine.state.value.currentScene)
        assertEquals(1, varsOf(engine)[affection])

        engine.choose(0)
        assertEquals("a", engine.state.value.currentScene)
        assertEquals(1, varsOf(engine)[affection])
    }

    @Test
    fun enabledOptionsFilterChoices() {
        val affection = varInt("affection")
        val script = game {
            scene("start") {
                dialogue("n", "x")
                choice {
                    option("locked", "end", enabledWhen = { intGe(affection, 5) })
                    option("open", "end")
                }
            }
            scene("end") { dialogue("n", "end") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()
        assertEquals(listOf("open"), engine.frame.value.choices)
    }

    @Test
    fun audioCuesFireOnSceneEnter() {
        val script = game {
            scene("start") {
                bgm("theme.mp3")
                sfx("click.wav")
                dialogue("n", "hi")
            }
        }
        val audio = FakeAudioManager()
        val engine = GameEngine(script, audio)
        engine.start()
        assertEquals(listOf("theme.mp3" to true), audio.bgm)
        assertEquals(listOf("click.wav"), audio.sfx)
    }

    @Test
    fun visibleSpritesArePopulatedWithAttributes() {
        val script = game {
            scene("start") {
                show("hero", at = Position.Left, scale = 0.9f, layer = 2, bias = -0.3f)
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val sprite = engine.state.value.visibleSprites.single()
        assertEquals("hero", sprite.id)
        assertEquals(Position.Left, sprite.position)
        assertEquals(0.9f, sprite.scale)
        assertEquals(2, sprite.layer)
        assertEquals(-0.3f, sprite.bias)
    }

    @Test
    fun reachingEndFinishesAndRestartResets() {
        val affection = varInt("affection")
        val script = game {
            scene("start") {
                onEnter { vars[affection] = 4 }
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        assertEquals(4, varsOf(engine)[affection])
        engine.advance()
        assertTrue(engine.finished.value)
        engine.restart()
        assertFalse(engine.finished.value)
        assertEquals("start", engine.state.value.currentScene)
        assertEquals(4, varsOf(engine)[affection])
    }

    @Test
    fun unknownSceneDoesNotCrash() {
        val script = SceneScript()
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()
        engine.tick(16)
        assertEquals("start", engine.state.value.currentScene)
    }

    @Test
    fun historyRecordsSceneVisits() {
        val script = game {
            scene("start") { dialogue("n", "hi"); next("end") }
            scene("end") { dialogue("n", "bye") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()
        assertEquals(listOf("start", "end"), engine.state.value.history)
    }
}
