package com.vnengine.audio

import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

class DesktopAudioManager : AudioManager {
    private var bgmClip: Clip? = null
    private var masterVolume: Float = 1.0f
    private var bgmVolume: Float = 1.0f
    private val sfxClips = mutableListOf<Clip>()

    override fun playBgm(path: String, loop: Boolean) {
        stopBgm()
        val file = File(path)
        if (!file.exists()) return
        try {
            val clip = AudioSystem.getClip()
            AudioSystem.getAudioInputStream(file).use { stream ->
                clip.open(stream)
            }
            clip.loop(if (loop) Clip.LOOP_CONTINUOUSLY else 0)
            applyVolume(clip, masterVolume * bgmVolume)
            clip.start()
            bgmClip = clip
        } catch (e: Exception) {
            println("Failed to play BGM $path: ${e.message}")
        }
    }

    override fun stopBgm() {
        bgmClip?.stop()
        bgmClip?.close()
        bgmClip = null
    }

    override fun pauseBgm() {
        bgmClip?.stop()
    }

    override fun resumeBgm() {
        bgmClip?.start()
    }

    override fun setBgmVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        bgmClip?.let { applyVolume(it, masterVolume * bgmVolume) }
    }

    override fun playSfx(path: String) {
        val file = File(path)
        if (!file.exists()) return
        try {
            val clip = AudioSystem.getClip()
            AudioSystem.getAudioInputStream(file).use { stream ->
                clip.open(stream)
            }
            applyVolume(clip, masterVolume)
            clip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close()
                    sfxClips.remove(clip)
                }
            }
            clip.start()
            sfxClips.add(clip)
        } catch (e: Exception) {
            println("Failed to play SFX $path: ${e.message}")
        }
    }

    override fun playVoice(path: String) {
        playSfx(path)
    }

    override fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        bgmClip?.let { applyVolume(it, masterVolume * bgmVolume) }
    }

    private fun applyVolume(clip: Clip, volume: Float) {
        try {
            val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val dB = if (volume <= 0f) {
                control.minimum
            } else {
                (20f * kotlin.math.log10(volume.toDouble())).toFloat()
            }
            control.value = dB.coerceIn(control.minimum, control.maximum)
        } catch (_: Exception) {
        }
    }

    override fun dispose() {
        stopBgm()
        sfxClips.forEach { it.close() }
        sfxClips.clear()
    }
}
