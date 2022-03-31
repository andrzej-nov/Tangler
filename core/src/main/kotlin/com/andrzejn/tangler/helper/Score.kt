package com.andrzejn.tangler.helper

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Path
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.utils.Align

/**
 * Maintains current game score and draws it
 */
class Score(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {
    // Score counters
    private var moves: Int = 0
    private var closedLoopPoints: Int = 0

    private var recordMoves: Int = 0
    private var recordPoints: Int = 0

    // Font and text drawing objects
    private lateinit var font: BitmapFont
    private lateinit var fcMoves: BitmapFontCache
    private lateinit var fcPoints: BitmapFontCache
    private lateinit var fcFloatUp: BitmapFontCache

    private var textY: Float = 0f
    private var textMovesX: Float = 0f
    private var textPointsX: Float = 0f
    private var textWidth: Float = 0f
    private var floatTextWidth: Float = 0f
    private var fontHeight: Int = 0

    /**
     * Serialize the current score for the save game
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(moves, 5).append(closedLoopPoints, 5)
    }

    /**
     * Deserialize and set the current score from saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 10) {
            reset()
            return false
        }
        val m = s.substring(0..4).toIntOrNull()
        val p = s.substring(5..9).toIntOrNull()
        if (m == null || p == null) {
            reset()
            return false
        }
        moves = m
        closedLoopPoints = p
        // By this moment the game settings have been deserialized already, so we may rely on them
        loadRecords()
        return true
    }

    /**
     * Load record values for the current game settings
     */
    private fun loadRecords() {
        recordMoves = ctx.gs.recordMoves
        recordPoints = ctx.gs.recordPoints
    }

    /**
     * Reset score to zero
     */
    fun reset() {
        saveRecords()
        moves = 0
        closedLoopPoints = 0
        loadRecords()
        if (fontHeight > 0)
            setTexts()
    }

    /**
     * Set font size and text positions for the current window size
     */
    fun setCoords(fontHeight: Int, y: Float, xMoves: Float, xPoints: Float, width: Float) {
        if (this.fontHeight != fontHeight) {
            if (this::font.isInitialized) // Check if lateinit property has been initialized
                font.dispose()
            font = ctx.a.createFont(fontHeight)
            this.fontHeight = fontHeight
            fcPoints = BitmapFontCache(font)
            fcMoves = BitmapFontCache(font)
            fcFloatUp = BitmapFontCache(font)
        }
        textY = y
        textMovesX = xMoves
        textPointsX = xPoints
        textWidth = width
        floatTextWidth = width / 3
        setTexts()
    }

    /**
     * Calculate closed loops length and update the score
     */
    fun pointsFor(paths: List<Path>): Int =
        paths.fold(0) { acc, path -> acc + path.segment.size } * when (paths.size) {
            1 -> 1
            2 -> 3
            3 -> 5
            else -> 10
        }

    /**
     * Increment current moves counter
     */
    fun incrementMoves() {
        moves++
        setMovesText()
    }

    /**
     * Add score points, update the text object that displays floating-up and fading score numbers
     */
    fun addPoints(points: Int, x: Float, y: Float) {
        closedLoopPoints += points
        setPointsText()
        fcFloatUp.setText(points.toString(), x, y, floatTextWidth, Align.left, false)
        fcFloatUp.setColors(ctx.drw.theme.scorePoints)
    }

    /**
     * Update text objects with the current score values (then that text will be siply rendered as needed)
     */
    private fun setTexts() {
        setMovesText()
        setPointsText()
    }

    /**
     * Update text object with the current moves value (then that text will be siply rendered as needed)
     */
    private fun setMovesText() {
        fcMoves.setText(
            moves.toString() + if (moves > recordMoves) " !" else "",
            textMovesX,
            textY,
            textWidth,
            Align.right,
            false
        )
        fcMoves.setColors(ctx.drw.theme.scoreMoves)
    }

    /**
     * Update text object with the current score value (then that text will be siply rendered as needed)
     */
    private fun setPointsText() {
        fcPoints.setText(
            closedLoopPoints.toString() + if (closedLoopPoints > recordPoints) " !" else "",
            textPointsX,
            textY,
            textWidth,
            Align.left,
            false
        )
        fcPoints.setColors(ctx.drw.theme.scorePoints)
    }

    /**
     * Draw the scores in the provided batch
     */
    fun draw(batch: PolygonSpriteBatch) {
        fcMoves.draw(batch)
        fcPoints.draw(batch)
    }

    /**
     * Alpha channel for the floating-up score numbers text
     */
    var pointsAlpha: Float = 1f

    /**
     * Draw the score numbers and move it a bit up
     */
    fun drawFloatUpPoints(batch: PolygonSpriteBatch) {
        fcFloatUp.translate(0f, 1.5f)
        fcFloatUp.draw(batch, pointsAlpha)
    }

    /**
     * Clean up the font object
     */
    fun dispose() {
        if (this::font.isInitialized)
            font.dispose()
    }

    /**
     * Update current records to the settings stotage, as needed
     */
    fun saveRecords() {
        if (moves > recordMoves) {
            ctx.gs.recordMoves = moves
            recordMoves = moves
        }
        if (closedLoopPoints > recordPoints) {
            ctx.gs.recordPoints = closedLoopPoints
            recordPoints = closedLoopPoints
        }
    }
}