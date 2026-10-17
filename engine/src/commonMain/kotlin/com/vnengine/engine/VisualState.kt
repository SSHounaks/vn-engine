package com.vnengine.engine

import com.vnengine.dsl.Position
import com.vnengine.live2d.Live2DVisual

data class SpriteVisual(
    val id: String,
    val position: Position,
    val frameSrc: String? = null,
    val alpha: Float = 1f,
    val xOffset: Float = 0f,
    val yOffset: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val live2d: Live2DVisual? = null
)

data class SceneVisual(
    val sprites: List<SpriteVisual>,
    val flashAlpha: Float = 0f,
    val flashColor: Int = 0xFFFFFFFF.toInt(),
    val transitionAlpha: Float = 0f,
    val backgroundError: String? = null
)
