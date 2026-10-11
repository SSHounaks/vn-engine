package com.vnengine.harness

import com.vnengine.dsl.SceneScript
import kotlin.random.Random

/**
 * Random-walks a script, taking random choices and advancing dialogue, to
 * surface crashes, stalls, and dead ends. Deterministic for a given [seed].
 */
class FuzzWalker(
    private val script: SceneScript,
    private val seed: Int = 1,
    private val maxSteps: Int = 500
) {
    data class Result(
        val steps: Int,
        val finished: Boolean,
        val visitedScenes: Set<String>,
        val choicesTaken: Int,
        val stuck: Boolean
    ) {
        fun describe(): String =
            "steps=$steps finished=$finished choices=$choicesTaken stuck=$stuck visited=$visitedScenes"
    }

    fun run(): Result {
        val driver = GameDriver(script)
        driver.start()
        val rng = Random(seed)
        val visited = linkedSetOf<String>()
        var choicesTaken = 0
        var steps = 0
        var stuck = false
        visited += driver.scene

        while (steps < maxSteps && !driver.finished) {
            val before = driver.scene to driver.dialogueIndex
            val choices = driver.choices
            if (choices.isNotEmpty()) {
                driver.choose(rng.nextInt(choices.size))
                choicesTaken++
            } else {
                driver.advance()
            }
            driver.tick(16L)
            visited += driver.scene
            steps++
            if (!driver.finished && (driver.scene to driver.dialogueIndex) == before) {
                stuck = true
                break
            }
        }

        return Result(steps, driver.finished, visited, choicesTaken, stuck)
    }
}
