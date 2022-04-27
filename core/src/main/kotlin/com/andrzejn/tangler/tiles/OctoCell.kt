package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Octagonal (8 sides) screen cell. Actually it is drawn as square cell, with segments joining not just the square
 * sides but the square corners as well.
 */
class OctoCell : BaseCell() {
    /**
     * Cell polygon, calculated on each screen resize. Element 0 is the top-left corner, then clockwise.
     */
    private val polygon = Array(4) { Vector2() }

    /**
     * Side arc connects two adjacent logical sides (separated by the single common corner).
     * Actually on screen, the even logical sides are the drawn square corners
     * and the odd logical corners are the middles of the drawn square sides.
     * sideArc index matches the index of the corner/side wrapped by the arc (0 goes over top-left corner,
     * 1 over top side, then so on clockwise)
     */
    val sideArc: Array<ArcParams> = Array(8) { ArcParams() }

    /**
     * Line connects two opposite sides.
     * line index matches the polygon side index where it starts (0 is the top-left side, then clockwise)
     */
    val line: Array<Array<Vector2>> = Array(4) { Array(2) { Vector2() } }

    /**
     * Short corner segment goes from square corner to nearest square side. Since single arc cannot provide required
     * 45 degrees angle on one side and 90 degrees angle on another, that segment is combined from a side arc
     * and corner line. Index numbering starts from the top-left segment, then clockwise.
     */
    val shortCornerline: Array<Array<Vector2>> = Array(4) { Array(2) { Vector2() } }

    /**
     * Short corner segment goes from square corner to nearest square side. Since single arc cannot provide required
     * 45 degrees angle on one side and 90 degrees angle on another, that segment is combined from a side arc
     * and corner line. Index numbering starts from the top-left segment, then clockwise.
     */
    val shortCornerArc: Array<ArcParams> = Array(8) { ArcParams() }

    /**
     * Long corner segment goes from square corner over nearest side to next square side
     * (corner A - side - corner - side B). Since single arc cannot provide required
     * 45 degrees angle on one side and 90 degrees angle on another, that segment is combined from a side arc
     * and corner line. Index numbering starts from the top-left segment, then clockwise.
     */
    val longCornerline: Array<Array<Vector2>> = Array(4) { Array(2) { Vector2() } }

    /**
     * Long corner segment goes from square corner over nearest side to next square side
     * (corner A - side - corner - side B). Since single arc cannot provide required
     * 45 degrees angle on one side and 90 degrees angle on another, that segment is combined from a side arc
     * and corner line. Index numbering starts from the top-left segment, then clockwise.
     */
    val longCornerArc: Array<ArcParams> = Array(8) { ArcParams() }

    /**
     * Corner border markers are not filled circles but small rectangles (because visually two paths overlap
     * at square corners, so we need to see which marker joins which segments pair).
     * The rectangle length is also scaled on windows resized.
     */
    private var cornerMarkerLength = 0f

    /**
     * Arc angles do not change on resizes, so they are calculated once.
     */
    init {
        sideArc.forEach { it.radians = PI.toFloat() / 2 }
        sideArc[0].startAngle = 0f
        sideArc[1].startAngle = PI.toFloat() / 4
        sideArc[2].startAngle = PI.toFloat() / 2
        sideArc[3].startAngle = 3 * PI.toFloat() / 4
        sideArc[4].startAngle = PI.toFloat()
        sideArc[5].startAngle = -3 * PI.toFloat() / 4
        sideArc[6].startAngle = -PI.toFloat() / 2
        sideArc[7].startAngle = -PI.toFloat() / 4

        shortCornerArc.forEach { it.radians = 3 * PI.toFloat() / 4 }
        shortCornerArc[0].startAngle = 0f
        shortCornerArc[1].startAngle = PI.toFloat() / 4
        shortCornerArc[2].startAngle = PI.toFloat() / 2
        shortCornerArc[3].startAngle = 3 * PI.toFloat() / 4
        shortCornerArc[4].startAngle = PI.toFloat()
        shortCornerArc[5].startAngle = -3 * PI.toFloat() / 4
        shortCornerArc[6].startAngle = -PI.toFloat() / 2
        shortCornerArc[7].startAngle = -PI.toFloat() / 4

        longCornerArc.forEach { it.radians = PI.toFloat() / 4 }
        longCornerArc[0].startAngle = PI.toFloat() / 2
        longCornerArc[1].startAngle = PI.toFloat() / 4
        longCornerArc[2].startAngle = PI.toFloat()
        longCornerArc[3].startAngle = 3 * PI.toFloat() / 4
        longCornerArc[4].startAngle = -PI.toFloat() / 2
        longCornerArc[5].startAngle = -3 * PI.toFloat() / 4
        longCornerArc[6].startAngle = 0f
        longCornerArc[7].startAngle = -PI.toFloat() / 4
    }

    /**
     * On windows resize, when cell sizes change, need to recalculate all segment coordinates.
     */
    override fun setLength(sideLength: Float) {
        this.sideLength = sideLength
        lineWidthDark = sideLength / 6
        lineWidthLight = sideLength / 8
        cornerMarkerLength = 3 * lineWidthLight / 4

        polygon[0].x = 0f
        polygon[0].y = 0f
        polygon[1].x = sideLength
        polygon[1].y = 0f
        polygon[2].x = sideLength
        polygon[2].y = sideLength
        polygon[3].x = 0f
        polygon[3].y = sideLength

        polygonFloatArray = polygon.flatMap { listOf(it.x, it.y) }.toFloatArray()

        sideArc[0].centerX = polygon[0].x
        sideArc[0].centerY = polygon[0].y
        sideArc[0].radius = sideLength / 2
        sideArc[1].centerX = (polygon[0].x + polygon[1].x) / 2
        sideArc[1].centerY = polygon[0].y - sideLength / 2
        sideArc[1].radius = sideLength / sqrt(2f)
        sideArc[2].centerX = polygon[1].x
        sideArc[2].centerY = polygon[1].y
        sideArc[2].radius = sideLength / 2
        sideArc[3].centerX = polygon[1].x + sideLength / 2
        sideArc[3].centerY = (polygon[1].y + polygon[2].y) / 2
        sideArc[3].radius = sideLength / sqrt(2f)
        sideArc[4].centerX = polygon[2].x
        sideArc[4].centerY = polygon[2].y
        sideArc[4].radius = sideLength / 2
        sideArc[5].centerX = (polygon[2].x + polygon[3].x) / 2
        sideArc[5].centerY = polygon[2].y + sideLength / 2
        sideArc[5].radius = sideLength / sqrt(2f)
        sideArc[6].centerX = polygon[3].x
        sideArc[6].centerY = polygon[3].y
        sideArc[6].radius = sideLength / 2
        sideArc[7].centerX = polygon[3].x - sideLength / 2
        sideArc[7].centerY = (polygon[3].y + polygon[0].y) / 2
        sideArc[7].radius = sideLength / sqrt(2f)

        line[0][0].x = polygon[0].x
        line[0][0].y = polygon[0].y
        line[0][1].x = polygon[2].x
        line[0][1].y = polygon[2].y
        line[1][0].x = (polygon[0].x + polygon[1].x) / 2
        line[1][0].y = polygon[0].y
        line[1][1].x = line[1][0].x
        line[1][1].y = polygon[2].y
        line[2][0].x = polygon[1].x
        line[2][0].y = polygon[1].y
        line[2][1].x = polygon[3].x
        line[2][1].y = polygon[3].y
        line[3][0].x = polygon[1].x
        line[3][0].y = (polygon[1].y + polygon[2].y) / 2
        line[3][1].x = polygon[0].x
        line[3][1].y = line[3][0].y

        val shortOffset = sideLength / (4 + 2 * sqrt(2f))
        shortCornerline[0][0].x = polygon[0].x
        shortCornerline[0][0].y = polygon[0].y
        shortCornerline[0][1].x = polygon[0].x + shortOffset
        shortCornerline[0][1].y = polygon[0].y + shortOffset
        shortCornerline[1][0].x = polygon[1].x
        shortCornerline[1][0].y = polygon[1].y
        shortCornerline[1][1].x = polygon[1].x - shortOffset
        shortCornerline[1][1].y = polygon[1].y + shortOffset
        shortCornerline[2][0].x = polygon[2].x
        shortCornerline[2][0].y = polygon[2].y
        shortCornerline[2][1].x = polygon[2].x - shortOffset
        shortCornerline[2][1].y = polygon[2].y - shortOffset
        shortCornerline[3][0].x = polygon[3].x
        shortCornerline[3][0].y = polygon[3].y
        shortCornerline[3][1].x = polygon[3].x + shortOffset
        shortCornerline[3][1].y = polygon[3].y - shortOffset

        shortCornerArc.forEach { it.radius = sideLength / (2 + 2 * sqrt(2f)) }
        shortCornerArc[0].centerX = polygon[0].x + shortOffset * 2
        shortCornerArc[0].centerY = polygon[0].y
        shortCornerArc[1].centerX = polygon[1].x - shortOffset * 2
        shortCornerArc[1].centerY = polygon[1].y
        shortCornerArc[2].centerX = polygon[1].x
        shortCornerArc[2].centerY = polygon[1].y + shortOffset * 2
        shortCornerArc[3].centerX = polygon[2].x
        shortCornerArc[3].centerY = polygon[2].y - shortOffset * 2
        shortCornerArc[4].centerX = polygon[2].x - shortOffset * 2
        shortCornerArc[4].centerY = polygon[2].y
        shortCornerArc[5].centerX = polygon[3].x + shortOffset * 2
        shortCornerArc[5].centerY = polygon[3].y
        shortCornerArc[6].centerX = polygon[3].x
        shortCornerArc[6].centerY = polygon[3].y - shortOffset * 2
        shortCornerArc[7].centerX = polygon[0].x
        shortCornerArc[7].centerY = polygon[0].y + shortOffset * 2

        val longOffset = sideLength * (sqrt(2f) - 1) / (2 * sqrt(2f))
        longCornerline[0][0].x = polygon[0].x
        longCornerline[0][0].y = polygon[0].y
        longCornerline[0][1].x = polygon[0].x + longOffset
        longCornerline[0][1].y = polygon[0].y + longOffset
        longCornerline[1][0].x = polygon[1].x
        longCornerline[1][0].y = polygon[1].y
        longCornerline[1][1].x = polygon[1].x - longOffset
        longCornerline[1][1].y = polygon[1].y + longOffset
        longCornerline[2][0].x = polygon[2].x
        longCornerline[2][0].y = polygon[2].y
        longCornerline[2][1].x = polygon[2].x - longOffset
        longCornerline[2][1].y = polygon[2].y - longOffset
        longCornerline[3][0].x = polygon[3].x
        longCornerline[3][0].y = polygon[3].y
        longCornerline[3][1].x = polygon[3].x + longOffset
        longCornerline[3][1].y = polygon[3].y - longOffset

        longCornerArc.forEach { it.radius = sideLength * 3 * sqrt(2f) / (2 + 1 * sqrt(2f)) }
        val centerOffset = longCornerArc[0].radius - sideLength / 2
        longCornerArc[0].centerX = polygon[1].x
        longCornerArc[0].centerY = polygon[1].y - centerOffset
        longCornerArc[1].centerX = polygon[0].x
        longCornerArc[1].centerY = polygon[0].y - centerOffset
        longCornerArc[2].centerX = polygon[2].x + centerOffset
        longCornerArc[2].centerY = polygon[2].y
        longCornerArc[3].centerX = polygon[1].x + centerOffset
        longCornerArc[3].centerY = polygon[1].y
        longCornerArc[4].centerX = polygon[3].x
        longCornerArc[4].centerY = polygon[3].y + centerOffset
        longCornerArc[5].centerX = polygon[2].x
        longCornerArc[5].centerY = polygon[2].y + centerOffset
        longCornerArc[6].centerX = polygon[0].x - centerOffset
        longCornerArc[6].centerY = polygon[0].y
        longCornerArc[7].centerX = polygon[3].x - centerOffset
        longCornerArc[7].centerY = polygon[3].y
    }

    /**
     * Draw a color marker on a border touched/intersected by colored segment. Markers are small filled
     * circles or rectangles (on square corners) that ensure visual seamless segment joins and also let the player
     * see the cell border colors that muct be matched by current tile.
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
        val cDark = ctx.drw.dark[color]
        val cLight = ctx.drw.light[color]
        val sd = ctx.drw.sd
        if (i % 2 == 0) {
            val x = polygon[i]
            val y = polygon[i + 1]
            when (i) {
                0, 4 -> {
                    sd.setColor(cDark)
                    sd.line(
                        x - cornerMarkerLength,
                        y + cornerMarkerLength,
                        x + cornerMarkerLength,
                        y - cornerMarkerLength,
                        lineWidthDark
                    )
                    sd.setColor(cLight)
                    sd.line(
                        x - cornerMarkerLength,
                        y + cornerMarkerLength,
                        x + cornerMarkerLength,
                        y - cornerMarkerLength,
                        lineWidthLight
                    )
                }
                else -> {
                    sd.setColor(cDark)
                    sd.line(
                        x - cornerMarkerLength,
                        y - cornerMarkerLength,
                        x + cornerMarkerLength,
                        y + cornerMarkerLength,
                        lineWidthDark
                    )
                    sd.setColor(cLight)
                    sd.line(
                        x - cornerMarkerLength,
                        y - cornerMarkerLength,
                        x + cornerMarkerLength,
                        y + cornerMarkerLength,
                        lineWidthLight
                    )
                }
            }
        } else {
            val x = if (i == 7) (polygon[6] + polygon[0]) / 2 else (polygon[i - 1] + polygon[i + 1]) / 2
            val y = if (i == 7) (polygon[7] + polygon[1]) / 2 else (polygon[i] + polygon[i + 2]) / 2
            sd.setColor(cLight)
            sd.filledCircle(x, y, lineWidthLight / 2)
        }
    }
}