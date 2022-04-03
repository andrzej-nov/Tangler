package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.andrzejn.tangler.logic.Tile
import com.andrzejn.tangler.tiles.*
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.math.Vector2

/**
 * The game UI logic for square tiles gameboard. It handles both the Square and Octo cell cases, because
 * the UI logic difference is minimal between them (OctoCell is also drawn as a square, just the segments could
 * end not just on square sides, but on the corners, too)
 */
class SquareGameboard(ctx: Context) :
    BaseGameboard(ctx) {
    private val useOctoCell = ctx.gs.sidesCount == 8
    private val cell = if (useOctoCell) OctoCell() else SquareCell()
    private val boardSize = ctx.gs.boardSize

    // The coordinates of all the grid corner points on the board.
    private val coordX = Array(boardSize + 1) { 0f }
    private val coordY = Array(boardSize + 1) { 0f }

    /**
     * Resize everything on the board grid and sprites when the screen size changes
     */
    override fun resize() {
        val squareSize = boardSquareSize
        val cellSize = (squareSize - 2 * indent) / boardSize
        cell.setLength(cellSize)
        resetSpriteSize(cellSize, cellSize)

        var x = (ctx.viewportWidth - squareSize) / 2 + indent
        var y =
            ctx.viewportHeight + indent - squareSize - (ctx.viewportHeight - squareSize * (1 + minControlsHeightProportion)) / 2

        coordX.indices.forEach { i ->
            coordX[i] = x
            coordY[i] = y
            x += cellSize
            y += cellSize
        }
        ctrl.setCoords(
            coordX[0],
            coordY.last(),
            coordX.last(),
            coordY[0],
            boardSquareSize * minControlsHeightProportion
        )
        repositionSprites()
    }

    /**
     * Returns the tile tween rotation angle corresponding to given rotation steps.
     * Rotation angle values are in degrees, positive angles are counterclockwise.
     */
    override fun rotateDegrees(steps: Int): Float = -90f * steps

    /**
     * Render the board grid, tiles and animated score.
     */
    override fun render() {
        super.render()
        renderBoardBackground(coordX[0], coordY[0], coordX.last(), coordY.last())
        with(ctx.drw.sd) {
            setColor(ctx.drw.theme.screenBackground)
            for (i in coordX.indices) {
                line(coordX[i], coordY.first(), coordX[i], coordY.last(), lineWidth)
                line(coordX.first(), coordY[i], coordX.last(), coordY[i], lineWidth)
            }
        }
        renderTiles()
        renderBorderMarkers()
        if (ctx.fader.inFade)
            ctx.score.drawFloatUpPoints(ctx.batch)
    }

    /**
     * Draw the cell border marker
     */
    override fun drawBorderMarker(
        cell: Cell,
        i: Int,
        color: Int,
        polygon: FloatArray,
        batch: PolygonSpriteBatch
    ) {
        this.cell.drawBorderMarker(cell, i, color, polygon, ctx)
    }

    /**
     * Creates new UI Tile object for the given logic tile.
     */
    override fun newUITile(t: Tile): BaseTile =
        if (useOctoCell) OctoTile(t, ctx, cell as OctoCell) else SquareTile(t, ctx, cell as SquareCell)

    /**
     * The board grid is scrolled not smoothly by pixels, but but whole cells (to simplify the board rendering).
     */
    override val scrollYstepMultiplier: Int = 1

    /**
     * Converts screen pointer coordinates to the screen cell coordinates.
     * Returns -1,-1 if no cell is pointed
     */
    override fun coordToScreenCell(x: Float, y: Float): Coord {
        if (x < coordX.first() || x >= coordX.last() || y < coordY.first() || y >= coordY.last())
            return Coord(-1, -1)
        return Coord(coordX.indexOfLast { it < x }, coordY.indexOfLast { it < y })
    }

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
     */
    override fun cellCorner(c: Coord): Vector2 {
        val cs = coordFieldToScreen(c)
        return Vector2(coordX[cs.x], coordY[cs.y])
    }

    /**
     * Converts logic cell coordinates to the screen coordinates array of the cell polygon
     */
    override fun cellPolygon(c: Coord): FloatArray {
        val cs = coordFieldToScreen(c)
        return floatArrayOf(
            coordX[cs.x], coordY[cs.y + 1],
            coordX[cs.x + 1], coordY[cs.y + 1],
            coordX[cs.x + 1], coordY[cs.y],
            coordX[cs.x], coordY[cs.y]
        )
    }

    /**
     * Ensures that the rotation is in correct range. If it is not, wraps it over and returns corrected value.
     */
    override fun clipWrapRotation(rotation: Int): Int {
        if (rotation > 2)
            return rotation - 4
        if (rotation < -1)
            return rotation + 4
        return rotation
    }

}