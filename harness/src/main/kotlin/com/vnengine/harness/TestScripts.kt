package com.vnengine.harness

import com.vnengine.dsl.Position
import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.game
import com.vnengine.dsl.intGe
import com.vnengine.engine.varBool
import com.vnengine.engine.varInt
import com.vnengine.engine.varString

/** Reusable scripts that exercise the engine's features for tests and fuzzing. */
object TestScripts {
    val affection = varInt("affection")
    val met = varBool("met")
    val playerName = varString("playerName", "traveler")

    /** Two dialogue lines, then a plain `next` transition to a terminal scene. */
    fun linear(): SceneScript = game {
        scene("start") {
            background("night_sky")
            dialogue("narrator", "Line one.")
            dialogue("narrator", "Line two.")
            next("end")
        }
        scene("end") {
            dialogue("narrator", "Done.")
        }
    }

    /** A branching scene with a mood-gated option and effectful choices. */
    fun branching(): SceneScript = game {
        scene("start") {
            background("night_sky")
            show("hero", at = Position.Center)
            dialogue("narrator", "Choose.")
            choice {
                option("Warm", "warm") { add(affection, 2) }
                option("Cold", "cold") { add(affection, -1) }
                option("Secret", "secret", enabledWhen = { intGe(affection, 5) })
            }
        }
        scene("warm") { dialogue("narrator", "Warm."); next("end") }
        scene("cold") { dialogue("narrator", "Cold."); next("end") }
        scene("secret") { dialogue("narrator", "Secret."); next("end") }
        scene("end") {
            onEnter { vars[met] = true }
            dialogue("narrator", "The end.")
        }
    }

    /** A scene whose `next` points at a scene that does not exist. */
    fun dangling(): SceneScript = game {
        scene("start") { next("missing") }
    }

    /** Two scenes that `next` each other forever with no player input. */
    fun autoCycle(): SceneScript = game {
        scene("start") { next("b") }
        scene("b") { next("start") }
    }

    /** A scene nobody can reach from "start". */
    fun unreachable(): SceneScript = game {
        scene("start") { dialogue("narrator", "Hello.") }
        scene("orphan") { dialogue("narrator", "Nobody gets here.") }
    }
}
