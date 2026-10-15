package com.lukeneedham.androiddock

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * The maths for laying icons out on the fan's rings, with no Android or Compose in it. The sheet
 * ([DockActivity]) and the preview on the layout screen ([FanPreview]) both use it, so they cannot
 * drift apart.
 *
 * Angles are measured from the fan's centre, as the angle up from the corner's horizontal edge.
 */
internal object FanGeometry {

    const val MAX_ICON_SIZE_DP = 64
    const val MIN_ICON_SIZE_DP = DockPrefs.ICON_SIZE_MIN

    /** A ring's band is this many times as thick as the icon in it, at most. */
    private const val ROW_SPACING = 1.2f

    /** The chord between neighbouring icons is this many times the icon's size, at least. */
    private const val ICON_SPACING = 1.15f

    /**
     * Where the icons of one ring go. [size] is the icon's side. The first icon sits at angle
     * [from] and the last at [to]; the rest are spread evenly between them.
     */
    class Placement(val size: Float, val from: Float, val to: Float) {
        /**
         * The angle of icon [slot] of a ring holding [count]. The first slot is nearest the
         * corner's horizontal edge. A ring of one icon sits halfway.
         */
        fun angle(slot: Int, count: Int): Float =
            if (count == 1) (from + to) / 2 else from + (to - from) * slot / (count - 1)
    }

    /**
     * The angles of the first and last icon of a ring of icons [size] across, whose centres are
     * [centreRadius] from the fan's centre. The first and last icon touch the two straight edges of
     * the fan. The pair crosses over (the second is smaller than the first) when the icon is too
     * big for the ring: then it does not fit.
     */
    fun arc(centreRadius: Float, size: Float): Pair<Float, Float> {
        val inset = asin((size / 2f / centreRadius).coerceAtMost(1f))
        return inset to (PI / 2).toFloat() - inset
    }

    /**
     * Lays out a ring of [count] icons. The ring's band runs from [bandStart] to [bandEnd] from the
     * centre, and its icons sit on the circle halfway across it. The icons are as large as fit in
     * the band, and without touching along the chord between neighbours, from [maxSize] down to
     * [minSize], in steps of one. With [wholePixels] the sizes are whole numbers, as the sheet's
     * views need ([maxSize] and [minSize] must then be whole too). A ring is laid out as if it were full, whatever it holds.
     *
     * If even [minSize] is too big for the ring, its icons are put on the diagonal.
     */
    fun place(bandStart: Float, bandEnd: Float, count: Int, maxSize: Float, minSize: Float, wholePixels: Boolean = false): Placement {
        val centreRadius = (bandStart + bandEnd) / 2
        var size = min(maxSize, (bandEnd - bandStart) / ROW_SPACING)
        if (wholePixels) size = floor(size)
        while (size > minSize && count > 0) {
            val (from, to) = arc(centreRadius, size)
            val fits = if (count == 1) to >= from else {
                val step = (to - from) / (count - 1)
                to > from && 2 * centreRadius * sin(step / 2) >= size * ICON_SPACING
            }
            if (fits) break
            size -= 1f
        }
        size = size.coerceAtLeast(minSize)
        val (from, to) = arc(centreRadius, size).let { (a, b) ->
            if (b >= a) a to b else (PI / 4).toFloat().let { it to it }
        }
        return Placement(size, from, to)
    }
}
