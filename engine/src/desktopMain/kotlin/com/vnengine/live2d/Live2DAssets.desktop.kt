package com.vnengine.live2d

actual object Live2DAssets {
    actual fun readText(path: String): String? = try {
        readBytes(path)?.decodeToString()
    } catch (e: Exception) {
        null
    }

    actual fun readBytes(path: String): ByteArray? = try {
        Live2DAssets::class.java.getResourceAsStream("/$path")?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}
