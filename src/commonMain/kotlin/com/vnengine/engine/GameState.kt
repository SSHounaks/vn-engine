package com.vnengine.engine

import com.vnengine.dsl.DialogueLine
import com.vnengine.dsl.Position
import com.vnengine.dsl.SpriteRef

data class GameState(
    val currentScene: String = "start",
    val dialogueIndex: Int = 0,
    val visibleSprites: List<SpriteRef> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
    val history: List<String> = emptyList()
)

data class DialogueDisplay(
    val speaker: String?,
    val text: String,
    val isComplete: Boolean = false
)

data class SceneFrame(
    val sprites: List<SpriteRef>,
    val dialogue: DialogueDisplay?,
    val background: String? = null,
    val choices: List<String> = emptyList()
)
