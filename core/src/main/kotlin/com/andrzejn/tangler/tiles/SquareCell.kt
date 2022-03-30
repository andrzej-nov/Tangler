package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI

/**
 * Square (4 sides) screen cell
 */
class SquareCell : BaseCell() {
    /**
     * Cell polygon, calculated on each screen resize. Element 0 is the top-left corner, then clockwise.
     */
    private val polygon = Array(4) { Vector2() }

    /**
     * Short arc connects two adjacent sides (separated by the single common corner).
     * arc index matches the index of the polygon corner wrapped by the arc (0 is over top-left corner,
     * then clockwise)
     */
    val arc: Array<ArcParams> = Array(4) { ArcParams() }

    /**
     * Line connects two opposite sides.
     * line index matches the polygon side index where it starts (0 is the top-left side, then clockwise)
     */
    val line: Array<Array<Vector2>> = Array(2) { Array(2) { Vector2() } }

    /**
     * Arc angles do not change on resizes, so they are calculated once.
     */
    init {
        arc.forEach { it.radians = PI.toFloat() / 2 }
        arc[0].startAngle = 0f
        arc[1].startAngle = PI.toFloat() / 2
        arc[2].startAngle = PI.toFloat()
        arc[3].startAngle = -PI.toFloat() / 2
    }

    /**
     * On windows resize, when cell sizes change, need to recalculate all segment coordinates.
     */
    override fun setLength(sideLength: Float) {
        this.sideLength = sideLength
        lineWidthDark = sideLength / 3
        lineWidthLight = sideLength / 4

        polygon[0].x = 0f
        polygon[0].y = 0f
        polygon[1].x = sideLength
        polygon[1].y = 0f
        polygon[2].x = sideLength
        polygon[2].y = sideLength
        polygon[3].x = 0f
        polygon[3].y = sideLength
        polygonFloatArray = polygon.flatMap { listOf(it.x, it.y) }.toFloatArray()

        arc.forEachIndexed { i, a ->
            a.centerX = polygon[i].x
            a.centerY = polygon[i].y
            a.radius = sideLength / 2
        }

        line.indices.forEach {
            line[it][0].x = (polygon[it].x + polygon[it + 1].x) / 2
            line[it][0].y = (polygon[it].y + polygon[it + 1].y) / 2
            line[it][1].x = (polygon[it + 2].x + polygon[if (it == 1) 0 else it + 3].x) / 2
            line[it][1].y = (polygon[it + 2].y + polygon[if (it == 1) 0 else it + 3].y) / 2
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
        val x = if (i == 3) (polygon[6] + polygon[0]) / 2 else (polygon[i * 2] + polygon[(i + 1) * 2]) / 2
        val y = if (i == 3) (polygon[7] + polygon[1]) / 2 else (polygon[i * 2 + 1] + polygon[(i + 1) * 2 + 1]) / 2
        ctx.drw.sd.setColor(ctx.drw.light[color])
        ctx.drw.sd.filledCircle(x, y, lineWidthLight / 2)
    }

}