package com.vnengine.live2d

/**
 * Process-wide registry of live renderers, keyed by model path. The draw layer
 * resolves a [Live2DRenderer] from here so renderers never have to travel
 * through serializable engine state.
 */
object Live2DRuntime {
    private val renderers = mutableMapOf<String, Live2DRenderer>()

    fun register(modelPath: String, renderer: Live2DRenderer) {
        renderers[modelPath] = renderer
    }

    fun renderer(modelPath: String): Live2DRenderer? = renderers[modelPath]

    fun clear() {
        renderers.clear()
    }
}
