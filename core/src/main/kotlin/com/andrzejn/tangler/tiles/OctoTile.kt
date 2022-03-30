package com.andrzejn.tangler.tiles


import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Segment
import com.andrzejn.tangler.logic.Tile
import kotlin.math.abs

/**
 * Octagonal (8 sides) screen tile. Actually it is drawn as square tile, with segments joining not just the square
 * sides but the square corners as well.
 */
class OctoTile(
    t: Tile, ctx: Context,
    /**
     * Respecive octagonal cell that provides all the segment coordinates
     */
    val cell: OctoCell
) : BaseTile(t, ctx) {
    /**
     * The current tile background polygon
     */
    override val tilePolygon: FloatArray get() = cell.polygonFloatArray

    /**
     * Draws given segment, using its color and end sides.
     * Complicated logic determines which segment type and which array index to use.
     */
    override fun drawSegment(segment: Segment) {
        val endpoint = segment.endsAtSide.sortedArray()
        val cDark = segmentColor(segment, ctx.drw.dark[segment.color])
        val cLight = segmentColor(segment, ctx.drw.light[segment.color])
        val tsd = ctx.drw.tsd
        when (abs(endpoint[1] - endpoint[0])) {
            2 -> with(cell.sideArc[endpoint[0] + 1]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }
            6 -> with(cell.sideArc[if (endpoint[1] == 6) 7 else 0]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }
            4 -> with(cell.line[endpoint[0]]) {
                tsd.setColor(cDark)
                tsd.line(this[0], this[1], cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.line(this[0], this[1], cell.lineWidthLight)
            }
            1 -> {
                val i = endpoint[0]
                val j = (endpoint[0] + 1) / 2
                tsd.setColor(cDark)
                with(cell.shortCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthDark) }
                with(cell.shortCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthDark
                    )
                }
                tsd.setColor(cLight)
                with(cell.shortCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthLight) }
                with(cell.shortCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthLight
                    )
                }
            }
            7 -> {
                val i = 7
                val j = 0
                tsd.setColor(cDark)
                with(cell.shortCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthDark) }
                with(cell.shortCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthDark
                    )
                }
                tsd.setColor(cLight)
                with(cell.shortCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthLight) }
                with(cell.shortCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthLight
                    )
                }
            }
            3 -> {
                val i = when (endpoint[0]) {
                    0 -> 0
                    1 -> 3
                    2 -> 2
                    3 -> 5
                    4 -> 4
                    else -> -1
                }
                val j = (i + 1) / 2
                tsd.setColor(cDark)
                with(cell.longCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthDark) }
                with(cell.longCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthDark
                    )
                }
                tsd.setColor(cLight)
                with(cell.longCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthLight) }
                with(cell.longCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthLight
                    )
                }
                with(cell.longCornerline[j]) { tsd.filledCircle(this[1], cell.lineWidthLight * 1.1f / 2) }
            }
            5 -> {
                val i = when (endpoint[0]) {
                    0 -> 7
                    1 -> 6
                    2 -> 1
                    else -> -1
                }
                tsd.setColor(cDark)
                val j = if (i == 7) 0 else (i + 1) / 2
                with(cell.longCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthDark) }
                with(cell.longCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthDark
                    )
                }
                tsd.setColor(cLight)
                with(cell.longCornerline[j]) { tsd.line(this[0], this[1], cell.lineWidthLight) }
                with(cell.longCornerArc[i]) {
                    tsd.arc(
                        centerX,
                        centerY,
                        radius,
                        startAngle,
                        radians,
                        cell.lineWidthLight
                    )
                }
                with(cell.longCornerline[j]) { tsd.filledCircle(this[1], cell.lineWidthLight * 1.1f / 2) }
            }
        }
    }

    /**
     * Rotates the base logic tile segments by given steps, then the base method invalidates the sprite.
     * For OctoTiles, one UI rotation step (of the drawn square tile) corresponds to two logical tile
     * rotation steps
     */
    override fun rotateBy(steps: Int) {
        t.rotateBy(steps * 2)
        super.rotateBy(steps)
    }
}