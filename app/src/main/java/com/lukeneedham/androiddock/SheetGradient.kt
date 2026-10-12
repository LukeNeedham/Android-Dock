package com.lukeneedham.androiddock

import androidx.core.graphics.ColorUtils

/**
 * One colour of the sheet's background gradient. [position] is how far out from the screen corner
 * it falls: 0 at the corner, 1 at the outer edge of the background.
 */
data class ColorStop(val position: Float, val color: Int)

/**
 * The sheet's background as colour stops, and the pure helpers that read and edit them. A single
 * stop is a flat colour; before the first stop and after the last the gradient holds that stop's
 * colour.
 */
object SheetGradient {

    const val STOPS_MAX = 8

    /** Opaque black at the corner fading to nothing at the edge. */
    val DEFAULT = listOf(ColorStop(0f, 0xFF000000.toInt()), ColorStop(1f, 0x00000000))

    /** Stored as "position:argbhex,...". */
    fun encode(stops: List<ColorStop>): String =
        stops.joinToString(",") { "${it.position}:${Integer.toHexString(it.color)}" }

    /** The stops in [text], sorted, or null if there is none to be had. */
    fun decode(text: String?): List<ColorStop>? =
        text?.split(',')?.mapNotNull { part ->
            val position = part.substringBefore(':').toFloatOrNull() ?: return@mapNotNull null
            val color = part.substringAfter(':', "").toLongOrNull(16)?.toInt() ?: return@mapNotNull null
            ColorStop(position.coerceIn(0f, 1f), color)
        }?.sortedBy { it.position }?.take(STOPS_MAX)?.takeIf { it.isNotEmpty() }

    /**
     * [stops] sorted and padded out to run from 0 to 1, so there are always at least two and a
     * gradient can be built from them directly.
     */
    fun expand(stops: List<ColorStop>): List<ColorStop> {
        val sorted = stops.sortedBy { it.position }.ifEmpty { DEFAULT }
        val out = sorted.toMutableList()
        if (out.first().position > 0f) out.add(0, ColorStop(0f, out.first().color))
        if (out.last().position < 1f) out.add(ColorStop(1f, out.last().color))
        return out
    }

    /** The colour of the gradient at [t], from 0 (corner) to 1 (edge). */
    fun colorAt(stops: List<ColorStop>, t: Float): Int {
        val expanded = expand(stops)
        val next = expanded.indexOfFirst { it.position >= t }
        if (next < 0) return expanded.last().color
        if (next == 0) return expanded.first().color
        val from = expanded[next - 1]
        val to = expanded[next]
        val span = to.position - from.position
        if (span <= 0f) return to.color
        return ColorUtils.blendARGB(from.color, to.color, (t - from.position) / span)
    }

    /** [stops] with the one at [index] moved to [position]; also its new index, as the order may change. */
    fun moved(stops: List<ColorStop>, index: Int, position: Float): Pair<List<ColorStop>, Int> {
        val moved = stops[index].copy(position = position.coerceIn(0f, 1f))
        val rest = stops.filterIndexed { i, _ -> i != index }
        val at = rest.indexOfFirst { it.position > moved.position }.let { if (it < 0) rest.size else it }
        return (rest.take(at) + moved + rest.drop(at)) to at
    }

    /**
     * [stops] with one more, in the middle of the widest gap between stops (or the ends), in the
     * colour the gradient already has there; also its index.
     */
    fun added(stops: List<ColorStop>): Pair<List<ColorStop>, Int> {
        val edges = listOf(0f) + stops.map { it.position } + 1f
        val gap = (0 until edges.lastIndex).maxBy { edges[it + 1] - edges[it] }
        val position = (edges[gap] + edges[gap + 1]) / 2
        val added = ColorStop(position, colorAt(stops, position))
        val at = stops.indexOfFirst { it.position > position }.let { if (it < 0) stops.size else it }
        return (stops.take(at) + added + stops.drop(at)) to at
    }

    /** [stops] without the one at [index]. The last stop is kept: a background needs a colour. */
    fun removed(stops: List<ColorStop>, index: Int): List<ColorStop> =
        if (stops.size <= 1) stops else stops.filterIndexed { i, _ -> i != index }
}
