package com.vnengine.effect

import com.vnengine.dsl.Position
import com.vnengine.dsl.SpriteRef
import com.vnengine.dsl.TimelineEvent
import com.vnengine.dsl.TransitionType
import com.vnengine.engine.SceneVisual
import com.vnengine.engine.SpriteVisual
import kotlin.math.min
import kotlin.math.sin

data class ScheduledEffect(
    val startMs: Long,
    val durationMs: Long,
    val kind: EffectKind
)

sealed class EffectKind {
    data class Transition(val type: TransitionType) : EffectKind()
    data class Move(val sprite: String, val from: Position, val to: Position) : EffectKind()
    data class Shake(val sprite: String, val intensity: Float) : EffectKind()
    data class Flash(val color: Int) : EffectKind()
    data class Zoom(val sprite: String, val from: Float, val to: Float) : EffectKind()
    data class Anim(val sprite: String, val group: String, val loop: Boolean? = null) : EffectKind()
    object Wait : EffectKind()
}

class EffectRunner {
    val scheduled = mutableListOf<ScheduledEffect>()
    var elapsedMs: Long = 0
        private set

    fun build(baseSprites: List<SpriteRef>, events: List<TimelineEvent>) {
        scheduled.clear()
        elapsedMs = 0
        scheduleSequential(baseSprites, events, 0L)
    }

    fun reset(elapsedMs: Long = 0) {
        this.elapsedMs = elapsedMs
    }

    fun tick(deltaMs: Long) {
        elapsedMs += deltaMs
    }

    fun isComplete(): Boolean = scheduled.all { it.startMs + it.durationMs <= elapsedMs }

    fun computeVisuals(baseSprites: List<SpriteRef>): SceneVisual {
        var transitionAlpha = 0f
        var flashAlpha = 0f
        var flashColor = 0xFFFFFFFF.toInt()

        val visuals = baseSprites.map { sprite ->
            SpriteVisual(
                id = sprite.id,
                position = sprite.position
            )
        }.toMutableList()

        val byId = visuals.associateBy { it.id }.toMutableMap()

        scheduled.forEach { effect ->
            val progress = progressOf(effect)
            if (progress <= 0f) return@forEach

            when (val kind = effect.kind) {
                is EffectKind.Transition -> {
                    transitionAlpha = when (kind.type) {
                        TransitionType.Fade, TransitionType.Dissolve -> {
                            if (effect.durationMs == 0L) 0f else (1f - progress / 0.2f).coerceIn(0f, 1f)
                        }
                        TransitionType.Slide -> 0f
                    }
                }
                is EffectKind.Flash -> {
                    val alpha = (1f - progress).coerceIn(0f, 1f) * 0.6f
                    if (alpha > flashAlpha) {
                        flashAlpha = alpha
                        flashColor = kind.color
                    }
                }
                is EffectKind.Move -> {
                    val v = byId[kind.sprite] ?: return@forEach
                    val fromW = positionOffset(kind.from)
                    val toW = positionOffset(kind.to)
                    val eased = easeInOut(progress)
                    val x = fromW + (toW - fromW) * eased
                    byId[kind.sprite] = v.copy(xOffset = x * 100f)
                }
                is EffectKind.Shake -> {
                    val v = byId[kind.sprite] ?: return@forEach
                    val damp = (1f - progress).coerceIn(0f, 1f)
                    val amp = kind.intensity * damp
                    byId[kind.sprite] = v.copy(
                        xOffset = v.xOffset + sin(elapsedMs * 0.8f) * amp,
                        yOffset = sin(elapsedMs * 1.3f) * amp * 0.7f,
                        rotation = sin(elapsedMs * 1.1f) * amp * 0.02f
                    )
                }
                is EffectKind.Zoom -> {
                    val v = byId[kind.sprite] ?: return@forEach
                    val eased = easeInOut(progress)
                    byId[kind.sprite] = v.copy(scale = kind.from + (kind.to - kind.from) * eased)
                }
                is EffectKind.Anim -> Unit
                EffectKind.Wait -> Unit
            }
        }

        return SceneVisual(
            sprites = visuals.map { byId[it.id] ?: it },
            flashAlpha = flashAlpha,
            flashColor = flashColor,
            transitionAlpha = transitionAlpha
        )
    }

    private fun scheduleSequential(baseSprites: List<SpriteRef>, events: List<TimelineEvent>, startAt: Long): Long {
        var cursor = startAt
        events.forEach { event ->
            when (event) {
                is TimelineEvent.Transition -> {
                    scheduled.add(
                        ScheduledEffect(cursor, (event.durationSeconds * 1000).toLong(), EffectKind.Transition(event.type))
                    )
                    cursor += (event.durationSeconds * 1000).toLong()
                }
                is TimelineEvent.Move -> {
                    val from = baseSprites.firstOrNull { it.id == event.sprite }?.position ?: Position.Center
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Move(event.sprite, from, event.to)))
                    cursor += event.durationMs
                }
                is TimelineEvent.Shake -> {
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Shake(event.sprite, event.intensity)))
                    cursor += event.durationMs
                }
                is TimelineEvent.Flash -> {
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Flash(event.color)))
                    cursor += event.durationMs
                }
                is TimelineEvent.Zoom -> {
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Zoom(event.sprite, 1f, event.to)))
                    cursor += event.durationMs
                }
                is TimelineEvent.Anim -> {
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Anim(event.sprite, event.group, event.loop)))
                    cursor += event.durationMs
                }
                is TimelineEvent.Wait -> {
                    scheduled.add(ScheduledEffect(cursor, event.durationMs, EffectKind.Wait))
                    cursor += event.durationMs
                }
                is TimelineEvent.Parallel -> {
                    var maxEnd = 0L
                    event.events.forEach { child ->
                        val end = scheduleSequential(baseSprites, listOf(child), cursor)
                        maxEnd = maxOf(maxEnd, end)
                    }
                    cursor = maxEnd
                }
            }
        }
        return cursor
    }

    private fun progressOf(effect: ScheduledEffect): Float {
        if (effect.durationMs == 0L) return 1f
        val done = elapsedMs - effect.startMs
        return (done.toFloat() / effect.durationMs.toFloat()).coerceIn(0f, 1f)
    }

    private fun easeInOut(t: Float): Float =
        if (t < 0.5f) 2f * t * t else 1f - ((-2f * t + 2f) * (-2f * t + 2f)) / 2f

    private fun positionOffset(p: Position): Float = when (p) {
        Position.Left -> -0.28f
        Position.Center -> 0f
        Position.Right -> 0.28f
    }
}
