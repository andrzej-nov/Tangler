package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell

/**
 * Base class of the screen cell (an element of the gameboard grid). Provides common properties and methods,
 * independent from the cell sides count.
 */
abstract class BaseCell {
    /**
     * Length of one cell polygon side. Recalculated on each screen resize.
     * For square cells it equals tileWidth and tileHeight, for hex cells there are more complicated calculations.
     */
    protected var sideLength: Float = 0f

    /**
     * Thickness of the segment curve lines that are painted with darker palette color.
     * Recalculated on each screen resize.
     */
    var lineWidthDark: Float = 0f

    /**
     * Thickness of the segment curve lines that are painted with lighter/brighter palette color.
     * Recalculated on each screen resize.
     */
    var lineWidthLight: Float = 0f

    /**
     * Sequence of the X,Y coordinate pairs specifying the cell polygon. Element 0 is the top left corner,
     * then clockwise. Coordinates are relative to the cell bounding rectangle, so the leftmost X and bottom Y are 0.
     */
    lateinit var polygonFloatArray: FloatArray

    /**
     * Cell side count-specific calculations, overridden in the subclass implementations.
     */
    abstract fun setLength(sideLength: Float)

    /**
     * Coordinates and values needed to draw an arc. Tile segments are mostly arcs.
     * Arc is a part of circle bounded by two angles.
     */
    data class ArcParams(
        /**
         * Arc circle center X
         */
        var centerX: Float = 0f,
        /**
         * Arc circle center Y
         */
        var centerY: Float = 0f,
        /**
         * Arc circle radius
         */
        var radius: Float = 0f,
        /**
         * Starting angle of the arc. Angle is in radians, horizontal right direction is 0, increases counterclockwise
         */
        var startAngle: Float = 0f,
        /**
         * Arc angular size, in radians.
         */
        var radians: Float = 0f
    )

    /**
     * Draw a color marker on a border touched/intersected by colored segment. Markers are small filled
     * circles/rectangles that ensure visual seamless segment joins and also let the player see the cell border
     * colors that muct be matched by current tile.
     */
    abstract fun drawBorderMarker(
        cell: Cell,
        i: Int,
        color: Int,
        polygon: FloatArray,
        ctx: Context
    )

}