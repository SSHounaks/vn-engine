package com.vnengine

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.vnengine.dsl.SceneScript
import com.vnengine.engine.GameEngine
import com.vnengine.save.FileSaveStorage
import com.vnengine.save.SaveSlotManager

fun main() = application {
    val engine = remember { GameEngine(resolveStory()) }
    val saves = remember { SaveSlotManager(engine, storage = FileSaveStorage()) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "VN Engine",
        state = WindowState(size = DpSize(1280.dp, 720.dp)),
        resizable = true
    ) {
        MaterialTheme {
            GameScreen(engine, saves)
        }
    }
}

/**
 * Uses the local `SampleGame` when it is on the classpath (development builds
 * that include the withheld sample content), otherwise the bundled [demoStory].
 */
private fun resolveStory(): SceneScript =
    runCatching {
        Class.forName("com.vnengine.dsl.SampleGameKt")
            .getMethod("getSampleGame")
            .invoke(null) as SceneScript
    }.getOrElse { demoStory }
