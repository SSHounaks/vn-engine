package com.vnengine

import com.vnengine.dsl.Position
import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.TimelineEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineBuilderDefaultsTest {
    @Test
    fun defaultArgumentsProduceExpectedDurations() {
        val b = SceneBuilder()
        b.timeline {
            dissolve()
            slide()
            move("hero", Position.Left)
            shake("hero")
            flash()
            zoom("hero")
            wait()
            play("hero", "idle")
        }
        val e = b.timeline.events
        assertEquals(800L, (e[0] as TimelineEvent.Transition).durationSeconds.let { (it * 1000).toLong() })
        assertEquals(600L, (e[1] as TimelineEvent.Transition).durationSeconds.let { (it * 1000).toLong() })
        assertEquals(1000L, (e[2] as TimelineEvent.Move).durationMs)
        assertEquals(500L, (e[3] as TimelineEvent.Shake).durationMs)
        assertEquals(200L, (e[4] as TimelineEvent.Flash).durationMs)
        assertEquals(800L, (e[5] as TimelineEvent.Zoom).durationMs)
        assertEquals(500L, (e[6] as TimelineEvent.Wait).durationMs)
        assertEquals(1000L, (e[7] as TimelineEvent.Anim).durationMs)
    }
}
