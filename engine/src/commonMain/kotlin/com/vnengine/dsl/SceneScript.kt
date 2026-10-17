package com.vnengine.dsl

import com.vnengine.engine.GameVars

class SceneScript {
    val scenes = mutableMapOf<String, SceneBuilder.() -> Unit>()

    fun scene(name: String, block: SceneBuilder.() -> Unit) {
        scenes[name] = block
    }
}

class SceneBuilder {
    val timeline = TimelineBuilder()
    val sprites = mutableListOf<SpriteRef>()
    val dialogues = mutableListOf<DialogueLine>()
    val audioCues = mutableListOf<AudioCue>()
    var backgroundColor: String? = null
    var nextSceneName: String? = null
    var onSceneEnd: (() -> Unit)? = null
    var vars: GameVars = GameVars()
    val enterActions = mutableListOf<() -> Unit>()
    var optionGroup: ChoiceBuilder? = null

    fun background(name: String) {
        backgroundColor = name
    }

    fun next(sceneName: String) {
        nextSceneName = sceneName
    }

    fun onEnd(block: () -> Unit) {
        onSceneEnd = block
    }

    fun timeline(block: TimelineBuilder.() -> Unit) {
        timeline.apply(block)
    }

    fun show(
        sprite: String,
        at: Position = Position.Center,
        anim: String? = null,
        loop: Boolean? = null,
        scale: Float = 1f,
        layer: Int = 0,
        bias: Float? = null
    ) {
        sprites.add(SpriteRef(sprite, at, anim, loop, scale, layer, bias))
    }

    /**
     * Shows a Live2D model. [model] is either a model directory or a path to a
     * `.model3.json` file relative to the asset root. [motion] and [expression]
     * optionally start an animation, Ren'Py-style.
     */
    fun live2d(
        model: String,
        at: Position = Position.Center,
        motion: String? = null,
        expression: String? = null,
        scale: Float = 1f,
        layer: Int = 0,
        bias: Float? = null
    ) {
        val id = com.vnengine.live2d.Live2DPaths.modelId(model)
        sprites.add(
            SpriteRef(
                id = id,
                position = at,
                scale = scale,
                layer = layer,
                bias = bias,
                live2dModel = model,
                motion = motion,
                expression = expression
            )
        )
    }

    fun hide(sprite: String) {
        sprites.removeAll { it.id == sprite }
    }

    fun bgm(path: String, loop: Boolean = true) {
        audioCues.add(AudioCue.Bgm(path, loop))
    }

    fun sfx(path: String) {
        audioCues.add(AudioCue.Sfx(path))
    }

    fun voice(path: String) {
        audioCues.add(AudioCue.Voice(path))
    }

    fun dialogue(speaker: String? = null, text: String) {
        dialogues.add(DialogueLine(speaker, text))
    }

    /** Runs once when the scene is entered (not on frame rebuilds). Use for one-time vars setup. */
    fun onEnter(action: () -> Unit) {
        enterActions.add(action)
    }

    /** Presents a branching menu once this scene's dialogue is exhausted. */
    fun choice(block: ChoiceBuilder.() -> Unit) {
        optionGroup = ChoiceBuilder().apply(block)
    }
}

class ChoiceBuilder {
    val options = mutableListOf<ChoiceOption>()

    fun option(
        label: String,
        target: String,
        enabledWhen: GameVars.() -> Boolean = { true },
        effects: OptionsBuilder.() -> Unit = {}
    ) {
        options.add(ChoiceOption(label, target, enabledWhen, OptionsBuilder().apply(effects).effects))
    }
}

data class ChoiceOption(
    val label: String,
    val target: String,
    val enabledWhen: GameVars.() -> Boolean,
    val effects: List<VarEffect>
)

class TimelineBuilder {
    val events = mutableListOf<TimelineEvent>()

    fun fade(duration: Double = 0.5) {
        events.add(TimelineEvent.Transition(TransitionType.Fade, duration))
    }

    fun dissolve(duration: Double = 0.8) {
        events.add(TimelineEvent.Transition(TransitionType.Dissolve, duration))
    }

    fun slide(duration: Double = 0.6) {
        events.add(TimelineEvent.Transition(TransitionType.Slide, duration))
    }

    fun move(sprite: String, to: Position, duration: Double = 1.0) {
        events.add(TimelineEvent.Move(sprite, to, toMillis(duration)))
    }

    fun motion(sprite: String, name: String) {
        events.add(TimelineEvent.Motion(sprite, name))
    }

    fun expression(sprite: String, name: String) {
        events.add(TimelineEvent.Expression(sprite, name))
    }

    fun shake(sprite: String, intensity: Float = 8f, duration: Double = 0.5) {
        events.add(TimelineEvent.Shake(sprite, intensity, toMillis(duration)))
    }

    fun flash(color: Int = 0xFFFFFFFF.toInt(), duration: Double = 0.2) {
        events.add(TimelineEvent.Flash(color, toMillis(duration)))
    }

    fun zoom(sprite: String, to: Float = 1.2f, duration: Double = 0.8) {
        events.add(TimelineEvent.Zoom(sprite, to, toMillis(duration)))
    }

    fun wait(duration: Double = 0.5) {
        events.add(TimelineEvent.Wait(toMillis(duration)))
    }

    /** Switch sprite animation group at this point on the timeline. */
    fun play(sprite: String, group: String, duration: Double = 1.0, loop: Boolean? = null) {
        events.add(TimelineEvent.Anim(sprite, group, toMillis(duration), loop))
    }

    fun parallel(block: TimelineBuilder.() -> Unit) {
        val nested = TimelineBuilder()
        nested.apply(block)
        events.add(TimelineEvent.Parallel(nested.events))
    }

    private fun toMillis(seconds: Double): Long = (seconds * 1000).toLong()
}

data class SpriteRef(
    val id: String,
    val position: Position,
    val anim: String? = null,
    val loop: Boolean? = null,
    val scale: Float = 1f,
    val layer: Int = 0,
    val bias: Float? = null,
    val live2dModel: String? = null,
    val motion: String? = null,
    val expression: String? = null
) {
    val isLive2D: Boolean get() = live2dModel != null
}
data class DialogueLine(val speaker: String?, val text: String)

sealed class AudioCue {
    data class Bgm(val path: String, val loop: Boolean) : AudioCue()
    data class Sfx(val path: String) : AudioCue()
    data class Voice(val path: String) : AudioCue()
}

enum class TransitionType { Fade, Dissolve, Slide }

sealed class TimelineEvent {
    data class Transition(val type: TransitionType, val durationSeconds: Double) : TimelineEvent()
    data class Move(val sprite: String, val to: Position, val durationMs: Long) : TimelineEvent()
    data class Shake(val sprite: String, val intensity: Float, val durationMs: Long) : TimelineEvent()
    data class Flash(val color: Int, val durationMs: Long) : TimelineEvent()
    data class Zoom(val sprite: String, val to: Float, val durationMs: Long) : TimelineEvent()
    data class Anim(val sprite: String, val group: String, val durationMs: Long, val loop: Boolean? = null) : TimelineEvent()
    data class Motion(val sprite: String, val name: String) : TimelineEvent()
    data class Expression(val sprite: String, val name: String) : TimelineEvent()
    data class Wait(val durationMs: Long) : TimelineEvent()
    data class Parallel(val events: List<TimelineEvent>) : TimelineEvent()
}

enum class Position { Left, Center, Right }

fun game(block: SceneScript.() -> Unit): SceneScript {
    return SceneScript().apply(block)
}
