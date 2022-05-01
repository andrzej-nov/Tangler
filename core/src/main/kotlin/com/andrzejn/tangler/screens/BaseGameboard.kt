package com.andrzejn.tangler.screens

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import com.andrzejn.tangler.Context
import com.andrzejn.tangler.helper.BoardShapshot
import com.andrzejn.tangler.helper.TW_ALPHA
import com.andrzejn.tangler.helper.TW_ANGLE
import com.andrzejn.tangler.helper.TW_POS_XY
import com.andrzejn.tangler.logic.Cell
import com.andrzejn.tangler.logic.Path
import com.andrzejn.tangler.logic.PlayField
import com.andrzejn.tangler.logic.Tile
import com.andrzejn.tangler.tiles.BaseTile
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.min

/**
 * Grid lines width. Used in some other places to draw lines.
 */
const val lineWidth: Float = 2f

/**
 * Minimal distance from the board to the screen edges. Used in other layout calculations, too.
 */
const val indent: Float = 20f

/**
 * The main game UI logic class. Created once for each new game.
 * It is the largest class of the app because all UI logic is here, and I see no pretty way to decompose it.
 */
abstract class BaseGameboard(
    /**
     * Reference to the app Context
     */
    val ctx: Context
) {
    /**
     * The non-UI game logic
     */
    private var playField =
        PlayField(ctx.gs.boardSize, ctx.gs.sidesCount, ctx.gs.colorsCount, ctx.gs.allowDuplicateColors)

    /**
     * The UI tiles. Indexes match the playfield.cell array
     */
    protected val tile: Array<Array<BaseTile?>> = Array(ctx.gs.boardSize) { Array(ctx.gs.boardSize) { null } }

    /**
     * Convenience shortcut method to get all available tiles
     */
    private fun flatTile() = tile.flatten().filterNotNull()

    /**
     * The tile generated for next move. Displayed below the board.
     * Actually it is not a BaseTile but the specific subclass depending on current cell sides count.
     */
    private lateinit var nextTile: BaseTile

    /**
     * Convenience shortcut method to get all available tiles plus nextTile
     */
    private fun allTiles() = flatTile().plus(nextTile)

    /**
     * Keeps the last position to undo last move.
     */
    private val lastMove = BoardShapshot(ctx.gs)

    /**
     * The UI board can be scrolled. The only vaue that changes during the scroll is this X,Y offset
     * from the logic board cell coordinates to the drawn board cells.
     */
    private val scrollOffset = Coord(0, 0)

    /**
     * A copy of the nextTile sprite with the alpha channel tuned down. Used to draw possible moves
     * on the board.
     */
    private lateinit var shadowSprite: Sprite

    /**
     * The game UI controls and calculated layout coordinates.
     */
    protected val ctrl: Controls = Controls(ctx)

    /**
     * Previous clicked/pressed screen area
     */
    private var lastPressedArea: PressedArea = PressedArea.None

    /**
     * The clicked/pressed board cell, if any. Coordinates are for the logic and cell arrays (the UI scroll offset
     * is already subtracted)
     */
    private var pressedCell = Coord(-1, -1)

    /**
     * When nextTile is dragged to the board, this variable holds offset from the tile corner to the pointer position.
     * That way the tile is dragging smoothly and does not jump on drag start.
     */
    private val tileDragDelta = Vector2(0f, 0f)

    /**
     * When scrolling the board by dragging, this variable holds the cell coordinates from where the dragging started.
     * That allows calculation where should we scroll the board. Cell coordinates are for the UI screen cells,
     * not for the logical tile/cell arrays.
     */
    private var fieldScrollBase = Coord(-1, -1)

    /**
     * Used to determine if we are dragging the nextTile or scrolling the board.
     */
    private var dragStartedFrom = PressedArea.None

    // Those sprites are overlayed over the shadow sprite to inducate possible moves quality
    private val ok = Sprite(ctx.a.accept).apply { setAlpha(0.5f) }
    private val bad = Sprite(ctx.a.cancel).apply { setAlpha(0.5f) }

    /**
     * The list of closed path loops, if any, to fade out and clean up
     */
    private var pathsToClear = emptyList<Path>()

    /**
     * Resize everything on the board grid and sprites when the screen size changes
     */
    abstract fun resize()

    /**
     * Returns the tile tween rotation angle corresponding to given rotation steps (depending on the tile sides count).
     * Rotation angle values are in degrees, positive angles are counterclockwise.
     */
    protected abstract fun rotateDegrees(steps: Int): Float

    /**
     * Controls area minimum heingt, in proportion to board height
     */
    protected val minControlsHeightProportion: Float = 0.3f

    /**
     * Board square size for the current viewport size
     */
    protected val boardSquareSize: Float
        get() = min(ctx.viewportWidth, ctx.viewportHeight / (1 + minControlsHeightProportion))

    /**
     * Gets the pressed/clicked coordinates, determines which element has been clicked and invokes respective actions
     */
    fun dispatchClick(x: Float, y: Float): Boolean {
        if (ctx.tweenAnimationRunning())
            return false
        if (input.isButtonPressed(Input.Buttons.RIGHT))
            return false
        val pressed = ctrl.pressedArea(x, y)

        lastPressedArea = pressed
        when (pressed) {
            PressedArea.Help -> autoMove()
            PressedArea.Exit -> Gdx.app.exit()
            PressedArea.Play -> return true // And the Gameboard Screen will start new game
            PressedArea.Home -> ctx.game.setScreen<HomeScreen>()
            PressedArea.UndoMove -> undoLastMove()
            PressedArea.NextTile -> { // Prepare for possible tile drag
                tileDragDelta.x = nextTile.sprite.x - x
                tileDragDelta.y = nextTile.sprite.y - y
            }
            PressedArea.RotateRight -> safeRotateNextTile(1)
            PressedArea.RotateLeft -> safeRotateNextTile(-1)
            PressedArea.Board -> if (ctx.tweenAnimationRunning()) { // Animation not ended yet
                pressedCell.x = -1
                pressedCell.y = -1
            } else { // Clicked on the board, try to put the nextTile
                val c = coordToValidMoveCell(x, y)
                if (c.x < 0 || c.y < 0) { // Not a valid move cell clicked, check for the rotation zones
                    val scrollStep = scrollAreaHitTest(x, y)
                    if (scrollStep.x != 0 || scrollStep.y != 0) // Border click, need to rotate field
                        scrollField(scrollStep)
                }
            }
            else -> {} // do nothing
        }
        return false
    }

    /**
     * If we are scrolling the board by dragging, record starting point of the drag
     */
    private val dragStartedAt = Vector2(-1f, -1f)

    /**
     * Process the sustem touch-up / mouse-up events.
     * Here we either finish the dragging or drop the tile to cell.
     */
    fun touchUp(x: Float, y: Float) {
        if (dragStartedAt.x > 0 && dragStartedAt.y > 0
            && abs(x - dragStartedAt.x) > 5 && abs(y - dragStartedAt.y) > 5
        ) { // filter out erroneous drags when pointer shifts on clich by several pixels
            dragEnd(x, y)
            return
        }
        resetDragState()
        if (!ctx.tweenAnimationRunning())
            when (ctrl.pressedArea(x, y)) {
                PressedArea.Board -> {
                    val c = coordToValidMoveCell(x, y)
                    if (c.x >= 0 && c.y >= 0) { // Valid cell clicked
                        doNextTileDrop(c)
                        return
                    }
                }
                PressedArea.NextTile -> {
                    rotateNextTile(if (x < ctrl.centerX) -1 else 1)
                }
                else -> {}
            }
        if (lastPressedArea != PressedArea.Help)
            dragEnd(x, y)
    }

    /**
     * Process end-of-drag event. If the nextTile was dragged, drop it to valid target cell or return back
     * to its place below the board
     */
    private fun dragEnd(x: Float, y: Float) {
        resetDragState()
        if (dragStartedFrom == PressedArea.NextTile && ctrl.pressedArea(x, y) == PressedArea.Board)
            doNextTileDrop(coordToValidMoveCell(x, y))
        else // tile dropped to wrong area
            cancelDrag()
    }

    /**
     * Process 'dragging-in-progress' event
     */
    fun dragTo(x: Float, y: Float) {
        if (ctx.tweenAnimationRunning())
            return
        if (input.isButtonPressed(Input.Buttons.RIGHT))
            return
        if (dragStartedAt.x < 0 && dragStartedAt.y < 0) {
            dragStartedAt.x = x
            dragStartedAt.y = y
        }
        if (dragStartedFrom == PressedArea.None)
            dragStartedFrom = lastPressedArea // We not always get the press event before the drag.
        // So it's more reliable to determine what is been dragging, when we get the first dragTo event for it.
        when (dragStartedFrom) {
            PressedArea.NextTile ->
                nextTile.sprite.setPosition(x + tileDragDelta.x, y + tileDragDelta.y)
            PressedArea.Board -> {
                val c = coordToScreenCell(x, y)
                if (c.x == -1 && c.y == -1)
                    return
                if (fieldScrollBase.x < 0 && fieldScrollBase.y < 0) {
                    fieldScrollBase = c
                    return
                }
                val scrollStep = Coord(c.x - fieldScrollBase.x, (c.y - fieldScrollBase.y) / scrollYstepMultiplier)
                if (scrollStep.x != 0 || scrollStep.y != 0) {
                    fieldScrollBase.x += scrollStep.x
                    fieldScrollBase.y += scrollStep.y * scrollYstepMultiplier
                    scrollField(scrollStep)
                }
            }
            else -> {}
        }
    }

    /**
     * Dragged nextTile was dropped to wrong place. Animate its return back to its position below the board.
     */
    private fun cancelDrag() {
        with(nextTile.sprite) {
            Tween.to(nextTile.sprite, TW_POS_XY, 0.1f)
                .target(ctrl.centerX - width / 2, ctrl.circleY - height / 2)
                .start(ctx.tweenManager)
        }
        clearNextTileDrag()
    }

    /**
     * Reset the "nextTile is dragging" state
     */
    private fun clearNextTileDrag() {
        resetDragState()
        dragStartedFrom = PressedArea.None
    }

    /**
     * Sets the current "in drag" markers to "not in drag"
     */
    private fun resetDragState() {
        dragStartedAt.set(-1f, -1f)
        fieldScrollBase.x = -1
        fieldScrollBase.y = -1
        tileDragDelta.set(0f, 0f)
    }

    /**
     * When scrolling by clicking/pressing board edges, returns the respective scroll step.
     */
    private fun scrollAreaHitTest(x: Float, y: Float): Coord {
        val scrollStep = Coord(0, 0)
        if (x < ctrl.boardLeftX)
            scrollStep.x = -1
        else if (x > ctrl.boardRightX)
            scrollStep.x = 1
        if (y < ctrl.boardBottomY && y > ctrl.circleY + ctrl.tileHeight / 4
            && x > ctrl.boardLeftX + ctrl.tileWidth && x < ctrl.boardRightX - ctrl.tileWidth
        )
            scrollStep.y = -1
        else if (y > ctrl.boardTopY)
            scrollStep.y = 1
        return scrollStep
    }

    // Durations of the tween animation phases
    private val tileRotateTweenDuration = 0.3f
    private val tileDropTweenDuration = 0.2f
    private val shadowSpritesShowTweenDuration = 0.2f


    /**
     * Set by the "Hint next move" button click when there are no more moves available.
     */
    private var noMoreMoves = false
    private var suggestedMove: PlayField.Move? = null

    private fun lookForGoodMove(): PlayField.Move? {
        if (suggestedMove == null)
            suggestedMove = playField.suggestBestMove(nextTile.t)
        noMoreMoves = suggestedMove == null
        if (noMoreMoves && flatTile().isEmpty()) {
            putFirstTile()
            return lookForGoodMove()
        }
        return suggestedMove
    }

    /**
     * Evaluate the suboptimal move for the current nextCell and board position,
     * and perform the move.
     */
    private fun autoMove() {
        if (ctx.tweenAnimationRunning()) return // Previous animation not ended yet
        val suggested = lookForGoodMove() ?: return
        val targetCell = Coord(suggested.move.x, suggested.move.y)
        if (suggested.rotation == 0)
            doNextTileDrop(targetCell)
        else {
            rotateNextTile(suggested.rotation)
            with(Timeline.createSequence()) {
                pushPause(tileRotateTweenDuration + shadowSpritesShowTweenDuration)
                    .setCallback { _, _ -> doNextTileDrop(targetCell) }
                    .start(ctx.tweenManager)
            }
        }
    }

    /**
     * Rotate nextTile, with a check that there is no current animation in progress
     */
    fun safeRotateNextTile(step: Int) {
        if (ctx.tweenAnimationRunning()) return // Previous animation not ended yet
        rotateNextTile(step)
    }

    /**
     * Rotate nextTile, both the animaton and the logic values adjustments afterwards
     */
    private fun rotateNextTile(step: Int) {
        Tween.to(nextTile.sprite, TW_ANGLE, tileRotateTweenDuration).target(rotateDegrees(step))
            .setCallback { _, _ ->
                nextTile.rotateBy(step)
                with(suggestedMove) {
                    if (this != null)
                        rotation = clipWrapRotation(rotation - step)
                }
                validMovesList = null
            }.start(ctx.tweenManager)
    }

    /**
     * Ensures that the rotation is in correct range. If it is not, wraps it over and returns corrected value.
     */
    abstract fun clipWrapRotation(rotation: Int): Int

    /**
     * Used to blink the target cell when droppint nextTile to it
     */
    private var tileDropTargetCell: Coord = Coord(-1, -1)

    private var inTileDrop = false

    /**
     * Perform the move: animate nextTile movement, put it to the board and recalculate logical values,
     * generate next tile.
     */
    private fun doNextTileDrop(targetCell: Coord) {
        if (inTileDrop)
            return
        inTileDrop = true
        tileDropTargetCell = targetCell
        if (targetCell.x <= -1 || targetCell.y <= -1) {
            cancelDrag()
            inTileDrop = false
            return
        }
        clearNextTileDrag()
        val dropTo = cellCorner(targetCell)
        val sprite = nextTile.sprite
        Timeline.createSequence()
            .push(Tween.to(sprite, TW_POS_XY, tileDropTweenDuration).target(dropTo.x, dropTo.y))
            .pushPause(0.1f)
            .setCallback { _, _ ->
                if (putNextTileToBoard(targetCell)) // Closed paths loops cleanup, as needed, is done there
                    regenerateNextTile()
                clearTileDrop()
                inTileDrop = false
                prepareSprites() // Hack. For some reason the usual call from GameboardScreen.render() is not enough
                // sometimes. So let's add a safeguard and refresh the field sprites after each tile drop, too.
            }
            .start(ctx.tweenManager)
    }

    /**
     * Put nextTile to the given place, updates all values, removes closed path loops if any
     */
    private fun putNextTileToBoard(place: Coord): Boolean {
        if (place.x < 0 || place.y < 0)
            return false // (-1, -1) should not come here, but somehow sometimes does. Let's add a safeguard.
        val nTile = nextTile // Save reference in case nextTile changes in parallel thread
        ctx.sav.saveGame(this)
        lastMove.takeShapshot(playField, nTile.t, ctx.score)
        nTile.x = place.x
        nTile.y = place.y
        tile[place.x][place.y] = nTile
        nTile.isSpriteValid = false
        pathsToClear = playField.putTileToCell(nTile.t, playField.cell[place.x][place.y])
        validMovesList = null
        ctx.score.incrementMoves()
        if (pathsToClear.isNotEmpty()) { // We have some closed loops
            val points = ctx.score.pointsFor(pathsToClear)
            val f = cellCorner(place)
            ctx.score.addPoints(points, f.x + ctrl.tileWidth / 3, f.y + ctrl.tileHeight)
            invalidateSprites()
            ctx.fader.fadeDown(pathsToClear) { clearPaths() }
        }
        return true
    }

    /**
     * Create new nextTile after putting previous one to board
     */
    private fun regenerateNextTile() {
        var nTile = nextTile
        val w = nTile.sprite.width.toInt()
        val h = nTile.sprite.height.toInt()
        createNextTile()
        nTile = nextTile
        nTile.setSpriteSize(w, h)
        nTile.buildSprite()
        if (ctx.gs.hints)
            updateShadowSprite()
        with(nTile.sprite) {
            setPosition(ctrl.centerX - width / 2, ctrl.circleY - height / 2)
        }
    }

    /**
     * Clear the closed path loops
     */
    private fun clearPaths() {
        if (pathsToClear.isEmpty()) return
        playField.clearPaths(pathsToClear)
        validMovesList = null
        lookForGoodMove()
        invalidateSprites()
        ctx.sav.saveGame(this)
    }

    /**
     * Cleanup the tile drop target cell
     */
    private fun clearTileDrop() {
        tileDropTargetCell.x = -1
        tileDropTargetCell.y = -1
    }

    /**
     * Render game screen controls and UI elements. The board grid and tiles are rendered by subclasses in this method
     * overload.
     */
    open fun render() {
        ctx.drw.sd.filledRectangle(0f, 0f, ctx.viewportWidth, ctx.viewportHeight, ctx.drw.theme.screenBackground)
        ctrl.render(noMoreMoves, lastMove.isEmpty)
        ctx.score.draw(ctx.batch)
        if (ctx.fader.inFade)
            invalidateSprites()
    }

    /**
     * Render the board background, edges and the target cell highlight, if any.
     */
    protected fun renderBoardBackground(leftX: Float, bottomY: Float, rightX: Float, topY: Float) {
        val middleY = (topY + bottomY) / 2
        val middleX = (rightX + leftX) / 2
        val halfHeight = ctrl.tileHeight / 2
        val thickWidth = lineWidth * 2
        val higlightedCell = if (input.isTouched) {
            val v = ctx.drw.pointerPosition(input.x, input.y)
            coordToCell(v.x, v.y)
        } else
            tileDropTargetCell

        with(ctx.drw.sd) {
            filledRectangle(
                leftX - indent, bottomY - indent,
                rightX - leftX + 2 * indent, topY - bottomY + 2 * indent, ctx.drw.theme.gameboardBackground
            )
            if (higlightedCell.x > -1 && higlightedCell.y > -1) {
                setColor(ctx.drw.theme.polygonHighlight)
                filledPolygon(cellPolygon(higlightedCell))
            }
            setColor(ctx.drw.theme.screenBackground)
            filledTriangle(
                leftX - indent, middleY, leftX - thickWidth, middleY - halfHeight,
                leftX - thickWidth, middleY + halfHeight
            )
            filledTriangle(
                rightX + indent, middleY, rightX + thickWidth, middleY - halfHeight,
                rightX + thickWidth, middleY + halfHeight
            )
            filledTriangle(
                middleX, bottomY - indent, middleX - halfHeight, bottomY - thickWidth,
                middleX + halfHeight, bottomY - thickWidth
            )
            filledTriangle(
                middleX, topY + indent, middleX - halfHeight, topY + thickWidth,
                middleX + halfHeight, topY + thickWidth
            )
        }
    }

    /**
     * Render all the tile sprites.
     */
    protected fun renderTiles() {
        flatTile().forEach {
            it.sprite.draw(ctx.batch)
        }
        nextTile.sprite.draw(ctx.batch)
        if (ctx.gs.hints)
            validMoves.forEach {
                val v = cellCorner(it.c)
                shadowSprite.setPosition(v.x, v.y)
                shadowSprite.draw(ctx.batch)
                val hint = if (it.badMove) bad else ok
                hint.setPosition(
                    v.x + shadowSprite.width / 2 - hint.width / 2,
                    v.y + shadowSprite.height / 2 - hint.height / 2
                )
                hint.draw(ctx.batch)
            }
    }

    /**
     * Render all cell border markers.
     * This way, most of the markers are drawn twice (because each inner border is drawn by the both neighbour cells),
     * but that is simpler and more consistent than eliminate uneeded duplicate draws.
     */
    protected fun renderBorderMarkers() {
        playField.allCells().forEach { cell ->
            val polygon = cellPolygon(Coord(cell.x, cell.y))
            cell.border.forEachIndexed { i, border ->
                if (border?.color != null) drawBorderMarker(
                    cell,
                    i,
                    border.color ?: return@forEachIndexed,
                    polygon,
                    ctx.batch
                )
            }
        }
    }

    /**
     * Draw the cell border marker
     */
    protected abstract fun drawBorderMarker(
        cell: Cell,
        i: Int,
        color: Int,
        polygon: FloatArray,
        batch: PolygonSpriteBatch
    )

    /**
     * When nextTile changes or rotates, update the shadow sprite respectively and blink possible moves on board
     */
    private fun updateShadowSprite() {
        shadowSprite = Sprite(nextTile.sprite)
        Timeline.createSequence()
            .push(Tween.set(shadowSprite, TW_ALPHA).target(1f))
            .push(Tween.set(ok, TW_ALPHA).target(1f))
            .push(Tween.set(bad, TW_ALPHA).target(1f))
            .beginParallel()
            .push(Tween.to(shadowSprite, TW_ALPHA, shadowSpritesShowTweenDuration).target(0.4f))
            .push(Tween.to(ok, TW_ALPHA, shadowSpritesShowTweenDuration).target(0.4f))
            .push(Tween.to(bad, TW_ALPHA, shadowSpritesShowTweenDuration).target(0.4f))
            .end()
            .start(ctx.tweenManager)
    }

    /**
     * Invalidate all sprites to rebuild them. For example, when the screen resizes, or when closed path loops
     * need to be cleared.
     */
    private fun invalidateSprites() {
        val tilesToRemove = mutableListOf<BaseTile>()
        flatTile().forEach {
            it.isSpriteValid = false
            if (it.t.cell == null)
                tilesToRemove.add(it)
        }
        tilesToRemove.forEach {
            it.disposeFrameBuffer()
            tile[it.x][it.y] = null
        }
    }

    /**
     * Resize the sprites to new tile width/height
     */
    protected fun resetSpriteSize(tileWidth: Float, tileHeight: Float) {
        val (tWidth, tHeight) = tileWidth.toInt() to tileHeight.toInt()
        if (tWidth == ctrl.tileWidth && tHeight == ctrl.tileHeight)
            return
        ctx.drw.resizeTile(tileWidth, tileHeight, ctx.tileBatch)
        allTiles().forEach { it.setSpriteSize(tWidth, tHeight) }
        ok.setSize(tileWidth / 2, tileWidth / 2)
        bad.setSize(tileWidth / 3, tileWidth / 3)
        ctrl.tileWidth = tWidth
        ctrl.tileHeight = tHeight
    }

    /**
     * Regenerate invalidated sprites
     */
    fun prepareSprites() {
        val resetShadowSprite = !nextTile.isSpriteValid
        allTiles().filterNot { it.isSpriteValid }.forEach {
            it.buildSprite()
        }
        if (resetShadowSprite && ctx.gs.hints)
            updateShadowSprite()
    }

    /**
     * Update sprite positions on screen resize
     */
    protected fun repositionSprites() {
        prepareSprites()
        flatTile().forEach {
            val p = cellCorner(Coord(it.x, it.y))
            it.sprite.setPosition(p.x, p.y)
        }
        with(nextTile.sprite) {
            setPosition(ctrl.centerX - width / 2, ctrl.circleY - height / 2)
        }
    }

    /**
     * Returns logic coordinates (accounted for board scrolling) of the cell at x,y (screen coordinates),
     * it that is an empty cell with valid move to it. Otherwise returns -1,-1
     */
    private fun coordToValidMoveCell(x: Float, y: Float): Coord {
        var c = coordToCell(x, y)
        if (validMoves.none { (coord, _) -> coord.x == c.x && coord.y == c.y })
            c = Coord(-1, -1)
        pressedCell = c
        return c
    }

    /**
     * Adds the evaluated move quality to a possible move coordinates
     */
    private data class CoordAndSuggestion(val c: Coord, val badMove: Boolean)

    /**
     * The list of valid moves on the current position for the current nextTile in its present orientation
     */
    private var validMovesList: List<CoordAndSuggestion>? = null

    /**
     * The list of valid moves on the current position for the current nextTile in its present orientation.
     * Generated as needed, then returns cached copy of the list.
     */
    private val validMoves: List<CoordAndSuggestion>
        get() {
            if (validMovesList != null) return validMovesList!!
            val tile = nextTile.t
            validMovesList = playField.evaluateMoves(tile)
                .map { (cell, moveQuality) ->
                    CoordAndSuggestion(
                        Coord(cell.x, cell.y),
                        (moveQuality.pathsBlocked > 0 && moveQuality.loopsClosed < tile.segment.size)
                                || moveQuality.cellsBlocked > 0
                    )
                }
            return validMovesList!!
        }

    /**
     * Put the first seeded tile at the new game start
     */
    fun putFirstTile() {
        val boardCenter = ctx.gs.boardSize / 2
        tile[boardCenter][boardCenter] = newUITile(playField.putFirstTile(boardCenter))
    }

    /**
     * Create new UI Tile object for the given logic tile.
     */
    protected abstract fun newUITile(t: Tile): BaseTile

    /**
     * Create new UI Tile object for the given logic tile and assign it to nextTile
     */
    private fun assignNextTile(t: Tile) {
        validMovesList = null
        suggestedMove = null
        nextTile = newUITile(t)
    }

    /**
     * Create new tile and assign it to nextTile
     */
    fun createNextTile() {
        assignNextTile(playField.generateNextTile())
        lookForGoodMove()
    }

    /**
     * Clips the coordinate to fit the (0 until boardSize) range, wrapping from the next side as needed
     */
    private fun clipWrapCoord(c: Int): Int {
        val boardSize = ctx.gs.boardSize
        if (c < 0)
            return c + (-(c + 1) / boardSize + 1) * boardSize
        if (c >= boardSize)
            return c - (c / boardSize) * boardSize
        return c
    }

    /**
     * Applies scroll offset to logic cell coordinates to get screen cell coordinates
     */
    fun coordFieldToScreen(c: Coord): Coord =
        Coord(clipWrapCoord(c.x + scrollOffset.x), clipWrapCoord(c.y + scrollOffset.y))

    /**
     * Applies scroll offset to screen cell coordinates to get logic cell coordinates
     */
    private fun coordScreenToField(c: Coord): Coord =
        Coord(clipWrapCoord(c.x - scrollOffset.x), clipWrapCoord(c.y - scrollOffset.y))

    /**
     * Converts screen pointer coordinates to the logic cell coordinates.
     * Returns -1,-1 if no cell is pointed
     */
    private fun coordToCell(x: Float, y: Float): Coord {
        val c = coordToScreenCell(x, y)
        if (c.x == -1 && c.y == -1)
            return c
        return coordScreenToField(c)
    }

    /**
     * The board grid is scrolled not smoothly by pixels, but but whole cells (to simplify the board rendering).
     * Hexagonal cell grid has rows shifted right or left, depending on the row, so it can be scrolled vertically
     * by the step of two cells at a time to keep the drawn grid coordinates the same
     * and do not mess the neighbour cell references.
     */
    abstract val scrollYstepMultiplier: Int

    /**
     * Converts screen pointer coordinates to the screen cell coordinates.
     * Returns -1,-1 if no cell is pointed
     */
    abstract fun coordToScreenCell(x: Float, y: Float): Coord

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
     */
    abstract fun cellCorner(c: Coord): Vector2

    /**
     * Converts logic cell coordinates to the screen coordinates array of the cell polygon
     */
    abstract fun cellPolygon(c: Coord): FloatArray

    /**
     * Scroll the screen board by scrollStep
     */
    private fun scrollField(scrollStep: Coord) {
        scrollOffset.x = clipWrapCoord(scrollOffset.x + scrollStep.x)
        scrollOffset.y = clipWrapCoord(scrollOffset.y + scrollStep.y * scrollYstepMultiplier)
        repositionSprites()
    }

    /**
     * Serialize the game board and nextTile for save game
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(scrollOffset.x).append(scrollOffset.y)
        nextTile.t.serialize(sb)
        playField.serialize(sb)
        if (lastMove.isEmpty)
            return
        sb.append('-')
        lastMove.serialize(sb)
    }

    /**
     * Deserialize the game board and nextTile when loading game
     */
    fun deserialize(s: String): Boolean {
        try {
            val x = s[15].digitToIntOrNull()
            val y = s[16].digitToIntOrNull()
            if (x == null || x >= ctx.gs.boardSize || y == null || y >= ctx.gs.boardSize)
                return false
            scrollOffset.x = x
            scrollOffset.y = y
            assignNextTile(playField.generateEmptyTile())
            val i = playField.deserialize(s, nextTile.t.deserialize(s, 17))
            if (i < 0)
                return false
            if (i < s.length && s[i] == '-' && !lastMove.deserialize(s, i + 1))
                return false
        } catch (ex: Exception) {
            // Do not go into details. Any exception during deserialization means it has failed.
            return false
        }
        refreshBoadAfterRestore()
        return true
    }

    /**
     *  Undo last move
     */
    private fun undoLastMove() {
        if (lastMove.isEmpty)
            return
        tile.indices.forEach { i ->
            tile[i].indices.forEach { j ->
                tile[i][j]?.disposeFrameBuffer()
                tile[i][j] = null
            }
        }
        assignNextTile(playField.generateEmptyTile())
        lastMove.restoreSnapshot(playField, nextTile.t, ctx.score)
        refreshBoadAfterRestore()
        val width = ctrl.tileWidth.toFloat()
        val height = ctrl.tileHeight.toFloat()
        ctrl.tileWidth = 0
        ctrl.tileHeight = 0
        resetSpriteSize(width, height)
        repositionSprites()
        resetDragState()
        ctx.sav.saveGame(this)
    }

    /**
     * Update everything after deserialize or undo last move
     */
    private fun refreshBoadAfterRestore() {
        playField.allCellsWithTiles().forEach {
            tile[it.x][it.y] = newUITile(it.tile ?: return@forEach)
        }
        lookForGoodMove()
        resize()
    }

    /**
     * Clean up
     */
    fun dispose() {
        allTiles().forEach {
            it.disposeFrameBuffer()
        }
    }
}