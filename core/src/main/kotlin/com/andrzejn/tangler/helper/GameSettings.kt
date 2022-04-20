package com.andrzejn.tangler.helper

import com.badlogic.gdx.Gdx

/**
 * Game settings and saved game. Stored in the GDX system-dependent Preferences
 */
class GameSettings {
    private val sBOARDSIZE = "boardSize"
    private val sSIDESCOUNT = "sidesCount"
    private val sCOLORSCOUNT = "colorsCount"
    private val sALLOWDUPLICATECOLORS = "allowDuplicateColors"
    private val sSAVEDGAME = "savedGame"
    private val sDARKTHEME = "darkTheme"
    private val sINGAMEDURATION = "inGameDuration"
    private val sRECORDMOVES = "recordMoves"
    private val sRECORDPOINTS = "recordPoints"
    private val pref by lazy { Gdx.app.getPreferences("com.andrzejn.tangler") }
    private var iBoardSize: Int = 6
    private var iSidesCount: Int = 4
    private var iColorsCount: Int = 3
    private var iAllowDuplicateColors: Boolean = false
    private var iDarkTheme: Boolean = true
    private var iInGameDuration: Long = 0

    /**
     * Reset game settings to default values
     */
    fun reset() {
        iBoardSize = pref.getInteger(sBOARDSIZE, 6)
        if (iBoardSize !in listOf(6, 8, 10)) iBoardSize = 6
        boardSize = iBoardSize
        iSidesCount = pref.getInteger(sSIDESCOUNT, 4)
        if (iSidesCount !in listOf(4, 6, 8)) iSidesCount = 4
        sidesCount = iSidesCount
        iColorsCount = pref.getInteger(sCOLORSCOUNT, 3)
        if (iColorsCount > 6) iColorsCount = 6
        else if (iColorsCount < iSidesCount / 2) iColorsCount = iSidesCount / 2
        colorsCount = iColorsCount
        iAllowDuplicateColors = pref.getBoolean(sALLOWDUPLICATECOLORS, false)
        allowDuplicateColors = iAllowDuplicateColors
        iDarkTheme = pref.getBoolean(sDARKTHEME, true)
        iInGameDuration = pref.getLong(sINGAMEDURATION, 0)
    }

    /**
     * Board size. Board is square, allowed values are 6, 8, 10
     */
    var boardSize: Int
        get() = iBoardSize
        set(value) {
            iBoardSize = value
            pref.putInteger(sBOARDSIZE, value)
            pref.flush()
        }

    /**
     * Cell/tile sizes count. Allowed values are 4, 6, 8
     */
    var sidesCount: Int
        get() = iSidesCount
        set(value) {
            iSidesCount = value
            pref.putInteger(sSIDESCOUNT, value)
            pref.flush()
        }

    /**
     * Number of different colors used for tile segments. Allowed values are from (sidesCount / 2) to 6
     */
    var colorsCount: Int
        get() = iColorsCount
        set(value) {
            iColorsCount = value
            pref.putInteger(sCOLORSCOUNT, value)
            pref.flush()
        }

    /**
     * Can tiles have repeating colors (3 or 4 segments with the same color are still not allowed)
     */
    var allowDuplicateColors: Boolean
        get() = iAllowDuplicateColors
        set(value) {
            iAllowDuplicateColors = value
            pref.putBoolean(sALLOWDUPLICATECOLORS, value)
            pref.flush()
        }

    /**
     * Dark/Light color theme selector
     */
    var isDarkTheme: Boolean
        get() = iDarkTheme
        set(value) {
            iDarkTheme = value
            pref.putBoolean(sDARKTHEME, value)
            pref.flush()
        }

    /**
     * Total time spent in game
     */
    var inGameDuration: Long
        get() = iInGameDuration
        set(value) {
            iInGameDuration = value
            pref.putLong(sINGAMEDURATION, value)
            pref.flush()
        }

    /**
     * Serialized save game
     */
    var savedGame: String
        get() = pref.getString(sSAVEDGAME, "")
        set(value) {
            pref.putString(sSAVEDGAME, value)
            pref.flush()
        }

    /**
     * Key name for storing the records for the current tile type - game size - colors
     */
    private fun keyName(prefix: String): String {
        return "$prefix$iSidesCount$iColorsCount${if (allowDuplicateColors) 1 else 0}$iBoardSize"
    }

    /**
     * Record moves value for the current tile type - game size - colors
     */
    var recordMoves: Int
        get() = pref.getInteger(keyName(sRECORDMOVES), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDMOVES), value)
            pref.flush()
        }

    /**
     * Record moves value for the current tile type - game size - colors
     */
    var recordPoints: Int
        get() = pref.getInteger(keyName(sRECORDPOINTS), 0)
        set(value) {
            pref.putInteger(keyName(sRECORDPOINTS), value)
            pref.flush()
        }

    /**
     * Serialize game settings, to include into the saved game. Always 4 characters.
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(boardSize / 2).append(sidesCount).append(colorsCount).append(if (allowDuplicateColors) 1 else 0)
    }

    /**
     * Deserialize game settings from the saved game
     */
    fun deserialize(s: String): Boolean {
        if (s.length != 4) {
            reset()
            return false
        }
        val bs = s[0].digitToIntOrNull()
        val sc = s[1].digitToIntOrNull()
        val cc = s[2].digitToIntOrNull()
        val adc = s[3].digitToIntOrNull()
        if (bs == null || bs !in listOf(3, 4, 5)
            || sc == null || sc !in listOf(4, 6, 8)
            || cc == null || cc > 6 || cc < sc / 2
            || adc == null || adc !in listOf(0, 1)
        ) {
            reset()
            return false
        }
        boardSize = bs * 2
        sidesCount = sc
        colorsCount = cc
        allowDuplicateColors = adc == 1
        return true
    }
}
