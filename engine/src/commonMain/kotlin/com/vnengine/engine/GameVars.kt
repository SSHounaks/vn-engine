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
 *
 * An optional [observer] may be supplied to record every (name, type) access;
 * the test harness uses it to lint scripts for name/type collisions.
 */
class GameVars(
    private val observer: ((String, String) -> Unit)? = null
) {
    private val store = mutableMapOf<String, Any?>()

    operator fun <T> get(key: VarKey<T>): T {
        observer?.invoke(key.name, typeName(key.default))
        return store[key.name] as? T ?: key.default
    }

    operator fun <T> set(key: VarKey<T>, value: T) {
        observer?.invoke(key.name, typeName(value))
        store[key.name] = value
    }

    internal fun storeSet(name: String, value: Any?) {
        observer?.invoke(name, typeName(value))
        store[name] = value
    }

    fun add(key: VarKey<Int>, delta: Int): Int {
        observer?.invoke(key.name, "Int")
        val n = get(key) + delta
        store[key.name] = n
        return n
    }

    fun toSaveMap(): Map<String, String> = store.mapValues { (_, v) -> encode(v) }

    fun loadFrom(map: Map<String, String>) {
        store.clear()
        map.forEach { (k, v) -> store[k] = decode(v) }
    }

    /** Tagged, type-preserving encoding so Int/Long/Float/Double round-trip exactly. */
    private fun encode(v: Any?): String = when (v) {
        null -> "z:"
        is Boolean -> "b:$v"
        is Int -> "i:$v"
        is Long -> "l:$v"
        is Float -> "f:$v"
        is Double -> "d:$v"
        is String -> "s:$v"
        else -> "s:$v"
    }

    private fun decode(v: String): Any? = when {
        v.startsWith("z:") -> null
        v.startsWith("b:") -> v.removePrefix("b:").toBooleanStrictOrNull() ?: false
        v.startsWith("i:") -> v.removePrefix("i:").toIntOrNull() ?: 0
        v.startsWith("l:") -> v.removePrefix("l:").toLongOrNull() ?: 0L
        v.startsWith("f:") -> v.removePrefix("f:").toFloatOrNull() ?: 0f
        v.startsWith("d:") -> v.removePrefix("d:").toDoubleOrNull() ?: 0.0
        v.startsWith("s:") -> v.removePrefix("s:")
        else -> v
    }

    private fun typeName(v: Any?): String = when (v) {
        null -> "Null"
        is Int -> "Int"
        is Long -> "Long"
        is Boolean -> "Boolean"
        is Float -> "Float"
        is Double -> "Double"
        is String -> "String"
        else -> v::class.simpleName ?: "Unknown"
    }
}
