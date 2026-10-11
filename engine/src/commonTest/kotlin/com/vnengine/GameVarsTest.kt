package com.vnengine

import com.vnengine.engine.GameVars
import com.vnengine.engine.VarKey
import com.vnengine.engine.varBool
import com.vnengine.engine.varInt
import com.vnengine.engine.varString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameVarsTest {
    @Test
    fun typedValuesRoundTripThroughSaveMap() {
        val vars = GameVars()
        val i = varInt("i")
        val b = varBool("b")
        val s = varString("s")
        val ratio = VarKey("ratio", 0.0)
        val big = VarKey("big", 0L)
        val f = VarKey("f", 0f)

        vars[i] = 7
        vars[b] = true
        vars[s] = "hi:there"
        vars[ratio] = 3.5
        vars[big] = 9_000_000_000L
        vars[f] = 1.25f

        val restored = GameVars()
        restored.loadFrom(vars.toSaveMap())

        assertEquals(7, restored[i])
        assertEquals(true, restored[b])
        assertEquals("hi:there", restored[s])
        assertEquals(3.5, restored[ratio])
        assertEquals(9_000_000_000L, restored[big])
        assertEquals(1.25f, restored[f])
    }

    @Test
    fun addAccumulatesFromDefault() {
        val vars = GameVars()
        val affection = varInt("affection")
        assertEquals(3, vars.add(affection, 3))
        assertEquals(5, vars.add(affection, 2))
        assertEquals(5, vars[affection])
    }

    @Test
    fun missingValueFallsBackToDefault() {
        assertEquals(42, GameVars()[varInt("nope", 42)])
        assertEquals("x", GameVars()[varString("nope", "x")])
        assertTrue(GameVars()[varBool("nope", true)])
    }
}
