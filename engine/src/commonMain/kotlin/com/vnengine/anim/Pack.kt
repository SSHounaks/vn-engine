package com.vnengine.anim

import kotlinx.serialization.Serializable

@Serializable
data class Pack(
    val name: String = "",
    val defaultAnimation: String = "idle",
    val animations: Map<String, AnimDef> = emptyMap()
)

@Serializable
data class AnimDef(
    val loop: Boolean = true,
    val frames: List<FrameDef> = emptyList()
) {
    val totalDurationMs: Long get() = frames.sumOf { it.durationMs }
}

@Serializable
data class FrameDef(
    val src: String = "",
    val t: Int = 100
) {
    val durationMs: Long get() = t.toLong()
}
