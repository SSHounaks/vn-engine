package com.vnengine

import com.vnengine.dsl.AudioCue
import com.vnengine.dsl.Position
import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.TimelineEvent
import com.vnengine.dsl.TransitionType
import com.vnengine.dsl.and
import com.vnengine.dsl.anyOf
import com.vnengine.dsl.applyEffects
import com.vnengine.dsl.between
import com.vnengine.dsl.flag
import com.vnengine.dsl.flagSet
import com.vnengine.dsl.game
import com.vnengine.dsl.intEq
import com.vnengine.dsl.intGe
import com.vnengine.dsl.intLe
import com.vnengine.dsl.or
import com.vnengine.dsl.unSet
import com.vnengine.engine.GameVars
import com.vnengine.engine.varBool
import com.vnengine.engine.varInt
import com.vnengine.engine.varString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DSLTest {
    @Test
    fun timelineMillisConversionAndEvents() {
        val b = SceneBuilder()
        b.timeline {
            fade()
            dissolve(1.0)
            slide(0.4)
            move("hero", Position.Right, 1.5)
            shake("hero", 12f, 0.25)
            flash(0xFF0000, 0.1)
            zoom("hero", 1.4f, 0.5)
            wait(0.2)
            play("hero", "walk", 0.9, loop = true)
        }
        val events = b.timeline.events
        assertEquals(TransitionType.Fade, (events[0] as TimelineEvent.Transition).type)
        assertEquals(0.5, (events[0] as TimelineEvent.Transition).durationSeconds)
        assertEquals(1500L, (events[3] as TimelineEvent.Move).durationMs)
        assertEquals(250L, (events[4] as TimelineEvent.Shake).durationMs)
        assertEquals(100L, (events[5] as TimelineEvent.Flash).durationMs)
        assertEquals(1.4f, (events[6] as TimelineEvent.Zoom).to)
        assertEquals(200L, (events[7] as TimelineEvent.Wait).durationMs)
        assertEquals("walk", (events[8] as TimelineEvent.Anim).group)
    }

    @Test
    fun timelineParallelNestsEvents() {
        val b = SceneBuilder()
        b.timeline {
            parallel {
                wait(0.5)
                move("hero", Position.Left)
            }
        }
        val par = b.timeline.events.single() as TimelineEvent.Parallel
        assertEquals(2, par.events.size)
    }

    @Test
    fun sceneBuilderCollectsEverything() {
        val s = SceneBuilder()
        s.background("school")
        s.next("chapter2")
        s.onEnd { }
        s.show("hero", Position.Left, anim = "idle", scale = 0.8f, layer = 3, bias = -0.2f)
        s.show("villain")
        s.hide("villain")
        s.bgm("theme.mp3", loop = false)
        s.sfx("hit.wav")
        s.voice("line1.ogg")
        s.dialogue("Aru", "hello")
        s.dialogue(text = "narration")
        var entered = false
        s.onEnter { entered = true }

        assertEquals("school", s.backgroundColor)
        assertEquals("chapter2", s.nextSceneName)
        assertNotNull(s.onSceneEnd)
        assertEquals(1, s.sprites.size)
        assertEquals("hero", s.sprites[0].id)
        assertEquals(3, s.sprites[0].layer)
        assertEquals(3, s.audioCues.size)
        assertEquals(AudioCue.Sfx::class, s.audioCues[1]::class)
        assertEquals("Aru", s.dialogues[0].speaker)
        assertEquals(null, s.dialogues[1].speaker)
        s.enterActions.single().invoke()
        assertTrue(entered)
    }

    @Test
    fun choiceBuildsOptionsWithEffectsAndPredicates() {
        val score = varInt("score")
        val seen = varBool("seen")
        val name = varString("name")
        val received = varInt("received")

        val s = SceneBuilder()
        s.choice {
            option("gain", "a") {
                add(score, 10)
                set(score, 42)
                set(seen, true)
                set(name, "Zoe")
                flag(seen)
                unflag(seen)
                run { it[received] = 7 }
            }
            option("locked", "b", enabledWhen = { intGe(score, 100) })
        }

        val group = assertNotNull(s.optionGroup)
        assertEquals(2, group.options.size)

        val vars = GameVars()
        applyEffects(group.options[0].effects, vars)
        assertEquals(42, vars[score])
        assertEquals("Zoe", vars[name])
        assertEquals(7, vars[received])
        assertFalse(vars[seen])

        assertFalse(group.options[1].enabledWhen.invoke(vars))
    }

    @Test
    fun predicateCombinators() {
        val vars = GameVars()
        val n = varInt("n", 5)
        val f = varBool("f", true)
        val g = varBool("g", false)
        assertTrue(vars.flag(f))
        assertTrue(vars.flagSet(f))
        assertFalse(vars.unSet(f))
        assertTrue(vars.unSet(g))
        assertTrue(vars.intEq(n, 5))
        assertTrue(vars.and({ intGe(n, 1) }, { intLe(n, 9) }))
        assertTrue(vars.or({ intEq(n, 0) }, { between(n, 1, 9) }))
        assertTrue(vars.anyOf({ intEq(n, 0) }, { intEq(n, 5) }))
        assertFalse(vars.anyOf({ intEq(n, 0) }, { intEq(n, 1) }))
    }

    @Test
    fun gameRegistersNamedScenes() {
        val script = game {
            scene("a") { dialogue("n", "x") }
            scene("b") { dialogue("n", "y") }
        }
        assertEquals(setOf("a", "b"), script.scenes.keys)
    }
}
