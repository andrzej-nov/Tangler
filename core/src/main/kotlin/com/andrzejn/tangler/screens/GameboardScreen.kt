package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.Gdx.graphics
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import ktx.app.KtxScreen
import java.util.*

/**
 * The main game screen.
 */
class GameboardScreen(ctx: Context) :
    BaseScreen(ctx), KtxScreen {

    /**
     * The main gameboard object with all the game UI logic. Depending on the game settings, it is initialized
     * either to Hex or Square gameboard
     */
    lateinit var gameboard: BaseGameboard

    /**
     * The input adapter instance for this screen
     */
    private val ia = IAdapter()

    /**
     * Initialize the gameboard according to the current game settings
     */
    private fun createNewGameboard() {
        gameboard = if (ctx.gs.sidesCount == 6)
            HexGameboard(ctx)
        else
            SquareGameboard(ctx) // Both Square and Octo tiles use the same square tile shapes, so there is
        // very little difference in the gameboard logic
    }

    private var timeStart: Long = 0

    /**
     * Invoked by the GDX library when the screen is displayed. Sets the input processing to our input adapter
     * and start measuring in-game time.
     */
    override fun show() {
        super<BaseScreen>.show()
        input.inputProcessor = ia
        timeStart = Calendar.getInstance().timeInMillis
    }

    override fun hide() {
        super<BaseScreen>.hide()
        updateInGameDuration()
    }

    /**
     * Invoked when the screen is about to close, for any reason.
     * Update the in-game time.
     */
    override fun pause() {
        updateInGameDuration()
        ctx.score.saveRecords()
        super<BaseScreen>.pause()
    }

    private fun updateInGameDuration() {
        ctx.gs.inGameDuration += Calendar.getInstance().timeInMillis - timeStart
        timeStart = Calendar.getInstance().timeInMillis
    }

    /**
     * Invoked by the GDX library on each screen resize. Triggers all the coordinate recalculations
     * and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super<BaseScreen>.resize(width, height)
        gameboard.resize()
    }

    /**
     * Invoked by the GDX library when screen needs to be updated. It is invoked frequently, especially when
     * animations are running, so avoid objects creation and complex calculations here, just do the drawing.
     */
    override fun render(delta: Float) {
        super<BaseScreen>.render(delta)

        // First, draw the tile sprites as needed. The sprites are actually valid and ready most of the time,
        // except when screen is resized or a path loop is closed and removed. So most of the time that call passes
        // fast.
        // Note: this method uses the tileBatch and the main gameboard.render() uses the main batch,
        // so we should not overlap them (only one batch may be active at a time)
        gameboard.prepareSprites()

        // Here the Tween Engine updates all our respective object fields when any tween animation is requested
        ctx.tweenManager.update(if (graphics.isContinuousRendering) delta else 0.01f)
        // Hack: enable continuous rendering only when there are tween animations in progress
        graphics.isContinuousRendering = ctx.tweenAnimationRunning()

        if (!ctx.batch.isDrawing) ctx.batch.begin()
        gameboard.render() // The main gameboard rendering
        if (ctx.batch.isDrawing) ctx.batch.end()
    }

    /**
     * Start new game
     */
    fun newGame() {
        ctx.sav.clearSavedGame()
        createNewGameboard()
        gameboard.putFirstTile()
        gameboard.createNextTile()
        ctx.score.reset()
        gameboard.resize()
    }

    /**
     * Load the saved game. Returns false if the load failed.
     */
    fun loadSavedGame(s: String): Boolean {
        createNewGameboard()
        return gameboard.deserialize(s)
    }

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {
        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.RIGHT) // Right click (on desktop) provides convenient tile rotation access
                gameboard.safeRotateNextTile(1)
            else {
                val v = ctx.drw.pointerPosition(input.x, input.y)
                if (gameboard.dispatchClick(v.x, v.y)) { // hitTest does a lot of actions based on the clicked area
                    // It returns true when the player clicks "New game" button. Here we start that new game.
                    gameboard.dispose()
                    newGame()
                }
            }
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Called when screen is untouched (mouse button released). That's either a drag end or tile drop.
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.drw.pointerPosition(input.x, input.y)
            gameboard.touchUp(v.x, v.y)
            return super.touchUp(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val v = ctx.drw.pointerPosition(input.x, input.y)
            gameboard.dragTo(v.x, v.y)
            return super.touchDragged(screenX, screenY, pointer)
        }
    }

}