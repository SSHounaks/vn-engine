package com.vnengine

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnengine.engine.GameEngine
import com.vnengine.renderer.SceneRenderer
import com.vnengine.save.SaveSlot
import com.vnengine.save.SaveSlotManager
import com.vnengine.save.SlotInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class MenuMode { Load, Save }

/**
 * Wraps [SceneRenderer] with an in-game "Save / Load" button that opens a menu
 * listing every [SaveSlot] with its metadata. Loading applies the slot and
 * closes the menu so the player sees the restored scene immediately.
 */
@Composable
fun GameScreen(engine: GameEngine, saves: SaveSlotManager) {
    var menuOpen by remember { mutableStateOf(false) }
    val finished by engine.finished.collectAsState()

    LaunchedEffect(Unit) { engine.start() }

    Box(Modifier.fillMaxSize()) {
        SceneRenderer(engine)

        if (!finished) {
            Button(
                onClick = { menuOpen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text("Save / Load")
            }
        }

        if (menuOpen) {
            SaveLoadMenu(
                saves = saves,
                onClose = { menuOpen = false },
                onLoaded = { menuOpen = false }
            )
        }
    }
}

@Composable
private fun SaveLoadMenu(
    saves: SaveSlotManager,
    onClose: () -> Unit,
    onLoaded: () -> Unit
) {
    var mode by remember { mutableStateOf(MenuMode.Load) }
    var revision by remember { mutableIntStateOf(0) }
    val rows = remember(revision) { saves.allSlots().map { it to saves.info(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.85f)
                .padding(16.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Save / Load", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeTab("Load", mode == MenuMode.Load) { mode = MenuMode.Load }
                    ModeTab("Save", mode == MenuMode.Save) { mode = MenuMode.Save }
                }
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rows.forEach { (slot, info) ->
                        SlotRow(
                            slot = slot,
                            info = info,
                            mode = mode,
                            onSave = { saves.save(slot); revision++ },
                            onLoad = { if (saves.load(slot)) onLoaded() },
                            onDelete = { saves.delete(slot); revision++ }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun SlotRow(
    slot: SaveSlot,
    info: SlotInfo?,
    mode: MenuMode,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(slot.displayName, fontWeight = FontWeight.SemiBold)
                if (info == null) {
                    Text("Empty", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Text(
                        buildString {
                            append(info.scene)
                            append("  ·  step ")
                            append(info.steps)
                            if (info.savedAt > 0L) {
                                append("  ·  ")
                                append(formatTime(info.savedAt))
                            }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    info.preview?.let { preview ->
                        Text(
                            preview,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            when (mode) {
                MenuMode.Save -> Button(onClick = onSave) { Text("Save") }
                MenuMode.Load -> {
                    Button(onClick = onLoad, enabled = info != null) { Text("Load") }
                    if (info != null) {
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = onDelete) { Text("Delete") }
                    }
                }
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

private fun formatTime(epochMs: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(epochMs))
