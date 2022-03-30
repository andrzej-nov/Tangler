package com.andrzejn.tangler.logic

/**
 * A playfield grid cell. All cells are created and put into the grid on new game start, and not moved or deleted
 * afterwards. Tiles with colored segments then are put on cells.
 */
class Cell(sidesCount: Int) {
    private val halfSidesCount = sidesCount / 2

    /**
     * Cell X coordinate. Starts from 0, increased from left to right.
     */
    var x: Int = -1

    /**
     * Cell Y coordinate. Starts from 0, increased from bottom to top.
     */
    var y: Int = -1

    /**
     * References to neighbour cells. Always start from the top-left border, enumerated clockwise
     */
    val neighbour: Array<Cell?> = arrayOfNulls(sidesCount)

    /**
     * References to borders between adjacent cells. Order of the border array matches the neighbour array,
     * e.g. border`[`i`]` always belongs to neighbour`[`i`]`
     */
    val border: Array<Border?> = arrayOfNulls(sidesCount)

    /**
     * The tile that has been put on that cell. (There can be empty cells without tiles, of course)
     */
    var tile: Tile? = null

    /**
     * Initialize neighbour cell link. Invoked on playfield creation
     */
    fun setNeighbour(i: Int, that: Cell) {
        neighbour[i] = that
        that.neighbour[i + halfSidesCount] = this
        border[i] = Border(this, that)
        that.border[i + halfSidesCount] = border[i]
    }

    /**
     * The tile has been put into this cell. Update all respective references.
     * There are no checks if the cell was empty before, assuming it has been handled in the calling code.
     */
    fun putTileToCell(tile: Tile) {
        this.tile = tile
        tile.cell = this
        tile.segment.forEach { segment ->
            (0..1).forEach {
                segment.border[it] = this.border[segment.endsAtSide[it]]
                segment.neighbourCell[it] = this.neighbour[segment.endsAtSide[it]]
                segment.border[it]?.addSegment(segment)
            }
        }
    }

    /**
     * Assuming that the color is added on an empty side (if there is at least one
     * empty side for that), check if a valid tile could fit into this cell afterwards
     * (i.e. all present colors, plus remaining empty sides,
     * could all be connected by some segments, and no colored side remains unconnected).
     *
     * Used to check if a tile placed to neighbour cell does not cause color conflicts
     * that will cause some broken paths afterwards/
     */
    fun isBlockedBy(color: Int, allowDuplicateColors: Boolean): Boolean {
        val (colorCount, emptySidesCount) = borderColorCounts()
        return checkBlocked(color, colorCount, emptySidesCount, allowDuplicateColors)
    }

    /**
     * Returns a set of all possible colors that could touch a border of this cell
     * without making it unsuitable for any valid tile
     */
    fun nonBlockingColors(colorsCount: Int, allowDuplicateColors: Boolean): MutableSet<Int> {
        val (colorCount, emptySidesCount) = borderColorCounts()
        return (1..colorsCount).filter {
            !checkBlocked(
                it,
                colorCount.toMutableMap(),
                emptySidesCount,
                allowDuplicateColors
            )
        }.toMutableSet()
    }

    /**
     * Check if adding the color to the cell that already has some border colors and some empty sides
     * will make it unsuitablefor any valid tile.
     */
    private fun checkBlocked(
        color: Int,
        colorCount: MutableMap<Int, Int>,
        emptySidesCount: Int,
        allowDuplicateColors: Boolean
    ): Boolean {
        if (color > 0)
            colorCount[color] = (colorCount[color] ?: 0) + 1
        if (colorCount.values.count { it % 2 != 0 } > emptySidesCount) // A color segment put into a cell always touches
        // two borders. So if we have unmatched even count of particular border color, there should be enough free
        // cell sides to bring the segments out.
            return true // There are too many unmatched colors
        return !allowDuplicateColors && colorCount.values.any { it > 2 } // With unique colors per tile, there could be
        // no more that 1 segment of particular color, touching 2 borders/
    }

    /**
     * Count occurences of each border color of this cell. Empty sides count is decremented by 1, accounting for
     * one more color that will touch that cell
     */
    private fun borderColorCounts(): Pair<MutableMap<Int, Int>, Int> {
        val colorCount = border.zip(neighbour).filter { (brdr, ngbr) -> (brdr?.color ?: 0) != 0 || ngbr?.tile == null }
            .map { (brdr, _) -> brdr?.color ?: 0 }.groupingBy { it }.eachCount().toMutableMap()
        val emptySidesCount = if (colorCount.containsKey(0)) colorCount[0]!! - 1 else 0
        colorCount.remove(0)
        return colorCount to emptySidesCount
    }

    /**
     * This cell border colors, in the same order as the border array
     */
    val borderColors: List<Int> get() = border.map { it?.color ?: 0 }

    /**
     * Color of the given cell border
     */
    fun borderColor(side: Int): Int = border[side]?.color ?: 0

    /**
     * Tile on respective neighbour cell, if any
     */
    fun neighbourTile(side: Int): Tile? = neighbour[side]?.tile

    /**
     * Path adjacent to given cell side, if any
     */
    fun pathAtSide(side: Int): Path? = border[side]?.otherSegmentFor(this)?.path

}
