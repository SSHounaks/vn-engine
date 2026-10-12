package com.vnengine.audio

private class NoOpAudioManager : AudioManager {
    override fun playBgm(path: String, loop: Boolean) {}
    override fun stopBgm() {}
    override fun pauseBgm() {}
    override fun resumeBgm() {}
    override fun setBgmVolume(volume: Float) {}
    override fun playSfx(path: String) {}
    override fun playVoice(path: String) {}
    override fun setMasterVolume(volume: Float) {}
    override fun dispose() {}
}

actual fun createAudioManager(): AudioManager = NoOpAudioManager()
