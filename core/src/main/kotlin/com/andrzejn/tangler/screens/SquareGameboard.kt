package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Cell
import com.andrzejn.tangler.logic.Tile
import com.andrzejn.tangler.tiles.*
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.math.Vector2
import kotlin.math.floor

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

    private var cellSize = 0f
    private var boardWidth = 0f

    /**
     * Resize everything on the board grid and sprites when the screen size changes
     */
    override fun resize() {
        val squareSize = boardSquareSize
        cellSize = (squareSize - 2 * indent) / boardSize
        panStepY = cellSize
        cell.setLength(cellSize)
        resetSpriteSize(cellSize, cellSize)

        val leftX = (ctx.viewportWidth - squareSize) / 2 + indent
        val bottomY = ctx.viewportHeight + indent - squareSize -
                (ctx.viewportHeight - squareSize * (1 + minControlsHeightProportion)) / 2
        boardWidth = cellSize * boardSize
        ctrl.setCoords(
            leftX, bottomY + boardWidth, leftX + boardWidth, bottomY,
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
     * Render the board grid.
     */
    override fun renderBoadGrid() {
        val boardWidth = cellSize * (boardSize + 1)
        with(ctx.drw.sd) {
            setColor(ctx.drw.theme.screenBackground)
            var z = -cellSize
            for (i in -1..boardSize + 1) {
                line(z, -cellSize, z, boardWidth, lineWidth)
                line(-cellSize, z, boardWidth, z, lineWidth)
                z += cellSize
            }
        }
    }

    private val bottomLeft = Coord(0, 0)
    private val topRight = Coord(boardSize - 1, boardSize - 1)

    /**
     * Calculate field indexes of the cells at the board corners (to render duplicates as needed)
     */
    override fun updateCornerIndexes() {
        bottomLeft.set(boardToFieldIndices(bottomLeft.set(0, 0)))
        topRight.set(boardToFieldIndices(topRight.set(boardSize - 1, boardSize - 1)))
    }

    /**
     * Execute given draw method, duplicating it around the board corners as needed
     */
    override fun renderSpriteWithBoardCorners(t: BaseTile, draw: () -> Unit) {
        draw()
        //println("${t.x}: ${bottomLeft.x} ${topRight.x} - ${t.y}: ${bottomLeft.y} ${topRight.y}")
        val savedX = t.sprite.x
        val savedY = t.sprite.y
        var xAtBorder = false
        if (t.x == bottomLeft.x) {
            t.sprite.x += boardWidth
            xAtBorder = true
            draw()
        } else if (t.x == topRight.x) {
            xAtBorder = true
            t.sprite.x -= boardWidth
            draw()
        }
        if (xAtBorder) {
            if (t.y == bottomLeft.y) {
                t.sprite.y += boardWidth
                draw()
            } else if (t.y == topRight.y) {
                t.sprite.y -= boardWidth
                draw()
            }
        }
        t.sprite.x = savedX
        if (t.y == bottomLeft.y) {
            t.sprite.y = savedY + boardWidth
            draw()
        } else if (t.y == topRight.y) {
            t.sprite.y = savedY - boardWidth
            draw()
        }
        t.sprite.y = savedY
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
    ): Unit = this.cell.drawBorderMarker(cell, i, color, polygon, ctx)

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
     * Converts screen pointer coordinates (unprojected by the board viewport) to the screen cell indices.
     * Returns unset coord if no cell is pointed
     */
    override fun boardCoordToIndices(x: Float, y: Float): Coord =
        boardIndices.set(floor(x / cellSize).toInt(), floor(y / cellSize).toInt())

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
     * (relative to the board viewport)
     */
    override fun cellCorner(c: Coord): Vector2 =
        with(fieldToBoardIndices(c)) { cCorner.set(x * cellSize, y * cellSize) }

    /**
     * Converts logic cell coordinates to the screen coordinates array of the cell polygon
     */
    override fun cellPolygon(c: Coord): FloatArray {
        val cs = fieldToBoardIndices(c)
        return floatArrayOf(
            cs.x * cellSize, (cs.y + 1) * cellSize,
            (cs.x + 1) * cellSize, (cs.y + 1) * cellSize,
            (cs.x + 1) * cellSize, cs.y * cellSize,
            cs.x * cellSize, cs.y * cellSize
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