package com.vnengine.save

import com.vnengine.engine.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class SaveData(
    val version: Int = 2,
    val savedAt: Long = 0L,
    val preview: String? = null,
    val steps: Int = 0,
    val state: GameStateSnapshot,
    val recentEvents: List<String> = emptyList()
)

@Serializable
data class GameStateSnapshot(
    val currentScene: String,
    val dialogueIndex: Int,
    val visibleSprites: List<SpriteSnapshot>,
    val variables: Map<String, String>,
    val history: List<String>
)

@Serializable
data class SpriteSnapshot(
    val id: String,
    val position: String,
    val anim: String? = null,
    val loop: Boolean? = null,
    val scale: Float = 1f,
    val layer: Int = 0,
    val bias: Float? = null,
    val live2dModel: String? = null,
    val motion: String? = null,
    val expression: String? = null
)

/** Serializes [GameState] to and from the JSON stored in a [SaveSlot]. */
class SaveSystem {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun save(
        state: GameState,
        savedAt: Long = 0L,
        preview: String? = null,
        steps: Int = state.history.size
    ): String {
        val snapshot = GameStateSnapshot(
            currentScene = state.currentScene,
            dialogueIndex = state.dialogueIndex,
            visibleSprites = state.visibleSprites.map {
                SpriteSnapshot(
                    it.id, it.position.name, it.anim, it.loop, it.scale, it.layer, it.bias,
                    it.live2dModel, it.motion, it.expression
                )
            },
            variables = state.variables,
            history = state.history
        )
        val saveData = SaveData(
            version = 2,
            savedAt = savedAt,
            preview = preview,
            steps = steps,
            state = snapshot,
            recentEvents = state.history.takeLast(10)
        )
        return json.encodeToString(saveData)
    }

    /** Parses raw save JSON into metadata + snapshot, or null if malformed. */
    fun parse(saveJson: String): SaveData? = try {
        json.decodeFromString<SaveData>(saveJson)
    } catch (e: Exception) {
        null
    }

    fun load(saveJson: String): GameState? {
        val saveData = parse(saveJson) ?: return null
        return try {
            GameState(
                currentScene = saveData.state.currentScene,
                dialogueIndex = saveData.state.dialogueIndex,
                visibleSprites = saveData.state.visibleSprites.map {
                    com.vnengine.dsl.SpriteRef(
                        it.id,
                        com.vnengine.dsl.Position.valueOf(it.position),
                        it.anim,
                        it.loop,
                        it.scale,
                        it.layer,
                        it.bias,
                        it.live2dModel,
                        it.motion,
                        it.expression
                    )
                },
                variables = saveData.state.variables,
                history = saveData.state.history
            )
        } catch (e: Exception) {
            null
        }
    }
}
