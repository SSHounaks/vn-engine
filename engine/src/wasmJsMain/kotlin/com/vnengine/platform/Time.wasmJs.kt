package com.vnengine.platform

@JsFun("() => Date.now()")
private external fun jsNow(): Double

actual fun currentTimeMillis(): Long = jsNow().toLong()
