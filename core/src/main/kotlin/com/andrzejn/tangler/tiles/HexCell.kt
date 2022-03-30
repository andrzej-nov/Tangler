package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI

/**
 * Hexagonal (6 sides) screen cell
 */
class HexCell : BaseCell() {
    /**
     * Cell polygon, calculated on each screen resize. Element 0 is the top-left corner, then clockwise.
     */
    private val polygon = Array(6) { Vector2() }

    /**
     * Short arc connects two adjacent sides (separated by the single common corner).
     * shortArc index matches the index of the polygon corner wrapped by the arc (0 is over top-left corner,
     * then clockwise)
     */
    val shortArc: Array<ArcParams> = Array(6) { ArcParams() }

    /**
     * Long arc connects two sides over one (side A - corner - side - corner - side B).
     * longArc index matches the side index that it wraps (0 is over top-left side, then clockwise)
     */
    val longArc: Array<ArcParams> = Array(6) { ArcParams() }

    /**
     * Line connects two opposite sides.
     * line index matches the polygon side index where it starts (0 is the top-left side, then clockwise)
     */
    val line: Array<Array<Vector2>> = Array(3) { Array(2) { Vector2() } }

    /**
     * Arc angles do not change on resizes, so they are calculated once.
     */
    init {
        shortArc.forEach { it.radians = PI.toFloat() * 2 / 3 }
        shortArc[0].startAngle = -PI.toFloat() / 6
        shortArc[1].startAngle = PI.toFloat() / 6
        shortArc[2].startAngle = PI.toFloat() / 2
        shortArc[3].startAngle = PI.toFloat() * 5 / 6
        shortArc[4].startAngle = -PI.toFloat() * 5 / 6
        shortArc[5].startAngle = -PI.toFloat() / 2

        longArc.forEach { a -> a.radians = PI.toFloat() / 3 }
        longArc[0].startAngle = PI.toFloat() / 6
        longArc[1].startAngle = PI.toFloat() / 2
        longArc[2].startAngle = PI.toFloat() * 5 / 6
        longArc[3].startAngle = -PI.toFloat() * 5 / 6
        longArc[4].startAngle = -PI.toFloat() / 2
        longArc[5].startAngle = -PI.toFloat() / 6
    }

    /**
     * On windows resize, when cell sizes change, need to recalculate all segment coordinates.
     */
    override fun setLength(sideLength: Float) {
        this.sideLength = sideLength
        lineWidthDark = sideLength / 3
        lineWidthLight = sideLength / 4

        polygon[0].x = 0f
        polygon[0].y = sideLength / 2
        polygon[1].x = sideLength * 20 / 23
        polygon[1].y = 0f
        polygon[2].x = sideLength * 40 / 23
        polygon[2].y = polygon[0].y
        polygon[3].x = polygon[2].x
        polygon[3].y = sideLength * 3 / 2
        polygon[4].x = polygon[1].x
        polygon[4].y = sideLength * 2
        polygon[5].x = polygon[0].x
        polygon[5].y = polygon[3].y

        polygonFloatArray = polygon.flatMap { listOf(it.x, it.y) }.toFloatArray()

        shortArc.forEachIndexed { i, a ->
            a.centerX = polygon[i].x
            a.centerY = polygon[i].y
            a.radius = sideLength / 2
        }
        longArc.forEach { a ->
            a.radius = sideLength * 3 / 2
        }
        longArc[0].centerX = polygon[0].x
        longArc[0].centerY = polygon[0].y - sideLength
        longArc[1].centerX = polygon[2].x
        longArc[1].centerY = longArc[0].centerY
        longArc[2].centerX = polygon[2].x + (polygon[2].x - polygon[0].x) / 2
        longArc[2].centerY = (polygon[2].y + polygon[3].y) / 2
        longArc[3].centerX = polygon[2].x
        longArc[3].centerY = polygon[3].y + sideLength
        longArc[4].centerX = polygon[5].x
        longArc[4].centerY = polygon[5].y + sideLength
        longArc[5].centerX = polygon[5].x - (polygon[2].x - polygon[0].x) / 2
        longArc[5].centerY = longArc[2].centerY

        line.indices.forEach {
            line[it][0].x = (polygon[it].x + polygon[it + 1].x) / 2
            line[it][0].y = (polygon[it].y + polygon[it + 1].y) / 2
            line[it][1].x = (polygon[it + 3].x + polygon[if (it == 2) 0 else it + 4].x) / 2
            line[it][1].y = (polygon[it + 3].y + polygon[if (it == 2) 0 else it + 4].y) / 2
        }
    }

    /**
     * Draw a color marker on a border touched/intersected by colored segment. Markers are small filled
     * circles that ensure visual seamless segment joins and also let the player see the cell border
     * colors that muct be matched by current tile.
     */
    override fun drawBorderMarker(
        cell: Cell,
        i: Int,
        color: Int,
        polygon: FloatArray,
        ctx: Context
    ) {
        if (ctx.fader.inFade && ctx.fader.affected(cell.border[i] ?: return))
        // When segments are fading out, respective markers are just switched off
            return
        var j = 4 - i
        if (j < 0) j = 5
        val x = if (j == 5) (polygon[10] + polygon[0]) / 2 else (polygon[j * 2] + polygon[(j + 1) * 2]) / 2
        val y = if (j == 5) (polygon[11] + polygon[1]) / 2 else (polygon[j * 2 + 1] + polygon[(j + 1) * 2 + 1]) / 2
        ctx.drw.sd.setColor(ctx.drw.light[color])
        ctx.drw.sd.filledCircle(x, y, lineWidthLight / 2)
    }
}