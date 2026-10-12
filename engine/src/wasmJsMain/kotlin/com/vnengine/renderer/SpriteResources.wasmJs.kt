package com.vnengine.renderer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.vnengine.anim.Pack
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import kotlin.js.JsAny

actual object SpriteResources {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, ImageBitmap?>()
    private var pack: Pack? = null

    actual fun loadPack(): Pack? {
        if (pack == null) {
            pack = try {
                fetchText("/arc/character.json")?.let { json.decodeFromString<Pack>(it) }
            } catch (t: Throwable) {
                println("VN: loadPack failed: ${t.message}")
                null
            }
        }
        return pack
    }

    actual fun loadFrame(src: String): ImageBitmap? = load("/arc/$src")
    actual fun loadStaticSprite(id: String): ImageBitmap? = load("/sprites/$id.png")
    actual fun loadBackground(name: String): ImageBitmap? = load("/backgrounds/$name.jpg")

    private fun load(path: String): ImageBitmap? {
        cache[path]?.let { return it }
        val bitmap = try {
            fetchBytes(path)?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
        } catch (t: Throwable) {
            println("VN: load $path failed: ${t.message}")
            null
        }
        cache[path] = bitmap
        return bitmap
    }
}

// Synchronous XHR cannot use responseType='arraybuffer' (throws InvalidAccessError).
// Use the x-user-defined trick: read responseText and mask each char code to a byte.
@JsFun(
    """(url) => {
        const x = new XMLHttpRequest();
        x.open('GET', url, false);
        x.overrideMimeType('text/plain; charset=x-user-defined');
        x.send();
        if (x.status !== 200) return null;
        const t = x.responseText;
        const b = new Uint8Array(t.length);
        for (let i = 0; i < t.length; i++) b[i] = t.charCodeAt(i) & 0xff;
        return b.buffer;
    }"""
)
private external fun fetchArrayBuffer(url: String): JsAny?

@JsFun(
    """(url) => {
        const x = new XMLHttpRequest();
        x.open('GET', url, false);
        x.send();
        return x.status === 200 ? x.responseText : null;
    }"""
)
private external fun fetchText(url: String): String?

@JsFun("(b) => new Uint8Array(b)")
private external fun toUint8Array(buffer: JsAny): JsAny

@JsFun("(u) => u.length")
private external fun uint8Length(u: JsAny): Int

@JsFun("(u, i) => u[i]")
private external fun uint8At(u: JsAny, i: Int): Int

private fun fetchBytes(url: String): ByteArray? {
    val buffer = fetchArrayBuffer(url) ?: return null
    val u = toUint8Array(buffer)
    val n = uint8Length(u)
    return ByteArray(n) { i -> uint8At(u, i).toByte() }
}
