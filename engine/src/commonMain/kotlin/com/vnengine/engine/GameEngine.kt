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
import com.vnengine.live2d.Live2DInstance
import com.vnengine.live2d.Live2DLoader
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
    private val sceneCache = mutableMapOf<String, SceneBuilder>()
    private val animators = mutableMapOf<String, Animator>()
    private val live2d = mutableMapOf<String, Live2DInstance>()
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

    /**
     * Executes a selected choice: runs its effects then jumps to its target scene.
     *
     * Only accepted while the current scene's dialogue is exhausted and an
     * enabled option group is present, so a repeated tap on the same menu is a
     * no-op instead of double-applying effects or selecting from the next scene.
     */
    fun choose(index: Int) {
        val current = _state.value
        val builder = buildScene(current.currentScene) ?: return
        if (current.dialogueIndex < builder.dialogues.size) return
        val optionGroup = builder.optionGroup ?: return
        val enabled = optionGroup.options.filter { it.enabledWhen(vars) }
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
            _state.value = _state.value.copy(visibleSprites = builder.sprites)
            effects.build(builder.sprites, builder.timeline.events)
            sceneEffectsDone = effects.scheduled.isEmpty()
            setupAnimators(builder.sprites)
            setupLive2D(builder.sprites)
            val base = builder.sprites.map { baseVisual(it) }
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
        applyLive2D(deltaMs)
    }

    private fun applyLive2D(deltaMs: Long) {
        if (live2d.isEmpty()) return
        live2d.values.forEach { it.tick(deltaMs.toDouble()) }
        _visuals.value = _visuals.value.copy(
            sprites = _visuals.value.sprites.map { visual ->
                live2d[visual.id]?.let { visual.copy(live2d = it.toVisual()) } ?: visual
            }
        )
    }

    private fun triggerAnimationEvents() {
        if (effects.scheduled.isEmpty()) return
        effects.scheduled.forEach { effect ->
            val kind = effect.kind
            if (effect.startMs > effects.elapsedMs || effect in triggeredAnimEffects) return@forEach
            when (kind) {
                is EffectKind.Anim -> {
                    triggeredAnimEffects += effect
                    animators[kind.sprite]?.play(kind.group, kind.loop)
                }
                is EffectKind.Motion -> {
                    triggeredAnimEffects += effect
                    live2d[kind.sprite]?.playMotion(kind.name)
                }
                is EffectKind.Expression -> {
                    triggeredAnimEffects += effect
                    live2d[kind.sprite]?.setExpression(kind.name)
                }
                else -> Unit
            }
        }
    }

    private fun resetVisuals(): List<SpriteVisual> =
        currentBaseSprites().map { baseVisual(it) }

    private fun baseVisual(sprite: com.vnengine.dsl.SpriteRef): SpriteVisual =
        SpriteVisual(
            id = sprite.id,
            position = sprite.position,
            frameSrc = currentFrameOf(sprite),
            scale = sprite.scale,
            live2d = live2d[sprite.id]?.toVisual()
        )

    private fun setupLive2D(sprites: List<com.vnengine.dsl.SpriteRef>) {
        live2d.clear()
        sprites.forEach { sprite ->
            val model = sprite.live2dModel ?: return@forEach
            val loaded = Live2DLoader.load(model) ?: return@forEach
            val instance = Live2DInstance(loaded)
            instance.show(sprite.motion, sprite.expression)
            live2d[sprite.id] = instance
        }
    }

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
            _state.value = _state.value.copy(
                variables = vars.toSaveMap(),
                visibleSprites = builder.sprites
            )
            effects.build(builder.sprites, builder.timeline.events)
            triggeredAnimEffects.clear()
            sceneEffectsDone = effects.scheduled.isEmpty()
            setupAnimators(builder.sprites)
            setupLive2D(builder.sprites)
            val base = builder.sprites.map { baseVisual(it) }
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
        val block = script.scenes[name] ?: return null
        return sceneCache.getOrPut(name) {
            val builder = SceneBuilder()
            builder.vars = vars
            block(builder)
            builder
        }
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
