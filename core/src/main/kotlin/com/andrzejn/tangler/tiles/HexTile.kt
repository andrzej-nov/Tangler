package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Segment
import com.andrzejn.tangler.logic.Tile
import kotlin.math.abs

/**
 * Hexagonal (6 sides) screen tile
 */
class HexTile(
    t: Tile, ctx: Context,
    /**
     * Respecive hexagonal cell that provides all the segment coordinates
     */
    val cell: HexCell
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
            1 -> with(cell.shortArc[endpoint[0] + 1]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }

            5 -> with(cell.shortArc[0]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }

            2 -> with(cell.longArc[endpoint[0] + 1]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }

            4 -> with(cell.longArc[if (endpoint[0] == 0) 5 else 0]) {
                tsd.setColor(cDark)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.arc(centerX, centerY, radius, startAngle, radians, cell.lineWidthLight)
            }

            3 -> with(cell.line[endpoint[0]]) {
                tsd.setColor(cDark)
                tsd.line(this[0], this[1], cell.lineWidthDark)
                tsd.setColor(cLight)
                tsd.line(this[0], this[1], cell.lineWidthLight)
            }
        }
    }

    /**
     * Rotates the base logic tile segments by given steps, then the base method invalidates the sprite
     */
    override fun rotateBy(steps: Int) {
        t.rotateBy(steps)
        super.rotateBy(steps)
    }
}