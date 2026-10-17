package com.vnengine.live2d

/**
 * Evaluates a single Cubism motion curve at a given time (in seconds).
 *
 * The [segments] list uses Cubism's flat encoding: the first pair is the start
 * point `(t0, v0)`, then a sequence of segments. Each segment begins with a
 * type discriminator:
 *  - `0` linear          -> `endX, endY`
 *  - `1` bezier          -> `cp1X, cp1Y, cp2X, cp2Y, endX, endY`
 *  - `2` stepped         -> `endX, endY`
 *  - `3` inverse-stepped -> `endX, endY`
 */
class MotionCurve(val segments: List<Double>) {

    private sealed interface Piece {
        val x0: Double
        val x1: Double
        val y0: Double
        val y1: Double
        fun eval(time: Double): Double
    }

    private class Linear(override val x0: Double, override val y0: Double, override val x1: Double, override val y1: Double) : Piece {
        override fun eval(time: Double): Double {
            val span = x1 - x0
            if (span <= 0.0) return y1
            val t = ((time - x0) / span).coerceIn(0.0, 1.0)
            return y0 + (y1 - y0) * t
        }
    }

    private class Stepped(override val x0: Double, override val y0: Double, override val x1: Double, override val y1: Double) : Piece {
        override fun eval(time: Double): Double = y0
    }

    private class InverseStepped(override val x0: Double, override val y0: Double, override val x1: Double, override val y1: Double) : Piece {
        override fun eval(time: Double): Double = y1
    }

    private class Bezier(
        override val x0: Double, override val y0: Double,
        override val x1: Double, override val y1: Double,
        val c1x: Double, val c1y: Double, val c2x: Double, val c2y: Double
    ) : Piece {
        override fun eval(time: Double): Double {
            if (x1 <= x0) return y1
            val t = solveTForX(time.coerceIn(x0, x1))
            return cubic(y0, c1y, c2y, y1, t)
        }

        private fun solveTForX(x: Double): Double {
            var lo = 0.0
            var hi = 1.0
            repeat(40) {
                val mid = (lo + hi) / 2.0
                if (cubic(x0, c1x, c2x, x1, mid) < x) lo = mid else hi = mid
            }
            return (lo + hi) / 2.0
        }

        private fun cubic(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
            val u = 1.0 - t
            return u * u * u * p0 + 3.0 * u * u * t * p1 + 3.0 * u * t * t * p2 + t * t * t * p3
        }
    }

    private val pieces: List<Piece> = parse(segments)

    /** Samples the curve's value at [time] seconds, clamped to the curve's range. */
    fun sample(time: Double): Double {
        if (pieces.isEmpty()) return 0.0
        val first = pieces.first()
        if (time <= first.x0) return first.y0
        for (piece in pieces) {
            if (time <= piece.x1) return piece.eval(time)
        }
        return pieces.last().y1
    }

    private companion object {
        fun parse(segments: List<Double>): List<Piece> {
            if (segments.size < 2) return emptyList()
            val result = mutableListOf<Piece>()
            var index = 2
            var prevX = segments[0]
            var prevY = segments[1]
            while (index < segments.size) {
                val type = segments[index].toInt()
                index++
                when (type) {
                    0 -> {
                        val ex = segments.getOrNull(index) ?: break
                        val ey = segments.getOrNull(index + 1) ?: break
                        index += 2
                        result.add(Linear(prevX, prevY, ex, ey)); prevX = ex; prevY = ey
                    }
                    1 -> {
                        if (index + 5 >= segments.size) break
                        val c1x = segments[index]; val c1y = segments[index + 1]
                        val c2x = segments[index + 2]; val c2y = segments[index + 3]
                        val ex = segments[index + 4]; val ey = segments[index + 5]
                        index += 6
                        result.add(Bezier(prevX, prevY, ex, ey, c1x, c1y, c2x, c2y)); prevX = ex; prevY = ey
                    }
                    2 -> {
                        val ex = segments.getOrNull(index) ?: break
                        val ey = segments.getOrNull(index + 1) ?: break
                        index += 2
                        result.add(Stepped(prevX, prevY, ex, ey)); prevX = ex; prevY = ey
                    }
                    3 -> {
                        val ex = segments.getOrNull(index) ?: break
                        val ey = segments.getOrNull(index + 1) ?: break
                        index += 2
                        result.add(InverseStepped(prevX, prevY, ex, ey)); prevX = ex; prevY = ey
                    }
                    else -> return result
                }
            }
            return result
        }
    }
}
