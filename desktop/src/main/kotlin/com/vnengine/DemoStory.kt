package com.vnengine

import com.vnengine.dsl.SceneScript
import com.vnengine.dsl.game

/**
 * Minimal, asset-free story bundled with the app so a clean checkout runs
 * without the withheld sample content. Used as the fallback when the local
 * `SampleGame` is not present on the classpath.
 */
val demoStory: SceneScript = game {
    scene("start") {
        dialogue("Narrator", "Welcome to VN Engine.")
        dialogue("Narrator", "This is the built-in demo. Open the Save / Load menu to try the slot system.")
        dialogue("Narrator", "That's all for now. Add your own scenes with the DSL.")
    }
}
