package com.vnengine.renderer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.vnengine.anim.Pack
import kotlinx.serialization.json.Json

actual object SpriteResources {
    private val json = Json { ignoreUnknownKeys = true }
    private val bitmapCache = mutableMapOf<String, ImageBitmap?>()

    actual fun loadPack(): Pack? {
        return try {
            val stream = SpriteResources::class.java.getResourceAsStream("/arc/character.json")
                ?: return null
            json.decodeFromString<Pack>(stream.readBytes().decodeToString())
        } catch (e: Exception) {
            println("Failed to load pack: ${e.message}")
            null
        }
    }

    actual fun loadFrame(src: String): ImageBitmap? {
        val path = "/arc/$src"
        return bitmapCache.getOrPut(path) {
            try {
                val stream = SpriteResources::class.java.getResourceAsStream(path) ?: return@getOrPut null
                loadImageBitmap(stream)
            } catch (e: Exception) {
                println("Failed to load frame $path: ${e.message}")
                null
            }
        }
    }

    actual fun loadStaticSprite(id: String): ImageBitmap? {
        val path = "/sprites/$id.png"
        return bitmapCache.getOrPut(path) {
            try {
                val stream = SpriteResources::class.java.getResourceAsStream(path) ?: return@getOrPut null
                loadImageBitmap(stream)
            } catch (e: Exception) {
                println("Failed to load sprite $path: ${e.message}")
                null
            }
        }
    }

    actual fun loadBackground(name: String): ImageBitmap? {
        val path = "/backgrounds/$name.jpg"
        return bitmapCache.getOrPut(path) {
            try {
                val stream = SpriteResources::class.java.getResourceAsStream(path) ?: return@getOrPut null
                loadImageBitmap(stream)
            } catch (e: Exception) {
                println("Failed to load background $path: ${e.message}")
                null
            }
        }
    }
}
