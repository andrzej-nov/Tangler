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
    private val maxCount = (boardSize + 1) * 2

    // The coordinates of all the grid corner points on the board.
    // Because the cells are hexagonal, not all of the X,Y combinations are the cell corners.
    private val coordX = Array(maxCount) { 0f }
    private val coordY = Array(maxCount) { 0f }

    // The grid is rendered as a set of horizontal polylines for top and bottom cell sides
    // and a set of vertical lines for the left and right cell sides
    private val horizontalBorder = Array(boardSize + 1) { i ->
        FloatArray(2 * if (i == 0 || i == boardSize) boardSize * 2 + 1 else (boardSize + 1) * 2)
    }
    private val verticalBorder = Array(boardSize * (boardSize + 1)) { FloatArray(4) }

    /**
     * Resize everything on the board grid and sprites when the screen size changes
     */
    override fun resize() {
        val squareSize = boardSquareSize
        val stepX = (squareSize - 2 * indent) / (boardSize * 2 + 1)
        val stepY = stepX * 23 / 40

        cell.setLength(stepY * 2)
        resetSpriteSize(stepX * 2, stepY * 4)

        var x = (ctx.viewportWidth - squareSize) / 2 + indent
        val boardYSize = stepY * (boardSize * 3 + 1)
        var y = ctx.viewportHeight - boardYSize - indent -
                (ctx.viewportHeight - squareSize * (1 + minControlsHeightProportion)) / 2 -
                (squareSize - boardYSize) / 3

        for (i in coordX.indices) {
            coordX[i] = x
            x += stepX
            coordY[i] = y
            y += stepY * (1 + i % 2)
        }

        var ky = 0
        horizontalBorder.forEachIndexed { i, a ->
            var kx: Int
            var dy: Int
            if (i < boardSize) {
                kx = 0
                dy = 1 - i % 2
            } else {
                kx = 1
                dy = 0
            }
            for (j in a.indices step 2) {
                a[j] = coordX[kx]
                a[j + 1] = coordY[ky + dy]
                kx++
                dy = 1 - dy
            }
            ky += 2
        }

        var kx = 0
        ky = 1
        verticalBorder.forEach { a ->
            a[0] = coordX[kx]
            a[1] = coordY[ky]
            a[2] = coordX[kx]
            a[3] = coordY[ky + 1]
            kx += 2
            if (kx >= coordX.size) {
                kx = 1 - kx % 2
                ky += 2
            }
        }

        ctrl.setCoords(
            coordX[0], coordY.last(), coordX.last(), coordY[0], boardSquareSize * minControlsHeightProportion
        )
        repositionSprites()
    }

    /**
     * Returns the tile tween rotation angle corresponding to given rotation steps.
     * Rotation angle values are in degrees, positive angles are counterclockwise.
     */
    override fun rotateDegrees(steps: Int): Float = -60f * steps

    /**
     * Render the board grid, tiles and animated score.
     */
    override fun render() {
        super.render()
        renderBoardBackground(coordX[0], coordY[0], coordX.last(), coordY.last())
        with(ctx.drw.sd) {
            setColor(ctx.drw.theme.screenBackground)
            horizontalBorder.forEach { v -> path(v, lineWidth, JoinType.POINTY, true) }
            verticalBorder.forEach { a -> line(a[0], a[1], a[2], a[3], lineWidth) }
        }
        renderTiles()
        renderBorderMarkers()
        drawGhostOrBalloon()
        if (ctx.fader.inFade) ctx.score.drawFloatUpPoints(ctx.batch)
    }

    /**
     * Draw the cell border marker
     */
    override fun drawBorderMarker(
        cell: Cell, i: Int, color: Int, polygon: FloatArray, batch: PolygonSpriteBatch
    ) {
        this.cell.drawBorderMarker(cell, i, color, polygon, ctx)
    }

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
     * Converts screen pointer coordinates to the screen cell coordinates.
     * Returns -1,-1 if no cell is pointed
     */
    override fun boardCoordToIndices(x: Float, y: Float): Coord {
        if (x < coordX.first() || x >= coordX.last() || y < coordY.first() || y >= coordY.last()) boardIndices.unSet()
        val kx = coordX.indexOfLast { it < x }
        val ky = coordY.indexOfLast { it < y }
        if (kx < 0 || ky < 0) return boardIndices.unSet()
        var iy = (ky + 1) / 2 - 1
        var ix = if (iy % 2 == 0) kx / 2 else if (kx < 1) -1 else (kx - 1) / 2
        if (ix >= boardSize) return boardIndices.unSet()

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
        return boardIndices.set(ix, iy)
    }

    private val arrayIndices = Coord()

    /**
     * Converts the cell X,Y coordinates to the cell bounding rectangle corner indexes in oordX,coordY arrays
     */
    private fun boardIndexesToCoordArrayIndexes(c: Coord): Coord = arrayIndices.set(c.x * 2 + c.y % 2, c.y * 2 + 1)

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
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