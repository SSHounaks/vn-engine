package com.vnengine

import com.vnengine.audio.AudioManager

/** Minimal recording audio double for engine tests (no sound system). */
class FakeAudioManager : AudioManager {
    val bgm = mutableListOf<Pair<String, Boolean>>()
    val sfx = mutableListOf<String>()
    val voice = mutableListOf<String>()

    override fun playBgm(path: String, loop: Boolean) { bgm += path to loop }
    override fun stopBgm() {}
    override fun pauseBgm() {}
    override fun resumeBgm() {}
    override fun setBgmVolume(volume: Float) {}
    override fun playSfx(path: String) { sfx += path }
    override fun playVoice(path: String) { voice += path }
    override fun setMasterVolume(volume: Float) {}
    override fun dispose() {}
}
