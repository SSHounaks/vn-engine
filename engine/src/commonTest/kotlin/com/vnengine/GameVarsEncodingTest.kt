package com.vnengine

import com.vnengine.engine.GameVars
import com.vnengine.engine.VarKey
import com.vnengine.engine.varBool
import com.vnengine.engine.varInt
import com.vnengine.engine.varString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameVarsEncodingTest {
    @Test
    fun allScalarTypesRoundTrip() {
        val vars = GameVars()
        vars[VarKey("long", 1L)] = 9_000_000_000L
        vars[VarKey("float", 1f)] = 1.5f
        vars[VarKey("double", 1.0)] = 2.5
        vars[VarKey("bool", true)] = false
        vars[VarKey("str", "a")] = "hello"
        vars.storeSet("nil", null)

        val encoded = vars.toSaveMap()
        assertEquals("l:9000000000", encoded["long"])
        assertEquals("f:1.5", encoded["float"])
        assertEquals("d:2.5", encoded["double"])
        assertEquals("b:false", encoded["bool"])
        assertEquals("s:hello", encoded["str"])
        assertEquals("z:", encoded["nil"])

        val view = GameVars()
        view.loadFrom(encoded)
        assertEquals(9_000_000_000L, view[VarKey("long", 0L)])
        assertEquals(1.5f, view[VarKey("float", 0f)])
        assertEquals(2.5, view[VarKey("double", 0.0)])
        assertEquals(false, view[varBool("bool")])
        assertEquals("hello", view[varString("str")])
        assertEquals("z:", view.toSaveMap()["nil"])
    }

    @Test
    fun observerRecordsEveryAccessType() {
        val seen = mutableListOf<String>()
        val vars = GameVars { name, type -> seen += "$name:$type" }

        vars[varInt("a")] = 1
        vars.add(varInt("a"), 2)
        vars[varBool("b")] = true
        vars[varString("s")] = "x"
        vars[VarKey("l", 0L)] = 1L
        vars[VarKey("f", 0f)] = 1f
        vars[VarKey("d", 0.0)] = 1.0
        vars.storeSet("n", null)
        vars[varInt("readOnly")]

        assertEquals(3, vars[varInt("a")])
        for (expected in listOf("a:Int", "b:Boolean", "s:String", "l:Long", "f:Float", "d:Double", "n:Null")) {
            assertTrue(seen.contains(expected), "missing $expected in $seen")
        }
    }

    @Test
    fun decodeFallsBackToDefaultsForGarbage() {
        val view = GameVars()
        view.loadFrom(mapOf("junk" to "not-tagged"))
        assertEquals("not-tagged", view[varString("junk")])
    }
}
