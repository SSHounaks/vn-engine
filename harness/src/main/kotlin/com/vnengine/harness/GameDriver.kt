package com.vnengine.harness

import com.vnengine.anim.Pack
import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.SpriteRef
import com.vnengine.engine.GameEngine
import com.vnengine.engine.GameState
import com.vnengine.engine.GameVars
import com.vnengine.engine.SceneFrame
import com.vnengine.engine.SceneVisual
import com.vnengine.engine.VarKey
import com.vnengine.save.InMemorySaveStorage
import com.vnengine.save.SaveSlot
import com.vnengine.save.SaveSlotManager
import com.vnengine.save.SaveStorage
import com.vnengine.save.SaveSystem
import com.vnengine.save.SlotInfo

/**
 * Thin, fluent driver around [GameEngine] for content-level tests: it exposes
 * the current line/choices/vars, and steps dialogue, choices, and ticks.
 */
class GameDriver(
    val script: SceneScript,
    val audio: RecordingAudioManager = RecordingAudioManager(),
    pack: Pack? = null,
    storage: SaveStorage = InMemorySaveStorage(),
    clock: () -> Long = { 0L }
) {
    val engine = GameEngine(script, audio, pack)
    private val saveSystem = SaveSystem()

    /** Fixed-slot save/load manager backed by [storage]. */
    val saves = SaveSlotManager(engine, saveSystem, storage, clock = clock)

    val state: GameState get() = engine.state.value
    val frame: SceneFrame get() = engine.frame.value
    val visuals: SceneVisual get() = engine.visuals.value
    val finished: Boolean get() = engine.finished.value

    val scene: String get() = state.currentScene
    val dialogueIndex: Int get() = state.dialogueIndex
    val line: String? get() = frame.dialogue?.text
    val speaker: String? get() = frame.dialogue?.speaker
    val choices: List<String> get() = frame.choices
    val background: String? get() = frame.background
    val sprites: List<SpriteRef> get() = frame.sprites

    fun start(): GameDriver = apply { engine.start() }
    fun advance(): GameDriver = apply { engine.advance() }
    fun choose(index: Int): GameDriver = apply { engine.choose(index) }
    fun choose(label: String): GameDriver {
        val i = choices.indexOf(label)
        require(i >= 0) { "No choice labelled '$label'; available=$choices" }
        return choose(i)
    }

    fun tick(deltaMs: Long): GameDriver = apply { engine.tick(deltaMs) }
    fun tickFrames(frames: Int, deltaMs: Long = 16L): GameDriver =
        apply { repeat(frames) { engine.tick(deltaMs) } }

    /** Advances through every dialogue line until a menu, scene change, or the end. */
    fun advanceDialogue(limit: Int = 1000): GameDriver {
        var guard = 0
        while (!finished && frame.dialogue != null && guard++ < limit) engine.advance()
        return this
    }

    /** Reads a typed variable from the current saved state. */
    fun <T> value(key: VarKey<T>): T {
        val view = GameVars()
        view.loadFrom(state.variables)
        return view[key]
    }

    fun intValue(key: VarKey<Int>): Int = value(key)
    fun boolValue(key: VarKey<Boolean>): Boolean = value(key)
    fun stringValue(key: VarKey<String>): String = value(key)

    fun save(savedAt: Long = 0L): String = saveSystem.save(state, savedAt = savedAt)

    fun load(saveJson: String): Boolean {
        val restored = saveSystem.load(saveJson) ?: return false
        engine.loadState(restored)
        return true
    }

    fun quickSave(): Boolean = saves.quickSave()
    fun quickLoad(): Boolean = saves.quickLoad()
    fun saveToSlot(index: Int): Boolean = saves.saveToSlot(index)
    fun loadFromSlot(index: Int): Boolean = saves.loadFromSlot(index)
    fun slotInfo(index: Int): SlotInfo? = saves.info(SaveSlot.Manual(index))
    fun slotInfos(): List<SlotInfo?> = saves.infos()

    fun restart(): GameDriver = apply { engine.restart() }
}
