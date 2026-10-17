package com.vnengine.live2d

import androidx.compose.ui.graphics.ImageBitmap

/**
 * A platform Live2D renderer: applies parameter values, deforms the model's
 * meshes, and rasterizes them into an [ImageBitmap] for the given viewport.
 *
 * Implemented per platform. The desktop implementation binds the proprietary
 * Cubism Core native library at runtime (via JNA); platforms without a backend
 * simply return null from [Live2DBackend.load] and the engine falls back to a
 * static preview.
 */
interface Live2DRenderer {
    /** Applies parameter values (in model parameter units). Unknown ids are ignored. */
    fun setParameterValues(values: Map<String, Double>)

    /**
     * Sets part opacities (0..1) used by Cubism pose switching. Parts not present in
     * [values] keep their current opacity. Renderers without part support may ignore this.
     */
    fun setPartOpacities(values: Map<String, Double>) {}

    /** Renders the current model state into an image of [widthPx] x [heightPx]. */
    fun render(widthPx: Int, heightPx: Int): ImageBitmap

    fun dispose()
}

/**
 * Loads native Live2D models. When no backend is available (or the Cubism Core
 * library cannot be found) [isSupported] is false and [load] returns null.
 */
expect object Live2DBackend {
    /** True when the Cubism Core library is available at runtime. */
    fun isSupported(): Boolean

    /**
     * Creates a renderer for a `.moc3` model. [textureBytes] are the decoded
     * PNG bytes listed in the model3 file, in order. Returns null if unsupported
     * or the model cannot be loaded.
     */
    fun load(mocBytes: ByteArray, textureBytes: List<ByteArray>): Live2DRenderer?
}
