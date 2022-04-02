package com.andrzejn.tangler.helper

import com.andrzejn.tangler.logic.PlayField
import com.andrzejn.tangler.logic.Tile
import com.badlogic.gdx.utils.StringBuilder

/**
 * Snapshot of the board position, to save/restore last move
 */
class BoardShapshot(
    /**
     * Current game settings
     */
    val gs: GameSettings
) {
    private val playField = PlayField(gs.boardSize, gs.sidesCount, gs.colorsCount, gs.allowDuplicateColors)
    private val nextTile = Tile(gs.sidesCount, gs.colorsCount, gs.allowDuplicateColors)
    private var scoreMoves = 0
    private var scorePoints = 0

    /**
     * No board position presently stored here
     */
    var isEmpty: Boolean = true

    /**
     * Save provided position from given objects
     */
    fun takeShapshot(pf: PlayField, next: Tile, score: Score) {
        playField.cloneFrom(pf)
        nextTile.cloneFrom(next)
        scoreMoves = score.moves
        scorePoints = score.points
        isEmpty = false
    }

    /**
     * Restore saved position into given parameter objects
     */
    fun restoreSnapshot(pf: PlayField, next: Tile, score: Score) {
        pf.cloneFrom(playField)
        next.cloneFrom(nextTile)
        score.moves = scoreMoves
        score.points = scorePoints
        isEmpty = true
    }

    /**
     * Serialize this state as part of save game
     */
    fun serialize(sb: StringBuilder) {
        nextTile.serialize(sb)
        playField.serialize(sb)
    }

    /**
     * Deserialize this state as part of game load
     */
    fun deserialize(s: String, i: Int): Boolean {
        val j = playField.deserialize(s, nextTile.deserialize(s, i))
        isEmpty = false
        return j > 0
    }
}