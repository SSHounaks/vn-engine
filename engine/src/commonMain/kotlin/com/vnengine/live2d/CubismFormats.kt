package com.vnengine.live2d

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models for the Live2D Cubism 3/4/5 asset formats. These are plain JSON files
 * emitted by the Cubism editor; only the parts needed to drive motion playback and
 * expressions are modeled. Unknown fields are ignored by the JSON parser.
 *
 * See: https://github.com/Live2D/CubismNativeSamples (model3/motion3/exp3 schemas)
 */

@Serializable
data class CubismModelDef(
    @SerialName("Version") val version: Int = 3,
    @SerialName("FileReferences") val fileReferences: FileReferences = FileReferences(),
    @SerialName("Groups") val groups: List<CubismGroup> = emptyList()
)

@Serializable
data class CubismGroup(
    @SerialName("Target") val target: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Ids") val ids: List<String> = emptyList()
)

@Serializable
data class FileReferences(
    @SerialName("Moc") val moc: String = "",
    @SerialName("Textures") val textures: List<String> = emptyList(),
    @SerialName("Motions") val motions: Map<String, List<MotionReference>> = emptyMap(),
    @SerialName("Expressions") val expressions: List<ExpressionReference> = emptyList(),
    @SerialName("Pose") val pose: String = ""
)

@Serializable
data class MotionReference(
    @SerialName("File") val file: String = "",
    @SerialName("FadeInTime") val fadeInTime: Double? = null,
    @SerialName("FadeOutTime") val fadeOutTime: Double? = null
)

@Serializable
data class ExpressionReference(
    @SerialName("Name") val name: String = "",
    @SerialName("File") val file: String = ""
)

@Serializable
data class CubismMotion(
    @SerialName("Version") val version: Int = 3,
    @SerialName("Meta") val meta: MotionMeta = MotionMeta(),
    @SerialName("Curves") val curves: List<MotionCurveDef> = emptyList()
)

@Serializable
data class MotionMeta(
    @SerialName("Duration") val duration: Double = 0.0,
    @SerialName("Loop") val loop: Boolean = false,
    @SerialName("Fps") val fps: Double = 30.0,
    @SerialName("AreBeziersRestricted") val areBeziersRestricted: Boolean = true
)

@Serializable
data class MotionCurveDef(
    @SerialName("Target") val target: String = "Parameter",
    @SerialName("Id") val id: String = "",
    /** Flat cubism segment encoding; see [MotionCurve]. */
    @SerialName("Segments") val segments: List<Double> = emptyList()
)

@Serializable
data class CubismPose(
    @SerialName("Type") val type: String = "Live2D Pose",
    @SerialName("Groups") val groups: List<List<PosePart>> = emptyList()
)

@Serializable
data class PosePart(
    @SerialName("Id") val id: String = "",
    @SerialName("Link") val link: List<String> = emptyList()
)

@Serializable
data class CubismExpression(
    @SerialName("Type") val type: String = "Live2D Expression",
    @SerialName("FadeInTime") val fadeInTime: Double = 1.0,
    @SerialName("FadeOutTime") val fadeOutTime: Double = 1.0,
    @SerialName("Parameters") val parameters: List<ExpressionParameter> = emptyList()
)

@Serializable
data class ExpressionParameter(
    @SerialName("Id") val id: String = "",
    @SerialName("Value") val value: Double = 0.0,
    @SerialName("Blend") val blend: String = "Add"
)
