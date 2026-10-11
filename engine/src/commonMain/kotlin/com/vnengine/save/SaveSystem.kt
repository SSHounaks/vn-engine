package com.vnengine.save

import com.vnengine.engine.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class SaveData(
    val version: Int = 1,
    val timestamp: Long = 0L,
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
    val bias: Float? = null
)

class SaveSystem {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun save(state: GameState, slot: Int = 0): String {
        val snapshot = GameStateSnapshot(
            currentScene = state.currentScene,
            dialogueIndex = state.dialogueIndex,
            visibleSprites = state.visibleSprites.map {
                SpriteSnapshot(it.id, it.position.name, it.anim, it.loop, it.scale, it.layer, it.bias)
            },
            variables = state.variables,
            history = state.history
        )
        val saveData = SaveData(
            version = 1,
            timestamp = 0L,
            state = snapshot,
            recentEvents = state.history.takeLast(10)
        )
        return json.encodeToString(saveData)
    }

    fun load(saveJson: String): GameState? {
        return try {
            val saveData = json.decodeFromString<SaveData>(saveJson)
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
                        it.bias
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
