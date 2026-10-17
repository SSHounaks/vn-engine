package com.vnengine.live2d

actual object Live2DBackend {
    actual fun isSupported(): Boolean = false
    actual fun load(mocBytes: ByteArray, textureBytes: List<ByteArray>): Live2DRenderer? = null
}
