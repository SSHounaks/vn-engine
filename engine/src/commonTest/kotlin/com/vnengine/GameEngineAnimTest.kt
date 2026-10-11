package com.vnengine

import com.vnengine.anim.AnimDef
import com.vnengine.anim.FrameDef
import com.vnengine.anim.Pack
import com.vnengine.dsl.Position
import com.vnengine.dsl.game
import com.vnengine.engine.GameEngine
import com.vnengine.save.SaveSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineAnimTest {
    private val pack = Pack(
        name = "t",
        animations = mapOf(
            "idle" to AnimDef(loop = true, frames = listOf(FrameDef("a", 100), FrameDef("b", 100))),
            "walk" to AnimDef(loop = true, frames = listOf(FrameDef("w", 100)))
        )
    )

    @Test
    fun animatorDrivesSpriteFrames() {
        val script = game {
            scene("start") {
                show("hero", anim = "idle")
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager(), pack)
        engine.start()
        assertEquals("a", engine.visuals.value.sprites.first().frameSrc)

        engine.tick(100)
        assertEquals("b", engine.visuals.value.sprites.first().frameSrc)
    }

    @Test
    fun timelineAnimEventSwitchesGroup() {
        val script = game {
            scene("start") {
                show("hero", anim = "idle")
                timeline { play("hero", "walk", 0.1) }
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager(), pack)
        engine.start()
        assertEquals("a", engine.visuals.value.sprites.first().frameSrc)

        engine.tick(16)
        assertEquals("w", engine.visuals.value.sprites.first().frameSrc)
    }

    @Test
    fun unknownAnimationGroupLeavesStaticFrame() {
        val script = game {
            scene("start") {
                show("hero", anim = "missing")
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager(), pack)
        engine.start()
        assertNull(engine.visuals.value.sprites.first().frameSrc)
    }

    @Test
    fun withoutPackSpritesHaveNoFrame() {
        val script = game {
            scene("start") {
                show("hero")
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        assertNull(engine.visuals.value.sprites.first().frameSrc)
    }

    @Test
    fun voiceCueAndBackgroundReachFrame() {
        val script = game {
            scene("start") {
                background("night_sky")
                voice("line1.ogg")
                dialogue("narrator", "hi")
            }
        }
        val audio = FakeAudioManager()
        val engine = GameEngine(script, audio)
        engine.start()
        assertEquals("night_sky", engine.frame.value.background)
        assertEquals(listOf("line1.ogg"), audio.voice)
    }

    @Test
    fun makeChoiceJumpsDirectly() {
        val script = game {
            scene("start") { dialogue("n", "hi") }
            scene("end") { dialogue("n", "bye") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.makeChoice("end")
        assertEquals("end", engine.state.value.currentScene)
    }

    @Test
    fun advanceFallsThroughToNextWhenAllOptionsDisabled() {
        val script = game {
            scene("start") {
                dialogue("n", "only")
                next("end")
                choice { option("locked", "end", enabledWhen = { false }) }
            }
            scene("end") { dialogue("n", "done") }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()
        assertEquals("end", engine.state.value.currentScene)
        assertEquals("done", engine.frame.value.dialogue?.text)
    }

    @Test
    fun onEndRunsWhenSceneTerminates() {
        var ended = false
        val script = game {
            scene("start") {
                onEnd { ended = true }
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.advance()
        assertTrue(ended)
        assertTrue(engine.finished.value)
    }

    @Test
    fun loadStateRebuildsAndCompletesTimeline() {
        val script = game {
            scene("start") {
                show("hero", at = Position.Center)
                timeline { zoom("hero", 2f, 0.2) }
                dialogue("n", "hi")
            }
        }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        val saved = SaveSystem().save(engine.state.value)
        engine.tick(1000)

        val restored = SaveSystem().load(saved)!!
        engine.loadState(restored)
        assertFalse(engine.finished.value)

        engine.tick(100)
        assertTrue(engine.visuals.value.sprites.first().scale > 1f)
        engine.tick(200)
        assertEquals(1f, engine.visuals.value.sprites.first().scale)
    }
}
