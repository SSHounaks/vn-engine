package com.vnengine.anim

/**
 * Port of the review tool's Animator (animator.ts).
 * Advances through a group's frames on elapsed time, looping when the
 * animation is marked `loop`.
 */
class Animator(private val pack: Pack) {
    var group: String = ""
        private set
    var index: Int = 0
        private set
    var finished: Boolean = false
        private set
    private var elapsedMs: Long = 0

    val currentSrc: String
        get() = pack.animations[group]?.frames?.getOrNull(index)?.src ?: ""

    private var loopOverride: Boolean? = null

    val currentLoop: Boolean
        get() = loopOverride ?: (pack.animations[group]?.loop ?: false)

    fun play(target: String, loop: Boolean? = null): Boolean {
        val anim = pack.animations[target] ?: return false
        if (anim.frames.isEmpty()) return false
        val changed = group != target
        group = target
        loopOverride = loop
        if (changed || index >= anim.frames.size) index = 0
        elapsedMs = 0
        finished = false
        return true
    }

    fun advance(deltaMs: Long) {
        if (finished) return
        val anim = pack.animations[group] ?: return
        elapsedMs += deltaMs
        var guard = 0
        while (elapsedMs >= anim.frames.getOrNull(index)?.durationMs ?: 0L && guard++ < 64) {
            elapsedMs -= anim.frames[index].durationMs
            index++
            if (index >= anim.frames.size) {
                if (currentLoop) {
                    index = 0
                } else {
                    index = anim.frames.size - 1
                    finished = true
                    break
                }
            }
        }
    }
}
