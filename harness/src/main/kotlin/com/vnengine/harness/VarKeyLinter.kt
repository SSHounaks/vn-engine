package com.vnengine.harness

import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.applyEffects
import com.vnengine.engine.GameVars

/**
 * Static-ish linter for script variables. It drives every scene block with an
 * instrumented [GameVars], recording each `(name, type)` access made by
 * dialogue text, choice predicates, choice effects, and `onEnter` actions.
 *
 * A single variable name accessed with more than one type is a conflict: at
 * runtime `GameVars.get` silently falls back to the key default, so these are
 * exactly the mistakes that would otherwise hide.
 */
object VarKeyLinter {
    data class Report(
        val accesses: Map<String, Set<String>>,
        val conflicts: Map<String, Set<String>>
    ) {
        val isClean: Boolean get() = conflicts.isEmpty()

        fun describe(): String =
            if (isClean) "No variable name/type conflicts (${accesses.size} vars)"
            else conflicts.entries.joinToString("\n") { (name, types) ->
                "VarKey '$name' accessed with conflicting types: $types"
            }
    }

    fun lint(script: SceneScript): Report {
        val seen = linkedMapOf<String, MutableSet<String>>()
        val vars = GameVars { name, type -> seen.getOrPut(name) { linkedSetOf() }.add(type) }

        script.scenes.values.forEach { block ->
            val builder = SceneBuilder()
            builder.vars = vars
            runCatching { block(builder) }
            builder.optionGroup?.options?.forEach { option ->
                runCatching { option.enabledWhen(vars) }
                runCatching { applyEffects(option.effects, vars) }
            }
            runCatching { builder.enterActions.forEach { it() } }
        }

        return Report(seen.mapValues { it.value.toSet() }, seen.filterValues { it.size > 1 }.mapValues { it.value.toSet() })
    }
}
