package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.andrzejn.tangler.logic.Tile
import com.andrzejn.tangler.tiles.HexCell
import com.andrzejn.tangler.tiles.HexTile
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.math.Vector2
import space.earlygrey.shapedrawer.JoinType


/**
 * The game UI logic for hexagonal tiles gameboard
 */
class HexGameboard(ctx: Context) : BaseGameboard(ctx) {
    private val cell = HexCell()
    private val boardSize = ctx.gs.boardSize
    private val maxCount = (boardSize + 2) * 2 + 1

    // The coordinates of all the grid corner points on the board.
    // Because the cells are hexagonal, not all of the X,Y combinations are the cell corners.
    private val coordX = Array(maxCount) { 0f }
    private val coordY = Array(maxCount + 4) { 0f }

    // The grid is rendered as a set of horizontal polylines for top and bottom cell sides
    // and a set of vertical lines for the left and right cell sides
    private val horizontalBorder = Array(2) { Array(maxCount) { Vector2() } }
    private val verticalBorder = Array(maxCount) { Array(2) { Vector2() } }
    private val renderHBorder =
        Array(2) {
            com.badlogic.gdx.utils.Array<Vector2>(true, maxCount).apply { repeat(maxCount) { add(Vector2()) } }
        }
    private val renderVBorder = Array(maxCount) { Array(2) { Vector2() } }


    private var boardWidth = 0f
    private var boardHeight = 0f

    /**
     * Resize everything on the board grid and sprites when the screen size changes
     */
    override fun resize() {
        val squareSize = boardSquareSize
        val stepX = (squareSize - 2 * indent) / (boardSize * 2 + 1)
        val stepY = stepX * 23 / 40
        panStepY = stepY * 6
        cell.setLength(stepY * 2)
        resetSpriteSize(stepX * 2, stepY * 4)
        boardWidth = boardSize * 2 * stepX
        boardHeight = boardSize * 3 * stepY

        var x = -2 * stepX
        for (i in coordX.indices) {
            coordX[i] = x
            x += stepX
        }
        var y = -6 * stepY
        var dY = 0
        for (i in coordY.indices) {
            coordY[i] = y
            y += stepY * (1 + dY)
            dY = 1 - dY
        }

        dY = 1
        for (i in horizontalBorder[0].indices) {
            horizontalBorder[0][i].set(coordX[i], coordY[dY])
            horizontalBorder[1][i].set(coordX[i], coordY[3 - dY])
            dY = 1 - dY
        }

        dY = 0
        for (i in verticalBorder.indices) {
            verticalBorder[i][0].set(coordX[i], coordY[1 + dY])
            verticalBorder[i][1].set(coordX[i], coordY[2 + dY])
            dY = 2 - dY
        }

        val leftX = (ctx.viewportWidth - squareSize) / 2 + indent
        val boardYSize = stepY * (boardSize * 3 + 1)
        val bottomY = ctx.viewportHeight - boardYSize - indent -
                (ctx.viewportHeight - squareSize * (1 + minControlsHeightProportion)) / 2 -
                (squareSize - boardYSize) / 3

        ctrl.setCoords(
            leftX,
            bottomY + boardHeight + stepY,
            leftX + boardWidth + stepX,
            bottomY, boardSquareSize * minControlsHeightProportion
        )
        repositionSprites()
    }

    /**
     * Returns the tile tween rotation angle corresponding to given rotation steps.
     * Rotation angle values are in degrees, positive angles are counterclockwise.
     */
    override fun rotateDegrees(steps: Int): Float = -60f * steps

    /**
     * Render the board grid.
     */
    override fun renderBoadGrid() {
        with(ctx.drw.sd) {
            setColor(ctx.drw.theme.screenBackground)
            repeat(boardSize / 2 + 2) {
                if (it == 0) {
                    horizontalBorder.forEachIndexed { i, a -> a.forEachIndexed { j, v -> renderHBorder[i][j].set(v) } }
                    verticalBorder.forEachIndexed { i, a -> a.forEachIndexed { j, v -> renderVBorder[i][j].set(v) } }
                } else {
                    renderHBorder.forEach { a -> a.forEach { v -> v.y += panStepY } }
                    renderVBorder.forEach { a -> a.forEach { v -> v.y += panStepY } }
                }
                renderHBorder.forEach { v -> path(v, lineWidth, JoinType.POINTY, true) }
                renderVBorder.forEach { a -> line(a[0], a[1], lineWidth) }
            }
        }
    }

    /**
     * Draw the cell border marker
     */
    override fun drawBorderMarker(
        cell: Cell, i: Int, color: Int, polygon: FloatArray, batch: PolygonSpriteBatch
    ): Unit = this.cell.drawBorderMarker(cell, i, color, polygon, ctx)

    /**
     * Creates new UI Tile object for the given logic tile.
     */
    override fun newUITile(t: Tile): HexTile = HexTile(t, ctx, cell)

    /**
     * The board grid is scrolled not smoothly by pixels, but but whole cells (to simplify the board rendering).
     * Hexagonal cell grid has rows shifted right or left, depending on the row, so it can be scrolled vertically
     * by the step of two cells at a time to keep the drawn grid coordinates the same
     * and do not mess the neighbour cell references.
     */
    override val scrollYstepMultiplier: Int = 2

    /**
     * Converts screen pointer coordinates (unprojected by the board viewport) to the screen cell indices.
     * Returns unset coord if no cell is pointed
     */
    override fun boardCoordToIndices(x: Float, y: Float): Coord {
        if (x < coordX.first() || x >= coordX.last() || y < coordY.first() || y >= coordY.last()) boardIndices.unSet()
        val kx = coordX.indexOfLast { it < x }
        val ky = coordY.indexOfLast { it < y }
        if (kx < 0 || ky < 0) return boardIndices.unSet()
        var iy = (ky + 1) / 2 - 1
        var ix = if (iy % 2 == 0) kx / 2 else if (kx < 1) -1 else (kx - 1) / 2
        if (ix >= boardSize + 2) return boardIndices.unSet()

        if (ky % 2 == 0) { // rectangle with diagonal side
            if ((ky + 2 * (kx % 2)) % 4 == 0) { // left-bottom to right-top diagonal
                if (23 * (1 - (x - coordX[kx]) / 40) < (y - coordY[ky])) { // below diagonal
                    iy++
                    if (iy >= boardSize) iy = -1
                    if (iy % 2 == 0) {
                        ix++
                        if (ix >= boardSize) ix = -1
                    }
                }
            } else { // left-top to right-bottom diagonal
                if (23 * (x - coordX[kx]) / 40 < (y - coordY[ky])) { // below diagonal
                    iy++
                    if (iy >= boardSize) iy = -1
                    if (iy % 2 != 0) {
                        ix--
                        if (ix < -1) ix = -1
                    }
                }
            }
        }
        if (ix < 0 || iy < 0) boardIndices.unSet()
        return boardIndices.set(ix - 1, iy - 2)
    }

    private val arrayIndices = Coord()

    /**
     * Converts the cell X,Y coordinates to the cell bounding rectangle corner indexes in oordX,coordY arrays
     */
    private fun boardIndexesToCoordArrayIndexes(c: Coord): Coord =
        arrayIndices.set((c.x + 1) * 2 + c.y % 2, (c.y + 2) * 2 + 1)

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
     * (relative to the board viewport)
     */
    override fun cellCorner(c: Coord): Vector2 {
        val base = boardIndexesToCoordArrayIndexes(fieldToBoardIndices(c))
        return cCorner.set(coordX[base.x], coordY[base.y - 1])
    }

    /**
     * Converts logic cell coordinates to the screen coordinates array of the cell polygon
     */
    override fun cellPolygon(c: Coord): FloatArray {
        val base = boardIndexesToCoordArrayIndexes(fieldToBoardIndices(c))
        return floatArrayOf(
            coordX[base.x], coordY[base.y],
            coordX[base.x + 1], coordY[base.y - 1],
            coordX[base.x + 2], coordY[base.y],
            coordX[base.x + 2], coordY[base.y + 1],
            coordX[base.x + 1], coordY[base.y + 2],
            coordX[base.x], coordY[base.y + 1],
        )
    }

    /**
     * Ensures that the rotation is in correct range. If it is not, wraps it over and returns corrected value.
     */
    override fun clipWrapRotation(rotation: Int): Int {
        if (rotation > 3) return rotation - 6
        if (rotation < -2) return rotation + 6
        return rotation
    }

}