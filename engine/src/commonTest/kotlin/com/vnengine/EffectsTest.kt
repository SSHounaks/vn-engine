package com.vnengine

import com.vnengine.dsl.VarEffect
import com.vnengine.dsl.allOf
import com.vnengine.dsl.applyEffects
import com.vnengine.dsl.between
import com.vnengine.dsl.intGe
import com.vnengine.dsl.intLe
import com.vnengine.dsl.not
import com.vnengine.engine.GameVars
import com.vnengine.engine.varBool
import com.vnengine.engine.varInt
import com.vnengine.engine.varString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectsTest {
    @Test
    fun effectsApplyInOrderExactlyOnce() {
        val vars = GameVars()
        val a = varInt("a")
        val b = varBool("b")
        val s = varString("s")

        applyEffects(
            listOf(
                VarEffect.Add(a, 3),
                VarEffect.Set(s, "x"),
                VarEffect.Flag(b),
                VarEffect.Add(a, 2)
            ),
            vars
        )

        assertEquals(5, vars[a])
        assertEquals("x", vars[s])
        assertTrue(vars[b])
    }

    @Test
    fun runEffectExecutesBlock() {
        val vars = GameVars()
        val a = varInt("a")
        applyEffects(listOf(VarEffect.Run { it[a] = 11 }), vars)
        assertEquals(11, vars[a])
    }

    @Test
    fun predicatesEvaluate() {
        val vars = GameVars()
        val a = varInt("a", 3)
        assertTrue(vars.intGe(a, 3))
        assertTrue(vars.intLe(a, 5))
        assertTrue(vars.between(a, 1, 5))
        assertTrue(vars.not { intGe(a, 10) })
        assertTrue(vars.allOf({ intGe(a, 1) }, { intLe(a, 5) }))
        assertFalse(vars.intGe(a, 4))
    }
}
