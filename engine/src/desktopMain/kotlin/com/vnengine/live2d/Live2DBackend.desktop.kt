package com.vnengine.live2d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import org.jetbrains.skia.VertexMode
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * Desktop binding to the proprietary Live2D Cubism Core native library.
 *
 * The library is NOT bundled: install it from the Cubism SDK for Native
 * (https://www.live2d.com/en/sdk/download/native/) and make sure
 * `Live2DCubismCore` is on the JVM's library path, or point
 * `-Dvnengine.cubism.core=/path/to/live2dcubismcore` (or the
 * `VNENGINE_CUBISM_CORE` environment variable) at it. When the library is
 * absent [isSupported] is false and the engine falls back to a static preview.
 *
 * Only the Core API surface needed to drive and draw a model is bound (v5 header
 * `Live2DCubismCore.h`). Clipping masks and multiply/screen colors are applied;
 * offscreens are not yet.
 */
actual object Live2DBackend {
    private val libraryName: String =
        System.getProperty("vnengine.cubism.core")
            ?: System.getenv("VNENGINE_CUBISM_CORE")
            ?: "Live2DCubismCore"

    private val core: CubismCore? by lazy {
        try {
            Native.load(libraryName, CubismCore::class.java)
        } catch (t: Throwable) {
            null
        }
    }

    actual fun isSupported(): Boolean = core != null

    actual fun load(mocBytes: ByteArray, textureBytes: List<ByteArray>): Live2DRenderer? {
        val lib = core ?: return null
        return try {
            DesktopLive2DRenderer(lib, mocBytes, textureBytes)
        } catch (t: Throwable) {
            println("VN: Live2D model load failed: ${t.message}")
            null
        }
    }
}

private const val ALIGN_MOC = 64
private const val ALIGN_MODEL = 16
private const val BLEND_ADDITIVE = 1 shl 0
private const val BLEND_MULTIPLICATIVE = 1 shl 1
private const val IS_INVERTED_MASK = 1 shl 3
private val PTR: Long = Native.POINTER_SIZE.toLong()

/** JNA view of the Cubism Core symbol set (SDK v5). */
private interface CubismCore : Library {
    fun csmReviveMocInPlace(address: Pointer, size: Int): Pointer?
    fun csmGetSizeofModel(moc: Pointer): Int
    fun csmInitializeModelInPlace(moc: Pointer, address: Pointer, size: Int): Pointer?
    fun csmUpdateModel(model: Pointer)
    fun csmReadCanvasInfo(model: Pointer, sizeInPixels: Pointer, originInPixels: Pointer, pixelsPerUnit: Pointer)

    fun csmGetParameterCount(model: Pointer): Int
    fun csmGetParameterIds(model: Pointer): Pointer?
    fun csmGetParameterMinimumValues(model: Pointer): Pointer?
    fun csmGetParameterMaximumValues(model: Pointer): Pointer?
    fun csmGetParameterValues(model: Pointer): Pointer?

    fun csmGetDrawableCount(model: Pointer): Int
    fun csmGetDrawableTextureIndices(model: Pointer): Pointer?
    fun csmGetDrawableConstantFlags(model: Pointer): Pointer?
    fun csmGetDrawableVertexCounts(model: Pointer): Pointer?
    fun csmGetDrawableVertexPositions(model: Pointer): Pointer?
    fun csmGetDrawableVertexUvs(model: Pointer): Pointer?
    fun csmGetDrawableIndexCounts(model: Pointer): Pointer?
    fun csmGetDrawableIndices(model: Pointer): Pointer?
    fun csmGetDrawableOpacities(model: Pointer): Pointer?
    fun csmGetDrawableDrawOrders(model: Pointer): Pointer?
    fun csmGetDrawableRenderOrders(model: Pointer): Pointer?
    fun csmGetDrawableMaskCounts(model: Pointer): Pointer?
    fun csmGetDrawableMasks(model: Pointer): Pointer?
    fun csmGetDrawableMultiplyColors(model: Pointer): Pointer?
    fun csmGetDrawableScreenColors(model: Pointer): Pointer?
    fun csmGetPartCount(model: Pointer): Int
    fun csmGetPartIds(model: Pointer): Pointer?
    fun csmGetPartOpacities(model: Pointer): Pointer?
    fun csmResetDrawableDynamicFlags(model: Pointer)
}

/** Native buffer aligned to the requirement Cubism Core places on moc/model memory. */
private class AlignedBuffer(size: Int, alignment: Int) {
    private val memory = Memory((size + alignment).toLong())
    val pointer: Pointer

    init {
        val base = Pointer.nativeValue(memory)
        val mask = (alignment - 1).toLong()
        val aligned = (base + mask) and mask.inv()
        pointer = memory.share(aligned - base)
    }

    fun write(bytes: ByteArray) {
        pointer.write(0, bytes, 0, bytes.size)
    }
}

private class DesktopLive2DRenderer(
    private val core: CubismCore,
    mocBytes: ByteArray,
    textureBytes: List<ByteArray>
) : Live2DRenderer {
    private val mocBuffer = AlignedBuffer(mocBytes.size, ALIGN_MOC)
    private val modelBuffer: AlignedBuffer
    private val model: Pointer

    private val parameterIndex: Map<String, Int>
    private val parameterValues: Pointer
    private val parameterMin: FloatArray
    private val parameterMax: FloatArray

    private val drawableCount: Int
    private val textureIndices: IntArray
    private val vertexCounts: IntArray
    private val indexCounts: IntArray
    private val positionPtrs: Pointer
    private val uvPtrs: Pointer
    private val indicesPtrs: Pointer
    private val opacities: Pointer
    private val constantFlags: ByteArray?
    private val drawOrder: IntArray
    private val renderOrder: IntArray
    private val maskCounts: IntArray
    private val masksPtr: Pointer?
    private val multiplyColors: Pointer?
    private val screenColors: Pointer?
    private val partOpacities: Pointer?
    private val partIndex: Map<String, Int>
    private val textures: List<Image>
    private val atlasImage: Image?
    private val atlasOffsetY: FloatArray
    private val ownsAtlas: Boolean
    private val atlasEnabled: Boolean

    private val unitWidth: Float
    private val unitHeight: Float
    private val originX: Float
    private val originY: Float

    private var surface: Surface? = null

    private val paint = Paint()
    private val maskPath = Path()
    private val drawOrderSorted: IntArray
    private val blendModes: Array<BlendMode>
    private val uvsCache: Array<FloatArray?>
    private val indicesCache: Array<ShortArray?>
    private val positionsCache: Array<FloatArray?>
    private val shaders: Array<Shader?>
    private val filters: Array<ColorFilter?>
    private val maskDrawables: Array<IntArray?>

    init {
        mocBuffer.write(mocBytes)
        val moc = core.csmReviveMocInPlace(mocBuffer.pointer, mocBytes.size)
            ?: throw IllegalStateException("csmReviveMocInPlace failed")
        val modelSize = core.csmGetSizeofModel(moc)
        modelBuffer = AlignedBuffer(modelSize, ALIGN_MODEL)
        model = core.csmInitializeModelInPlace(moc, modelBuffer.pointer, modelSize)
            ?: throw IllegalStateException("csmInitializeModelInPlace failed")
        core.csmUpdateModel(model)

        val sizeMem = Memory(8)
        val originMem = Memory(8)
        val ppuMem = Memory(4)
        core.csmReadCanvasInfo(model, sizeMem, originMem, ppuMem)
        val ppu = ppuMem.getFloat(0).takeIf { it > 0f } ?: 1f
        unitWidth = sizeMem.getFloat(0) / ppu
        unitHeight = sizeMem.getFloat(4) / ppu
        originX = originMem.getFloat(0) / ppu
        originY = originMem.getFloat(4) / ppu

        val parameterCount = core.csmGetParameterCount(model)
        parameterIndex = buildMap {
            val ids = core.csmGetParameterIds(model)
            for (i in 0 until parameterCount) {
                val idPtr = ids?.getPointer(i.toLong() * PTR) ?: continue
                val id = idPtr.getString(0)
                put(id, i)
            }
        }
        parameterValues = core.csmGetParameterValues(model)
            ?: throw IllegalStateException("csmGetParameterValues failed")
        parameterMin = core.csmGetParameterMinimumValues(model)?.getFloatArray(0, parameterCount) ?: FloatArray(parameterCount)
        parameterMax = core.csmGetParameterMaximumValues(model)?.getFloatArray(0, parameterCount) ?: FloatArray(parameterCount)

        drawableCount = core.csmGetDrawableCount(model)
        textureIndices = core.csmGetDrawableTextureIndices(model)?.getIntArray(0, drawableCount) ?: IntArray(drawableCount)
        vertexCounts = core.csmGetDrawableVertexCounts(model)?.getIntArray(0, drawableCount) ?: IntArray(drawableCount)
        indexCounts = core.csmGetDrawableIndexCounts(model)?.getIntArray(0, drawableCount) ?: IntArray(drawableCount)
        positionPtrs = core.csmGetDrawableVertexPositions(model)
            ?: throw IllegalStateException("csmGetDrawableVertexPositions failed")
        uvPtrs = core.csmGetDrawableVertexUvs(model)
            ?: throw IllegalStateException("csmGetDrawableVertexUvs failed")
        indicesPtrs = core.csmGetDrawableIndices(model)
            ?: throw IllegalStateException("csmGetDrawableIndices failed")
        opacities = core.csmGetDrawableOpacities(model)
            ?: throw IllegalStateException("csmGetDrawableOpacities failed")
        val partCount = core.csmGetPartCount(model)
        partOpacities = core.csmGetPartOpacities(model)
        val partIds = core.csmGetPartIds(model)?.getPointerArray(0, partCount) ?: emptyArray()
        partIndex = buildMap {
            partIds.forEachIndexed { i, p -> p?.let { put(it.getString(0), i) } }
        }
        constantFlags = core.csmGetDrawableConstantFlags(model)?.getByteArray(0, drawableCount)
        drawOrder = resolveDrawOrder()
        renderOrder = core.csmGetDrawableRenderOrders(model)?.getIntArray(0, drawableCount) ?: drawOrder
        maskCounts = core.csmGetDrawableMaskCounts(model)?.getIntArray(0, drawableCount) ?: IntArray(drawableCount)
        masksPtr = core.csmGetDrawableMasks(model)
        multiplyColors = core.csmGetDrawableMultiplyColors(model)
        screenColors = core.csmGetDrawableScreenColors(model)

        textures = textureBytes.mapNotNull { Image.makeFromEncoded(it) }

        atlasEnabled = textures.size > 1
        if (atlasEnabled) {
            val maxW = textures.maxOf { it.width }
            val totalH = textures.sumOf { it.height }
            val surf = Surface.makeRasterN32Premul(maxW, totalH)
            atlasOffsetY = FloatArray(textures.size)
            var y = 0f
            textures.forEachIndexed { i, img ->
                atlasOffsetY[i] = y
                surf.canvas.drawImage(img, 0f, y)
                y += img.height
            }
            atlasImage = surf.makeImageSnapshot()
            surf.close()
            ownsAtlas = true
        } else {
            atlasOffsetY = FloatArray(textures.size)
            atlasImage = textures.firstOrNull()
            ownsAtlas = false
        }

        drawOrderSorted = (0 until drawableCount).sortedBy { renderOrder[it] }.toIntArray()
        blendModes = Array(drawableCount) { i ->
            val f = constantFlags?.get(i)?.toInt() ?: 0
            when {
                f and BLEND_ADDITIVE != 0 -> BlendMode.PLUS
                f and BLEND_MULTIPLICATIVE != 0 -> BlendMode.MULTIPLY
                else -> BlendMode.SRC_OVER
            }
        }
        uvsCache = Array(drawableCount) { i -> buildUvs(i) }
        indicesCache = Array(drawableCount) { i ->
            val ic = indexCounts[i]
            if (ic <= 0) null else indicesPtrs.getPointer(i.toLong() * PTR).getShortArray(0, ic)
        }
        positionsCache = Array(drawableCount) { i ->
            val vc = vertexCounts[i]
            if (vc <= 0) null else FloatArray(vc * 2)
        }
        val sampling = SamplingMode.DEFAULT
        shaders = if (atlasEnabled) {
            listOfNotNull(atlasImage?.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, sampling, Matrix33.IDENTITY)).toTypedArray()
        } else {
            Array(textures.size) { ti -> textures[ti].makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, sampling, Matrix33.IDENTITY) }
        }
        filters = Array(drawableCount) { i -> colorFilter(i) }
        maskDrawables = Array(drawableCount) { i -> maskIndicesOf(i) }
    }

    private fun resolveDrawOrder(): IntArray {
        return core.csmGetDrawableDrawOrders(model)?.getIntArray(0, drawableCount) ?: IntArray(drawableCount)
    }

    override fun setPartOpacities(values: Map<String, Double>) {
        val ptr = partOpacities ?: return
        for ((id, value) in values) {
            val index = partIndex[id] ?: continue
            ptr.setFloat(index.toLong() * 4, value.toFloat())
        }
    }

    override fun setParameterValues(values: Map<String, Double>) {
        for ((id, value) in values) {
            val index = parameterIndex[id] ?: continue
            val lo = parameterMin[index].toDouble()
            val hi = parameterMax[index].toDouble()
            val clamped = if (lo <= hi) value.coerceIn(lo, hi) else value
            parameterValues.setFloat(index.toLong() * 4, clamped.toFloat())
        }
    }

    override fun render(widthPx: Int, heightPx: Int): ImageBitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        core.csmResetDrawableDynamicFlags(model)
        core.csmUpdateModel(model)

        val scale = minOf(w / unitWidth, h / unitHeight)
        val offsetX = (w - unitWidth * scale) / 2f
        val offsetY = (h - unitHeight * scale) / 2f

        val target = surface?.takeIf { it.width == w && it.height == h }
            ?: Surface.makeRasterN32Premul(w, h).also { surface = it }
        val canvas = target.canvas
        canvas.clear(0x00000000)

        for (i in drawOrderSorted) {
            val opacity = opacities.getFloat(i.toLong() * 4)
            if (opacity <= 0.001f) continue
            val shader = (if (atlasEnabled) shaders.firstOrNull() else shaders.getOrNull(textureIndices[i])) ?: continue
            val positions = positionsCache[i] ?: continue
            val uvs = uvsCache[i] ?: continue
            val indices = indicesCache[i] ?: continue

            fillPositions(i, positions, offsetX, offsetY, scale)

            paint.shader = shader
            paint.colorFilter = filters[i]
            paint.setAlphaf(opacity.coerceIn(0f, 1f))

            val mask = buildMaskPath(i, offsetX, offsetY, scale)
            if (mask != null) {
                val mode = if ((constantFlags?.get(i)?.toInt() ?: 0) and IS_INVERTED_MASK != 0) ClipMode.DIFFERENCE else ClipMode.INTERSECT
                canvas.save()
                canvas.clipPath(mask, mode, true)
                canvas.drawVertices(VertexMode.TRIANGLES, positions, null, uvs, indices, blendModes[i], paint)
                canvas.restore()
            } else {
                canvas.drawVertices(VertexMode.TRIANGLES, positions, null, uvs, indices, blendModes[i], paint)
            }
        }
        return target.makeImageSnapshot().toComposeImageBitmap()
    }



    private fun buildUvs(index: Int): FloatArray? {
        val vc = vertexCounts[index]
        if (vc <= 0) return null
        val ti = textureIndices[index]
        val tex = textures.getOrNull(ti) ?: return null
        val src = uvPtrs.getPointer(index.toLong() * PTR).getFloatArray(0, vc * 2)
        val tw = tex.width.toFloat()
        val th = tex.height.toFloat()
        val oy = atlasOffsetY.getOrElse(ti) { 0f }
        val out = FloatArray(vc * 2)
        for (v in 0 until vc) {
            out[v * 2] = src[v * 2] * tw
            out[v * 2 + 1] = (1f - src[v * 2 + 1]) * th + oy
        }
        return out
    }

    private fun maskIndicesOf(index: Int): IntArray? {
        val count = maskCounts.getOrElse(index) { 0 }
        if (count <= 0) return null
        val ptr = masksPtr ?: return null
        val base = ptr.getPointer(index.toLong() * PTR)
        return IntArray(count) { k -> base.getInt(k.toLong() * 4) }
    }

    /** Cubism multiply then screen color, expressed as an affine color matrix per drawable. */
    private fun colorFilter(index: Int): ColorFilter? {
        val mult = try {
            multiplyColors?.getFloatArray(index.toLong() * 16, 4)
        } catch (t: Throwable) {
            null
        } ?: return null
        val screen = try {
            screenColors?.getFloatArray(index.toLong() * 16, 4)
        } catch (t: Throwable) {
            null
        } ?: floatArrayOf(0f, 0f, 0f, 0f)
        if (mult[0] == 1f && mult[1] == 1f && mult[2] == 1f && screen[0] == 0f && screen[1] == 0f && screen[2] == 0f) {
            return null
        }
        val sr = mult[0] * (1f - screen[0])
        val sg = mult[1] * (1f - screen[1])
        val sb = mult[2] * (1f - screen[2])
        return ColorFilter.makeMatrix(
            ColorMatrix(
                sr, 0f, 0f, 0f, screen[0],
                0f, sg, 0f, 0f, screen[1],
                0f, 0f, sb, 0f, screen[2],
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun transformedPositions(index: Int, offsetX: Float, offsetY: Float, scale: Float): FloatArray {
        val out = positionsCache.getOrNull(index) ?: return FloatArray(0)
        fillPositions(index, out, offsetX, offsetY, scale)
        return out
    }

    private fun fillPositions(index: Int, out: FloatArray, offsetX: Float, offsetY: Float, scale: Float) {
        val src = positionPtrs.getPointer(index.toLong() * PTR).getFloatArray(0, out.size)
        var v = 0
        while (v < out.size) {
            out[v] = offsetX + (src[v] + originX) * scale
            out[v + 1] = offsetY + (originY - src[v + 1]) * scale
            v += 2
        }
    }

    /** Union of every mask drawable's geometry, in screen space (reused buffer). */
    private fun buildMaskPath(index: Int, offsetX: Float, offsetY: Float, scale: Float): Path? {
        val maskIndices = maskDrawables.getOrNull(index) ?: return null
        if (maskIndices.isEmpty()) return null
        maskPath.reset()
        var any = false
        for (m in maskIndices) {
            if (m < 0 || m >= drawableCount) continue
            val ic = indexCounts[m]
            if (vertexCounts[m] <= 0 || ic <= 0) continue
            val idx = indicesCache.getOrNull(m) ?: continue
            val pos = transformedPositions(m, offsetX, offsetY, scale)
            var t = 0
            while (t + 2 < ic) {
                maskPath.moveTo(pos[idx[t].toInt() * 2], pos[idx[t].toInt() * 2 + 1])
                maskPath.lineTo(pos[idx[t + 1].toInt() * 2], pos[idx[t + 1].toInt() * 2 + 1])
                maskPath.lineTo(pos[idx[t + 2].toInt() * 2], pos[idx[t + 2].toInt() * 2 + 1])
                maskPath.closePath()
                t += 3
            }
            any = true
        }
        return if (any) maskPath else null
    }

    override fun dispose() {
        surface?.close()
        surface = null
        shaders.forEach { it?.close() }
        if (ownsAtlas) atlasImage?.close()
        textures.forEach { it.close() }
        maskPath.close()
        paint.close()
    }
}
