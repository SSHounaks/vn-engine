package com.vnengine.live2d

/**
 * Platform asset access for Live2D model files. [path] is relative to the asset
 * root (e.g. `Characters/Hiyori/Hiyori.model3.json`). Returns `null` when the
 * asset is missing so callers can degrade gracefully.
 */
expect object Live2DAssets {
    fun readText(path: String): String?
    fun readBytes(path: String): ByteArray?
}
