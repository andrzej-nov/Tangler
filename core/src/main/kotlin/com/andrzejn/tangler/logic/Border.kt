package com.andrzejn.tangler.logic

/**
 * A border between two playfield cells. Just the game logic, no UI related parts.
 */
class Border(cell1: Cell, cell2: Cell) {
    /**
     * The color of the path line that touches/intersects this border. Null if there is no line.
     */
    var color: Int? = null

    /**
     * The playfield cells that are adjacent on this border. No particular order.
     */
    private val neighbourCell = arrayOf(cell1, cell2)

    /**
     * The path segments that touch that border, if any. Particular segment is null when there is no path touching
     * this border on that side. Order of neighbourSegment matches the order of neighbourCell
     * (e.g. neighbourSegment`[`0`1]` belongs to neighbourCell`[`0`]`)
     */
    val neighbourSegment: Array<Segment?> = arrayOfNulls(2)

    /**
     * A tile has been added to one side of the border, and some tile segment touches the border. Add reference to it.
     * (There are no checks if the segment color matches; assuming that has beed checked before this call)
     */
    fun addSegment(segment: Segment) {
        neighbourSegment[neighbourCell.indexOf(segment.cell)] = segment
        color = segment.color
    }

    /**
     * Which segment touches given cell from outside on this border
     */
    fun otherSegmentFor(c: Cell): Segment? = neighbourSegment[1 - neighbourCell.indexOf(c)]

    /**
     * Clear the segment references (invoked when a closed loop disappears from the field).
     */
    fun clear() {
        color = null
        neighbourSegment.fill(null)
    }
}