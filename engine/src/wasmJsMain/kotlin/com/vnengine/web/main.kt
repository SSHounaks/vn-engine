package com.vnengine.web

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vnengine.engine.GameEngine
import com.vnengine.renderer.SceneRenderer

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "composeApp") {
        MaterialTheme {
            val engine = remember { GameEngine(webStory) }
            LaunchedEffect(Unit) { engine.start() }
            SceneRenderer(engine)
        }
    }
}
