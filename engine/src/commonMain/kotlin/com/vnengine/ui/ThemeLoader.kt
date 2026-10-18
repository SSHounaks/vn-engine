package com.vnengine.ui

import com.vnengine.live2d.Live2DAssets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Serializable
private data class ThemeJson(
    val panelColor: String? = null,
    val accentColor: String? = null,
    val textColor: String? = null,
    val mutedTextColor: String? = null,
    val choiceHoverColor: String? = null,
    val cornerRadius: Int? = null,
    val padding: Int? = null,
    val dialogueAnchor: String? = null,
    val dialogueMaxWidth: Int? = null,
    val dialogueBoxHeight: Int? = null
)

private val json = Json { ignoreUnknownKeys = true }

fun loadUITheme(): UITheme {
    val text = Live2DAssets.readText("ui/theme.json") ?: return DefaultUITheme
    val parsed = runCatching { json.decodeFromString<ThemeJson>(text) }.getOrNull() ?: return DefaultUITheme
    return DefaultUITheme.copy(
        panelColor = parsed.panelColor?.toColorOrNull() ?: DefaultUITheme.panelColor,
        accentColor = parsed.accentColor?.toColorOrNull() ?: DefaultUITheme.accentColor,
        textColor = parsed.textColor?.toColorOrNull() ?: DefaultUITheme.textColor,
        mutedTextColor = parsed.mutedTextColor?.toColorOrNull() ?: DefaultUITheme.mutedTextColor,
        choiceHoverColor = parsed.choiceHoverColor?.toColorOrNull() ?: DefaultUITheme.choiceHoverColor,
        cornerRadius = parsed.cornerRadius?.dp ?: DefaultUITheme.cornerRadius,
        padding = parsed.padding?.dp ?: DefaultUITheme.padding,
        dialogueAnchor = parseAnchor(parsed.dialogueAnchor) ?: DefaultUITheme.dialogueAnchor,
        dialogueMaxWidth = parsed.dialogueMaxWidth?.dp ?: DefaultUITheme.dialogueMaxWidth,
        dialogueBoxHeight = parsed.dialogueBoxHeight?.dp ?: DefaultUITheme.dialogueBoxHeight
    )
}

private fun parseAnchor(v: String?): DialogueAnchor? = when (v?.lowercase()) {
    "top" -> DialogueAnchor.TOP
    "bottom" -> DialogueAnchor.BOTTOM
    "center" -> DialogueAnchor.CENTER
    else -> null
}

private fun String.toColorOrNull(): Color? {
    if (isBlank()) return null
    val s = if (startsWith("#")) this else "#$this"
    return runCatching { Color(s.toLong(16).let { if (s.length == 7) 0xFF000000L or it else it }) }.getOrNull()
}
