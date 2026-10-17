package com.vnengine.live2d

/** Renderable state for a Live2D sprite: which model, current params, active motion/expression. */
data class Live2DVisual(
    val modelPath: String,
    val modelId: String,
    val parameters: Map<String, Double> = emptyMap(),
    val partOpacities: Map<String, Double> = emptyMap(),
    val motion: String? = null,
    val expression: String? = null,
    val supported: Boolean = false
)

/**
 * Drives one on-screen Live2D model: plays motions and expressions and produces
 * blended parameter values each frame. This is the platform-independent half of
 * Live2D; a [Live2DBackend] turns [Live2DVisual.parameters] into pixels.
 */
class Live2DInstance(
    val model: Live2DModel,
    private val fadeInMs: Double = 500.0
) {
    private val motions = MotionPlayer()
    private val expressions = ExpressionPlayer()

    var motionName: String? = null
        private set
    var expressionName: String? = null
        private set
    private var parameters: Map<String, Double> = emptyMap()
    private var partOpacities: Map<String, Double> = emptyMap()

    fun show(motion: String?, expression: String?) {
        motion?.let { playMotion(it) }
        expression?.let { setExpression(it) }
    }

    fun playMotion(name: String) {
        if (name.equals("still", ignoreCase = true)) {
            motionName = null
            return
        }
        val m = model.loadMotion(name) ?: return
        motions.play(m, loop = m.meta.loop, fadeInMs = fadeInMs)
        motionName = name
    }

    fun setExpression(name: String) {
        if (name.isBlank() || name.equals("null", ignoreCase = true)) {
            expressionName = null
            expressions.set(null)
            return
        }
        val e = model.loadExpression(name) ?: return
        expressions.set(e)
        expressionName = name
    }

    fun tick(deltaMs: Double) {
        val frame = motions.update(deltaMs)
        var params = frame.parameters
        expressions.update(deltaMs / 1000.0)
        expressions.parameters()?.forEach { p ->
            val current = params[p.id] ?: 0.0
            params = params + (p.id to ParameterBlend.apply(current, p.blend, p.value, expressions.blendWeight))
        }
        parameters = params
        partOpacities = applyPose(frame.partOpacities)
    }

    /**
     * Resolves Cubism pose groups: within each group only the part with the greatest
     * opacity stays visible. Parts the motion does not touch default to fully opaque,
     * so a fresh model keeps the group's first part (matching the Cubism SDK behaviour).
     */
    private fun applyPose(motionParts: Map<String, Double>): Map<String, Double> {
        if (model.poseGroups.isEmpty()) return motionParts
        val out = motionParts.toMutableMap()
        model.poseGroups.forEach { group ->
            var visible = -1
            var best = Double.NEGATIVE_INFINITY
            group.forEachIndexed { index, id ->
                val opacity = out[id] ?: 1.0
                if (opacity > best) {
                    best = opacity
                    visible = index
                }
            }
            if (visible < 0) return@forEach
            group.forEachIndexed { index, id -> out[id] = if (index == visible) 1.0 else 0.0 }
        }
        return out
    }

    fun toVisual(): Live2DVisual = Live2DVisual(
        modelPath = model.path,
        modelId = model.id,
        parameters = parameters,
        partOpacities = partOpacities,
        motion = motionName,
        expression = expressionName,
        supported = model.renderer != null
    )

    /** Latest blended parameter values (empty until the first [tick]). */
    val currentParameters: Map<String, Double> get() = parameters

    /** Latest resolved part opacities (empty until the first [tick]). */
    val currentPartOpacities: Map<String, Double> get() = partOpacities
}
