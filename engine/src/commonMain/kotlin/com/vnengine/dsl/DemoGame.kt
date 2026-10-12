package com.vnengine.dsl

/**
 * Minimal, asset-free story bundled with the engine so any target (desktop,
 * web) can run without the withheld sample content.
 */
val demoStory: SceneScript = game {
    scene("start") {
        dialogue("Narrator", "Welcome to VN Engine.")
        dialogue("Narrator", "This is the built-in demo. Add your own scenes with the DSL.")
        dialogue("Narrator", "That's all for now.")
    }
}
