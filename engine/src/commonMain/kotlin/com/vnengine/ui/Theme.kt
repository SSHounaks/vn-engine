package com.vnengine.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI theme for VN Engine UI elements (dialogue box, choices, etc.). Values can be
 * overridden by placing `ui/theme.json` in the asset root.
 */
data class UITheme(
    val panelColor: Color = Color(0xCC111827),
    val accentColor: Color = Color(0xFF7FA3FF),
    val textColor: Color = Color.White,
    val mutedTextColor: Color = Color.White.copy(alpha = 0.8f),
    val choiceHoverColor: Color = Color.White.copy(alpha = 0.2f),
    val cornerRadius: Dp = 12.dp,
    val padding: Dp = 16.dp,
    val dialogueAnchor: DialogueAnchor = DialogueAnchor.BOTTOM,
    val dialogueMaxWidth: Dp = 720.dp,
    val dialogueBoxHeight: Dp? = null
)

enum class DialogueAnchor {
    TOP, BOTTOM, CENTER
}

val DefaultUITheme = UITheme()

val LocalUITheme = staticCompositionLocalOf { DefaultUITheme }
