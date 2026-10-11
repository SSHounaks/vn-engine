package com.vnengine.harness

import com.vnengine.dsl.game
import com.vnengine.dsl.intGe
import com.vnengine.engine.varInt
import com.vnengine.engine.varString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameDriverTest {
    @Test
    fun walksDialogueAndTransitions() {
        val driver = GameDriver(TestScripts.linear()).start()
        assertEquals("start", driver.scene)
        assertEquals("Line one.", driver.line)
        driver.advanceDialogue()
        assertEquals("end", driver.scene)
    }

    @Test
    fun choicesApplyEffectsAndExposeValues() {
        val driver = GameDriver(TestScripts.branching()).start()
        driver.advance()
        assertTrue(driver.choices.isNotEmpty())
        driver.choose("Warm")
        assertEquals("warm", driver.scene)
        assertEquals(2, driver.intValue(TestScripts.affection))
    }

    @Test
    fun unknownChoiceLabelFails() {
        val driver = GameDriver(TestScripts.branching()).start()
        driver.advance()
        assertFailsWith<IllegalArgumentException> { driver.choose("Nope") }
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val driver = GameDriver(TestScripts.linear()).start()
        driver.advance()
        val json = driver.save()
        driver.advanceDialogue()
        assertEquals("end", driver.scene)
        assertTrue(driver.load(json))
        assertEquals(1, driver.dialogueIndex)
    }
}

class SceneGraphTest {
    @Test
    fun linearGraphIsHealthy() {
        val g = SceneGraph.analyze(TestScripts.linear())
        assertTrue(g.isHealthy())
        assertEquals(setOf("start", "end"), g.scenes)
        assertEquals(setOf("end"), g.outgoing("start"))
    }

    @Test
    fun detectsDanglingAndUnreachable() {
        val dangling = SceneGraph.analyze(TestScripts.dangling())
        assertFalse(dangling.isHealthy())
        assertTrue("missing" in dangling.dangling)

        val orphan = SceneGraph.analyze(TestScripts.unreachable())
        assertTrue("orphan" in orphan.unreachable)
    }

    @Test
    fun detectsAutoAdvanceCycle() {
        val g = SceneGraph.analyze(TestScripts.autoCycle())
        assertTrue(g.autoCycles.isNotEmpty())
        assertFalse(g.isHealthy())
        assertTrue(g.describe().contains("Auto-advance cycles"))
    }
}

class VarKeyLinterTest {
    @Test
    fun cleanScriptHasNoConflicts() {
        val report = VarKeyLinter.lint(TestScripts.branching())
        assertTrue(report.isClean, report.describe())
        assertTrue("affection" in report.accesses)
    }

    @Test
    fun detectsNameReusedWithDifferentTypes() {
        val script = game {
            scene("start") {
                onEnter { vars[varString("score")] = "high" }
                dialogue("n", "hi")
                choice {
                    option("go", "end", enabledWhen = { intGe(varInt("score"), 5) })
                }
            }
            scene("end") { dialogue("n", "bye") }
        }
        val report = VarKeyLinter.lint(script)
        assertFalse(report.isClean)
        assertEquals(setOf("Int", "String"), report.conflicts["score"])
    }
}

class FuzzWalkerTest {
    @Test
    fun completesBranchingScript() {
        val result = FuzzWalker(TestScripts.branching(), seed = 7).run()
        assertTrue(result.finished, result.describe())
        assertTrue("end" in result.visitedScenes)
    }

    @Test
    fun reportsDeadEndOnDanglingEdge() {
        val result = FuzzWalker(TestScripts.dangling(), seed = 1).run()
        assertFalse(result.finished)
        assertTrue(result.stuck, result.describe())
    }

    @Test
    fun autoCycleTerminatesAtStepLimit() {
        val result = FuzzWalker(TestScripts.autoCycle(), seed = 3, maxSteps = 20).run()
        assertFalse(result.finished)
        assertEquals(20, result.steps)
    }
}
