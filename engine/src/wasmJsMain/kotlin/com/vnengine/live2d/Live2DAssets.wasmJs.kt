package com.vnengine.live2d

import kotlin.js.JsAny

actual object Live2DAssets {
    actual fun readText(path: String): String? = try {
        fetchText("/$path")
    } catch (t: Throwable) {
        null
    }

    actual fun readBytes(path: String): ByteArray? = try {
        fetchBytes("/$path")
    } catch (t: Throwable) {
        null
    }
}

@JsFun(
    """(url) => {
        const x = new XMLHttpRequest();
        x.open('GET', url, false);
        x.send();
        return x.status === 200 ? x.responseText : null;
    }"""
)
private external fun fetchText(url: String): String?

// Synchronous XHR cannot use responseType='arraybuffer'; use the x-user-defined trick.
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
private external fun fetchArrayBuffer(url: String): kotlin.js.JsAny?

@JsFun("(b) => new Uint8Array(b)")
private external fun toUint8Array(buffer: kotlin.js.JsAny): kotlin.js.JsAny

@JsFun("(u) => u.length")
private external fun uint8Length(u: kotlin.js.JsAny): Int

@JsFun("(u, i) => u[i]")
private external fun uint8At(u: kotlin.js.JsAny, i: Int): Int

private fun fetchBytes(url: String): ByteArray? {
    val buffer = fetchArrayBuffer(url) ?: return null
    val u = toUint8Array(buffer)
    val n = uint8Length(u)
    return ByteArray(n) { i -> uint8At(u, i).toByte() }
}
