package com.vnengine.dsl

import com.vnengine.engine.GameVars
import com.vnengine.engine.VarKey

/** Declarative, once-only side effects. Serialized-friendly except for [Run]. */
sealed class VarEffect {
    data class Add(val key: VarKey<Int>, val delta: Int) : VarEffect()
    data class Set<T>(val key: VarKey<T>, val value: T) : VarEffect()
    data class Flag(val key: VarKey<Boolean>, val value: Boolean = true) : VarEffect()
    data class Run(val block: (GameVars) -> Unit) : VarEffect()

    fun apply(vars: GameVars) {
        when (this) {
            is Add -> vars.add(key, delta)
            is Set<*> -> setRaw(key, value, vars)
            is Flag -> vars[key] = value
            is Run -> block(vars)
        }
    }

    private fun setRaw(key: VarKey<*>, value: Any?, vars: GameVars) {
        vars.storeSet(key.name, value)
    }
}

/** Interpreter for a list of effects; executes each exactly once, in order. */
fun applyEffects(effects: List<VarEffect>, vars: GameVars) {
    effects.forEach { it.apply(vars) }
}

/** Effect builders available inside `option { ... }`. */
class OptionsBuilder {
    val effects = mutableListOf<VarEffect>()

    fun add(key: VarKey<Int>, delta: Int) {
        effects.add(VarEffect.Add(key, delta))
    }

    fun set(key: VarKey<Int>, value: Int) {
        effects.add(VarEffect.Set(key, value))
    }

    fun set(key: VarKey<Boolean>, value: Boolean) {
        effects.add(VarEffect.Set(key, value))
    }

    fun set(key: VarKey<String>, value: String) {
        effects.add(VarEffect.Set(key, value))
    }

    fun flag(key: VarKey<Boolean>) {
        effects.add(VarEffect.Flag(key, true))
    }

    fun unflag(key: VarKey<Boolean>) {
        effects.add(VarEffect.Flag(key, false))
    }

    /** Escape hatch for logic that isn't declarable. */
    fun run(block: (GameVars) -> Unit) {
        effects.add(VarEffect.Run(block))
    }
}

/* ------------------------------ Predicates ------------------------------ */

fun GameVars.flag(key: VarKey<Boolean>): Boolean = this[key]
fun GameVars.flagSet(key: VarKey<Boolean>): Boolean = this[key]
fun GameVars.unSet(key: VarKey<Boolean>): Boolean = !this[key]

fun GameVars.intGe(key: VarKey<Int>, value: Int): Boolean = this[key] >= value
fun GameVars.intLe(key: VarKey<Int>, value: Int): Boolean = this[key] <= value
fun GameVars.intEq(key: VarKey<Int>, value: Int): Boolean = this[key] == value
fun GameVars.between(key: VarKey<Int>, lower: Int, upper: Int): Boolean = this[key] in lower..upper

/** Encompassing logical combinators: `not { ... }`, `and(a, b)`, `or(a, b)`. */
fun GameVars.not(cond: GameVars.() -> Boolean): Boolean = !cond(this)
fun GameVars.and(a: GameVars.() -> Boolean, b: GameVars.() -> Boolean): Boolean = a(this) && b(this)
fun GameVars.or(a: GameVars.() -> Boolean, b: GameVars.() -> Boolean): Boolean = a(this) || b(this)
fun GameVars.allOf(vararg conds: GameVars.() -> Boolean): Boolean = conds.all { it(this) }
fun GameVars.anyOf(vararg conds: GameVars.() -> Boolean): Boolean = conds.any { it(this) }
