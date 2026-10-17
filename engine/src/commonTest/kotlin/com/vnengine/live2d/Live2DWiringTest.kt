package com.vnengine.live2d

import com.vnengine.FakeAudioManager
import com.vnengine.dsl.Position
import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.SceneScript
import com.vnengine.engine.GameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Live2DPathsTest {
    @Test
    fun resolvesDirectoryToModel3() {
        assertEquals("Characters/Hiyori/Hiyori.model3.json", Live2DPaths.model3Path("Characters/Hiyori"))
        assertEquals("Characters/Hiyori", Live2DPaths.modelDir("Characters/Hiyori"))
    }

    @Test
    fun keepsExplicitModel3Path() {
        val path = "Characters/Hiyori/Hiyori.model3.json"
        assertEquals(path, Live2DPaths.model3Path(path))
        assertEquals("Characters/Hiyori", Live2DPaths.modelDir(path))
    }

    @Test
    fun derivesModelId() {
        assertEquals("Hiyori", Live2DPaths.modelId("Characters/Hiyori"))
        assertEquals("Hiyori", Live2DPaths.modelId("Characters/Hiyori/Hiyori.model3.json"))
        assertEquals("Hiyori", Live2DPaths.modelId("Hiyori"))
    }
}

class Live2DNamingTest {
    private fun def(motions: Map<String, List<MotionReference>>, exprs: List<ExpressionReference>) =
        CubismModelDef(fileReferences = FileReferences(motions = motions, expressions = exprs))

    @Test
    fun indexesMotionsByGroupAndFileStem() {
        val d = def(
            mapOf("Idle" to listOf(MotionReference("motions/idle_01.motion3.json"))),
            emptyList()
        )
        val index = Live2DNaming.motionIndex(d)
        assertEquals("motions/idle_01.motion3.json", index["idle"])
        assertEquals("motions/idle_01.motion3.json", index["idle_01"])
    }

    @Test
    fun indexesExpressionsByName() {
        val d = def(emptyMap(), listOf(ExpressionReference("Happy", "exp/happy.expression3.json")))
        assertEquals("exp/happy.expression3.json", Live2DNaming.expressionIndex(d)["happy"])
    }

    @Test
    fun normalizesNames() {
        assertEquals("01", Live2DNaming.normalize("Hiyori_01", "Hiyori"))
        assertEquals("happy", Live2DNaming.normalize("Happy", "Hiyori"))
    }
}

class Live2DParserTest {
    @Test
    fun parsesModelDefinition() {
        val json = """
            { "Version": 3, "FileReferences": {
                "Moc": "Hiyori.moc3",
                "Textures": ["textures/00.png"],
                "Motions": { "Idle": [ { "File": "motions/m01.motion3.json", "FadeInTime": 0.4 } ] },
                "Expressions": [ { "Name": "happy", "File": "exp/happy.expression3.json" } ]
            } }
        """.trimIndent()
        val def = Live2DParser.model(json)
        assertNotNull(def)
        assertEquals("Hiyori.moc3", def.fileReferences.moc)
        assertEquals(1, def.fileReferences.textures.size)
        assertEquals("motions/m01.motion3.json", def.fileReferences.motions.getValue("Idle").first().file)
        assertEquals(0.4, def.fileReferences.motions.getValue("Idle").first().fadeInTime)
        assertEquals("happy", def.fileReferences.expressions.first().name)
    }

    @Test
    fun parsesMotionAndExpression() {
        val motion = Live2DParser.motion(
            """{ "Version": 3, "Meta": { "Duration": 2.0, "Loop": true },
                 "Curves": [ { "Target": "Parameter", "Id": "ParamAngleX", "Segments": [0,0,0,2.0,10.0] } ] }"""
        )
        assertNotNull(motion)
        assertEquals(2.0, motion.meta.duration)
        assertTrue(motion.meta.loop)
        assertEquals("ParamAngleX", motion.curves.first().id)

        val expr = Live2DParser.expression(
            """{ "FadeInTime": 1.0, "Parameters": [ { "Id": "ParamAngleX", "Value": 5.0, "Blend": "Add" } ] }"""
        )
        assertNotNull(expr)
        assertEquals(1, expr.parameters.size)
        assertEquals("ParamAngleX", expr.parameters.first().id)
        assertEquals(5.0, expr.parameters.first().value)
    }

    @Test
    fun parsesPoseGroups() {
        val pose = Live2DParser.pose(
            """{ "Type": "Live2D Pose", "Groups": [
                 [ { "Id": "Part01ArmRA001", "Link": [] }, { "Id": "Part01ArmRB001", "Link": [] } ]
            ] }"""
        )
        assertNotNull(pose)
        assertEquals(1, pose.groups.size)
        assertEquals("Part01ArmRA001", pose.groups.first().first().id)
    }
}

class Live2DInstanceTest {
    private fun model(): Live2DModel {
        val def = Live2DParser.model(
            """{ "Version": 3, "FileReferences": {
                 "Motions": { "Idle": [ { "File": "m01.motion3.json" } ] },
                 "Expressions": [ { "Name": "happy", "File": "exp/happy.expression3.json" } ]
            } }"""
        )!!
        val files = mapOf(
            "Characters/Hiyori/m01.motion3.json" to
                """{ "Meta": { "Duration": 2.0, "Loop": false },
                     "Curves": [ { "Id": "ParamAngleX", "Segments": [0,0,0,2.0,10.0] } ] }""",
            "Characters/Hiyori/exp/happy.expression3.json" to
                """{ "FadeInTime": 1.0, "Parameters": [ { "Id": "ParamAngleX", "Value": 5.0, "Blend": "Add" } ] }"""
        )
        return Live2DModel("Characters/Hiyori/Hiyori.model3.json", def) { files[it] }
    }

    @Test
    fun motionDrivesParameters() {
        val instance = Live2DInstance(model())
        instance.playMotion("idle")
        instance.tick(1000.0)
        assertEquals(5.0, instance.currentParameters.getValue("ParamAngleX"), 0.001)
    }

    @Test
    fun expressionBlendsOverMotion() {
        val instance = Live2DInstance(model())
        instance.playMotion("idle")
        instance.setExpression("happy")
        instance.tick(1000.0)
        assertEquals(10.0, instance.currentParameters.getValue("ParamAngleX"), 0.001)
        assertEquals("happy", instance.expressionName)
    }

    @Test
    fun unknownMotionIsIgnored() {
        val instance = Live2DInstance(model())
        instance.playMotion("nope")
        assertNull(instance.motionName)
    }

    private fun posedModel(): Live2DModel {
        val def = Live2DParser.model(
            """{ "Version": 3, "FileReferences": {
                 "Pose": "Haru.pose3.json",
                 "Motions": { "Idle": [ { "File": "m01.motion3.json" } ] }
            } }"""
        )!!
        val files = mapOf(
            "Characters/Hiyori/Haru.pose3.json" to
                """{ "Type": "Live2D Pose", "Groups": [
                     [ { "Id": "Part01ArmRA001" }, { "Id": "Part01ArmRB001" } ]
                   ] }""",
            "Characters/Hiyori/m01.motion3.json" to
                """{ "Meta": { "Duration": 1.0, "Loop": false },
                     "Curves": [
                         { "Target": "PartOpacity", "Id": "Part01ArmRB001", "Segments": [0,0,0,1.0,1.0] },
                         { "Target": "PartOpacity", "Id": "Part01ArmRA001", "Segments": [0,0,0,1.0,0.0] }
                     ] }"""
        )
        return Live2DModel("Characters/Hiyori/Hiyori.model3.json", def) { files[it] }
    }

    @Test
    fun loadsPoseGroupsFromModel() {
        assertEquals(
            listOf(listOf("Part01ArmRA001", "Part01ArmRB001")),
            posedModel().poseGroups
        )
    }

    @Test
    fun partOpacityFromMotionIsExposedAndPoseResolved() {
        val instance = Live2DInstance(posedModel())
        instance.playMotion("idle")
        instance.tick(1000.0)
        assertEquals(1.0, instance.currentPartOpacities.getValue("Part01ArmRB001"), 1e-6)
        assertEquals(0.0, instance.currentPartOpacities.getValue("Part01ArmRA001"), 1e-6)
    }

    @Test
    fun poseDefaultsToFirstPartWhenMotionDoesNotDriveIt() {
        val instance = Live2DInstance(posedModel())
        instance.tick(16.0)
        assertEquals(1.0, instance.currentPartOpacities.getValue("Part01ArmRA001"), 1e-6)
        assertEquals(0.0, instance.currentPartOpacities.getValue("Part01ArmRB001"), 1e-6)
    }

    @Test
    fun visualCarriesResolvedPartOpacities() {
        val instance = Live2DInstance(posedModel())
        instance.tick(16.0)
        assertEquals(1.0, instance.toVisual().partOpacities.getValue("Part01ArmRA001"), 1e-6)
    }
}

class Live2DDslTest {
    @Test
    fun live2dVerbAddsModelSprite() {
        val builder = SceneBuilder().apply {
            live2d("Characters/Hiyori", at = Position.Right, motion = "m01", expression = "happy", scale = 1.2f)
        }
        assertEquals(1, builder.sprites.size)
        val sprite = builder.sprites.first()
        assertEquals("Hiyori", sprite.id)
        assertTrue(sprite.isLive2D)
        assertEquals("Characters/Hiyori", sprite.live2dModel)
        assertEquals(Position.Right, sprite.position)
        assertEquals("m01", sprite.motion)
        assertEquals("happy", sprite.expression)
    }

    @Test
    fun missingModelFallsBackWithoutCrash() {
        val script = SceneScript().apply { scene("start") { live2d("Missing/Model") } }
        val engine = GameEngine(script, FakeAudioManager())
        engine.start()
        engine.tick(16)
        val visual = engine.visuals.value.sprites.first()
        assertNull(visual.live2d)
    }
}
