package com.vnengine.audio

interface AudioManager {
    fun playBgm(path: String, loop: Boolean = true)
    fun stopBgm()
    fun pauseBgm()
    fun resumeBgm()
    fun setBgmVolume(volume: Float)

    fun playSfx(path: String)
    fun playVoice(path: String)

    fun setMasterVolume(volume: Float)
    fun dispose()
}

expect fun createAudioManager(): AudioManager
