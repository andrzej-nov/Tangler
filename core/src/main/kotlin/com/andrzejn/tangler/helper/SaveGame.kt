package com.andrzejn.tangler.helper

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.screens.BaseGameboard

/**
 * Handles game save/load
 */
class SaveGame(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {

    /**
     * Serialize the whole game
     */
    private fun serialize(gameboard: BaseGameboard): String {
        val sb = com.badlogic.gdx.utils.StringBuilder()
        ctx.gs.serialize(sb)
        ctx.score.serialize(sb)
        gameboard.serialize(sb)
        return sb.toString()
    }

    private fun deserializeSettingsAndScore(s: String): Boolean {
        if (!ctx.gs.deserialize(s.substring(0..4))) return false
        if (!ctx.score.deserialize(s.substring(5..14))) return false
        return true
    }

    /**
     * Save current game to Preferences
     */
    fun saveGame(gameboard: BaseGameboard) {
        ctx.gs.savedGame = serialize(gameboard)
    }

    /**
     * Deletes saved game
     */
    fun clearSavedGame() {
        ctx.gs.savedGame = ""
    }

    /**
     * Serialized save game
     */
    fun savedGame(): String = ctx.gs.savedGame

    /**
     * Deserialize and set the game settings and score from the saved game
     */
    fun loadSettingsAndScore(s: String): Boolean {
        if (s.length < 27)
            return false
        return deserializeSettingsAndScore(s)
    }
}