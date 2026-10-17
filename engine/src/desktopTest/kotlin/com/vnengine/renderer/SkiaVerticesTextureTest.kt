package com.vnengine.renderer

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.VertexMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the texcoord convention used by the Live2D desktop backend: with an
 * image shader, [org.jetbrains.skia.Canvas.drawVertices] samples in texture
 * pixel (texel) space, so UVs must be multiplied by the texture size.
 */
class SkiaVerticesTextureTest {
    @Test
    fun texturedIndexedQuadSamplesInTexelSpace() {
        // Texture: left column red, right column blue.
        val textureSurface = Surface.makeRasterN32Premul(2, 2)
        textureSurface.canvas.apply {
            clear(0xFFFF0000.toInt())
            drawRect(Rect.makeLTRB(1f, 0f, 2f, 2f), Paint().apply { color = 0xFF0000FF.toInt() })
        }
        val texture = textureSurface.makeImageSnapshot()

        val target = Surface.makeRasterN32Premul(10, 10)
        val canvas = target.canvas
        canvas.clear(0x00000000)

        val positions = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        val uvs = floatArrayOf(0f, 0f, 2f, 0f, 2f, 2f, 0f, 2f)
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        val paint = Paint().apply {
            shader = texture.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, Matrix33.IDENTITY)
            setAlphaf(1f)
        }
        canvas.drawVertices(VertexMode.TRIANGLES, positions, null, uvs, indices, org.jetbrains.skia.BlendMode.SRC_OVER, paint)

        val bitmap = Bitmap.makeFromImage(target.makeImageSnapshot())
        assertEquals(0xFFFF0000.toInt(), bitmap.getColor(2, 5))
        assertEquals(0xFF0000FF.toInt(), bitmap.getColor(8, 5))

        texture.close()
        bitmap.close()
    }
}
