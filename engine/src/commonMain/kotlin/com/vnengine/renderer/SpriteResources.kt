package com.vnengine.renderer

import androidx.compose.ui.graphics.ImageBitmap
import com.vnengine.anim.Pack

/**
 * Platform-specific asset loading for the scene renderer. Each target provides
 * its own loader (classpath on desktop/JVM, network or bundled resources on web).
 * Missing assets return `null`; the renderer falls back to placeholder visuals.
 */
expect object SpriteResources {
    fun loadPack(): Pack?
    fun loadFrame(src: String): ImageBitmap?
    fun loadStaticSprite(id: String): ImageBitmap?
    fun loadBackground(name: String): ImageBitmap?
}
