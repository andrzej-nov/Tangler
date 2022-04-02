package com.andrzejn.tangler.logic

import kotlin.random.Random

/**
 * The logical playfield. Contains the cells grid and provides the game back-end logic that is not tied
 * to UI operations.
 *
 * The playfield is a torus (the top side connects to bottom, left to right), so there are no bord sides
 * where the paths could stuck.
 */
class PlayField(
    private val fieldSize: Int,
    private val sidesCount: Int,
    private val colorsCount: Int,
    private val allowDuplicateColors: Boolean
) {
    private val halfSidesCount = sidesCount / 2

    /**
     * The grid cells. Generated on the beginning of new game, not deleted or rearranged to the end of the game.
     */
    val cell: Array<Array<Cell>> = Array(fieldSize) { Array(fieldSize) { Cell(sidesCount) } }

    /**
     * All the color paths present on the board.
     */
    private val path = mutableListOf<Path>()

    /**
     * The set of free cells adjacent to the cells with tiles. Used to make next move.
     */
    private val firstFreeLine = mutableSetOf<Cell>()

    /**
     * The set of free cells adjacent to the firstFreeLine cells. Used to make next move if none of the first line
     * cells are suitable for the current tile.
     */
    private val secondFreeLine = mutableSetOf<Cell>()

    init {
        initPlayfield()
    }

    /**
     * Link neighbour cells together with proper references, side to side on common borders.
     */
    private fun initPlayfield() {
        (0 until fieldSize).forEach { x ->
            (0 until fieldSize).forEach { y ->
                with(cell[x][y]) {
                    this.x = x
                    this.y = y
                    setNeighbours(this, x, y)
                }
            }
        }
    }

    /**
     * Depending on the cell sides count, they have different number of neighbours and respective reference setting
     * rules
     */
    private fun setNeighbours(c: Cell, x: Int, y: Int) {
        when (halfSidesCount) {
            2 -> { // Square tiles
                c.setNeighbour(0, cell[x][y.plus1()])
                c.setNeighbour(1, cell[x.plus1()][y])
            }
            3 -> { // Hex tiles
                c.setNeighbour(0, if (y % 2 == 0) cell[x.minus1()][y.plus1()] else cell[x][y.plus1()])
                c.setNeighbour(1, if (y % 2 == 0) cell[x][y.plus1()] else cell[x.plus1()][y.plus1()])
                c.setNeighbour(2, cell[x.plus1()][y])
            }
            else -> { // Square tiles with corner borders
                c.setNeighbour(0, cell[x.minus1()][y.plus1()])
                c.setNeighbour(1, cell[x][y.plus1()])
                c.setNeighbour(2, cell[x.plus1()][y.plus1()])
                c.setNeighbour(3, cell[x.plus1()][y])
            }
        }
    }

    private fun Int.plus1(): Int = if (this == fieldSize - 1) 0 else this + 1
    private fun Int.minus1(): Int = if (this == 0) fieldSize - 1 else this - 1

    /**
     * The game always starts with a single seeder tile at the board center. That tile is randomly generated, always
     * with maximum number of segments
     */
    fun putFirstTile(tilePosition: Int): Tile {
        val t = generateEmptyTile()
        t.createRandomSegments(halfSidesCount)
        val startingCell = cell[tilePosition][tilePosition]
        putTileToCell(t, startingCell)
        setFreeLines()
        return t
    }

    private val lastCellSuggestions = mutableListOf<Cell>() // If the player refuses to put generated tile to the
    // cell for which it was generated, the algorythm will keep suggesting similar tiles for the same cell.
    // To avoid that, track the list of several last picked cells to repeat suggestions at least after several other
    // moves.

    /**
     * Find a free cell that has the most incoming neighbour paths, so it it the best candidate to extend and
     * join them all. Returns null when there are no free cells.
     */
    private fun mostCloggedCell(): Cell? {
        val freeCellsWithColorCounts =
            firstFreeLine.minus(lastCellSuggestions.toSet()).map { c -> c to c.borderColors.count { it != 0 } }
                .sortedByDescending { (_, colorCount) -> colorCount }
        if (freeCellsWithColorCounts.isEmpty())
            return null
        val maxCount = freeCellsWithColorCounts.first().second
        if (maxCount <= if (sidesCount == 4) 1 else 2)
        // Cells aren't that clogged yet, no need to generate special cell
            return null
        val suggestedCell =
            freeCellsWithColorCounts[Random.nextInt(freeCellsWithColorCounts.count { (_, colorCount) ->
                colorCount == maxCount
            })].first

        // Do not repeat suggestion for the same cell too often
        lastCellSuggestions.add(suggestedCell)
        if (lastCellSuggestions.size > 3) lastCellSuggestions.removeAt(0)
        return suggestedCell
    }

    /**
     * A cell side has a color and (if it is empty) the list of colors that could end on that side without
     * blocking respective neighbour cells
     */
    data class BorderColor(
        /**
         * Cell border color
         */
        var color: Int,
        /**
         * Set of colors that can safely end on that side, without blocking respective neighbour cell
         */
        val nonBlockingOptions: MutableSet<Int>
    )

    /**
     * Generate next random tile that player will put to the board.
     * Actually most of the time we create not just completely random next tiles,
     * but the ones that should fit into the most problematic first-line free cells,
     * with no more than one random segment per tile.
     * We resort to completely random next tile when there are either plenty of move possibilities on the field
     * or the last available cells are already too messed up.
     */
    fun generateNextTile(): Tile = generateEmptyTile().also {
        val candidateCell = mostCloggedCell()
        if (candidateCell != null) {
            it.createSegmentsForColors(candidateCell.borderColors.mapIndexed { i, color ->
                BorderColor(
                    color,
                    nonBlockingOptionsFor(candidateCell.neighbour[i]!!, color)
                )
            }.toTypedArray())
        }
        if (it.segment.size == 0)
            it.createRandomSegments() // Fallback to complete randomness
    }

    /**
     * Picks the list of available colors that could end on the cell border fith particular color
     * and the neighbour cell behind it
     */
    private fun nonBlockingOptionsFor(c: Cell, color: Int): MutableSet<Int> =
        if (color != 0) mutableSetOf(color) else c.nonBlockingColors(colorsCount, allowDuplicateColors)

    /**
     * Returns new empty Tile, to initialize it with some segments
     */
    fun generateEmptyTile(): Tile =
        Tile(sidesCount, colorsCount, allowDuplicateColors)

    /**
     * Returns list of the first-line (or second-line) cells where the tile could be put,
     * considering its current orientation
     */
    private fun validMovesFor(tile: Tile): List<Cell> {
        val fl = firstFreeLine.filter { c ->
            c.borderColors.zip(tile.tileColors)
                .all { (borderColor, tileColor) -> borderColor == 0 || tileColor == 0 || borderColor == tileColor }
                    && c.borderColors.zip(tile.tileColors)
                .any { (borderColor, tileColor) -> borderColor == tileColor && borderColor != 0 }
        }
        if (fl.isNotEmpty())
            return fl
        return firstFreeLine.filter { c ->
            c.borderColors.zip(tile.tileColors)
                .all { (borderColot, tileColor) -> borderColot == 0 || tileColor == 0 }
        }.plus(secondFreeLine.toList())
    }

    /**
     * Adds evaluated MoveQuality to each of the available validMoves
     */
    fun evaluateMoves(tile: Tile): Map<Cell, MoveQuality> =
        validMovesFor(tile).associateWith { c ->
            MoveQuality().also { mq ->
                mq.pathsBlocked =
                    tile.segment.count { s ->
                        s.endsAtSide.any {
                            c.neighbourTile(it) != null && c.border[it]?.color != s.color
                        }
                    } + c.borderColors.zip(tile.tileColors)
                        .count { (borderColor, tileColor) -> borderColor != 0 && borderColor != tileColor }

                mq.cellsBlocked =
                    tile.tileColors.filterIndexed { i, color ->
                        (c.neighbourTile(i) == null && c.borderColor(i) == 0
                                && c.neighbour[i]!!.isBlockedBy(color, allowDuplicateColors))
                    }.size

                val closedLoops = tile.segment.filter { s ->
                    c.pathAtSide(s.endsAtSide[0]) != null
                            && (c.pathAtSide(s.endsAtSide[0]) == c.pathAtSide(s.endsAtSide[1]))
                }.mapNotNull { s -> c.pathAtSide(s.endsAtSide[0]) }

                val extendedPaths = tile.tileColors.zip(c.borderColors).zip(tile.tileColors.indices)
                    .filter { (p, _) -> p.first != 0 && p.first == p.second }
                    .mapNotNull { (_, i) -> c.pathAtSide(i) }.distinct()

                mq.loopsClosed = closedLoops.size
                mq.closedLoopsTotalLength = closedLoops.sumOf { it.segment.size } + mq.loopsClosed

                mq.pathsExtended = extendedPaths.size - mq.loopsClosed
                mq.extendedPathTotalLength =
                    extendedPaths.sumOf { it.segment.size } + extendedPaths.size - mq.closedLoopsTotalLength
            }
        }

    /**
     * Suggested move: rotate tile, put to cell, respective evaluated move quality
     */
    data class Move(
        /**
         * Rotation steps (for the UI tiles, not the logical tile rotations)
         */
        var rotation: Int,
        /**
         * Target cell where to put tile after rotation
         */
        val move: Cell,
        /**
         * Evaluated quality of that move
         */
        val mq: MoveQuality
    )

    /**
     * Suggest the best of available moves for the given tile, evaluating all possible rotations and valid target
     * cells. Well, actually sometimes it is not the best possible move, given the MovieQuality heuristics score
     * limitations, but usually it is a decent move.
     *
     * Returns null when there are no more possible moves at all.
     */
    fun suggestBestMove(tile: Tile): Move? {
        val t = tile.clone()
        val moves = mutableListOf<Move>()
        val firstRotateBy = if (sidesCount == 4) -1 else -2
        val thenRotateBy = if (sidesCount == 8) 2 else 1
        var rotationStepsOut = if (sidesCount == 6) -2 else -1
        var r = firstRotateBy
        repeat(if (sidesCount == 6) 6 else 4) {
            t.rotateBy(r)
            moves.addAll(evaluateMoves(t).map { Move(rotationStepsOut, it.key, it.value) })
            rotationStepsOut++
            r = thenRotateBy
        }
        return moves.maxByOrNull { it.mq }
    }

    /**
     * Recalculate the first and second free lines for the current board position
     */
    private fun setFreeLines() {
        firstFreeLine.clear()
        firstFreeLine.addAll(cell.flatten().filter { it.tile != null }
            .flatMap { it.neighbour.filter { c -> c?.tile == null }.asSequence() }.filterNotNull())
        secondFreeLine.clear()
        secondFreeLine.addAll(firstFreeLine.flatMap { it.neighbour.filter { c -> c?.tile == null } }
            .subtract(firstFreeLine).filterNotNull())
    }

    /**
     * Puts the tile to the cell, assuming valid empty target cell and matching tile orientation.
     * Then updates the paths, and detects closed loops, if any.
     */
    fun putTileToCell(
        tile: Tile,
        cell: Cell,
        updateFreeLines: Boolean = true
    ): List<Path> {
        cell.putTileToCell(tile)
        val pathsToClear = mutableListOf<Path>()
        tile.segment.forEach { s ->
            val otherSegment = s.border.map { b -> b?.neighbourSegment?.first { it != s } }
            when {
                otherSegment[0] == null && otherSegment[1] == null -> path.add(Path(s))
                // Both ends of the segment are hanging. Create new path for it
                otherSegment[0] == null && otherSegment[1] != null
                        || otherSegment[0] != null && otherSegment[1] == null ->
                    (otherSegment[0] ?: otherSegment[1])?.path?.append(s)
                // One end is hanging, another points to path. Add segment to the path
                otherSegment[0]?.path != otherSegment[1]?.path -> with(otherSegment[1]?.path) {
                    // Segment points to two different paths. Merge paths
                    otherSegment[0]!!.path!!.mergeWith(s, this!!)
                    path.remove(this)
                }
                else -> {
                    // Segment closes both ends of the same path. Remove path, clean up empty tiles, if any
                    with(otherSegment[1]?.path) {
                        this!!.append(s)
                        path.remove(this)
                        pathsToClear.add(this)
                    }
                }
            }
        }
        if (updateFreeLines)
            setFreeLines()
        return pathsToClear
    }

    /**
     * Clear closed path loops
     */
    fun clearPaths(paths: List<Path>) {
        paths.forEach { it.clear() }
        setFreeLines()
    }

    /**
     * Serialize all the playfield tiles for save game
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        cell.flatten().filter { it.tile != null }.forEach {
            sb.append(it.x).append(it.y)
            (it.tile ?: return@forEach).serialize(sb)
        }
    }

    /**
     * Deserialize the playfield tiles on game load
     */
    fun deserialize(s: String, i: Int): Int {
        if (i < 0)
            return -1
        var j = i
        val length = s.length
        while (j < length) {
            val x = s[j].digitToInt()
            val y = s[j + 1].digitToInt()
            if (x >= fieldSize || y >= fieldSize)
                return -1
            val t = generateEmptyTile()
            j = t.deserialize(s, j + 2)
            if (j < 0)
                return -1
            putTileToCell(t, cell[x][y], false)
            if (j < length && s[j] == '-') // Last move data for undo is following
                break
        }
        setFreeLines()
        return j
    }

    /**
     * Copy tiles from another playfield. Used to save/restore last move.
     */
    fun cloneFrom(other: PlayField) {
        cell.flatten().filter { it.tile != null }.forEach {
            it.tile?.cell = null
            it.tile = null
        }
        other.cell.flatten().filter { it.tile != null }.forEach { c ->
            putTileToCell(generateEmptyTile().also { t ->
                t.cloneFrom(c.tile ?: return@forEach)
            }, cell[c.x][c.y], false)
        }
        setFreeLines()
    }

}