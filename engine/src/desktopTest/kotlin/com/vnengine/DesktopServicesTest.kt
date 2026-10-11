package com.vnengine

import com.vnengine.audio.createAudioManager
import com.vnengine.concurrency.createVirtualThreadDispatcher
import com.vnengine.renderer.SpriteResources
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpriteResourcesTest {
    @Test
    fun loadPackParsesBundledCharacterJson() {
        val pack = assertNotNull(SpriteResources.loadPack())
        assertEquals("test", pack.name)
        assertTrue("idle" in pack.animations)
        assertEquals(2, pack.animations.getValue("idle").frames.size)
        assertFalse(pack.animations.getValue("once").loop)
    }

    @Test
    fun loadStaticSpriteDecodesPng() {
        assertNotNull(SpriteResources.loadStaticSprite("hero"))
    }

    @Test
    fun loadFrameDecodesPng() {
        assertNotNull(SpriteResources.loadFrame("frame1.png"))
    }

    @Test
    fun loadBackgroundDecodesJpeg() {
        assertNotNull(SpriteResources.loadBackground("sky"))
    }

    @Test
    fun corruptImageReturnsNull() {
        assertNull(SpriteResources.loadStaticSprite("corrupt"))
    }

    @Test
    fun missingAssetsReturnNull() {
        assertNull(SpriteResources.loadStaticSprite("definitely-not-a-sprite"))
        assertNull(SpriteResources.loadBackground("definitely-not-a-background"))
        assertNull(SpriteResources.loadFrame("definitely-not-a-frame.png"))
    }
}

class DesktopAudioManagerTest {
    private fun blipFile(): File {
        val bytes = DesktopAudioManagerTest::class.java.getResourceAsStream("/audio/blip.wav")!!.readBytes()
        val tmp = File.createTempFile("vn-engine-blip", ".wav")
        tmp.deleteOnExit()
        tmp.writeBytes(bytes)
        return tmp
    }

    @Test
    fun realWavPlaybackAndVolumeControlsDoNotThrow() {
        val path = blipFile().absolutePath
        val audio = createAudioManager()
        try {
            audio.setMasterVolume(2f)
            audio.setMasterVolume(-1f)
            audio.playBgm(path, loop = false)
            audio.setBgmVolume(0.5f)
            audio.setBgmVolume(5f)
            audio.pauseBgm()
            audio.resumeBgm()
            audio.stopBgm()
            audio.playSfx(path)
            audio.playVoice(path)
        } finally {
            audio.dispose()
        }
    }

    @Test
    fun missingFilesAndNullClipOperationsAreSafe() {
        val audio = createAudioManager()
        try {
            audio.playBgm("/no/such/file.wav")
            audio.playSfx("/no/such/file.wav")
            audio.playVoice("/no/such/file.wav")
            audio.stopBgm()
            audio.pauseBgm()
            audio.resumeBgm()
            audio.dispose()
        } catch (e: Exception) {
            throw AssertionError("desktop audio operations must be exception-safe", e)
        }
    }
}

class ConcurrencyTest {
    @Test
    fun virtualThreadDispatcherRunsWork() = runBlocking {
        val dispatcher = createVirtualThreadDispatcher()
        var ran = false
        launch(dispatcher) { ran = true }.join()
        assertTrue(ran)
    }
}
