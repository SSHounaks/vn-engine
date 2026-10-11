package com.vnengine.engine

/** Typed, compile-time-checked variable key. */
data class VarKey<T>(val name: String, val default: T)

fun varInt(name: String, default: Int = 0): VarKey<Int> = VarKey(name, default)
fun varBool(name: String, default: Boolean = false): VarKey<Boolean> = VarKey(name, default)
fun varString(name: String, default: String = ""): VarKey<String> = VarKey(name, default)

/**
 * Typed mood/variable store shared across scenes. Reads may be used freely in
 * scene/dialogue blocks (they run at 60 Hz frame rebuilds). Mutations happen
 * only inside [com.vnengine.dsl.ChoiceOption] effects and scene `onEnter`
 * blocks, which the engine applies exactly once.
 */
class GameVars {
    private val store = mutableMapOf<String, Any?>()

    operator fun <T> get(key: VarKey<T>): T = store[key.name] as? T ?: key.default
    operator fun <T> set(key: VarKey<T>, value: T) { store[key.name] = value as? T ?: value }

    internal fun storeSet(name: String, value: Any?) {
        store[name] = value
    }

    fun add(key: VarKey<Int>, delta: Int): Int {
        val n = get(key) + delta
        store[key.name] = n
        return n
    }

    fun toSaveMap(): Map<String, String> = store.mapValues { (_, v) ->
        when (v) {
            is Boolean -> "b:$v"
            is Number -> "n:$v"
            else -> "s:$v"
        }
    }

    fun loadFrom(map: Map<String, String>) {
        store.clear()
        map.forEach { (k, v) ->
            store[k] = when {
                v.startsWith("b:") -> v.removePrefix("b:").toBooleanStrictOrNull() ?: v.removePrefix("b:")
                v.startsWith("n:") -> v.removePrefix("n:").toIntOrNull() ?: v.removePrefix("n:")
                v.startsWith("s:") -> v.removePrefix("s:")
                else -> v
            }
        }
    }
}
