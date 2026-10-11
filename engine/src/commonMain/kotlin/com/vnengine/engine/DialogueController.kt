package com.vnengine.engine

/**
 * Pure, platform-agnostic typewriter state machine for a single dialogue line.
 *
 * It holds no timers or coroutines of its own: the host advances it with
 * explicit elapsed-time deltas ([tick]) or reveals the whole line at once
 * ([completeNow]). This keeps the click-to-complete behavior unit-testable
 * without any UI or Compose dependency.
 */
class DialogueController(
    /** Milliseconds between each revealed character. */
    val charIntervalMs: Long = 55L
) {
    var text: String = ""
        private set

    /** Number of characters currently revealed. */
    var shown: Int = 0
        private set

    /** True once the full line has been revealed (naturally or via [completeNow]). */
    var complete: Boolean = false
        private set

    private var elapsedMs: Long = 0

    /** Binds a new line and restarts typing from zero. */
    fun setLine(newText: String) {
        text = newText
        shown = 0
        complete = newText.isEmpty()
        elapsedMs = 0
    }

    /**
     * Advances the typewriter by [deltaMs]. Returns true when the revealed
     * character count changed as a result.
     */
    fun tick(deltaMs: Long): Boolean {
        if (complete || text.isEmpty()) return false
        val before = shown
        elapsedMs += deltaMs
        val natural = (elapsedMs / charIntervalMs).toInt()
        shown = natural.coerceIn(0, text.length)
        if (shown >= text.length) complete = true
        return shown != before
    }

    /** Reveals the entire line immediately and marks it complete. */
    fun completeNow() {
        shown = text.length
        complete = true
        elapsedMs = text.length.toLong() * charIntervalMs
    }

    /** The substring currently visible to the player. */
    val visibleText: String
        get() = if (shown >= text.length) text else text.substring(0, shown.coerceIn(0, text.length))

    /** (revealed, total) character counts, for progress display. */
    val progress: Pair<Int, Int>
        get() = shown to text.length
}
