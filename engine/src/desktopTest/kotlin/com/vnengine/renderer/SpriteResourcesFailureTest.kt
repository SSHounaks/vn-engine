package com.vnengine.renderer

import kotlin.test.Test
import kotlin.test.assertNull

class SpriteResourcesFailureTest {
    @Test
    fun corruptFrameReturnsNull() {
        assertNull(SpriteResources.loadFrame("corrupt.png"))
    }

    @Test
    fun corruptBackgroundReturnsNull() {
        assertNull(SpriteResources.loadBackground("corrupt"))
    }
}
