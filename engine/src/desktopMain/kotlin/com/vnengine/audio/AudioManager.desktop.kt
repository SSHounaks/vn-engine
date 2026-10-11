package com.vnengine.audio

actual fun createAudioManager(): AudioManager = DesktopAudioManager()
