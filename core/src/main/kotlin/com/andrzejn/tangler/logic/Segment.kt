package com.andrzejn.tangler.logic

/**
 * Segment always belongs to a tile (usually there are several tile segments), has particular color,
 * starts and ends on tile sides.
 */
class Segment(
    /**
     * Segment color
     */
    val color: Int,
    /**
     * Segment start (a tile side number)
     */
    begin: Int,
    /**
     * Segment end (a tile side number)
     */
    end: Int,
    /**
     * To which tile that segment belongs
     */
    val tile: Tile
) {
    /**
     * Tile side numbers for the segment start and end. While the tile is not put to the cell yet, it could be rotated,
     * and the segment start and end change respectively. Once put on a field cell, the start and end do not change
     * anymore.
     */
    val endsAtSide: Array<Int> = arrayOf(begin, end)

    /**
     * Cell borders touched by the segment ends. They are set when the tile is put to a cell, until that they are null.
     * Order is the same as for the endsAtSide.
     */
    var border: Array<Border?> = arrayOfNulls(2)

    /**
     * Respective neighbour cells touched by the segment ends. They are set when the tile is put to a cell,
     * until that they are null. Order is the same as for the endsAtSide.
     */
    var neighbourCell: Array<Cell?> = arrayOfNulls(2)

    /**
     * The path to which this segment belongs. It is set when the tile is put to a cell, until that it is null.
     */
    var path: Path? = null

    /**
     * Shortcut accessot to the tile cell. It is null until the tile is put on the cell. After that it does not change
     * anymore.
     */
    val cell: Cell? get() = tile.cell

    /**
     * Clones this object. Used to create a tile copy to rotate and evaluate the best move.
     */
    fun cloneTo(t: Tile): Segment {
        return Segment(color, endsAtSide[0], endsAtSide[1], t)
    }

    /**
     * The object serialization, invoked during the game save.
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(color).append(endsAtSide[0]).append(endsAtSide[1])
    }
}