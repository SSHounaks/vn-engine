package com.vnengine.live2d

import kotlinx.serialization.json.Json

/** Pure helpers for resolving Cubism model paths and asset names. */
object Live2DPaths {
    /** Resolves a model argument to its `.model3.json` path. */
    fun model3Path(model: String): String {
        if (model.endsWith(".model3.json")) return model
        val dir = model.trimEnd('/')
        val name = dir.substringAfterLast('/')
        return "$dir/$name.model3.json"
    }

    /** Directory containing the model files. */
    fun modelDir(model: String): String {
        val path = model3Path(model)
        return path.substringBeforeLast('/')
    }

    /** Stable identifier for a model (used as the sprite id). */
    fun modelId(model: String): String {
        val base = model.trimEnd('/').substringAfterLast('/')
        return base.removeSuffix(".model3.json").removeSuffix(".json")
    }
}

/** Builds name -> file lookups from a model definition, Ren'Py-style (lowercased, extension stripped). */
object Live2DNaming {
    fun motionIndex(def: CubismModelDef): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        def.fileReferences.motions.forEach { (group, refs) ->
            if (refs.isNotEmpty()) putIfAbsent(map, group.lowercase(), refs.first().file)
            refs.forEach { ref ->
                putIfAbsent(map, fileStem(ref.file, ".motion3.json"), ref.file)
            }
        }
        return map
    }

    fun expressionIndex(def: CubismModelDef): Map<String, String> =
        def.fileReferences.expressions.associate { it.name.lowercase() to it.file }

    private fun fileStem(path: String, suffix: String): String =
        path.substringAfterLast('/').removeSuffix(suffix).lowercase()

    private fun putIfAbsent(map: MutableMap<String, String>, key: String, value: String) {
        if (!map.containsKey(key)) map[key] = value
    }

    /** Normalizes a motion/expression name the way Ren'Py does: lowercase, strip a `model_` prefix. */
    fun normalize(rawName: String, modelId: String): String =
        rawName.lowercase().removePrefix(modelId.lowercase() + "_")
}

/** Parses the Live2D Cubism JSON files. Unknown fields are ignored. */
object Live2DParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun model(text: String): CubismModelDef? =
        runCatching { json.decodeFromString<CubismModelDef>(text) }.getOrNull()

    fun motion(text: String): CubismMotion? =
        runCatching { json.decodeFromString<CubismMotion>(text) }.getOrNull()

    fun expression(text: String): CubismExpression? =
        runCatching { json.decodeFromString<CubismExpression>(text) }.getOrNull()

    fun pose(text: String): CubismPose? =
        runCatching { json.decodeFromString<CubismPose>(text) }.getOrNull()
}

/**
 * A loaded Live2D model: its definition plus lazily-loaded motions and expressions.
 * File access is injected via [reader] so the model is constructible (and testable)
 * without the platform asset backend.
 */
class Live2DModel(
    val path: String,
    val def: CubismModelDef,
    val renderer: Live2DRenderer? = null,
    private val reader: (String) -> String?
) {
    val id: String = Live2DPaths.modelId(path)
    private val dir: String = Live2DPaths.modelDir(path)
    private val motionFiles: Map<String, String> = Live2DNaming.motionIndex(def)
    private val expressionFiles: Map<String, String> = Live2DNaming.expressionIndex(def)

    private val motionCache = mutableMapOf<String, CubismMotion?>()
    private val expressionCache = mutableMapOf<String, CubismExpression?>()

    val motionNames: List<String> get() = motionFiles.keys.sorted()
    val expressionNames: List<String> get() = expressionFiles.keys.sorted()

    /** Cubism pose groups: within each group at most one part may be visible at a time. */
    val poseGroups: List<List<String>> by lazy {
        val file = def.fileReferences.pose.takeIf { it.isNotBlank() } ?: return@lazy emptyList()
        val text = reader("$dir/$file") ?: return@lazy emptyList()
        Live2DParser.pose(text)?.groups?.mapNotNull { group ->
            group.mapNotNull { it.id.takeIf(String::isNotEmpty) }.takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    fun loadMotion(name: String): CubismMotion? {
        val key = Live2DNaming.normalize(name, id)
        return motionCache.getOrPut(key) {
            val file = motionFiles[key] ?: motionFiles[name.lowercase()] ?: return@getOrPut null
            val text = reader("$dir/$file") ?: return@getOrPut null
            Live2DParser.motion(text)
        }
    }

    fun loadExpression(name: String): CubismExpression? {
        val key = Live2DNaming.normalize(name, id)
        return expressionCache.getOrPut(key) {
            val file = expressionFiles[key] ?: expressionFiles[name.lowercase()] ?: return@getOrPut null
            val text = reader("$dir/$file") ?: return@getOrPut null
            Live2DParser.expression(text)
        }
    }
}

/** Loads Live2D models from the platform assets, creating a native renderer when possible. */
object Live2DLoader {
    private val cache = mutableMapOf<String, Live2DModel>()

    fun load(model: String): Live2DModel? = cache[model] ?: loadUncached(model)?.also { cache[model] = it }

    private fun loadUncached(model: String): Live2DModel? {
        val path = Live2DPaths.model3Path(model)
        val text = Live2DAssets.readText(path) ?: return null
        val def = Live2DParser.model(text) ?: return null
        val dir = Live2DPaths.modelDir(path)
        val renderer = loadRenderer(dir, def)
        if (renderer != null) Live2DRuntime.register(path, renderer)
        return Live2DModel(path, def, renderer) { p -> Live2DAssets.readText(p) }
    }

    private fun loadRenderer(dir: String, def: CubismModelDef): Live2DRenderer? {
        if (!Live2DBackend.isSupported()) return null
        val moc = def.fileReferences.moc.takeIf { it.isNotEmpty() } ?: return null
        val mocBytes = Live2DAssets.readBytes("$dir/$moc") ?: return null
        val textureBytes = def.fileReferences.textures.mapNotNull { Live2DAssets.readBytes("$dir/$it") }
        if (textureBytes.isEmpty()) return null
        return Live2DBackend.load(mocBytes, textureBytes)
    }
}
