package com.vnengine.dsl

/**
 * Minimal, asset-free story bundled with the engine so any target (desktop,
 * web) can run without the withheld sample content.
 *
 * It also demonstrates the Live2D API. The model below is intentionally not
 * shipped with the engine: without the (proprietary) Cubism Core library and
 * model assets the engine renders a parameter preview instead of crashing, and
 * once you drop a real model into the asset root it renders for real.
 */
val demoStory: SceneScript = game {
    scene("start") {
        dialogue("Narrator", "Welcome to VN Engine.")
        dialogue("Narrator", "This is the built-in demo. Add your own scenes with the DSL.")
        next("live2d_demo")
    }

    scene("live2d_demo") {
        background("#101830")
        live2d(
            "Characters/Haru",
            at = Position.Center,
            motion = "Idle",
            expression = "F01"
        )
        dialogue("Narrator", "Live2D: live2d(\"Characters/Haru\", motion = \"Idle\", expression = \"F01\")")
        dialogue("Narrator", "Install the Cubism Core library and model assets to see it animate.")

        timeline {
            motion("Haru", "TapBody")
            expression("Haru", "F02")
        }
        dialogue("Narrator", "Timeline events can switch motions and expressions at any point.")
        next("haru_top")
    }

    scene("haru_top") {
        background("#0a0f1f")
        live2d(
            "Characters/Haru",
            at = Position.Center,
            motion = "Idle",
            expression = "F01",
            anchorTop = true
        )
        dialogue("Haru", "I'm showing from the upper half.")
        dialogue("Haru", "Tap to change pose and expression.")
        timeline {
            motion("Haru", "TapBody")
            expression("Haru", "F02")
        }
        dialogue("Haru", "Different expression.")
        timeline {
            expression("Haru", "F03")
        }
        dialogue("Haru", "Back to idle-like expression.")
        next("end")
    }
    scene("end") {
        dialogue("Narrator", "That's all for now.")
    }
}
