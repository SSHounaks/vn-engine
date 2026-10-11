package com.vnengine.engine

import com.vnengine.audio.AudioManager
import com.vnengine.audio.createAudioManager
import com.vnengine.dsl.AudioCue
import com.vnengine.dsl.applyEffects
import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.SceneScript
import com.vnengine.anim.Animator
import com.vnengine.anim.Pack
import com.vnengine.effect.EffectKind
import com.vnengine.effect.EffectRunner
import com.vnengine.effect.ScheduledEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameEngine(
    private val script: SceneScript,
    private val audio: AudioManager = createAudioManager(),
    private val pack: Pack? = null
) {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _frame = MutableStateFlow(SceneFrame(emptyList(), null))
    val frame: StateFlow<SceneFrame> = _frame.asStateFlow()

    private val _visuals = MutableStateFlow(SceneVisual(emptyList()))
    val visuals: StateFlow<SceneVisual> = _visuals.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val effects = EffectRunner()
    private val animators = mutableMapOf<String, Animator>()
    private val vars = GameVars()
    private val triggeredAnimEffects = mutableSetOf<ScheduledEffect>()
    private var lastAudioCueCount = 0
    private var sceneEffectsDone = true

    fun start() {
        loadScene("start")
    }

    fun advance() {
        val current = _state.value
        val builder = buildScene(current.currentScene) ?: return

        val nextIndex = current.dialogueIndex + 1
        if (nextIndex < builder.dialogues.size) {
            _state.value = current.copy(dialogueIndex = nextIndex)
            updateFrame()
        } else {
            val optionGroup = builder.optionGroup
            val enabledOptions = optionGroup?.options?.filter { it.enabledWhen(vars) }?.takeIf { it.isNotEmpty() }
            if (enabledOptions != null) {
            _state.value = current.copy(dialogueIndex = builder.dialogues.size)
            updateFrame()
            } else {
                val nextName = builder.nextSceneName
                if (nextName != null) {
                loadScene(nextName)
            } else {
                builder.onSceneEnd?.invoke()
                    _finished.value = true
                }
            }
        }
    }

    /** Executes a selected choice: runs its effects then jumps to its target scene. */
    fun choose(index: Int) {
        val builder = buildScene(_state.value.currentScene) ?: return
        val enabled = builder.optionGroup?.options?.filter { it.enabledWhen(vars) } ?: return
        val option = enabled.getOrNull(index) ?: return
        applyEffects(option.effects, vars)
        _state.value = _state.value.copy(variables = vars.toSaveMap())
        loadScene(option.target)
    }

    fun restart() {
        vars.loadFrom(emptyMap())
        _state.value = GameState()
        _finished.value = false
        loadScene("start")
    }

    fun makeChoice(sceneName: String) {
        loadScene(sceneName)
    }

    fun loadState(newState: GameState) {
        vars.loadFrom(newState.variables)
        _state.value = newState
        val builder = buildScene(newState.currentScene)
        if (builder != null) {
            effects.build(builder.sprites, builder.timeline.events)
            sceneEffectsDone = effects.scheduled.isEmpty()
            setupAnimators(builder.sprites)
            val base = builder.sprites.map { SpriteVisual(it.id, it.position, currentFrameOf(it), scale = it.scale) }
            _visuals.value = SceneVisual(base)
        }
        updateFrame()
    }

    fun tick(deltaMs: Long) {
        if (animators.isNotEmpty()) {
            animators.forEach { (_, animator) -> animator.advance(deltaMs) }
        }
        triggerAnimationEvents()
        if (!sceneEffectsDone) {
            effects.tick(deltaMs)
            _visuals.value = effects.computeVisuals(currentBaseSprites())
            if (effects.isComplete()) {
                sceneEffectsDone = true
                _visuals.value = SceneVisual(resetVisuals())
            }
        }
        applyAnimationFrames()
    }

    private fun triggerAnimationEvents() {
        if (effects.scheduled.isEmpty()) return
        effects.scheduled.forEach { effect ->
            val kind = effect.kind
            if (kind is EffectKind.Anim && effect !in triggeredAnimEffects) {
                if (effect.startMs <= effects.elapsedMs) {
                    triggeredAnimEffects += effect
                    animators[kind.sprite]?.play(kind.group, kind.loop)
                }
            }
        }
    }

    private fun resetVisuals(): List<SpriteVisual> =
        currentBaseSprites().map { SpriteVisual(it.id, it.position, currentFrameOf(it), scale = it.scale) }

    private fun loadScene(name: String) {
        _state.value = _state.value.copy(
            currentScene = name,
            dialogueIndex = 0,
            history = _state.value.history + name
        )
        lastAudioCueCount = 0
        val builder = buildScene(name)
        if (builder != null) {
            playAudioCues(builder, 0)
            builder.enterActions.forEach { it() }
            _state.value = _state.value.copy(variables = vars.toSaveMap())
            effects.build(builder.sprites, builder.timeline.events)
            triggeredAnimEffects.clear()
            sceneEffectsDone = effects.scheduled.isEmpty()
            setupAnimators(builder.sprites)
            val base = builder.sprites.map { SpriteVisual(it.id, it.position, currentFrameOf(it), scale = it.scale) }
            _visuals.value = SceneVisual(base)
        }
        updateFrame()
    }

    private fun setupAnimators(sprites: List<com.vnengine.dsl.SpriteRef>) {
        animators.clear()
        if (pack == null) return
        sprites.forEach { sprite ->
            val group = sprite.anim ?: return@forEach
            val animator = Animator(pack)
            if (animator.play(group, sprite.loop)) {
                animators[sprite.id] = animator
            }
        }
    }

    private fun currentFrameOf(sprite: com.vnengine.dsl.SpriteRef): String? =
        animators[sprite.id]?.currentSrc?.takeIf { it.isNotEmpty() }

    private fun applyAnimationFrames() {
        _visuals.value = _visuals.value.copy(
            sprites = _visuals.value.sprites.map { visual ->
                val src = animators[visual.id]?.currentSrc
                if (src != null && src.isNotEmpty()) visual.copy(frameSrc = src) else visual
            }
        )
    }

    private fun buildScene(name: String): SceneBuilder? {
        if (!script.scenes.containsKey(name)) return null
        val builder = SceneBuilder()
        builder.vars = vars
        script.scenes[name]?.invoke(builder)
        return builder
    }

    private fun currentBaseSprites(): List<com.vnengine.dsl.SpriteRef> {
        val builder = buildScene(_state.value.currentScene) ?: return emptyList()
        return builder.sprites
    }

    private fun playAudioCues(builder: SceneBuilder, dialogueIndex: Int) {
        if (dialogueIndex != 0) return
        if (builder.audioCues.isEmpty()) return
        if (lastAudioCueCount >= builder.audioCues.size) return
        builder.audioCues.drop(lastAudioCueCount).forEach { playCue(it) }
        lastAudioCueCount = builder.audioCues.size
    }

    private fun playCue(cue: AudioCue) {
        when (cue) {
            is AudioCue.Bgm -> audio.playBgm(cue.path, cue.loop)
            is AudioCue.Sfx -> audio.playSfx(cue.path)
            is AudioCue.Voice -> audio.playVoice(cue.path)
        }
    }

    private fun updateFrame() {
        val current = _state.value
        val builder = buildScene(current.currentScene) ?: return

        val dialogue = if (current.dialogueIndex < builder.dialogues.size) {
            val line = builder.dialogues[current.dialogueIndex]
            DialogueDisplay(line.speaker, line.text)
        } else null

        val optionGroup = builder.optionGroup
        val choices = if (current.dialogueIndex >= builder.dialogues.size && optionGroup != null)
            optionGroup.options.filter { it.enabledWhen(vars) }.map { it.label }
        else emptyList()

        _frame.value = SceneFrame(
            sprites = builder.sprites,
            dialogue = dialogue,
            background = builder.backgroundColor,
            choices = choices
        )
    }
}
