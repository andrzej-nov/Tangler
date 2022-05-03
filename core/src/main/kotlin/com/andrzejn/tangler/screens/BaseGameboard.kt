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
import com.badlogic.gdx.Gdx.graphics
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Grid lines width. Used in some other places to draw lines.
 */
const val lineWidth: Float = 2f

/**
 * Minimal distance from the board to the screen edges. Used in other layout calculations, too.
 */
const val indent: Float = 20f

/**
 * Sprite coordinate that is surely out of screen
 */
const val farOutOfScreen: Float = -1000f

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
    private var drawGhost: Boolean = false

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
    private val pressedCell = Coord()

    /**
     * When nextTile is dragged to the board, this variable holds offset from the tile corner to the pointer position.
     * That way the tile is dragging smoothly and does not jump on drag start.
     */
    private val tileDragDelta = Vector2()

    /**
     * Used to determine if we are dragging the nextTile or scrolling the board.
     */
    private var dragStartedFrom = PressedArea.None

    // Those sprites are overlayed over the shadow sprite to inducate possible moves quality
    private val ok = Sprite(ctx.a.accept).apply { setAlpha(0.5f) }
    private val bad = Sprite(ctx.a.cancel).apply { setAlpha(0.5f) }
    private val ghost = Sprite(ctx.a.ghost).apply {
        setAlpha(0.85f)
        setPosition(farOutOfScreen, farOutOfScreen)
    }
    private val balloon = Sprite(ctx.a.balloon).apply {
        setAlpha(0.85f)
        setPosition(farOutOfScreen, farOutOfScreen)
    }

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
    fun dispatchClick(screenX: Int, screenY: Int): Boolean {
        if (ctx.tweenAnimationRunning())
            return false
        if (input.isButtonPressed(Input.Buttons.RIGHT))
            return false
        val v = ctx.drw.pointerPositionScreen(screenX, screenY)
        val pressed = ctrl.pressedArea(v.x, v.y)
        lastPressedArea = pressed
        when (pressed) {
            PressedArea.Help -> autoMove()
            PressedArea.Exit -> Gdx.app.exit()
            PressedArea.Play -> return true // And the Gameboard Screen will start new game
            PressedArea.Home -> ctx.game.setScreen<HomeScreen>()
            PressedArea.UndoMove -> undoLastMove()
            PressedArea.NextTile -> // Prepare for possible tile drag
                tileDragDelta.set(nextTile.sprite.x - v.x, nextTile.sprite.y - v.y)
            PressedArea.RotateRight -> safeRotateNextTile(1)
            PressedArea.RotateLeft -> safeRotateNextTile(-1)
            PressedArea.Board -> if (ctx.tweenAnimationRunning()) // Animation not ended yet
                pressedCell.unSet()
            else { // Clicked on the board, handle rotation zones click
                if (with(ctx.drw.pointerPositionBoard(screenX, screenY)) {
                        boardCoordToValidMoveIndices(
                            x,
                            y
                        )
                    }.isNotSet()) { // Not a valid move cell clicked, check for the rotation zones
                    scrollStep.set(ctrl.scrollAreaHitTest(v.x, v.y))
                    if (scrollStep.isNotZero()) // Border click, need to rotate field
                        scrollBoard(scrollStep, false)
                }
            }
            else -> {} // do nothing
        }
        return false
    }

    /**
     * If we are scrolling the board by dragging, record starting point of the drag (screen coordinates)
     */
    private val dragStartedAt = Vector2(-1f, -1f)

    /**
     * Process the sustem touch-up / mouse-up events.
     * Here we either finish the dragging or drop the tile to cell.
     */
    fun touchUp(screenX: Int, screenY: Int) {
        val v = ctx.drw.pointerPositionScreen(screenX, screenY)
        if (dragStartedAt.x > 0 && dragStartedAt.y > 0
            && abs(v.x - dragStartedAt.x) > 5 && abs(v.y - dragStartedAt.y) > 5
        ) { // filter out erroneous drags when pointer shifts on clich by several pixels
            dragEnd(v.x, v.y, screenX, screenY)
            return
        }
        resetDragState()
        if (!ctx.tweenAnimationRunning())
            when (ctrl.pressedArea(v.x, v.y)) {
                PressedArea.Board -> {
                    val c = with(ctx.drw.pointerPositionBoard(screenX, screenY)) { boardCoordToValidMoveIndices(x, y) }
                    if (c.isSet()) { // Valid cell clicked
                        doNextTileDrop(c)
                        return
                    }
                }
                PressedArea.NextTile -> rotateNextTile(if (v.x < ctrl.centerX) -1 else 1)
                else -> {}
            }
        if (lastPressedArea != PressedArea.Help)
            dragEnd(v.x, v.y, screenX, screenY)
    }

    /**
     * Process end-of-drag event. If the nextTile was dragged, drop it to valid target cell or return back
     * to its place below the board
     */
    private fun dragEnd(xs: Float, ys: Float, screenX: Int, screenY: Int) {
        resetDragState()
        if (dragStartedFrom == PressedArea.NextTile && ctrl.pressedArea(xs, ys) == PressedArea.Board)
            doNextTileDrop(with(ctx.drw.pointerPositionBoard(screenX, screenY)) { boardCoordToValidMoveIndices(x, y) })
        else // tile dropped to wrong area
            cancelDrag()
    }

    /**
     * Which field shift by pixels should be converted to the cell indices remapping
     */
    protected var panStepY: Float = 0f
    private val scrollStep = Coord()

    /**
     * Smooth scroll field by field world coordinates
     */
    private fun panFieldBy(deltaX: Float, deltaY: Float) {
        with(ctx.drw.boardCamPos) {
            x -= deltaX
            y -= deltaY
        }
        scrollStep.set(
            ((ctx.drw.board.worldWidth / 2 - ctx.drw.boardCamPos.x) / ctrl.tileWidth).toInt(),
            ((ctx.drw.board.worldHeight / 2 - ctx.drw.boardCamPos.y) / panStepY).toInt()
        )
        if (scrollStep.isNotZero())
            scrollBoard(scrollStep, true)
    }

    /**
     * Scroll the screen board by scrollStep
     */
    private fun scrollBoard(scrollStep: Coord, moveCamera: Boolean) {
        scrollOffset.set(
            clipWrapCoord(scrollOffset.x + scrollStep.x),
            clipWrapCoord(scrollOffset.y + scrollStep.y * scrollYstepMultiplier)
        )
        if (moveCamera) {
            val deltaX = scrollStep.x.toFloat() * ctrl.tileWidth
            val deltaY = scrollStep.y.toFloat() * panStepY
            with(ctx.drw.boardCamPos) {
                x += deltaX
                y += deltaY
            }
        }
        repositionSprites()
    }

    private val dragPos = Vector2()
    private val prevDragPos = Vector2()
    private val v = Vector2()
    private var inFieldDrag = false

    /**
     * Process 'dragging-in-progress' event
     */
    fun dragTo(screenX: Int, screenY: Int) {
        if (ctx.tweenAnimationRunning())
            return
        if (input.isButtonPressed(Input.Buttons.RIGHT))
            return
        prevDragPos.set(dragPos)
        dragPos.set(ctx.drw.pointerPositionScreen(screenX, screenY))
        if (dragStartedAt.x < 0 && dragStartedAt.y < 0)
            dragStartedAt.set(dragPos.x, dragPos.y)
        if (dragStartedFrom == PressedArea.None)
            dragStartedFrom = lastPressedArea // We not always get the press event before the drag.
        // So it's more reliable to determine what is been dragging, when we get the first dragTo event for it.
        when (dragStartedFrom) {
            PressedArea.NextTile -> nextTile.sprite.setPosition(v.x + tileDragDelta.x, v.y + tileDragDelta.y)
            PressedArea.Board -> {
                val c = with(ctx.drw.pointerPositionBoard(screenX, screenY)) { boardCoordToIndices(x, y) }
                if (c.isNotSet()) {
                    inFieldDrag = false
                    return
                }
                if (!inFieldDrag) {
                    inFieldDrag = true
                    return
                }
                v.set(dragPos).sub(prevDragPos)
                panFieldBy(v.x, v.y)
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
        tileDragDelta.set(0f, 0f)
        inFieldDrag = false
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
        if (noMoreMoves) {
            if (flatTile().isEmpty()) {
                ghostAnimation(true)
                putFirstTile()
                return lookForGoodMove()
            } else ghostAnimation(false)
        }
        return suggestedMove
    }

    /**
     * Perform ghost/balloon animatio when where are no more moves left
     */
    private fun ghostAnimation(forceBalloon: Boolean) {
        drawGhost = true
        val sprite = (if (Random.nextFloat() < 0.5f) ghost else balloon).apply {
            setSize(ctrl.tileWidth * 2f, ctrl.tileWidth * 2f)
            setOriginCenter()
            setPosition((graphics.width - width) / 2f, 0f)
        }
        Tween.to(sprite, TW_POS_XY, if (forceBalloon) 1.5f else 3f)
            .target((graphics.width - sprite.width) / 2f, graphics.height.toFloat())
            .setCallback { _, _ ->
                drawGhost = false
                sprite.setPosition(farOutOfScreen, farOutOfScreen)
            }
            .start(ctx.tweenManager)
    }

    private val targetCell = Coord()

    /**
     * Evaluate the suboptimal move for the current nextCell and board position,
     * and perform the move.
     */
    private fun autoMove() {
        if (ctx.tweenAnimationRunning()) return // Previous animation not ended yet
        val suggested = lookForGoodMove() ?: return
        targetCell.set(suggested.move)
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
    private val tileDropTargetCell = Coord()

    private var inTileDrop = false

    /**
     * Perform the move: animate nextTile movement, put it to the board and recalculate logical values,
     * generate next tile.
     */
    private fun doNextTileDrop(targetCell: Coord) {
        if (inTileDrop)
            return
        inTileDrop = true
        tileDropTargetCell.set(targetCell)
        if (targetCell.isNotSet()) {
            cancelDrag()
            inTileDrop = false
            return
        }
        clearNextTileDrag()
        val dropTo = ctx.drw.board.project(cellCorner(targetCell))
        val sprite = nextTile.sprite
        val tCell = Coord(targetCell)
        Timeline.createSequence()
            .push(Tween.to(sprite, TW_POS_XY, tileDropTweenDuration).target(dropTo.x, dropTo.y))
            .pushPause(0.1f)
            .setCallback { _, _ ->
                if (putNextTileToBoard(tCell)) // Closed paths loops cleanup, as needed, is done there
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
        if (place.isNotSet())
            return false // Unset indices should not come here, but somehow sometimes do. Let's add a safeguard.
        val nTile = nextTile // Save reference in case nextTile changes in parallel thread
        ctx.sav.saveGame(this)
        lastMove.takeShapshot(playField, nTile.t, ctx.score)
        nTile.x = place.x
        nTile.y = place.y
        tile[place.x][place.y] = nTile
        val p = cellCorner(renderCoord.set(nTile))
        nTile.sprite.setPosition(p.x, p.y)
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
    private fun clearTileDrop() = tileDropTargetCell.unSet()

    /**
     * Render game screen controls and UI elements. The board grid and tiles are rendered by subclasses in this method
     * overload.
     */
    open fun render() {
        ctx.drw.drawToScreen()
        ctx.drw.sd.filledRectangle(0f, 0f, ctx.viewportWidth, ctx.viewportHeight, ctx.drw.theme.screenBackground)
        ctrl.renderBoardBackground()
        ctx.batch.flush()
        ctx.drw.drawToBoard()
        renderCellHighlight()
        ctx.batch.flush()
        ctx.drw.drawToScreen()
        ctrl.render(noMoreMoves, lastMove.isEmpty)
        renderNextTile()
        ctx.score.draw(ctx.batch)
        if (ctx.fader.inFade)
            invalidateSprites()
        ctx.batch.flush()
        ctx.drw.drawToBoard()
        renderBoadGrid()
        renderTiles()
        renderBorderMarkers()
        if (ctx.fader.inFade) ctx.score.drawFloatUpPoints(ctx.batch)
        ctx.batch.flush()
        ctx.drw.drawToScreen()
        renderGhostOrBalloon()
    }

    /**
     * Render the grid lines
     */
    abstract fun renderBoadGrid()

    /**
     * Draw the ghost/balloon sprites
     */
    private fun renderGhostOrBalloon() {
        if (drawGhost) {
            ghost.draw(ctx.batch)
            balloon.draw(ctx.batch)
        }
    }

    /**
     * Render the target cell highlight, if any
     */
    private fun renderCellHighlight() {
        val higlightedCell = if (input.isTouched) {
            val v = ctx.drw.pointerPositionBoard(input.x, input.y)
            boardCoordToFieldIndices(v.x, v.y)
        } else
            tileDropTargetCell
        if (!higlightedCell.isSet()) return
        with(ctx.drw.sd) {
            setColor(ctx.drw.theme.polygonHighlight)
            filledPolygon(cellPolygon(higlightedCell))
        }
    }

    /**
     * Render all the tile sprites.
     */
    private fun renderTiles() {
        flatTile().forEach { it.sprite.draw(ctx.batch) }
        if (!ctx.gs.hints) return
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

    private fun renderNextTile() {
        nextTile.sprite.draw(ctx.batch)
    }

    private val renderCoord = Coord()

    /**
     * Render all cell border markers.
     * This way, most of the markers are drawn twice (because each inner border is drawn by the both neighbour cells),
     * but that is simpler and more consistent than eliminate uneeded duplicate draws.
     */
    private fun renderBorderMarkers() {
        playField.allCells().forEach { cell ->
            val polygon = cellPolygon(renderCoord.set(cell))
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
        ctx.drw.setTileSize(tileWidth, tileHeight, ctx.tileBatch)
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
        allTiles().filterNot { it.isSpriteValid }.forEach { it.buildSprite() }
        if (resetShadowSprite && ctx.gs.hints)
            updateShadowSprite()
    }

    /**
     * Update sprite positions on screen resize
     */
    protected fun repositionSprites() {
        prepareSprites()
        flatTile().forEach {
            val p = cellCorner(renderCoord.set(it))
            it.sprite.setPosition(p.x, p.y)
        }
        with(nextTile.sprite) { setPosition(ctrl.centerX - width / 2, ctrl.circleY - height / 2) }
    }

    /**
     * Returns logic coordinates (accounted for board scrolling) of the cell at x,y (screen coordinates),
     * it that is an empty cell with valid move to it. Otherwise returns unset coord
     */
    private fun boardCoordToValidMoveIndices(x: Float, y: Float): Coord {
        val c = boardCoordToFieldIndices(x, y)
        if (validMoves.none { (coord, _) -> coord == c })
            c.unSet()
        return pressedCell.set(c)
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
                        Coord(cell),
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

    private val cwCoord = Coord()

    /**
     * Applies scroll offset to logic cell coordinates to get screen cell coordinates
     */
    fun fieldToBoardIndices(c: Coord): Coord =
        cwCoord.set(clipWrapCoord(c.x + scrollOffset.x), clipWrapCoord(c.y + scrollOffset.y))

    /**
     * Applies scroll offset to screen cell coordinates to get logic cell coordinates
     */
    private fun boardIndicesToFieldIndices(c: Coord): Coord =
        cwCoord.set(clipWrapCoord(c.x - scrollOffset.x), clipWrapCoord(c.y - scrollOffset.y))

    /**
     * Converts screen pointer coordinates to the logic cell coordinates.
     * Returns unset Coord if no cell is pointed
     */
    private fun boardCoordToFieldIndices(x: Float, y: Float): Coord {
        val c = boardCoordToIndices(x, y)
        if (c.isNotSet())
            return c
        return boardIndicesToFieldIndices(c)
    }

    /**
     * The board grid is scrolled not smoothly by pixels, but but whole cells (to simplify the board rendering).
     * Hexagonal cell grid has rows shifted right or left, depending on the row, so it can be scrolled vertically
     * by the step of two cells at a time to keep the drawn grid coordinates the same
     * and do not mess the neighbour cell references.
     */
    abstract val scrollYstepMultiplier: Int

    /**
     * Variable for internal calculations to reduce GC load
     */
    protected val boardIndices: Coord = Coord()

    /**
     * Converts screen pointer coordinates (unprojected by the board viewport) to the screen cell indices.
     * Returns unset coord if no cell is pointed
     */
    abstract fun boardCoordToIndices(x: Float, y: Float): Coord

    /**
     * Variable for internal calculations, to reduce GC load
     */
    protected val cCorner: Vector2 = Vector2()

    /**
     * Converts logic cell coordinates to the screen coordinates of the cell bounding rectangle corner
     * (relative to the board viewport)
     */
    abstract fun cellCorner(c: Coord): Vector2

    /**
     * Converts logic cell coordinates to the screen coordinates array of the cell polygon
     */
    abstract fun cellPolygon(c: Coord): FloatArray

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
        playField.allCellsWithTiles().forEach { tile[it.x][it.y] = newUITile(it.tile ?: return@forEach) }
        lookForGoodMove()
        resize()
    }

    /**
     * Clean up
     */
    fun dispose(): Unit = allTiles().forEach { it.disposeFrameBuffer() }
}