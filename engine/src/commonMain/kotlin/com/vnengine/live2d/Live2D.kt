package com.vnengine.live2d

/** True when a native Live2D backend is available; mirrors Ren'Py's `renpy.has_live2d()`. */
fun isLive2DSupported(): Boolean = Live2DBackend.isSupported()
