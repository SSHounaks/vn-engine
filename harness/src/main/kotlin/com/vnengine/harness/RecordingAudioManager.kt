package com.vnengine.harness

import com.vnengine.audio.AudioManager

/**
 * A no-op [AudioManager] that records every call, so tests can assert the
 * engine's audio cues without touching the real sound system.
 */
class RecordingAudioManager : AudioManager {
    data class Bgm(val path: String, val loop: Boolean)

    val bgm = mutableListOf<Bgm>()
    val sfx = mutableListOf<String>()
    val voice = mutableListOf<String>()

    var stopCount = 0
    var pauseCount = 0
    var resumeCount = 0
    var recordedMasterVolume = 1f
    var recordedBgmVolume = 1f
    var disposed = false

    override fun playBgm(path: String, loop: Boolean) { bgm += Bgm(path, loop) }
    override fun stopBgm() { stopCount++ }
    override fun pauseBgm() { pauseCount++ }
    override fun resumeBgm() { resumeCount++ }
    override fun setBgmVolume(volume: Float) { recordedBgmVolume = volume }
    override fun playSfx(path: String) { sfx += path }
    override fun playVoice(path: String) { voice += path }
    override fun setMasterVolume(volume: Float) { recordedMasterVolume = volume }
    override fun dispose() { disposed = true }
}
